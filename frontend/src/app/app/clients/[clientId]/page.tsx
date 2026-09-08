"use client";

import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import Link from "next/link";
import { useParams } from "next/navigation";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  ClientView,
} from "@/lib/types";

export default function ClientDetailPage() {
  const params = useParams();

  const clientId =
    String(params.clientId);

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

  const [client, setClient] =
    useState<ClientView | null>(
      null
    );

  const [firstName, setFirstName] =
    useState("");

  const [lastName, setLastName] =
    useState("");

  const [jobTitle, setJobTitle] =
    useState("");

  const [email, setEmail] =
    useState("");

  const [phone, setPhone] =
    useState("");

  const [error, setError] =
    useState("");



  async function loadClient() {
    try {
      const result =
        await apiGet<ClientView>(
          `/clients/${clientId}`,
          organizationId
        );

      setClient(result);
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not load client."
      );
    }
  }

  async function addContact(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost(
        `/clients/${clientId}/contacts`,
        {
          firstName,
          lastName,
          jobTitle:
            jobTitle || null,
          email:
            email || null,
          phone:
            phone || null,
          primary:
            client?.contacts.length === 0,
        },
        organizationId
      );

      setFirstName("");
      setLastName("");
      setJobTitle("");
      setEmail("");
      setPhone("");

      await loadClient();
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Could not add contact."
      );
    }
  }

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (
          organizationId &&
          clientId
        ) {
          loadClient();
        }
        },
        0
      );

    return () => {
      window.clearTimeout(
        timer
      );
    };
  }, [
    organizationId,
    clientId,
  ]);
  /* eslint-enable react-hooks/exhaustive-deps */

  if (
    loading ||
    !user ||
    !client
  ) {
    return (
      <main className="appLoading">
        {error || "Loading..."}
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="clients" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>
              CLIENT
            </p>

            <h1>
              {client.displayName}
            </h1>
          </div>

          <Link
            className="smallButton"
            href="/app/clients"
          >
            Back to clients
          </Link>
        </header>

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="detailCards">
          <article>
            <span>Type</span>
            <strong>
              {client.type}
            </strong>
          </article>

          <article>
            <span>Status</span>
            <strong>
              {client.status}
            </strong>
          </article>

          <article>
            <span>Email</span>
            <strong>
              {client.email ?? "—"}
            </strong>
          </article>

          <article>
            <span>Phone</span>
            <strong>
              {client.phone ?? "—"}
            </strong>
          </article>
        </section>

        <section className="moduleLayout">
          <div className="moduleTableCard">
            <div className="moduleCardHeader">
              <div>
                <h2>
                  Contacts
                </h2>

                <p>
                  People associated with this client.
                </p>
              </div>
            </div>

            <div className="dataList">
              {client.contacts.length ===
                0 && (
                <div className="emptyState">
                  No contacts yet.
                </div>
              )}

              {client.contacts.map(
                (contact) => (
                  <div
                    className="dataRow"
                    key={contact.id}
                  >
                    <div>
                      <strong>
                        {contact.firstName}
                        {" "}
                        {contact.lastName}
                      </strong>

                      <span>
                        {contact.jobTitle ??
                          "Contact"}
                      </span>
                    </div>

                    <span>
                      {contact.email ??
                        "—"}
                    </span>

                    <span>
                      {contact.phone ??
                        "—"}
                    </span>

                    <span>
                      {contact.primary
                        ? "PRIMARY"
                        : ""}
                    </span>
                  </div>
                )
              )}
            </div>
          </div>

          <aside className="moduleFormCard">
            <h2>Add contact</h2>

            <form
              className="authForm"
              onSubmit={addContact}
            >
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

              <label>
                Job title
                <input
                  value={jobTitle}
                  onChange={(event) =>
                    setJobTitle(
                      event.target.value
                    )
                  }
                />
              </label>

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
                Add contact
              </button>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
