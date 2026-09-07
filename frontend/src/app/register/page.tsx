"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FormEvent,
  useState,
} from "react";

import { useAuth } from "@/components/AuthProvider";
import { ApiClientError } from "@/lib/api";

export default function RegisterPage() {
  const router = useRouter();

  const { register } =
    useAuth();

  const [firstName, setFirstName] =
    useState("");

  const [lastName, setLastName] =
    useState("");

  const [email, setEmail] =
    useState("");

  const [password, setPassword] =
    useState("");

  const [confirmPassword, setConfirmPassword] =
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

    if (
      password !==
      confirmPassword
    ) {
      setError(
        "Passwords do not match."
      );
      return;
    }

    setSubmitting(true);

    try {
      await register({
        firstName,
        lastName,
        email,
        password,
      });

      router.push("/app");
    } catch (caught) {
      setError(
        caught instanceof ApiClientError
          ? caught.message
          : "Registration failed."
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
          <p>GET STARTED</p>
          <h1>Create your account</h1>
          <span>
            Your organization can be created
            immediately after registration.
          </span>
        </div>

        <form
          className="authForm"
          onSubmit={submit}
        >
          <div className="fieldRow">
            <label>
              First name
              <input
                required
                value={firstName}
                onChange={(event) =>
                  setFirstName(
                    event.target.value
                  )
                }
              />
            </label>

            <label>
              Last name
              <input
                required
                value={lastName}
                onChange={(event) =>
                  setLastName(
                    event.target.value
                  )
                }
              />
            </label>
          </div>

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
              minLength={12}
              autoComplete="new-password"
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
              required
              minLength={12}
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) =>
                setConfirmPassword(
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
              ? "Creating account..."
              : "Create account"}
          </button>
        </form>

        <div className="authLinks">
          <span>
            Already registered?{" "}
            <Link href="/login">
              Sign in
            </Link>
          </span>
        </div>
      </section>

      <aside className="authVisual">
        <div>
          <p>BUILT FOR PROJECT TEAMS</p>

          <h2>
            Plan.
            <br />
            Build.
            <br />
            Control.
          </h2>
        </div>
      </aside>
    </main>
  );
}
