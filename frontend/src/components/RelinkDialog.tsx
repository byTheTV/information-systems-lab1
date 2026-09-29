import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

export function RelinkDialog(props: {
  title: string;
  message: string;
  requiresReplacement: boolean;
  options: { id: string; label: string }[];
  busy?: boolean;
  error?: string;
  onCancel: () => void;
  onConfirm: (replacementId: string | null) => void;
}) {
  const [replacement, setReplacement] = useState(props.options[0]?.id ?? "");
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") props.onCancel();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [props]);

  const blocked = props.requiresReplacement && props.options.length === 0;
  return createPortal(
    <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) props.onCancel(); }}>
      <div className="dialog narrow" role="dialog" aria-modal="true" aria-labelledby="relink-title">
        <h2 id="relink-title">{props.title}</h2>
        <p>{props.message}</p>
        {props.requiresReplacement ? (
          props.options.length > 0 ? (
            <label className="field">
              <span>Перенести связи на</span>
              <select value={replacement} onChange={(event) => setReplacement(event.target.value)}>
                {props.options.map((option) => (
                  <option key={option.id} value={option.id}>{option.label}</option>
                ))}
              </select>
            </label>
          ) : (
            <p className="error">Нет другого объекта того же типа. Сначала создайте его, затем повторите удаление.</p>
          )
        ) : (
          <p>Связанных объектов нет.</p>
        )}
        {props.error ? <p className="error">{props.error}</p> : null}
        <div className="actions">
          <button type="button" className="ghost" onClick={props.onCancel}>Отмена</button>
          <button
            type="button"
            className="danger"
            disabled={props.busy || blocked || (props.requiresReplacement && !replacement)}
            onClick={() => props.onConfirm(props.requiresReplacement ? replacement : null)}
          >
            Удалить
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
