import { useEffect, useState } from "react";
import { ApiError, api } from "../api";
import type { DeleteInfo } from "../types";
import { RelinkDialog } from "./RelinkDialog";

export function DeleteLabWorkDialog(props: { id: number; name: string; onClose: () => void; onDeleted: () => void }) {
  const [info, setInfo] = useState<DeleteInfo | null>(null);
  const [options, setOptions] = useState<{ id: string; label: string }[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([api.deleteInfo(props.id), api.briefs()])
      .then(([loaded, briefs]) => {
        setInfo(loaded);
        setOptions(briefs.filter((item) => item.id !== props.id).map((item) => ({
          id: String(item.id),
          label: `#${item.id} ${item.name}`,
        })));
      })
      .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Не удалось подготовить удаление"));
  }, [props.id]);

  if (!info) {
    return (
      <RelinkDialog
        title={`Удаление «${props.name}»`}
        message={error || "Проверяем связи…"}
        requiresReplacement={false}
        options={[]}
        error={error}
        onCancel={props.onClose}
        onConfirm={() => undefined}
      />
    );
  }

  return (
    <RelinkDialog
      title={`Удаление «${props.name}»`}
      message={info.message}
      requiresReplacement={info.requiresReplacement}
      options={options}
      busy={busy}
      error={error}
      onCancel={props.onClose}
      onConfirm={(replacementId) => {
        setBusy(true);
        setError("");
        void api.remove(props.id, replacementId ? Number(replacementId) : undefined)
          .then(() => props.onDeleted())
          .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Не удалось удалить"))
          .finally(() => setBusy(false));
      }}
    />
  );
}
