import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { api } from "./api";

type AuthState = {
  ready: boolean;
  token: string | null;
  username: string;
  role: string;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

function persist(token: string, username: string, role: string) {
  localStorage.setItem("token", token);
  localStorage.setItem("username", username);
  localStorage.setItem("role", role);
}

function clear() {
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("role");
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [token, setToken] = useState<string | null>(localStorage.getItem("token"));
  const [username, setUsername] = useState(localStorage.getItem("username") ?? "");
  const [role, setRole] = useState(localStorage.getItem("role") ?? "");

  useEffect(() => {
    const stored = localStorage.getItem("token");
    if (!stored) {
      setReady(true);
      return;
    }
    api.me()
      .then((user) => {
        setUsername(user.username);
        setRole(user.role);
        localStorage.setItem("username", user.username);
        localStorage.setItem("role", user.role);
      })
      .catch(() => {
        clear();
        setToken(null);
        setUsername("");
        setRole("");
      })
      .finally(() => setReady(true));
  }, []);

  const value = useMemo<AuthState>(() => ({
    ready,
    token,
    username,
    role,
    async login(name, password) {
      const user = await api.login(name, password);
      if (!user.token) throw new Error("Сервер не выдал токен");
      persist(user.token, user.username, user.role);
      setToken(user.token);
      setUsername(user.username);
      setRole(user.role);
    },
    async register(name, password) {
      const user = await api.register(name, password);
      if (!user.token) throw new Error("Сервер не выдал токен");
      persist(user.token, user.username, user.role);
      setToken(user.token);
      setUsername(user.username);
      setRole(user.role);
    },
    async logout() {
      try {
        await api.logout();
      } finally {
        clear();
        setToken(null);
        setUsername("");
        setRole("");
      }
    },
  }), [ready, token, username, role]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const value = useContext(AuthContext);
  if (!value) throw new Error("AuthProvider отсутствует");
  return value;
}
