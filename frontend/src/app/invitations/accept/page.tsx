"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { useAuth } from "@/components/AuthProvider";
import {
  ApiClientError,
  apiPost,
} from "@/lib/api";

import type {
  OrganizationSummary,
} from "@/lib/types";

export default function AcceptInvitationPage() {
  const router = useRouter();

  const {
    user,
    loading,
    refresh,
  } = useAuth();

  const [error, setError] =
    useState("");

  const [accepting, setAccepting] =
    useState(false);

  if (loading) {
    return (
      <main className="simpleAuthPage">
        <section className="simpleAuthCard">
          Loading...
        </section>
      </main>
    );
  }

  const currentPath =
    typeof window === "undefined"
      ? "/invitations/accept"
      : window.location.pathname
          + window.location.search;

  async function accept() {
    setError("");
    setAccepting(true);

    const token =
      new URLSearchParams(
        window.location.search
      ).get("token");

    if (!token) {
      setError(
        "The invitation token is missing."
      );
      setAccepting(false);
      return;
    }

    try {
      await apiPost<OrganizationSummary>(
        "/invitations/accept",
        {
          token,
        }
      );

      await refresh();

      router.push("/app");
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "The invitation could not be accepted."
      );
    } finally {
      setAccepting(false);
    }
  }

  return (
    <main className="simpleAuthPage">
      <section className="simpleAuthCard">
        <Link
          className="authBrand"
          href="/"
        >
          <span>B</span>
          BuildFlow
        </Link>

        <h1>Organization invitation</h1>

        {!user ? (
          <>
            <p>
              Sign in or create an account using
              the email address that received this
              invitation.
            </p>

            <div className="stackActions">
              <Link
                className="authSubmit"
                href={`/login?next=${encodeURIComponent(
                  currentPath
                )}`}
              >
                Sign in
              </Link>

              <Link
                className="secondaryFormButton"
                href="/register"
              >
                Create account
              </Link>
            </div>
          </>
        ) : (
          <>
            <p>
              Signed in as{" "}
              <strong>
                {user.email}
              </strong>
            </p>

            {error && (
              <div className="formError">
                {error}
              </div>
            )}

            <button
              className="authSubmit"
              type="button"
              disabled={accepting}
              onClick={accept}
            >
              {accepting
                ? "Joining..."
                : "Accept invitation"}
            </button>
          </>
        )}
      </section>
    </main>
  );
}
