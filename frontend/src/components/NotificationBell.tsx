"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";

import Link from "next/link";

import {
  useAuth,
} from "@/components/AuthProvider";

import {
  useOrganizationSelection,
} from "@/components/useOrganizationSelection";

import {
  apiGet,
} from "@/lib/api";

import type {
  NotificationUnreadCount,
} from "@/lib/notification-types";

export function NotificationBell() {
  const {
    organizations,
  } = useAuth();

  const selection =
    useOrganizationSelection(
      organizations
    );

  const organizationId =
    selection.organizationId;

  const [count, setCount] =
    useState(0);

  const loadCount =
    useCallback(
      async () => {
        if (!organizationId) {
          return;
        }

        try {
          const result =
            await apiGet<
              NotificationUnreadCount
            >(
              "/notifications/unread-count",
              organizationId
            );

          setCount(result.count);
        } catch {
          setCount(0);
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
          void loadCount();
        },
        0
      );

    return () => {
      window.clearTimeout(timer);
    };
  }, [
    organizationId,
    loadCount,
  ]);

  return (
    <Link
      className="notificationBell"
      href="/app/notifications"
      aria-label={
        count > 0
          ? `${count} unread notifications`
          : "Notifications"
      }
    >
      <span aria-hidden="true">
        🔔
      </span>

      {count > 0 && (
        <strong>
          {count > 99
            ? "99+"
            : count}
        </strong>
      )}
    </Link>
  );
}
