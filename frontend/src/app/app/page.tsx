"use client";

import type { FormEvent } from "react";

import { useRouter } from "next/navigation";

import {
  useEffect,
  useState,
} from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import {
  ApiClientError,
  apiPost,
} from "@/lib/api";

interface OrganizationView {
  id: string;
  name: string;
  currency: string;
  timezone: string;
}

export default function ApplicationHomePage() {
  const router = useRouter();

  const {
    user,
    organizations,
    loading,
    refresh,
  } = useAuth();

  const [name, setName] =
    useState("");

  const [currency, setCurrency] =
    useState("EUR");

  const [timezone, setTimezone] =
    useState(
      "Europe/Paris"
    );

  const [error, setError] =
    useState("");

  const [creating, setCreating] =
    useState(false);

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [
    loading,
    user,
    router,
  ]);

  if (
    loading ||
    !user
  ) {
    return (
      <main className="appLoading">
        Loading BuildFlow...
      </main>
    );
  }

  async function createOrganization(
    event: FormEvent
  ) {
    event.preventDefault();

    setError("");
    setCreating(true);

    try {
      const organization =
        await apiPost<OrganizationView>(
          "/organizations",
          {
            name,
            currency,
            timezone,
          }
        );

      localStorage.setItem(
        "buildflow.organizationId",
        organization.id
      );

      await refresh();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Organization creation failed."
      );
    } finally {
      setCreating(false);
    }
  }

  if (
    organizations.length === 0
  ) {
    return (
      <main className="onboardingPage">
        <section className="onboardingCard">
          <span className="appLogo">
            B
          </span>

          <p className="appEyebrow">
            ORGANIZATION SETUP
          </p>

          <h1>
            Create your construction workspace.
          </h1>

          <p className="onboardingDescription">
            Your organization contains your
            projects, clients, team, suppliers and
            financial operations.
          </p>

          <form
            className="authForm"
            onSubmit={
              createOrganization
            }
          >
            <label>
              Organization name
              <input
                required
                value={name}
                onChange={(event) =>
                  setName(
                    event.target.value
                  )
                }
              />
            </label>

            <div className="fieldRow">
              <label>
                Currency
                <select
                  value={currency}
                  onChange={(event) =>
                    setCurrency(
                      event.target.value
                    )
                  }
                >
                  <option value="EUR">
                    EUR
                  </option>
                  <option value="USD">
                    USD
                  </option>
                  <option value="GBP">
                    GBP
                  </option>
                  <option value="TND">
                    TND
                  </option>
                  <option value="MAD">
                    MAD
                  </option>
                  <option value="AED">
                    AED
                  </option>
                </select>
              </label>

              <label>
                Timezone
                <input
                  required
                  value={timezone}
                  onChange={(event) =>
                    setTimezone(
                      event.target.value
                    )
                  }
                />
              </label>
            </div>

            {error && (
              <div className="formError">
                {error}
              </div>
            )}

            <button
              type="submit"
              className="authSubmit"
              disabled={creating}
            >
              {creating
                ? "Creating..."
                : "Create organization"}
            </button>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="overview" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>
              WORKSPACE
            </p>

            <h1>
              Good to see you,{" "}
              {user.firstName}.
            </h1>
          </div>

          <div className="userIdentity">
            <strong>
              {user.firstName}{" "}
              {user.lastName}
            </strong>

            <span>
              {user.email}
            </span>
          </div>
        </header>

        {!user.emailVerified && (
          <div className="verificationBanner">
            Your email has not yet been verified.
            Open Mailpit to access the local
            verification message.
          </div>
        )}

        <section className="workspaceGrid">
          {organizations.map(
            (organization) => (
              <button
                className="workspaceCard"
                type="button"
                key={organization.id}
                onClick={() => {
                  localStorage.setItem(
                    "buildflow.organizationId",
                    organization.id
                  );
                }}
              >
                <span>
                  {organization.role}
                </span>

                <h2>
                  {organization.name}
                </h2>

                <p>
                  {organization.currency}
                  {" · "}
                  {organization.timezone}
                </p>
              </button>
            )
          )}
        </section>
      </section>
    </main>
  );
}
