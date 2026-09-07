"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ApiClientError,
  apiGet,
  apiPost,
} from "@/lib/api";

import type {
  AuthResponse,
  OrganizationSummary,
  User,
} from "@/lib/types";

interface LoginInput {
  email: string;
  password: string;
}

interface RegisterInput {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

interface AuthContextValue {
  user: User | null;
  organizations: OrganizationSummary[];
  loading: boolean;
  login: (
    input: LoginInput
  ) => Promise<void>;
  register: (
    input: RegisterInput
  ) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext =
  createContext<AuthContextValue | null>(
    null
  );

export function AuthProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [user, setUser] =
    useState<User | null>(null);

  const [
    organizations,
    setOrganizations,
  ] = useState<OrganizationSummary[]>([]);

  const [loading, setLoading] =
    useState(true);

  const applyAuth = useCallback(
    (response: AuthResponse) => {
      setUser(response.user);
      setOrganizations(
        response.organizations
      );
    },
    []
  );

  const refresh =
    useCallback(async () => {
      try {
        const response =
          await apiGet<AuthResponse>(
            "/auth/me"
          );

        applyAuth(response);
      } catch (error) {
        if (
          error instanceof ApiClientError &&
          error.status === 401
        ) {
          setUser(null);
          setOrganizations([]);
          return;
        }

        throw error;
      }
    }, [applyAuth]);

  useEffect(() => {
    refresh()
      .catch(() => {
        setUser(null);
        setOrganizations([]);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [refresh]);

  const login =
    useCallback(
      async (input: LoginInput) => {
        const response =
          await apiPost<AuthResponse>(
            "/auth/login",
            input
          );

        applyAuth(response);
      },
      [applyAuth]
    );

  const register =
    useCallback(
      async (
        input: RegisterInput
      ) => {
        const response =
          await apiPost<AuthResponse>(
            "/auth/register",
            input
          );

        applyAuth(response);
      },
      [applyAuth]
    );

  const logout =
    useCallback(async () => {
      try {
        await apiPost<void>(
          "/auth/logout"
        );
      } finally {
        setUser(null);
        setOrganizations([]);
      }
    }, []);

  const value =
    useMemo<AuthContextValue>(
      () => ({
        user,
        organizations,
        loading,
        login,
        register,
        logout,
        refresh,
      }),
      [
        user,
        organizations,
        loading,
        login,
        register,
        logout,
        refresh,
      ]
    );

  return (
    <AuthContext.Provider
      value={value}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context =
    useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside AuthProvider."
    );
  }

  return context;
}
