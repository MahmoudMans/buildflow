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

export default function ForgotPasswordPage() {
  const [email, setEmail] =
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

    try {
      await apiPost<void>(
        "/auth/forgot-password",
        {
          email,
        }
      );

      setMessage(
        "If an account exists for this email, a reset link has been sent."
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "The request failed."
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

        <h1>Reset password</h1>

        <p>
          Enter your email address to receive
          a password reset link.
        </p>

        <form
          className="authForm"
          onSubmit={submit}
        >
          <label>
            Email
            <input
              type="email"
              required
              value={email}
              onChange={(event) =>
                setEmail(
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
            Send reset link
          </button>
        </form>
      </section>
    </main>
  );
}
