"use client";

import styles from "./suppliers.module.css";

import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import { AppSidebar } from "@/components/AppSidebar";
import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";
import { OrganizationSelect } from "@/components/OrganizationSelect";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  Supplier,
} from "@/lib/types";


function formatSupplierDisplayValue(
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

export default function SuppliersPage() {

  const [
    creatingSupplier,
    setCreatingSupplier,
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

      <section className={`appContent ${styles.suppliersContent}`}>
        <header className="appHeader">
          <div>
            <p>
              PROCUREMENT
            </p>

            <h1>
              Suppliers
            </h1>

              <p
                className={
                  styles.pageDescription
                }
              >
                Manage supplier records and payment terms for the selected organization.
              </p>

              <a
                className={
                  styles.mobileAddSupplierLink
                }
                href="#add-supplier"
              >
                Add supplier
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
<OrganizationSelect disabled={creatingSupplier} />
            </label>
        </header>

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="moduleLayout" data-buildflow-suppliers-layout="true">
          <div className="moduleTableCard">
            <div className="moduleCardHeader">
              <div data-buildflow-supplier-directory="true" role="region" aria-label="Supplier directory">
                <h2>
                  Supplier directory
                </h2>

                <p>
                  {suppliers.length} {
                  suppliers.length === 1
                    ? "supplier"
                    : "suppliers"
                }
                </p>
              </div>
            </div>

            <div className="dataList">
              {suppliers.length === 0 && (
                <div
                className={
                  styles.emptyState
                }
                data-buildflow-supplier-empty="true"
              >
                <strong>
                  No suppliers in this organization
                </strong>

                <p>
                  Add a supplier to make it available to your procurement workflows.
                </p>

                <a
                  href="#add-supplier"
                >
                  Add supplier
                </a>
              </div>
              )}

              {suppliers.map(
                (supplier) => (
                  <div
                    className="dataRow"
                    key={supplier.id} data-buildflow-supplier-row="true">
                    <div>
                      <strong data-buildflow-supplier-cell="supplier" data-buildflow-label="Supplier">
                        {supplier.name}
                      </strong>

                      <span data-buildflow-supplier-cell="contact" data-buildflow-label="Contact">
                        {supplier.contactName
                          || "No contact"}
                      </span>
                    </div>

                    <span data-buildflow-supplier-cell="email" data-buildflow-label="Email">
                      {supplier.email || "—"}
                    </span>

                    <span data-buildflow-supplier-cell="phone" data-buildflow-label="Phone">
                      {supplier.phone || "—"}
                    </span>

                    <span className="statusBadge" data-buildflow-supplier-cell="status" data-buildflow-label="Status">
                      {formatSupplierDisplayValue(supplier.status)}
                    </span>
                  </div>
                )
              )}
            </div>
          </div>

          <aside className="moduleFormCard" data-buildflow-add-supplier-panel="true" id="add-supplier">
            <h2>
              Add supplier
            </h2>


            <div
              className={
                styles.supplierFormContext
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
              if (creatingSupplier) {
                event.preventDefault();
                return;
              }

              setCreatingSupplier(
                true,
              );

              try {
                await createSupplier(
                  event,
                );
              } finally {
                setCreatingSupplier(
                  false,
                );
              }
            }} data-buildflow-create-supplier-form="true">
              <label data-buildflow-required="true">
                <span
                  className={
                    styles.fieldLabelText
                  }
                >
                  Supplier name
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

                <span
                  className={
                    styles.paymentTermsHelper
                  }
                >
                  Enter the agreed payment terms for this supplier.
                </span>

              </label>

              <button
                className="authSubmit"
                type="submit"
               disabled={creatingSupplier} aria-busy={creatingSupplier}>
                {
                  creatingSupplier
                    ? "Creating supplier…"
                    : "Create supplier"
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
                  creatingSupplier
                    ? "Creating supplier…"
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
