"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import { useAuth } from "@/components/AuthProvider";

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

export default function TeamPage() {
  const router = useRouter();

  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const [organizationId, setOrganizationId] =
    useState("");

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

  useEffect(() => {
    if (
      organizations.length === 0
    ) {
      return;
    }

    const stored =
      localStorage.getItem(
        "buildflow.organizationId"
      );

    const valid =
      stored &&
      organizations.some(
        (organization) =>
          organization.id === stored
      );

    setOrganizationId(
      valid
        ? stored
        : organizations[0].id
    );
  }, [organizations]);

  useEffect(() => {
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
  }, [organizationId]);

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
      <aside className="appSidebar">
        <Link
          className="sidebarBrand"
          href="/app"
        >
          <span>B</span>
          BuildFlow
        </Link>

        <nav>
          <Link href="/app">
            Overview
          </Link>

          <Link
            className="active"
            href="/app/team"
          >
            Team
          </Link>
        </nav>
      </aside>

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>
              ORGANIZATION
            </p>

            <h1>
              Team management
            </h1>
          </div>

          <select
            className="organizationSelector"
            value={organizationId}
            onChange={(event) => {
              const value =
                event.target.value;

              setOrganizationId(
                value
              );

              localStorage.setItem(
                "buildflow.organizationId",
                value
              );
            }}
          >
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
        </header>

        {error && (
          <div className="formError">
            {error}
          </div>
        )}

        <section className="teamLayout">
          <div className="teamTableCard">
            <h2>Members</h2>

            <div className="teamTable">
              {members.map(
                (member) => (
                  <div
                    className="teamRow"
                    key={
                      member.membershipId
                    }
                  >
                    <div>
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
                        OWNER
                      </strong>
                    ) : (
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
                              {
                                roleOption
                              }
                            </option>
                          )
                        )}
                      </select>
                    )}

                    <span
                      className={
                        member.status ===
                        "ACTIVE"
                          ? "memberActive"
                          : "memberInactive"
                      }
                    >
                      {member.status}
                    </span>

                    {member.role !==
                      "OWNER" && (
                      <button
                        type="button"
                        className="smallButton"
                        onClick={() =>
                          toggleMembership(
                            member
                          )
                        }
                      >
                        {member.status ===
                        "ACTIVE"
                          ? "Deactivate"
                          : "Activate"}
                      </button>
                    )}
                  </div>
                )
              )}
            </div>
          </div>

          <aside className="inviteCard">
            <h2>Invite member</h2>

            <form
              className="authForm"
              onSubmit={invite}
            >
              <label>
                Email
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
                        {roleOption}
                      </option>
                    )
                  )}
                </select>
              </label>

              <button
                className="authSubmit"
                type="submit"
              >
                Send invitation
              </button>
            </form>

            <div className="pendingInvitations">
              <h3>
                Invitations
              </h3>

              {invitations.length ===
                0 && (
                <p>
                  No invitations yet.
                </p>
              )}

              {invitations.map(
                (invitation) => (
                  <div
                    key={
                      invitation.id
                    }
                  >
                    <strong>
                      {invitation.email}
                    </strong>

                    <span>
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
