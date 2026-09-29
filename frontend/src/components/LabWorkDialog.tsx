import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { ApiError, api } from "../api";
import { useCatalogs } from "../catalogs";
import { useLive } from "../live";
import type { LabWork, LabWorkInput } from "../types";
import { LabWorkForm } from "./LabWorkForm";

export function LabWorkDialog(props: { id: number; onClose: () => void; onSaved: () => void }) {
  const catalogs = useCatalogs();
  const [lab, setLab] = useState<LabWork | null>(null);
  const [error, setError] = useState("");
  const [banner, setBanner] = useState("");

  useEffect(() => {
    api.get(props.id).then(setLab).catch((err: unknown) => {
      setError(err instanceof ApiError ? err.message : "Не удалось открыть запись");
    });
  }, [props.id]);

  useLive((event) => {
    if (event.action === "DELETE" && event.id === props.id) props.onClose();
    else if (event.action === "RELOAD" || event.id === props.id) {
      setBanner("Запись изменилась у другого пользователя. Если сохранить форму, она перезапишет эти изменения.");
    }
  });

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") props.onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [props]);

  return createPortal(
    <div className="overlay">
      <div className="dialog wide" role="dialog" aria-modal="true" aria-labelledby="edit-title">
        <h2 id="edit-title">Изменение работы #{props.id}</h2>
        {banner ? <p className="note">{banner}</p> : null}
        {error ? <p className="error">{error}</p> : null}
        {lab && catalogs.data ? (
          <LabWorkForm
            initial={lab}
            catalogs={catalogs.data}
            onCatalogsChanged={catalogs.reload}
            onCancel={props.onClose}
            onSubmit={async (body: LabWorkInput) => {
              await api.update(props.id, body);
              props.onSaved();
            }}
          />
        ) : !error ? <p>Загрузка…</p> : null}
        {catalogs.error ? <p className="error">{catalogs.error}</p> : null}
      </div>
    </div>,
    document.body,
  );
}
