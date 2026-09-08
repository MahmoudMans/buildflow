"use client";

import Link from "next/link";
import {
  useState,
} from "react";

import {
  ApiClientError,
  apiPost,
} from "@/lib/api";

import { useAuth } from "@/components/AuthProvider";

export default function VerifyEmailPage() {
  const { refresh } =
    useAuth();

  const [message, setMessage] =
    useState("");

  const [error, setError] =
    useState("");

  async function verify() {
    setError("");

    const token =
      new URLSearchParams(
        window.location.search
      ).get("token");

    if (!token) {
      setError(
        "The verification token is missing."
      );
      return;
    }

    try {
      await apiPost<void>(
        "/auth/verify-email",
        {
          token,
        }
      );

      await refresh();

      setMessage(
        "Your email address has been verified."
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Verification failed."
      );
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

        <h1>Verify your email</h1>

        <p>
          Confirm your email address to complete
          your BuildFlow account setup.
        </p>

        {message && (
          <div className="formSuccess">
            {message}
          </div>
        )}

        {error && (
          <div className="formError">
            {error}
          </div>
        )}

        {!message && (
          <button
            className="authSubmit"
            type="button"
            onClick={verify}
          >
            Verify email
          </button>
        )}

        <Link
          className="textLink"
          href="/app"
        >
          Continue to BuildFlow
        </Link>
      </section>
    </main>
  );
}
