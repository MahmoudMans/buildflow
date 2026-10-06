"use client";

import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";

/**
 * The organization dropdown that pages put in their header. It reads and
 * writes the same stored selection as the sidebar, so they always agree.
 */
export function OrganizationSelect({
  disabled = false,
  className = "organizationSelector",
}: {
  disabled?: boolean;
  className?: string;
}) {
  const { organizations } = useAuth();
  const { organizationId, selectOrganization } =
    useOrganizationSelection(organizations);

  return (
    <select
      className={className}
      value={organizationId}
      onChange={(event) => selectOrganization(event.target.value)}
      disabled={disabled}
    >
      {organizations.map((organization) => (
        <option key={organization.id} value={organization.id}>
          {organization.name}
        </option>
      ))}
    </select>
  );
}
