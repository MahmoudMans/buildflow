"use client";

import styles from "./clients.module.css";

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


function formatClientDisplayValue(
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

export default function ClientsPage() {

  const [
    creatingClient,
    setCreatingClient,
  ] = useState(false);

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

      <section className={`appContent ${styles.clientsContent}`} data-buildflow-add-client-panel="true" id="add-client" data-buildflow-clients-layout="true">
        <header className="appHeader">
          <div>

            <h1>Clients</h1>

              <p
                className={
                  styles.pageDescription
                }
              >
                Review and manage client records for the selected organization.
              </p>

              <a
                className={
                  styles.mobileAddClientLink
                }
                href="#add-client"
              >
                Add client
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
            }} disabled={creatingClient}>
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
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="moduleLayout">
          <div className="moduleTableCard" data-buildflow-client-directory="true" role="region" aria-label="Client directory">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Client directory
                </h2>

                <p>
                  {clients.length} {
                  clients.length === 1
                    ? "client"
                    : "clients"
                }
                </p>
              </div>
            </div>

            <div className="dataList">
              {clients.length === 0 && (
                <div className="emptyState">
                  No clients in this organization
                </div>
              )}


            <div
              className={
                styles.clientColumns
              }
              aria-hidden="true"
            >
              <span>
                Client
              </span>

              <span>
                Type
              </span>

              <span>
                Phone
              </span>

              <span>
                Status
              </span>
            </div>

{clients.map(
                (client) => (
                  <Link
                    className="dataRow"
                    href={`/app/clients/${client.id}`}
                    key={client.id} data-buildflow-client-row="true">
                    <div data-buildflow-client-cell="client" data-buildflow-label="Client">
                      <strong className={styles.clientNameLink}>
                        {client.displayName}
                      </strong>

                      <span>
                        {client.email ??
                          "No email"}
                      </span>
                    </div>

                    <span data-buildflow-client-cell="type" data-buildflow-label="Type">
                      {formatClientDisplayValue(client.type)}
                    </span>

                    <span data-buildflow-client-cell="phone" data-buildflow-label="Phone">
                      {client.phone ??
                        "—"}
                    </span>

                    <span
                      className="statusBadge"
                     data-buildflow-client-cell="status" data-buildflow-label="Status" data-buildflow-client-status="true">
                      {formatClientDisplayValue(client.status)}
                    </span>
                  </Link>
                )
              )}
            </div>
          </div>

          <aside className="moduleFormCard">
            <h2>Add client</h2>


            <div
              className={
                styles.clientFormContext
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
              if (creatingClient) {
                event.preventDefault();
                return;
              }

              setCreatingClient(true);

              try {
                await createClient(
                  event
                );
              } finally {
                setCreatingClient(false);
              }
            }} data-buildflow-create-client-form="true">
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
                <label data-buildflow-required="true">
                <span
                  className={
                    styles.fieldLabelText
                  }
                >
                  Company name
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
                  <label data-buildflow-required="true">
                <span
                  className={
                    styles.fieldLabelText
                  }
                >
                  First name
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
                      required
                      value={firstName}
                      onChange={(event) =>
                        setFirstName(
                          event.target.value
                        )
                      }
                    />
                  </label>

                  <label data-buildflow-required="true">
                <span
                  className={
                    styles.fieldLabelText
                  }
                >
                  Last name
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
               disabled={creatingClient} aria-busy={creatingClient}>
                {
                  creatingClient
                    ? "Creating client…"
                    : "Create client"
                }
              </button>

              <span
                className={
                  styles.creationStatus
                }
                role="status"
                aria-live="polite"
                aria-atomic="true"
              >
                {
                  creatingClient
                    ? "Creating client…"
                    : ""
                }
              </span>

            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
