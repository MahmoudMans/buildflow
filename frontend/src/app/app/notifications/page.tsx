"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";

import {
  AppSidebar,
} from "@/components/AppSidebar";

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
  NotificationView,
} from "@/lib/notification-types";
import { formatDateTime } from "@/lib/format";

export default function NotificationsPage() {
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

  const [
    notifications,
    setNotifications,
  ] =
    useState<
      NotificationView[]
    >([]);

  const [error, setError] =
    useState("");

  const load =
    useCallback(
      async () => {
        if (!organizationId) {
          return;
        }

        try {
          setError("");

          const result =
            await apiGet<
              NotificationView[]
            >(
              "/notifications",
              organizationId
            );

          setNotifications(
            result
          );
        } catch (caught) {
          setError(
            caught
              instanceof ApiClientError
              ? caught.message
              : caught
                instanceof Error
                ? caught.message
                : "Unable to load notifications."
          );
        }
      },
      [organizationId]
    );

  useEffect(() => {
    if (!organizationId) {
      return;
    }

    const timer =
      window.setTimeout(
        () => {
          void load();
        },
        0
      );

    return () => {
      window.clearTimeout(timer);
    };
  }, [
    organizationId,
    load,
  ]);

  async function markRead(
    notificationId: string
  ) {
    await apiPost(
      `/notifications/${notificationId}/read`,
      undefined,
      organizationId
    );

    await load();
  }

  async function markAllRead() {
    await apiPost(
      "/notifications/read-all",
      undefined,
      organizationId
    );

    await load();
  }

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

  const unread =
    notifications.filter(
      (notification) =>
        notification.readAt
          === null
    ).length;

  return (
    <main className="appShell">
      <AppSidebar active="overview" />

      <section className="appContent">
        <header className="appHeader notificationHeader">
          <div>
            <p>
              ACTIVITY CENTER
            </p>

            <h1>
              Notifications
            </h1>
          </div>

          {unread > 0 && (
            <button
              type="button"
              onClick={() =>
                void markAllRead()
              }
            >
              Mark all read
            </button>
          )}
        </header>

        {error && (
          <div className="formError moduleError">
            {error}
          </div>
        )}

        <section className="notificationList">
          {notifications.length === 0 && (
            <div className="emptyState">
              No notifications yet.
            </div>
          )}

          {notifications.map(
            (notification) => (
              <article
                key={notification.id}
                className={
                  notification.readAt
                    ? "notificationCard"
                    : "notificationCard unread"
                }
              >
                <div className="notificationIcon">
                  🔔
                </div>

                <div>
                  <header>
                    <strong>
                      {
                        notification
                          .title
                      }
                    </strong>

                    <span>
                      {
                        formatDateTime(notification.createdAt)
                      }
                    </span>
                  </header>

                  <p>
                    {
                      notification
                        .message
                    }
                  </p>

                  <small>
                    {
                      notification
                        .type
                    }
                  </small>
                </div>

                {!notification.readAt && (
                  <button
                    type="button"
                    onClick={() =>
                      void markRead(
                        notification.id
                      )
                    }
                  >
                    Mark read
                  </button>
                )}
              </article>
            )
          )}
        </section>
      </section>
    </main>
  );
}
