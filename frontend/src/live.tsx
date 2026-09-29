import { createContext, useContext, useEffect, useMemo, useRef } from "react";
import type { ReactNode } from "react";
import { useAuth } from "./auth";

export type LiveEvent = { type: string; action: string; id: number | null };

type LiveApi = {
  subscribe: (handler: (event: LiveEvent) => void) => () => void;
};

const LiveContext = createContext<LiveApi | null>(null);

export function LiveProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const handlers = useRef(new Set<(event: LiveEvent) => void>());

  useEffect(() => {
    if (!token) return;
    let socket: WebSocket | null = null;
    let stopped = false;
    let reconnect = 0;
    const connect = () => {
      if (stopped) return;
      const protocol = window.location.protocol === "https:" ? "wss" : "ws";
      socket = new WebSocket(`${protocol}://${window.location.host}/ws/updates?token=${encodeURIComponent(token)}`);
      socket.onmessage = (message) => {
        if (typeof message.data !== "string" || message.data === "pong") return;
        try {
          const event = JSON.parse(message.data) as LiveEvent;
          handlers.current.forEach((handler) => handler(event));
        } catch {
          /* служебное сообщение */
        }
      };
      socket.onclose = () => {
        if (!stopped) reconnect = window.setTimeout(connect, 3000);
      };
    };
    connect();
    const ping = window.setInterval(() => {
      if (socket?.readyState === WebSocket.OPEN) socket.send("ping");
    }, 25000);
    return () => {
      stopped = true;
      window.clearTimeout(reconnect);
      window.clearInterval(ping);
      socket?.close();
    };
  }, [token]);

  const value = useMemo<LiveApi>(() => ({
    subscribe(handler) {
      handlers.current.add(handler);
      return () => handlers.current.delete(handler);
    },
  }), []);

  return <LiveContext.Provider value={value}>{children}</LiveContext.Provider>;
}

export function useLive(handler: (event: LiveEvent) => void) {
  const live = useContext(LiveContext);
  const current = useRef(handler);
  current.current = handler;
  useEffect(() => {
    if (!live) return;
    return live.subscribe((event) => current.current(event));
  }, [live]);
}
