"use client";

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
  Supplier,
} from "@/lib/types";

export default function SuppliersPage() {
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

  const [suppliers, setSuppliers] =
    useState<Supplier[]>([]);

  const [name, setName] =
    useState("");

  const [contactName, setContactName] =
    useState("");

  const [email, setEmail] =
    useState("");

  const [phone, setPhone] =
    useState("");

  const [paymentTerms, setPaymentTerms] =
    useState("");

  const [error, setError] =
    useState("");



  async function loadSuppliers() {
    try {
      const result =
        await apiGet<Supplier[]>(
          "/suppliers",
          organizationId
        );

      setSuppliers(result);
    } catch (caught) {
      handleError(caught);
    }
  }

  async function createSupplier(
    event: FormEvent
  ) {
    event.preventDefault();

    try {
      await apiPost(
        "/suppliers",
        {
          name,
          contactName:
            contactName || null,
          email:
            email || null,
          phone:
            phone || null,
          paymentTerms:
            paymentTerms || null,
        },
        organizationId
      );

      setName("");
      setContactName("");
      setEmail("");
      setPhone("");
      setPaymentTerms("");

      await loadSuppliers();
    } catch (caught) {
      handleError(caught);
    }
  }

  function handleError(
    caught: unknown
  ) {
    setError(
      caught instanceof ApiClientError
        ? caught.message
        : "The supplier operation failed."
    );
  }

    /* eslint-disable react-hooks/exhaustive-deps */
useEffect(() => {
    // BUILDFLOW_ASYNC_LOAD_EFFECT
    const timer =
      window.setTimeout(
        () => {
        if (organizationId) {
          loadSuppliers();
        }
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
    loading
    || !user
  ) {
    return (
      <main className="appLoading">
        Loading...
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="suppliers" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>
              PROCUREMENT
            </p>

            <h1>
              Suppliers
            </h1>
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
                  Supplier directory
                </h2>

                <p>
                  {suppliers.length} suppliers
                </p>
              </div>
            </div>

            <div className="dataList">
              {suppliers.length === 0 && (
                <div className="emptyState">
                  No suppliers yet.
                </div>
              )}

              {suppliers.map(
                (supplier) => (
                  <div
                    className="dataRow"
                    key={supplier.id}
                  >
                    <div>
                      <strong>
                        {supplier.name}
                      </strong>

                      <span>
                        {supplier.contactName
                          || "No contact"}
                      </span>
                    </div>

                    <span>
                      {supplier.email || "—"}
                    </span>

                    <span>
                      {supplier.phone || "—"}
                    </span>

                    <span className="statusBadge">
                      {supplier.status}
                    </span>
                  </div>
                )
              )}
            </div>
          </div>

          <aside className="moduleFormCard">
            <h2>
              Add supplier
            </h2>

            <form
              className="authForm"
              onSubmit={createSupplier}
            >
              <label>
                Supplier name
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

              <label>
                Contact name
                <input
                  value={contactName}
                  onChange={(event) =>
                    setContactName(
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

              <label>
                Payment terms
                <input
                  value={paymentTerms}
                  onChange={(event) =>
                    setPaymentTerms(
                      event.target.value
                    )
                  }
                />
              </label>

              <button
                className="authSubmit"
                type="submit"
              >
                Create supplier
              </button>
            </form>
          </aside>
        </section>
      </section>
    </main>
  );
}
