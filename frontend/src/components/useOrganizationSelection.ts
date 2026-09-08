"use client";

import {
  useSyncExternalStore,
} from "react";

type OrganizationLike = {
  id: string;
};

const STORAGE_KEY =
  "buildflow.organizationId";

const CHANGE_EVENT =
  "buildflow-organization-selection";

function subscribe(
  callback: () => void
) {
  if (
    typeof window === "undefined"
  ) {
    return () => {};
  }

  const handleStorage = () => {
    callback();
  };

  const handleSelection = () => {
    callback();
  };

  window.addEventListener(
    "storage",
    handleStorage
  );

  window.addEventListener(
    CHANGE_EVENT,
    handleSelection
  );

  return () => {
    window.removeEventListener(
      "storage",
      handleStorage
    );

    window.removeEventListener(
      CHANGE_EVENT,
      handleSelection
    );
  };
}

function getSnapshot() {
  if (
    typeof window === "undefined"
  ) {
    return "";
  }

  return (
    window.localStorage.getItem(
      STORAGE_KEY
    ) ?? ""
  );
}

function getServerSnapshot() {
  return "";
}

export function useOrganizationSelection<
  T extends OrganizationLike,
>(
  organizations: readonly T[]
) {
  const preferredOrganizationId =
    useSyncExternalStore(
      subscribe,
      getSnapshot,
      getServerSnapshot
    );

  const preferredExists =
    Boolean(
      preferredOrganizationId
    )
    && organizations.some(
      (organization) =>
        organization.id
          === preferredOrganizationId
    );

  const organizationId =
    preferredExists
      ? preferredOrganizationId
      : organizations[0]?.id ?? "";

  function selectOrganization(
    nextOrganizationId: string
  ) {
    if (
      typeof window === "undefined"
    ) {
      return;
    }

    window.localStorage.setItem(
      STORAGE_KEY,
      nextOrganizationId
    );

    window.dispatchEvent(
      new Event(
        CHANGE_EVENT
      )
    );
  }

  return {
    organizationId,
    selectOrganization,
  };
}
