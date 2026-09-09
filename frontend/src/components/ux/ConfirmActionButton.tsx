"use client";

import type {
  ButtonHTMLAttributes,
} from "react";

import {
  useState,
} from "react";

import {
  useUx,
} from "@/components/ux/UxProvider";

type ConfirmActionButtonProps =
  Omit<
    ButtonHTMLAttributes<HTMLButtonElement>,
    "onClick"
  > & {
    confirmationTitle: string;
    confirmationMessage: string;
    confirmLabel?: string;
    cancelLabel?: string;
    danger?: boolean;
    onConfirm:
      () =>
        void
        | Promise<void>;
  };

export function ConfirmActionButton({
  confirmationTitle,
  confirmationMessage,
  confirmLabel,
  cancelLabel,
  danger = false,
  onConfirm,
  disabled,
  children,
  ...button
}: ConfirmActionButtonProps) {
  const {
    confirm,
  } =
    useUx();

  const [busy, setBusy] =
    useState(false);

  async function activate() {
    const approved =
      await confirm({
        title:
          confirmationTitle,
        message:
          confirmationMessage,
        confirmLabel,
        cancelLabel,
        tone:
          danger
            ? "danger"
            : "default",
      });

    if (!approved) {
      return;
    }

    try {
      setBusy(true);

      await onConfirm();
    } finally {
      setBusy(false);
    }
  }

  return (
    <button
      {...button}
      type={
        button.type
        ?? "button"
      }
      disabled={
        disabled
        || busy
      }
      onClick={() =>
        void activate()
      }
    >
      {busy
        ? "Working..."
        : children}
    </button>
  );
}
