"use client";

import Link from "next/link";

import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  useParams,
} from "next/navigation";

import {
  apiPost,
} from "@/lib/api";

import {
  demoAccounts,
  portfolioConfig,
} from "../../../../lib/portfolio-config";

export default function DemoLoginPage() {
  const params =
    useParams<{
      role: string;
    }>();

  const [error, setError] =
    useState("");

  const account =
    useMemo(
      () =>
        demoAccounts.find(
          (candidate) =>
            candidate.key
            === params.role
        ),
      [
        params.role,
      ]
    );

  useEffect(() => {
    if (!account) {
      return;
    }

    let cancelled =
      false;

    async function login() {
      try {
        await apiPost(
          "/auth/login",
          {
            email:
              account!.email,
            password:
              portfolioConfig
                .demoPassword,
          }
        );

        if (!cancelled) {
          window.location.assign(
            account!
              .destination
          );
        }
      } catch (caught) {
        if (
          !cancelled
        ) {
          setError(
            caught
              instanceof Error
                ? caught.message
                : "Demo login failed."
          );
        }
      }
    }

    void login();

    return () => {
      cancelled =
        true;
    };
  }, [
    account,
  ]);

  if (!account) {
    return (
      <main className="demoLoginState">
        <h1>
          Unknown demo role
        </h1>

        <Link href="/demo">
          Return to demo
        </Link>
      </main>
    );
  }

  return (
    <main
      className="demoLoginState"
      aria-live="polite"
    >
      <div className="demoLoginSpinner" />

      <span className="portfolioEyebrow">
        BUILDFLOW DEMO
      </span>

      <h1>
        Signing in as {
          account.label
        }
      </h1>

      {!error ? (
        <p>
          Preparing the {
            account.label
          } workspace…
        </p>
      ) : (
        <>
          <p className="demoLoginError">
            {error}
          </p>

          <Link
            className="portfolioPrimaryButton"
            href="/demo"
          >
            Return to demo
          </Link>
        </>
      )}
    </main>
  );
}
