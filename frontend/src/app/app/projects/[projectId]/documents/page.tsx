"use client";

import {
  type FormEvent,
  useCallback,
  useEffect,
  useState,
} from "react";

import {
  useParams,
} from "next/navigation";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  ProjectWorkspaceNav,
} from "@/components/ProjectWorkspaceNav";

import {
  useAuth,
} from "@/components/AuthProvider";

import {
  useOrganizationSelection,
} from "@/components/useOrganizationSelection";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  ProjectView,
} from "@/lib/types";

import type {
  DocumentCategory,
  ProjectDocument,
} from "@/lib/document-types";

async function fileToBase64(
  file: File
): Promise<string> {
  return new Promise(
    (
      resolve,
      reject
    ) => {
      const reader =
        new FileReader();

      reader.onload = () => {
        if (
          typeof reader.result
            !== "string"
        ) {
          reject(
            new Error(
              "Unable to read file."
            )
          );

          return;
        }

        resolve(reader.result);
      };

      reader.onerror = () => {
        reject(
          reader.error
          ?? new Error(
            "Unable to read file."
          )
        );
      };

      reader.readAsDataURL(file);
    }
  );
}

export default function DocumentsPage() {
  const params = useParams();

  const projectId =
    String(params.projectId);

  const {
    user,
    organizations,
    loading,
  } = useAuth();

  const selection =
    useOrganizationSelection(
      organizations
    );

  const organizationId =
    selection.organizationId;

  const [project, setProject] =
    useState<ProjectView | null>(
      null
    );

  const [documents, setDocuments] =
    useState<ProjectDocument[]>(
      []
    );

  const [error, setError] =
    useState("");

  const [file, setFile] =
    useState<File | null>(
      null
    );

  const [description, setDescription] =
    useState("");

  const [category, setCategory] =
    useState<DocumentCategory>(
      "OTHER"
    );

  const [
    clientVisible,
    setClientVisible,
  ] =
    useState(false);

  const handleError =
    useCallback(
      (
        caught: unknown
      ) => {
        setError(
          caught
            instanceof ApiClientError
            ? caught.message
            : caught
              instanceof Error
              ? caught.message
              : "Document operation failed."
        );
      },
      []
    );

  const loadData =
    useCallback(
      async () => {
        if (!organizationId) {
          return;
        }

        try {
          setError("");

          const [
            projectResult,
            documentResult,
          ] =
            await Promise.all([
              apiGet<ProjectView>(
                `/projects/${projectId}`,
                organizationId
              ),

              apiGet<ProjectDocument[]>(
                `/projects/${projectId}/documents`,
                organizationId
              ),
            ]);

          setProject(projectResult);
          setDocuments(
            documentResult
          );
        } catch (caught) {
          handleError(caught);
        }
      },
      [
        organizationId,
        projectId,
        handleError,
      ]
    );

  useEffect(() => {
    if (!organizationId) {
      return;
    }

    const timer =
      window.setTimeout(
        () => {
          void loadData();
        },
        0
      );

    return () => {
      window.clearTimeout(timer);
    };
  }, [
    organizationId,
    loadData,
  ]);

  async function upload(
    event: FormEvent
  ) {
    event.preventDefault();

    if (!file) {
      setError(
        "Select a file first."
      );

      return;
    }

    try {
      const base64Data =
        await fileToBase64(file);

      await apiPost<ProjectDocument>(
        `/projects/${projectId}/documents`,
        {
          fileName: file.name,
          description:
            description || null,
          category,
          mimeType:
            file.type
            || "application/octet-stream",
          base64Data,
          clientVisible,
        },
        organizationId
      );

      setFile(null);
      setDescription("");
      setCategory("OTHER");
      setClientVisible(false);

      await loadData();
    } catch (caught) {
      handleError(caught);
    }
  }

  if (
    loading
    || !user
    || !project
  ) {
    return (
      <main className="appLoading">
        {error || "Loading..."}
      </main>
    );
  }

  return (
    <main className="appShell">
      <AppSidebar active="projects" />

      <section className="appContent">
        <header className="appHeader">
          <div>
            <p>
              {project.projectNumber}
            </p>

            <h1>
              Documents
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="documents"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="phase9Grid">
          <div className="phase9List">
            {documents.length === 0 && (
              <div className="emptyState">
                No documents uploaded.
              </div>
            )}

            {documents.map(
              (document) => (
                <article
                  key={document.id}
                  className="phase9FileCard"
                >
                  <header>
                    <div>
                      <strong>
                        {document.name}
                      </strong>

                      <span>
                        {document.category}
                      </span>
                    </div>

                    <span>
                      v{document.version}
                    </span>
                  </header>

                  <p>
                    {
                      document.description
                      ?? "No description"
                    }
                  </p>

                  <footer>
                    <span>
                      {
                        Math.ceil(
                          document.sizeBytes
                            / 1024
                        )
                      }
                      {" KB"}
                    </span>

                    <span>
                      {
                        document.clientVisible
                          ? "Client visible"
                          : "Internal"
                      }
                    </span>
                  </footer>
                </article>
              )
            )}
          </div>

          <form
            className="commercialForm"
            onSubmit={upload}
          >
            <h3>
              Upload document
            </h3>

            <label>
              File
              <input
                required
                type="file"
                accept=".pdf,.jpg,.jpeg,.png,.webp,.docx,.xlsx"
                onChange={(event) =>
                  setFile(
                    event.target
                      .files?.[0]
                    ?? null
                  )
                }
              />
            </label>

            <label>
              Category
              <select
                value={category}
                onChange={(event) =>
                  setCategory(
                    event.target.value as DocumentCategory
                  )
                }
              >
                <option value="CONTRACT">
                  Contract
                </option>
                <option value="DRAWING">
                  Drawing
                </option>
                <option value="PERMIT">
                  Permit
                </option>
                <option value="QUOTE">
                  Quote
                </option>
                <option value="INVOICE">
                  Invoice
                </option>
                <option value="RECEIPT">
                  Receipt
                </option>
                <option value="REPORT">
                  Report
                </option>
                <option value="INSURANCE">
                  Insurance
                </option>
                <option value="SAFETY">
                  Safety
                </option>
                <option value="TECHNICAL">
                  Technical
                </option>
                <option value="OTHER">
                  Other
                </option>
              </select>
            </label>

            <label>
              Description
              <textarea
                value={description}
                onChange={(event) =>
                  setDescription(
                    event.target.value
                  )
                }
              />
            </label>

            <label className="siteCheckbox">
              <input
                type="checkbox"
                checked={clientVisible}
                onChange={(event) =>
                  setClientVisible(
                    event.target.checked
                  )
                }
              />
              Client visible
            </label>

            <button type="submit">
              Secure upload
            </button>
          </form>
        </section>
      </section>
    </main>
  );
}
