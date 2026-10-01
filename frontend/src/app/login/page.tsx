"use client";

import type {
  FormEvent,
} from "react";

import Link from "next/link";

import {
  useRouter,
} from "next/navigation";

import {
  useState,
} from "react";

import {
  useAuth,
} from "@/components/AuthProvider";

import {
  ApiClientError,
} from "@/lib/api";

import styles from "./login.module.css";

export default function LoginPage() {
  const router =
    useRouter();

  const {
    login,
  } = useAuth();

  const [
    email,
    setEmail,
  ] = useState("");

  const [
    password,
    setPassword,
  ] = useState("");

  const [
    error,
    setError,
  ] = useState("");

  const [
    submitting,
    setSubmitting,
  ] = useState(false);

  const [
    passwordVisible,
    setPasswordVisible,
  ] = useState(false);

  async function submit(
    event: FormEvent,
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
          window.location.search,
        );

      const next =
        parameters.get(
          "next",
        );

      router.push(
        next
        && next.startsWith("/")
          ? next
          : "/app",
      );
    } catch (caught) {
      setError(
        caught instanceof
          ApiClientError
          ? caught.message
          : "Login failed.",
      );
    } finally {
      setSubmitting(
        false,
      );
    }
  }

  return (
    <main
      className={
        styles.page
      }
    >
      <section
        className={
          styles.authenticationPane
        }
        aria-labelledby="login-title"
      >
        <div
          className={
            styles.authenticationStack
          }
        >
          <Link
            href="/"
            className={
              styles.brand
            }
            aria-label="BuildFlow home"
          >
            <span
              className={
                styles.brandMark
              }
              aria-hidden="true"
            >
              BF
            </span>

            <span
              className={
                styles.brandName
              }
            >
              BuildFlow
            </span>
          </Link>

          <header
            className={
              styles.introduction
            }
          >
            <h1
              id="login-title"
            >
              Sign in
            </h1>

            <p>
              Use your email and password to access BuildFlow.
            </p>
          </header>

          <form
            className={
              styles.form
            }
            onSubmit={(
              event
            ) => {
              setPasswordVisible(
                false,
              );

              void submit(
                event,
              );
            }}
          >
            {error ? (
              <div
                className={
                  styles.formError
                }
                role="alert"
              >
                {error}
              </div>
            ) : null}

            <div
              className={
                styles.fieldGroup
              }
            >
              <label
                className={
                  styles.fieldLabel
                }
                htmlFor="buildflow-email"
              >
                Email address
              </label>

              <input
                id="buildflow-email"
                name="email"
                type="email"
                inputMode="email"
                autoComplete="username"
                required
                className={
                  styles.fieldControl
                }
                value={email}
                onChange={(
                  event
                ) =>
                  setEmail(
                    event
                      .target
                      .value,
                  )
                }
              />
            </div>

            <div
              className={
                styles.passwordGroup
              }
            >
              <div
                className={
                  styles.fieldGroup
                }
              >
                <label
                  className={
                    styles.fieldLabel
                  }
                  htmlFor="buildflow-password"
                >
                  Password
                </label>

                <div
                  className={
                    styles.passwordControl
                  }
                >
                  <input
                    id="buildflow-password"
                    name="password"
                    type={
                      passwordVisible
                        ? "text"
                        : "password"
                    }
                    autoComplete="current-password"
                    required
                    className={
                      styles.passwordInput
                    }
                    value={
                      password
                    }
                    onChange={(
                      event
                    ) =>
                      setPassword(
                        event
                          .target
                          .value,
                      )
                    }
                  />

                  <button
                    type="button"
                    className={
                      styles.visibilityButton
                    }
                    aria-label={
                      passwordVisible
                        ? "Hide password"
                        : "Show password"
                    }
                    onClick={() =>
                      setPasswordVisible(
                        (
                          current
                        ) =>
                          !current,
                      )
                    }
                  >
                    {
                      passwordVisible
                        ? "Hide"
                        : "Show"
                    }
                  </button>
                </div>
              </div>

              <div
                className={
                  styles.recoveryRow
                }
              >
                <Link
                  href="/forgot-password"
                >
                  Forgot password?
                </Link>
              </div>
            </div>

            <button
              type="submit"
              className={
                styles.submitButton
              }
              disabled={
                submitting
              }
            >
              {
                submitting
                  ? "Signing in…"
                  : "Sign in"
              }
            </button>

            <p
              className={
                styles.registrationLine
              }
            >
              New to BuildFlow?{" "}

              <Link
                href="/register"
              >
                Create an account
              </Link>
            </p>

            <div
              className={
                styles.demoAlternative
              }
            >
              <span
                className={
                  styles.demoDivider
                }
                aria-hidden="true"
              />

              <p>
                Here to explore?{" "}

                <Link
                  href="/demo"
                >
                  View the demo
                </Link>
              </p>
            </div>
          </form>
        </div>
      </section>

      <aside
        className={
          styles.supportingPane
        }
        aria-label="BuildFlow product context"
      >
        <div
          className={
            styles.supportingContent
          }
        >
          <p
            className={
              styles.supportingCategory
            }
          >
            Construction operations
          </p>

          <h2>
            Your construction workspace.
          </h2>

          <p
            className={
              styles.supportingDescription
            }
          >
            Projects, teams and costs—in one place.
          </p>
        </div>
      </aside>
    </main>
  );
}
