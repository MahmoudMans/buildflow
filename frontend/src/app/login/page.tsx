"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FormEvent,
  useState,
} from "react";

import { useAuth } from "@/components/AuthProvider";
import { ApiClientError } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();

  const { login } =
    useAuth();

  const [email, setEmail] =
    useState("");

  const [password, setPassword] =
    useState("");

  const [error, setError] =
    useState("");

  const [submitting, setSubmitting] =
    useState(false);

  async function submit(
    event: FormEvent
  ) {
    event.preventDefault();

    setError("");
    setSubmitting(true);

    try {
      await login({
        email,
        password,
      });

      const parameters =
        new URLSearchParams(
          window.location.search
        );

      const next =
        parameters.get("next");

      router.push(
        next &&
        next.startsWith("/")
          ? next
          : "/app"
      );
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Login failed."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="authPage">
      <section className="authPanel">
        <Link
          className="authBrand"
          href="/"
        >
          <span>B</span>
          BuildFlow
        </Link>

        <div className="authHeading">
          <p>WELCOME BACK</p>
          <h1>Sign in to BuildFlow</h1>
          <span>
            Continue managing your projects,
            teams and construction operations.
          </span>
        </div>

        <form
          className="authForm"
          onSubmit={submit}
        >
          <label>
            Email
            <input
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(event) =>
                setEmail(
                  event.target.value
                )
              }
            />
          </label>

          <label>
            Password
            <input
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(event) =>
                setPassword(
                  event.target.value
                )
              }
            />
          </label>

          {error && (
            <div className="formError">
              {error}
            </div>
          )}

          <button
            className="authSubmit"
            type="submit"
            disabled={submitting}
          >
            {submitting
              ? "Signing in..."
              : "Sign in"}
          </button>
        </form>

        <div className="authLinks">
          <Link href="/forgot-password">
            Forgot password?
          </Link>

          <span>
            No account?{" "}
            <Link href="/register">
              Create one
            </Link>
          </span>
        </div>
      </section>

      <aside className="authVisual">
        <div>
          <p>CONSTRUCTION CONTROL</p>

          <h2>
            Every project.
            <br />
            Every cost.
            <br />
            One workspace.
          </h2>
        </div>
      </aside>
    </main>
  );
}
