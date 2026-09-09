import { UxProvider } from "@/components/ux/UxProvider";
import "./ux.css";
import type { Metadata } from "next";

import { AuthProvider } from "@/components/AuthProvider";

import "./globals.css";
import "./auth.css";

export const metadata: Metadata = {
  title: "BuildFlow",
  description:
    "Construction project management and cost control platform.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <UxProvider>
            {children}
          </UxProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
