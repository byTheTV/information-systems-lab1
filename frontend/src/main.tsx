import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "@fontsource/manrope/400.css";
import "@fontsource/manrope/600.css";
import "@fontsource/source-serif-4/600.css";
import { App } from "./App";
import { AuthProvider } from "./auth";
import { LiveProvider } from "./live";
import "./styles.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <LiveProvider>
          <App />
        </LiveProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
