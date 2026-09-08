package com.buildflow.task.service;

import com.buildflow.milestone.repository.MilestoneRepository;
import com.buildflow.organization.entity.MembershipStatus;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.repository.OrganizationMembershipRepository;
import com.buildflow.project.entity.ProjectMember;
import com.buildflow.project.repository.ProjectMemberRepository;
import com.buildflow.project.service.ProjectAccessService;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.task.dto.TaskDtos;
import com.buildflow.task.entity.Task;
import com.buildflow.task.entity.TaskAssignment;
import com.buildflow.task.entity.TaskComment;
import com.buildflow.task.entity.TaskPriority;
import com.buildflow.task.entity.TaskStatus;
import com.buildflow.task.entity.TaskWorkLog;
import com.buildflow.task.repository.TaskAssignmentRepository;
import com.buildflow.task.repository.TaskCommentRepository;
import com.buildflow.task.repository.TaskRepository;
import com.buildflow.task.repository.TaskWorkLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository
        taskRepository;

    private final TaskAssignmentRepository
        assignmentRepository;

    private final TaskCommentRepository
        commentRepository;

    private final TaskWorkLogRepository
        workLogRepository;

    private final MilestoneRepository
        milestoneRepository;

    private final OrganizationMembershipRepository
        membershipRepository;

    private final ProjectMemberRepository
        projectMemberRepository;

    private final ProjectAccessService
        projectAccessService;

    private final TaskAccessService
        taskAccessService;

    private final ProjectProgressService
        projectProgressService;

    private final Duration
        commentEditWindow;

    public TaskService(
        TaskRepository taskRepository,
        TaskAssignmentRepository
            assignmentRepository,
        TaskCommentRepository
            commentRepository,
        TaskWorkLogRepository
            workLogRepository,
        MilestoneRepository
            milestoneRepository,
        OrganizationMembershipRepository
            membershipRepository,
        ProjectMemberRepository
            projectMemberRepository,
        ProjectAccessService
            projectAccessService,
        TaskAccessService
            taskAccessService,
        ProjectProgressService
            projectProgressService,
        @Value(
            "${app.planning.comment-edit-window:15m}"
        )
        Duration commentEditWindow
    ) {
        this.taskRepository =
            taskRepository;

        this.assignmentRepository =
            assignmentRepository;

        this.commentRepository =
            commentRepository;

        this.workLogRepository =
            workLogRepository;

        this.milestoneRepository =
            milestoneRepository;

        this.membershipRepository =
            membershipRepository;

        this.projectMemberRepository =
            projectMemberRepository;

        this.projectAccessService =
            projectAccessService;

        this.taskAccessService =
            taskAccessService;

        this.projectProgressService =
            projectProgressService;

        this.commentEditWindow =
            commentEditWindow;
    }

    @Transactional(readOnly = true)
    public List<
        TaskDtos.TaskSummary
    > list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        projectAccessService
            .requireVisible(
                organizationId,
                projectId,
                userId
            );

        return taskRepository
            .findByProjectIdAndOrganizationIdOrderByCreatedAtAsc(
                projectId,
                organizationId
            )
            .stream()
            .map(
                this::toSummary
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public TaskDtos.TaskView get(
        UUID organizationId,
        UUID userId,
        UUID taskId
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        projectAccessService
            .requireVisible(
                organizationId,
                task.getProjectId(),
                userId
            );

        return toView(task);
    }

    @Transactional
    public TaskDtos.TaskView create(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        TaskDtos.CreateTaskRequest request
    ) {
        taskAccessService
            .requirePlanner(
                organizationId,
                projectId,
                userId
            );

        projectAccessService
            .requireVisible(
                organizationId,
                projectId,
                userId
            );

        if (
            request.milestoneId()
                != null
        ) {
            requireMilestone(
                organizationId,
                projectId,
                request.milestoneId()
            );
        }

        if (
            request.parentTaskId()
                != null
        ) {
            Task parent =
                taskRepository
                    .findByIdAndOrganizationIdAndProjectId(
                        request.parentTaskId(),
                        organizationId,
                        projectId
                    )
                    .orElseThrow(
                        this::parentTaskNotFound
                    );

            if (
                parent.getParentTaskId()
                    != null
            ) {
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "TASK_NESTING_LIMIT_EXCEEDED",
                    "BuildFlow supports only one level of subtasks."
                );
            }
        }

        Instant now =
            Instant.now();

        Task task =
            Task.create(
                organizationId,
                projectId,
                request.milestoneId(),
                request.parentTaskId(),
                request.title().trim(),
                clean(
                    request.description()
                ),
                request.priority() == null
                    ? TaskPriority.MEDIUM
                    : request.priority(),
                request.startDate(),
                request.dueDate(),
                request.estimatedHours(),
                request.weight() == null
                    ? 1
                    : request.weight(),
                userId,
                now
            );

        taskRepository.save(
            task
        );

        projectProgressService
            .recalculate(
                organizationId,
                projectId
            );

        return toView(task);
    }

    @Transactional
    public TaskDtos.TaskView update(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        TaskDtos.UpdateTaskRequest request
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        taskAccessService
            .requirePlanner(
                organizationId,
                task.getProjectId(),
                userId
            );

        boolean milestoneSupplied =
            request.milestoneId()
                != null
            || Boolean.TRUE.equals(
                request.clearMilestone()
            );

        UUID milestoneId =
            null;

        if (
            request.milestoneId()
                != null
        ) {
            milestoneId =
                request.milestoneId();

            requireMilestone(
                organizationId,
                task.getProjectId(),
                milestoneId
            );
        }

        task.update(
            milestoneId,
            milestoneSupplied,
            cleanPreserveNull(
                request.title()
            ),
            cleanPreserveNull(
                request.description()
            ),
            request.priority(),
            request.startDate(),
            request.dueDate(),
            request.estimatedHours(),
            request.weight(),
            Instant.now()
        );

        projectProgressService
            .recalculate(
                organizationId,
                task.getProjectId()
            );

        return toView(task);
    }

    @Transactional
    public TaskDtos.TaskView transition(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        TaskStatus target
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        taskAccessService
            .requireOperator(
                task,
                userId
            );

        task.transition(
            target,
            Instant.now()
        );

        projectProgressService
            .recalculate(
                organizationId,
                task.getProjectId()
            );

        return toView(task);
    }

    @Transactional
    public TaskDtos.TaskView updateProgress(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        TaskDtos.UpdateProgressRequest request
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        taskAccessService
            .requireOperator(
                task,
                userId
            );

        task.setProgress(
            request.progressPercentage(),
            Instant.now()
        );

        projectProgressService
            .recalculate(
                organizationId,
                task.getProjectId()
            );

        return toView(task);
    }

    @Transactional
    public TaskDtos.TaskView addAssignment(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        TaskDtos.AddAssignmentRequest request
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        taskAccessService
            .requirePlanner(
                organizationId,
                task.getProjectId(),
                userId
            );

        OrganizationMembership membership =
            membershipRepository
                .findByIdAndOrganizationId(
                    request.membershipId(),
                    organizationId
                )
                .orElseThrow(
                    this::memberNotFound
                );

        if (
            membership.getStatus()
                != MembershipStatus.ACTIVE
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "ORGANIZATION_MEMBER_INACTIVE",
                "An inactive member cannot be assigned to a task."
            );
        }

        if (
            membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "CLIENT_TASK_ASSIGNMENT_FORBIDDEN",
                "Client accounts cannot be assigned internal tasks."
            );
        }

        ProjectMember projectMember =
            projectMemberRepository
                .findByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                    task.getProjectId(),
                    organizationId,
                    membership.getId()
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.CONFLICT,
                            "PROJECT_MEMBER_REQUIRED",
                            "The member must belong to the project team before task assignment."
                        )
                );

        if (
            projectMember.getRemovedAt()
                != null
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PROJECT_MEMBER_REMOVED",
                "A removed project member cannot be assigned to a task."
            );
        }

        if (
            assignmentRepository
                .existsByTaskIdAndMembershipId(
                    taskId,
                    membership.getId()
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "TASK_ASSIGNMENT_EXISTS",
                "This member is already assigned to the task."
            );
        }

        assignmentRepository.save(
            TaskAssignment.create(
                task.getId(),
                organizationId,
                task.getProjectId(),
                membership.getId(),
                Instant.now()
            )
        );

        return toView(task);
    }

    @Transactional
    public TaskDtos.TaskView removeAssignment(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        UUID membershipId
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        taskAccessService
            .requirePlanner(
                organizationId,
                task.getProjectId(),
                userId
            );

        if (
            !assignmentRepository
                .existsByTaskIdAndMembershipId(
                    taskId,
                    membershipId
                )
        ) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "TASK_ASSIGNMENT_NOT_FOUND",
                "The task assignment does not exist."
            );
        }

        assignmentRepository
            .deleteByTaskIdAndMembershipId(
                taskId,
                membershipId
            );

        assignmentRepository.flush();

        return toView(task);
    }

    @Transactional(readOnly = true)
    public List<
        TaskDtos.CommentView
    > comments(
        UUID organizationId,
        UUID userId,
        UUID taskId
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        projectAccessService
            .requireVisible(
                organizationId,
                task.getProjectId(),
                userId
            );

        return commentRepository
            .findByTaskIdAndOrganizationIdOrderByCreatedAtAsc(
                taskId,
                organizationId
            )
            .stream()
            .map(
                this::toCommentView
            )
            .toList();
    }

    @Transactional
    public TaskDtos.CommentView addComment(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        TaskDtos.AddCommentRequest request
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        OrganizationMembership membership =
            taskAccessService
                .requireCommenter(
                    task,
                    userId
                );

        TaskComment comment =
            TaskComment.create(
                organizationId,
                task.getProjectId(),
                taskId,
                membership.getId(),
                request.content().trim(),
                Instant.now()
            );

        commentRepository.save(
            comment
        );

        return toCommentView(
            comment
        );
    }

    @Transactional
    public TaskDtos.CommentView updateComment(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        UUID commentId,
        TaskDtos.UpdateCommentRequest request
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        OrganizationMembership membership =
            taskAccessService
                .requireCommenter(
                    task,
                    userId
                );

        TaskComment comment =
            commentRepository
                .findByIdAndTaskIdAndOrganizationId(
                    commentId,
                    taskId,
                    organizationId
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "TASK_COMMENT_NOT_FOUND",
                            "The task comment does not exist."
                        )
                );

        if (
            !comment
                .getAuthorMembershipId()
                .equals(
                    membership.getId()
                )
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "COMMENT_EDIT_FORBIDDEN",
                "Only the comment author can edit this comment."
            );
        }

        Instant now =
            Instant.now();

        if (
            comment
                .getCreatedAt()
                .plus(
                    commentEditWindow
                )
                .isBefore(now)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "COMMENT_EDIT_WINDOW_EXPIRED",
                "The comment editing window has expired."
            );
        }

        comment.edit(
            request.content().trim(),
            now
        );

        return toCommentView(
            comment
        );
    }

    @Transactional(readOnly = true)
    public List<
        TaskDtos.WorkLogView
    > workLogs(
        UUID organizationId,
        UUID userId,
        UUID taskId
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        projectAccessService
            .requireVisible(
                organizationId,
                task.getProjectId(),
                userId
            );

        return workLogRepository
            .findByTaskIdAndOrganizationIdOrderByWorkDateDescCreatedAtDesc(
                taskId,
                organizationId
            )
            .stream()
            .map(
                this::toWorkLogView
            )
            .toList();
    }

    @Transactional
    public TaskDtos.WorkLogView addWorkLog(
        UUID organizationId,
        UUID userId,
        UUID taskId,
        TaskDtos.AddWorkLogRequest request
    ) {
        Task task =
            requireTask(
                organizationId,
                taskId
            );

        OrganizationMembership membership =
            taskAccessService
                .requireWorkLogger(
                    task,
                    userId
                );

        TaskWorkLog log =
            TaskWorkLog.create(
                organizationId,
                task.getProjectId(),
                taskId,
                membership.getId(),
                request.workDate(),
                request.minutesWorked(),
                clean(
                    request.description()
                ),
                Instant.now()
            );

        workLogRepository.save(
            log
        );

        return toWorkLogView(log);
    }

    private Task requireTask(
        UUID organizationId,
        UUID taskId
    ) {
        return taskRepository
            .findByIdAndOrganizationId(
                taskId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "TASK_NOT_FOUND",
                        "The task does not exist or is not accessible."
                    )
            );
    }

    private void requireMilestone(
        UUID organizationId,
        UUID projectId,
        UUID milestoneId
    ) {
        milestoneRepository
            .findByIdAndProjectIdAndOrganizationId(
                milestoneId,
                projectId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "MILESTONE_NOT_FOUND",
                        "The milestone does not exist in this project."
                    )
            );
    }

    private TaskDtos.TaskSummary toSummary(
        Task task
    ) {
        return TaskDtos.TaskSummary.from(
            task,
            actualHours(task),
            assignments(task)
        );
    }

    private TaskDtos.TaskView toView(
        Task task
    ) {
        return TaskDtos.TaskView.from(
            task,
            actualHours(task),
            assignments(task)
        );
    }

    private BigDecimal actualHours(
        Task task
    ) {
        Long minutes =
            workLogRepository
                .totalMinutesForTask(
                    task.getId(),
                    task.getOrganizationId()
                );

        if (minutes == null) {
            minutes = 0L;
        }

        return BigDecimal
            .valueOf(minutes)
            .divide(
                BigDecimal.valueOf(60),
                2,
                RoundingMode.HALF_UP
            );
    }

    private List<
        TaskDtos.AssignmentView
    > assignments(
        Task task
    ) {
        return assignmentRepository
            .findByTaskIdOrderByAssignedAtAsc(
                task.getId()
            )
            .stream()
            .map(
                assignment -> {
                    OrganizationMembership
                        membership =
                            membershipRepository
                                .findByIdAndOrganizationId(
                                    assignment
                                        .getMembershipId(),
                                    task.getOrganizationId()
                                )
                                .orElseThrow(
                                    this::memberNotFound
                                );

                    return new TaskDtos
                        .AssignmentView(
                            membership.getId(),
                            membership
                                .getUser()
                                .getId(),
                            membership
                                .getUser()
                                .getFirstName(),
                            membership
                                .getUser()
                                .getLastName(),
                            membership
                                .getUser()
                                .getEmail(),
                            assignment
                                .getAssignedAt()
                        );
                }
            )
            .toList();
    }

    private TaskDtos.CommentView
        toCommentView(
            TaskComment comment
        ) {

        OrganizationMembership membership =
            membershipRepository
                .findByIdAndOrganizationId(
                    comment
                        .getAuthorMembershipId(),
                    requireTaskOrganization(
                        comment.getTaskId()
                    )
                )
                .orElseThrow(
                    this::memberNotFound
                );

        String authorName =
            membership
                .getUser()
                .getFirstName()
                + " "
                + membership
                    .getUser()
                    .getLastName();

        return new TaskDtos.CommentView(
            comment.getId(),
            membership.getId(),
            membership
                .getUser()
                .getId(),
            authorName,
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getEditedAt(),
            comment.getEditedAt()
                != null
        );
    }

    private TaskDtos.WorkLogView
        toWorkLogView(
            TaskWorkLog log
        ) {

        Task task =
            taskRepository
                .findById(
                    log.getTaskId()
                )
                .orElseThrow();

        OrganizationMembership membership =
            membershipRepository
                .findByIdAndOrganizationId(
                    log.getMembershipId(),
                    task.getOrganizationId()
                )
                .orElseThrow(
                    this::memberNotFound
                );

        String name =
            membership
                .getUser()
                .getFirstName()
                + " "
                + membership
                    .getUser()
                    .getLastName();

        return new TaskDtos.WorkLogView(
            log.getId(),
            membership.getId(),
            membership
                .getUser()
                .getId(),
            name,
            log.getWorkDate(),
            log.getMinutesWorked(),
            log.getDescription(),
            log.getCreatedAt()
        );
    }

    private UUID requireTaskOrganization(
        UUID taskId
    ) {
        return taskRepository
            .findById(taskId)
            .orElseThrow()
            .getOrganizationId();
    }

    private BusinessException
        parentTaskNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "PARENT_TASK_NOT_FOUND",
            "The parent task does not exist."
        );
    }

    private BusinessException
        memberNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "ORGANIZATION_MEMBER_NOT_FOUND",
            "The organization member does not exist."
        );
    }

    private String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String cleanPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }
}
