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
  ProjectPhoto,
  ProjectPhotoCategory,
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
              "Unable to read photo."
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
            "Unable to read photo."
          )
        );
      };

      reader.readAsDataURL(file);
    }
  );
}

export default function PhotosPage() {
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

  const [photos, setPhotos] =
    useState<ProjectPhoto[]>(
      []
    );

  const [error, setError] =
    useState("");

  const [file, setFile] =
    useState<File | null>(
      null
    );

  const [caption, setCaption] =
    useState("");

  const [takenDate, setTakenDate] =
    useState("");

  const [category, setCategory] =
    useState<ProjectPhotoCategory>(
      "PROGRESS"
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
              : "Photo operation failed."
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
            photoResult,
          ] =
            await Promise.all([
              apiGet<ProjectView>(
                `/projects/${projectId}`,
                organizationId
              ),

              apiGet<ProjectPhoto[]>(
                `/projects/${projectId}/photos`,
                organizationId
              ),
            ]);

          setProject(projectResult);
          setPhotos(photoResult);
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
        "Select a photo first."
      );

      return;
    }

    try {
      const base64Data =
        await fileToBase64(file);

      await apiPost<ProjectPhoto>(
        `/projects/${projectId}/photos`,
        {
          fileName: file.name,
          mimeType: file.type,
          base64Data,
          caption:
            caption || null,
          takenDate:
            takenDate || null,
          category,
          clientVisible,
        },
        organizationId
      );

      setFile(null);
      setCaption("");
      setTakenDate("");
      setCategory("PROGRESS");
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
              Photo gallery
            </h1>

            <span className="projectClientName">
              {project.name}
            </span>
          </div>
        </header>

        <ProjectWorkspaceNav
          projectId={projectId}
          active="photos"
        />

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="phase9Grid">
          <div className="phase9PhotoGrid">
            {photos.length === 0 && (
              <div className="emptyState">
                No gallery photos yet.
              </div>
            )}

            {photos.map(
              (photo) => (
                <article
                  key={photo.id}
                  className="phase9PhotoCard"
                >
                  <strong>
                    {photo.category}
                  </strong>

                  <p>
                    {
                      photo.caption
                      ?? "No caption"
                    }
                  </p>

                  <span>
                    {
                      photo.takenDate
                      ?? "Date not specified"
                    }
                  </span>

                  <small>
                    {
                      photo.clientVisible
                        ? "Client visible"
                        : "Internal"
                    }
                  </small>
                </article>
              )
            )}
          </div>

          <form
            className="commercialForm"
            onSubmit={upload}
          >
            <h3>
              Add gallery photo
            </h3>

            <label>
              Photo
              <input
                required
                type="file"
                accept="image/jpeg,image/png,image/webp"
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
                    event.target.value as ProjectPhotoCategory
                  )
                }
              >
                <option value="BEFORE">
                  Before
                </option>
                <option value="PROGRESS">
                  Progress
                </option>
                <option value="AFTER">
                  After
                </option>
                <option value="ISSUE">
                  Issue
                </option>
                <option value="DELIVERY">
                  Delivery
                </option>
                <option value="OTHER">
                  Other
                </option>
              </select>
            </label>

            <label>
              Taken date
              <input
                type="date"
                value={takenDate}
                onChange={(event) =>
                  setTakenDate(
                    event.target.value
                  )
                }
              />
            </label>

            <label>
              Caption
              <textarea
                value={caption}
                onChange={(event) =>
                  setCaption(
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
              Upload photo
            </button>
          </form>
        </section>
      </section>
    </main>
  );
}
