"use client";

import Link from "next/link";
import {
  FormEvent,
  useState,
} from "react";

import {
  ApiClientError,
  apiPost,
} from "@/lib/api";

export default function ResetPasswordPage() {
  const [password, setPassword] =
    useState("");

  const [confirmation, setConfirmation] =
    useState("");

  const [message, setMessage] =
    useState("");

  const [error, setError] =
    useState("");

  async function submit(
    event: FormEvent
  ) {
    event.preventDefault();

    setError("");
    setMessage("");

    if (
      password !== confirmation
    ) {
      setError(
        "Passwords do not match."
      );
      return;
    }

    const token =
      new URLSearchParams(
        window.location.search
      ).get("token");

    if (!token) {
      setError(
        "The reset token is missing."
      );
      return;
    }

    try {
      await apiPost<void>(
        "/auth/reset-password",
        {
          token,
          newPassword: password,
        }
      );

      setMessage(
        "Your password has been changed. You can now sign in."
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Password reset failed."
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

        <h1>Choose a new password</h1>

        <form
          className="authForm"
          onSubmit={submit}
        >
          <label>
            New password
            <input
              type="password"
              minLength={12}
              required
              value={password}
              onChange={(event) =>
                setPassword(
                  event.target.value
                )
              }
            />
          </label>

          <label>
            Confirm password
            <input
              type="password"
              minLength={12}
              required
              value={confirmation}
              onChange={(event) =>
                setConfirmation(
                  event.target.value
                )
              }
            />
          </label>

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

          <button
            className="authSubmit"
            type="submit"
          >
            Change password
          </button>
        </form>

        <Link
          className="textLink"
          href="/login"
        >
          Return to login
        </Link>
      </section>
    </main>
  );
}
