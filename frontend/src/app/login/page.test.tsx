import {
  readFileSync,
} from "node:fs";

import {
  resolve,
} from "node:path";

import {
  describe,
  expect,
  it,
} from "vitest";

const pageSource =
  readFileSync(
    resolve(
      process.cwd(),
      "src/app/login/page.tsx",
    ),
    "utf8",
  );

const styleSource =
  readFileSync(
    resolve(
      process.cwd(),
      "src/app/login/login.module.css",
    ),
    "utf8",
  );

describe(
  "BuildFlow login redesign",
  () => {
    it(
      "preserves authentication and routing",
      () => {
        expect(
          pageSource,
        ).toContain(
          "useAuth",
        );

        expect(
          pageSource,
        ).toContain(
          "await login({",
        );

        expect(
          pageSource,
        ).toContain(
          'parameters.get(',
        );

        expect(
          pageSource,
        ).toContain(
          '"next"',
        );

        expect(
          pageSource,
        ).toContain(
          'next.startsWith("/")',
        );

        expect(
          pageSource,
        ).toContain(
          "router.push(",
        );

        expect(
          pageSource,
        ).toContain(
          ': "/app",',
        );

        expect(
          pageSource,
        ).toContain(
          "ApiClientError",
        );
      },
    );

    it(
      "implements the focused authentication design",
      () => {
        expect(
          pageSource,
        ).toContain(
          "Use your email and password to access BuildFlow.",
        );

        expect(
          pageSource,
        ).toContain(
          'autoComplete="username"',
        );

        expect(
          pageSource,
        ).toContain(
          'autoComplete="current-password"',
        );

        expect(
          pageSource,
        ).toContain(
          "Show password",
        );

        expect(
          pageSource,
        ).toContain(
          "Hide password",
        );

        expect(
          pageSource,
        ).toContain(
          "Forgot password?",
        );

        expect(
          pageSource,
        ).toContain(
          "Create an account",
        );

        expect(
          pageSource,
        ).toContain(
          "View the demo",
        );

        expect(
          pageSource,
        ).toContain(
          "Your construction workspace.",
        );

        expect(
          styleSource,
        ).toContain(
          "max-width: 440px",
        );

        expect(
          styleSource,
        ).toContain(
          "minmax(0, 58fr)",
        );

        expect(
          styleSource,
        ).toContain(
          "minmax(0, 42fr)",
        );

        expect(
          styleSource,
        ).toContain(
          "--login-border: #64748b",
        );

        expect(
          styleSource,
        ).toContain(
          "@media (max-width: 1023px)",
        );
      },
    );
  },
);
