"use client";

import styles from "./team.module.css";

import type { FormEvent } from "react";

import { useRouter } from "next/navigation";

import {
  useEffect,
  useState,
} from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
  apiPatch,
  apiPost,
} from "@/lib/api";

import type {
  Invitation,
  Member,
  OrganizationRole,
} from "@/lib/types";

const assignableRoles: OrganizationRole[] = [
  "ADMIN",
  "PROJECT_MANAGER",
  "SITE_MANAGER",
  "WORKER",
  "ACCOUNTANT",
  "CLIENT",
];


function formatTeamRole(
  value: string | null | undefined
) {
  if (!value) {
    return "—";
  }

  return value
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(
      /\b\w/g,
      (letter) =>
        letter.toUpperCase()
    );
}

function formatTeamStatus(
  value:
    | string
    | boolean
    | null
    | undefined
) {
  if (
    typeof value
    === "boolean"
  ) {
    return value
      ? "Active"
      : "Inactive";
  }

  if (!value) {
    return "—";
  }

  return value
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(
      /\b\w/g,
      (letter) =>
        letter.toUpperCase()
    );
}

export default function TeamPage() {

  const [
    sendingInvitation,
    setSendingInvitation,
  ] = useState(false);

  const router = useRouter();

  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const organizationSelection =
    useOrganizationSelection(
      organizations
    );

  const organizationId =
    organizationSelection.organizationId;

  const [members, setMembers] =
    useState<Member[]>([]);

  const [invitations, setInvitations] =
    useState<Invitation[]>([]);

  const [email, setEmail] =
    useState("");

  const [role, setRole] =
    useState<OrganizationRole>(
      "PROJECT_MANAGER"
    );

  const [error, setError] =
    useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [
    loading,
    user,
    router,
  ]);



  async function loadTeam() {
    const [
      memberResult,
      invitationResult,
    ] = await Promise.all([
      apiGet<Member[]>(
        `/organizations/${organizationId}/members`
      ),
      apiGet<Invitation[]>(
        `/organizations/${organizationId}/invitations`
      ),
    ]);

    setMembers(memberResult);
    setInvitations(
      invitationResult
    );
  }

  async function invite(
    event: FormEvent
  ) {
    event.preventDefault();

    setError("");

    try {
      await apiPost<Invitation>(
        `/organizations/${organizationId}/invitations`,
        {
          email,
          role,
        }
      );

      setEmail("");

      await loadTeam();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Invitation failed."
      );
    }
  }

  async function changeRole(
    membershipId: string,
    newRole: OrganizationRole
  ) {
    setError("");

    try {
      await apiPatch<Member>(
        `/organizations/${organizationId}/members/${membershipId}/role`,
        {
          role: newRole,
        }
      );

      await loadTeam();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Role update failed."
      );
    }
  }

  async function toggleMembership(
    member: Member
  ) {
    const operation =
      member.status === "ACTIVE"
        ? "deactivate"
        : "activate";

    try {
      await apiPost<Member>(
        `/organizations/${organizationId}/members/${member.membershipId}/${operation}`
      );

      await loadTeam();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Member update failed."
      );
    }
  }

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (!organizationId) {
          return;
        }

        loadTeam().catch(
          (caught) => {
            setError(
              caught instanceof ApiClientError
                ? caught.message
                : "Could not load team."
            );
          }
        );
        },
        0
      );

    return () => {
      window.clearTimeout(
        timer
      );
    };
  }, [organizationId]);
  /* eslint-enable react-hooks/exhaustive-deps */

  if (
    loading ||
    !user
  ) {
    return (
      <main className="appLoading">
        Loading...
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="team" />

      <section className={`appContent ${styles.teamContent}`}>
        <header className="appHeader">
          <div>
            <p>
              ORGANIZATION
            </p>

            <h1>
              Team management
            </h1>

              <p
                className={
                  styles.pageDescription
                }
              >
                Manage organization members, roles, and invitations.
              </p>

              <a
                className={
                  styles.mobileInviteLink
                }
                href="#invite-member"
              >
                Invite member
              </a>

          </div>

          <label
              className={
                styles.organizationControl
              }
            >
              <span
                className={
                  styles.organizationLabel
                }
              >
                Organization
              </span>
<select
            className="organizationSelector"
            value={organizationId}
            onChange={(event) => {
              const value =
                event.target.value;

              organizationSelection.selectOrganization(
                value
              );

              localStorage.setItem(
                "buildflow.organizationId",
                value
              );
            }}
           disabled={sendingInvitation}>
            {organizations.map(
              (organization) => (
                <option
                  key={organization.id}
                  value={organization.id}
                >
                  {organization.name}
                </option>
              )
            )}
          </select>
            </label>
        </header>

        {error && (
          <div className="formError">
            {error}
          </div>
        )}

        <section className="teamLayout" data-buildflow-team-layout="true">
          <div className="teamTableCard" data-buildflow-members-panel="true" role="region" aria-label="Organization members">
            <h2>Members</h2>

            <p
              className={
                styles.memberCount
              }
            >
              {members.length} {
                members.length === 1
                  ? "member"
                  : "members"
              }
            </p>


            <div className="teamTable">

            <div
              className={
                styles.memberColumns
              }
              aria-hidden="true"
            >
              <span>
                Member
              </span>

              <span>
                Role
              </span>

              <span>
                Status
              </span>

              <span>
                Action
              </span>
            </div>

{members.map(
                (member) => (
                  <div
                    className="teamRow"
                    key={
                      member.membershipId
                    } data-buildflow-member-row="true">
                    <div data-buildflow-member-cell="member" data-buildflow-label="Member">
                      <strong>
                        {member.firstName}
                        {" "}
                        {member.lastName}
                      </strong>

                      <span>
                        {member.email}
                      </span>
                    </div>

                    {member.role ===
                    "OWNER" ? (
                      <strong>
Owner
</strong>
                    ) : (
                      <div
                  data-buildflow-member-cell="role"
                  data-buildflow-label="Role"
                >
<select
                        value={
                          member.role
                        }
                        onChange={(
                          event
                        ) =>
                          changeRole(
                            member.membershipId,
                            event
                              .target
                              .value as OrganizationRole
                          )
                        }
                      >
                        {assignableRoles.map(
                          (
                            roleOption
                          ) => (
                            <option
                              key={
                                roleOption
                              }
                              value={
                                roleOption
                              }
                            >
                  {formatTeamRole(roleOption)}
                </option>
                          )
                        )}
                      </select>
                </div>
                    )}

                    <span
                      className={
                        member.status ===
                        "ACTIVE"
                          ? "memberActive"
                          : "memberInactive"
                      }
                     data-buildflow-member-cell="status" data-buildflow-label="Status" data-buildflow-member-status="true">
                      {formatTeamStatus(member.status)}
                    </span>

                    {member.role !==
                      "OWNER" && (
                      <div
                  data-buildflow-member-cell="action"
                  data-buildflow-label="Action"
                >
<button
                        type="button"
                        className="smallButton"
                        onClick={() =>
                          toggleMembership(
                            member
                          )
                        } data-buildflow-deactivate-button="true">
                        {member.status ===
                        "ACTIVE"
                          ? "Deactivate"
                          : "Activate"}
                      </button>
                </div>
                    )}
                  </div>
                )
              )}
            </div>
          </div>

          <aside className="inviteCard" data-buildflow-invite-panel="true" id="invite-member">
            <h2>Invite member</h2>


            <div
              className={
                styles.inviteContext
              }
            >
              <p>
                Organization:{" "}
                <strong>
                  {
                    organizations.find(
                      (organization) =>
                        organization.id
                        === organizationId
                    )?.name
                    ?? "Selected organization"
                  }
                </strong>
              </p>

              <p
                className={
                  styles.requiredInstruction
                }
              >
                Fields marked{" "}
                <span
                  className={
                    styles.requiredMark
                  }
                  aria-hidden="true"
                >
                  *
                </span>
                {" "}are required.
              </p>

            </div>

<form
              className="authForm"
              onSubmit={async (event) => {
              if (sendingInvitation) {
                event.preventDefault();
                return;
              }

              setSendingInvitation(
                true,
              );

              try {
                await invite(
                  event,
                );
              } finally {
                setSendingInvitation(
                  false,
                );
              }
            }} data-buildflow-invite-form="true">
              <label data-buildflow-required="true">
                <span
                  className={
                    styles.fieldLabelText
                  }
                >
                  Email
                  <span
                    className={
                      styles.fieldRequiredMark
                    }
                    aria-hidden="true"
                  >
                    *
                  </span>
                </span>
<input
                  type="email"
                  required
                  value={email}
                  onChange={(
                    event
                  ) =>
                    setEmail(
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                Role
                <select
                  value={role}
                  onChange={(
                    event
                  ) =>
                    setRole(
                      event
                        .target
                        .value as OrganizationRole
                    )
                  }
                >
                  {assignableRoles.map(
                    (
                      roleOption
                    ) => (
                      <option
                        key={
                          roleOption
                        }
                        value={
                          roleOption
                        }
                      >
                  {formatTeamRole(roleOption)}
                </option>
                    )
                  )}
                </select>
              </label>

              <button
                className="authSubmit"
                type="submit"
               disabled={sendingInvitation} aria-busy={sendingInvitation}>
                {
                  sendingInvitation
                    ? "Sending invitation…"
                    : "Send invitation"
                }
              </button>

              <span
                className={
                  styles.invitationStatus
                }
                role="status"
                aria-live="polite"
                aria-atomic="true"
              >
                {
                  sendingInvitation
                    ? "Sending invitation…"
                    : ""
                }
              </span>

            </form>

            <div className="pendingInvitations">
              <h3>
                Invitations
              </h3>

              {invitations.length ===
                0 && (
                <div
                className={
                  styles.invitationEmpty
                }
                data-buildflow-invitation-empty="true"
              >
                <strong>
                  No invitations for this organization
                </strong>

                <p>
                  Invitations you send will appear here.
                </p>
              </div>
              )}

              {invitations.map(
                (invitation) => (
                  <div
                    key={
                      invitation.id
                    } data-buildflow-invitation-row="true">
                    <strong data-buildflow-invitation-cell="invitee" data-buildflow-label="Invitee">
                      {invitation.email}
                    </strong>

                    <span data-buildflow-invitation-cell="details" data-buildflow-label="Role / Status">
                      {
                        invitation.role
                      }
                      {" · "}
                      {
                        invitation.status
                      }
                    </span>
                  </div>
                )
              )}
            </div>
          </aside>
        </section>
      </section>
    </main>
  );
}
