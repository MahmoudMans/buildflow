"use client";

import Link from "next/link";
import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  ClientSummary,
  ClientType,
} from "@/lib/types";

export default function ClientsPage() {
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

  const [clients, setClients] =
    useState<ClientSummary[]>([]);

  const [type, setType] =
    useState<ClientType>(
      "COMPANY"
    );

  const [companyName, setCompanyName] =
    useState("");

  const [firstName, setFirstName] =
    useState("");

  const [lastName, setLastName] =
    useState("");

  const [email, setEmail] =
    useState("");

  const [phone, setPhone] =
    useState("");

  const [error, setError] =
    useState("");



  async function loadClients() {
    try {
      const result =
        await apiGet<ClientSummary[]>(
          "/clients",
          organizationId
        );

      setClients(result);
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load clients."
      );
    }
  }

  async function createClient(
    event: FormEvent
  ) {
    event.preventDefault();

    setError("");

    try {
      await apiPost(
        "/clients",
        {
          type,
          companyName:
            type === "COMPANY"
              ? companyName
              : null,
          firstName:
            type === "INDIVIDUAL"
              ? firstName
              : null,
          lastName:
            type === "INDIVIDUAL"
              ? lastName
              : null,
          email:
            email || null,
          phone:
            phone || null,
        },
        organizationId
      );

      setCompanyName("");
      setFirstName("");
      setLastName("");
      setEmail("");
      setPhone("");

      await loadClients();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Client creation failed."
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

        loadClients();
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
      <AppSidebar active="clients" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>CRM</p>
            <h1>Clients</h1>
          </div>

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
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="moduleLayout">
          <div className="moduleTableCard">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Client directory
                </h2>

                <p>
                  {clients.length} clients
                </p>
              </div>
            </div>

            <div className="dataList">
              {clients.length === 0 && (
                <div className="emptyState">
                  No clients yet.
                </div>
              )}

              {clients.map(
                (client) => (
                  <Link
                    className="dataRow"
                    href={`/app/clients/${client.id}`}
                    key={client.id}
                  >
                    <div>
                      <strong>
                        {client.displayName}
                      </strong>

                      <span>
                        {client.email ??
                          "No email"}
                      </span>
                    </div>

                    <span>
                      {client.type}
                    </span>

                    <span>
                      {client.phone ??
                        "—"}
                    </span>

                    <span
                      className="statusBadge"
                    >
                      {client.status}
                    </span>
                  </Link>
                )
              )}
            </div>
          </div>

          <aside className="moduleFormCard">
            <h2>Add client</h2>

            <form
              className="authForm"
              onSubmit={createClient}
            >
              <label>
                Client type
                <select
                  value={type}
                  onChange={(event) =>
                    setType(
                      event
                        .target
                        .value as ClientType
                    )
                  }
                >
                  <option value="COMPANY">
                    Company
                  </option>

                  <option value="INDIVIDUAL">
                    Individual
                  </option>
                </select>
              </label>

              {type === "COMPANY" ? (
                <label>
                  Company name
                  <input
                    required
                    value={companyName}
                    onChange={(event) =>
                      setCompanyName(
                        event.target.value
                      )
                    }
                  />
                </label>
              ) : (
                <div className="fieldRow">
                  <label>
                    First name
                    <input
                      required
                      value={firstName}
                      onChange={(event) =>
                        setFirstName(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label>
                    Last name
                    <input
                      required
                      value={lastName}
                      onChange={(event) =>
                        setLastName(
                          event.target.value
                        )
                      }
                    />
                  </label>
                </div>
              )}

              <label>
                Email
                <input
                  type="email"
                  value={email}
                  onChange={(event) =>
                    setEmail(
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                Phone
                <input
                  value={phone}
                  onChange={(event) =>
                    setPhone(
                      event.target.value
                    )
                  }
                />
              </label>

              <button
                className="authSubmit"
                type="submit"
              >
                Create client
              </button>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
