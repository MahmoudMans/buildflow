"use client";

import Link from "next/link";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import type {
  ReactNode,
} from "react";

import {
  usePathname,
} from "next/navigation";

type ToastTone =
  | "success"
  | "error"
  | "warning"
  | "info";

type ToastInput = {
  title: string;
  message?: string;
  tone?: ToastTone;
  durationMs?: number;
};

type ToastRecord =
  ToastInput & {
    id: number;
  };

type ConfirmationOptions = {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: "default" | "danger";
};

type ConfirmationState = {
  options: ConfirmationOptions;
  resolve: (
    confirmed: boolean
  ) => void;
};

type UxContextValue = {
  toast: (
    input: ToastInput
  ) => void;

  confirm: (
    options: ConfirmationOptions
  ) => Promise<boolean>;
};

const UxContext =
  createContext<
    UxContextValue | null
  >(null);

const mobileItems = [
  {
    href: "/app",
    label: "Home",
  },
  {
    href: "/app/projects",
    label: "Projects",
  },
  {
    href: "/app/clients",
    label: "Clients",
  },
  {
    href: "/app/notifications",
    label: "Alerts",
  },
  {
    href: "/app/reports",
    label: "Reports",
  },
];

function MobileNavigation() {
  const pathname =
    usePathname()
    ?? "";

  const internalApp =
    pathname === "/app"
    || (
      pathname.startsWith(
        "/app/"
      )
      && !pathname.startsWith(
        "/app/client"
      )
    );

  if (!internalApp) {
    return null;
  }

  return (
    <nav
      className="uxMobileNav"
      aria-label="Mobile application navigation"
    >
      {mobileItems.map(
        (item) => {
          const active =
            item.href === "/app"
              ? pathname === "/app"
              : pathname.startsWith(
                  item.href
                );

          return (
            <Link
              key={item.href}
              href={item.href}
              className={
                active
                  ? "active"
                  : ""
              }
              aria-current={
                active
                  ? "page"
                  : undefined
              }
            >
              {item.label}
            </Link>
          );
        }
      )}
    </nav>
  );
}

export function UxProvider({
  children,
}: {
  children: ReactNode;
}) {
  const [toasts, setToasts] =
    useState<ToastRecord[]>(
      []
    );

  const [
    confirmation,
    setConfirmation,
  ] =
    useState<
      ConfirmationState | null
    >(null);

  const sequence =
    useRef(0);

  const cancelButton =
    useRef<HTMLButtonElement>(
      null
    );

  const removeToast =
    useCallback(
      (
        id: number
      ) => {
        setToasts(
          (current) =>
            current.filter(
              (toast) =>
                toast.id !== id
            )
        );
      },
      []
    );

  const toast =
    useCallback(
      (
        input: ToastInput
      ) => {
        sequence.current += 1;

        const id =
          sequence.current;

        const record: ToastRecord =
          {
            id,
            title:
              input.title,
            message:
              input.message,
            tone:
              input.tone
              ?? "info",
            durationMs:
              input.durationMs
              ?? 4500,
          };

        setToasts(
          (current) => [
            ...current,
            record,
          ]
        );

        window.setTimeout(
          () => {
            removeToast(id);
          },
          record.durationMs
        );
      },
      [
        removeToast,
      ]
    );

  const confirm =
    useCallback(
      (
        options:
          ConfirmationOptions
      ) =>
        new Promise<boolean>(
          (resolve) => {
            setConfirmation({
              options,
              resolve,
            });
          }
        ),
      []
    );

  const finishConfirmation =
    useCallback(
      (
        result: boolean
      ) => {
        setConfirmation(
          (current) => {
            current?.resolve(
              result
            );

            return null;
          }
        );
      },
      []
    );

  useEffect(() => {
    const listener =
      (
        event: Event
      ) => {
        const detail =
          (
            event as
              CustomEvent<ToastInput>
          ).detail;

        if (
          detail
          && detail.title
        ) {
          toast(detail);
        }
      };

    window.addEventListener(
      "buildflow:toast",
      listener
    );

    return () => {
      window.removeEventListener(
        "buildflow:toast",
        listener
      );
    };
  }, [
    toast,
  ]);

  useEffect(() => {
    if (!confirmation) {
      return;
    }

    const escape =
      (
        event:
          KeyboardEvent
      ) => {
        if (
          event.key ===
          "Escape"
        ) {
          finishConfirmation(
            false
          );
        }
      };

    window.addEventListener(
      "keydown",
      escape
    );

    window.requestAnimationFrame(
      () => {
        cancelButton.current
          ?.focus();
      }
    );

    return () => {
      window.removeEventListener(
        "keydown",
        escape
      );
    };
  }, [
    confirmation,
    finishConfirmation,
  ]);

  useEffect(
    () =>
      () => {
        confirmation?.resolve(
          false
        );
      },
    [
      confirmation,
    ]
  );

  const context =
    useMemo(
      () => ({
        toast,
        confirm,
      }),
      [
        toast,
        confirm,
      ]
    );

  return (
    <UxContext.Provider
      value={context}
    >
      <a
        className="uxSkipLink"
        href="#buildflow-main-content"
      >
        Skip to main content
      </a>

      <div
        id="buildflow-main-content"
        className="uxApplicationRoot"
        tabIndex={-1}
      >
        {children}
      </div>

      <MobileNavigation />

      <div
        className="uxToastRegion"
        aria-live="polite"
        aria-relevant="additions"
      >
        {toasts.map(
          (item) => (
            <article
              key={item.id}
              className={`uxToast ${item.tone}`}
              role={
                item.tone ===
                "error"
                  ? "alert"
                  : "status"
              }
            >
              <div>
                <strong>
                  {item.title}
                </strong>

                {item.message && (
                  <p>
                    {item.message}
                  </p>
                )}
              </div>

              <button
                type="button"
                aria-label={`Dismiss ${item.title}`}
                onClick={() =>
                  removeToast(
                    item.id
                  )
                }
              >
                ×
              </button>
            </article>
          )
        )}
      </div>

      {confirmation && (
        <div
          className="uxDialogBackdrop"
          role="presentation"
          onMouseDown={(
            event
          ) => {
            if (
              event.target ===
              event.currentTarget
            ) {
              finishConfirmation(
                false
              );
            }
          }}
        >
          <section
            className="uxConfirmDialog"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="ux-confirm-title"
            aria-describedby="ux-confirm-description"
          >
            <h2
              id="ux-confirm-title"
            >
              {
                confirmation
                  .options
                  .title
              }
            </h2>

            <p
              id="ux-confirm-description"
            >
              {
                confirmation
                  .options
                  .message
              }
            </p>

            <div className="uxDialogActions">
              <button
                ref={cancelButton}
                type="button"
                className="uxSecondaryButton"
                onClick={() =>
                  finishConfirmation(
                    false
                  )
                }
              >
                {
                  confirmation
                    .options
                    .cancelLabel
                  ?? "Cancel"
                }
              </button>

              <button
                type="button"
                className={
                  confirmation
                    .options
                    .tone ===
                  "danger"
                    ? "uxDangerButton"
                    : "uxPrimaryButton"
                }
                onClick={() =>
                  finishConfirmation(
                    true
                  )
                }
              >
                {
                  confirmation
                    .options
                    .confirmLabel
                  ?? "Confirm"
                }
              </button>
            </div>
          </section>
        </div>
      )}
    </UxContext.Provider>
  );
}

export function useUx() {
  const context =
    useContext(
      UxContext
    );

  if (!context) {
    throw new Error(
      "useUx must be used inside UxProvider."
    );
  }

  return context;
}
