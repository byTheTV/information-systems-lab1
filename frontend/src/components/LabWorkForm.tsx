import { useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { ApiError, api } from "../api";
import type { CatalogKind, Catalogs, CoordinatesInput, DisciplineInput, LabWork, LabWorkInput, PersonInput } from "../types";
import { COLORS, COUNTRIES, DIFFICULTIES } from "../types";
import { RelinkDialog } from "./RelinkDialog";

type Mode = "existing" | "new";
type AuthorMode = "none" | "existing" | "new";
type LocMode = "none" | "existing" | "new";

type FormState = {
  name: string;
  description: string;
  difficulty: string;
  minimalPoint: string;
  tunedInWorks: string;
  coordMode: Mode;
  coordId: string;
  coordX: string;
  coordY: string;
  coordDirty: boolean;
  discMode: Mode;
  discId: string;
  discName: string;
  lectureHours: string;
  selfStudyHours: string;
  labsCount: string;
  discDirty: boolean;
  authorMode: AuthorMode;
  authorId: string;
  authorName: string;
  eyeColor: string;
  hairColor: string;
  weight: string;
  nationality: string;
  authorDirty: boolean;
  locMode: LocMode;
  locId: string;
  locX: string;
  locY: string;
  locZ: string;
  locDirty: boolean;
};

type Pending = { kind: CatalogKind; id: number; message: string; requiresReplacement: boolean } | null;

function integer(value: string): number | null {
  const text = value.trim();
  if (!/^-?\d+$/.test(text)) return null;
  const parsed = Number(text);
  return Number.isSafeInteger(parsed) ? parsed : null;
}

function decimal(value: string): number | null {
  const text = value.trim().replace(",", ".");
  if (!text) return null;
  const parsed = Number(text);
  return Number.isFinite(parsed) ? parsed : null;
}

function fromLabWork(lab: LabWork): FormState {
  return {
    name: lab.name,
    description: lab.description ?? "",
    difficulty: lab.difficulty ?? "",
    minimalPoint: String(lab.minimalPoint),
    tunedInWorks: lab.tunedInWorks == null ? "" : String(lab.tunedInWorks),
    coordMode: "existing",
    coordId: String(lab.coordinates.id),
    coordX: String(lab.coordinates.x),
    coordY: String(lab.coordinates.y),
    coordDirty: false,
    discMode: "existing",
    discId: String(lab.discipline.id),
    discName: lab.discipline.name,
    lectureHours: String(lab.discipline.lectureHours),
    selfStudyHours: String(lab.discipline.selfStudyHours),
    labsCount: String(lab.discipline.labsCount),
    discDirty: false,
    authorMode: lab.author ? "existing" : "none",
    authorId: lab.author ? String(lab.author.id) : "",
    authorName: lab.author?.name ?? "",
    eyeColor: lab.author?.eyeColor ?? "",
    hairColor: lab.author?.hairColor ?? "",
    weight: lab.author?.weight == null ? "" : String(lab.author.weight),
    nationality: lab.author?.nationality ?? "",
    authorDirty: false,
    locMode: lab.author?.location ? "existing" : "none",
    locId: lab.author?.location ? String(lab.author.location.id) : "",
    locX: lab.author?.location ? String(lab.author.location.x) : "",
    locY: lab.author?.location ? String(lab.author.location.y) : "",
    locZ: lab.author?.location ? String(lab.author.location.z) : "",
    locDirty: false,
  };
}

const emptyState: FormState = {
  name: "",
  description: "",
  difficulty: "",
  minimalPoint: "",
  tunedInWorks: "",
  coordMode: "new",
  coordId: "",
  coordX: "",
  coordY: "",
  coordDirty: true,
  discMode: "new",
  discId: "",
  discName: "",
  lectureHours: "",
  selfStudyHours: "",
  labsCount: "",
  discDirty: true,
  authorMode: "none",
  authorId: "",
  authorName: "",
  eyeColor: "",
  hairColor: "",
  weight: "",
  nationality: "",
  authorDirty: false,
  locMode: "none",
  locId: "",
  locX: "",
  locY: "",
  locZ: "",
  locDirty: false,
};

function build(state: FormState): { body?: LabWorkInput; errors: Record<string, string> } {
  const errors: Record<string, string> = {};
  const name = state.name.trim();
  if (!name) errors.name = "Название не может быть пустым";
  if (state.description.length > 2960) errors.description = "Длина описания не больше 2960";
  const minimalPoint = integer(state.minimalPoint);
  if (minimalPoint === null || minimalPoint <= 0) errors.minimalPoint = "minimalPoint должен быть целым числом больше 0";
  let tuned: number | null = null;
  if (state.tunedInWorks.trim()) {
    const parsed = integer(state.tunedInWorks);
    if (parsed === null) errors.tunedInWorks = "tunedInWorks должно быть целым числом";
    else tuned = parsed;
  }

  let coordinates: CoordinatesInput | undefined;
  if (state.coordMode === "existing" && !state.coordDirty) {
    if (!state.coordId) errors.coordinates = "Выберите координаты";
    else coordinates = { id: Number(state.coordId) };
  } else {
    const x = integer(state.coordX);
    const y = decimal(state.coordY);
    if (x === null) errors["coordinates.x"] = "Укажите целое x";
    else if (x > 139) errors["coordinates.x"] = "Максимальное значение x — 139";
    if (y === null) errors["coordinates.y"] = "Укажите число y";
    if (x !== null && y !== null && x <= 139) {
      coordinates = state.coordMode === "existing" ? { id: Number(state.coordId), x, y } : { x, y };
    }
  }

  let discipline: DisciplineInput | undefined;
  if (state.discMode === "existing" && !state.discDirty) {
    if (!state.discId) errors.discipline = "Выберите дисциплину";
    else discipline = { id: Number(state.discId) };
  } else {
    const lectureHours = integer(state.lectureHours);
    const selfStudyHours = integer(state.selfStudyHours);
    const labsCount = integer(state.labsCount);
    if (!state.discName.trim()) errors["discipline.name"] = "Название дисциплины не может быть пустым";
    if (lectureHours === null) errors["discipline.lectureHours"] = "Укажите целое число лекционных часов";
    if (selfStudyHours === null) errors["discipline.selfStudyHours"] = "Укажите целое число часов самостоятельной работы";
    if (labsCount === null) errors["discipline.labsCount"] = "Укажите целое число лабораторных";
    if (state.discName.trim() && lectureHours !== null && selfStudyHours !== null && labsCount !== null) {
      discipline = {
        ...(state.discMode === "existing" ? { id: Number(state.discId) } : {}),
        name: state.discName.trim(),
        lectureHours,
        selfStudyHours,
        labsCount,
      };
    }
  }

  let author: PersonInput | null | undefined = null;
  if (state.authorMode === "none") author = null;
  else if (state.authorMode === "existing" && !state.authorDirty) {
    if (!state.authorId) errors.author = "Выберите автора";
    else author = { id: Number(state.authorId) };
  } else {
    if (!state.authorName.trim()) errors["author.name"] = "Имя автора не может быть пустым";
    if (!state.hairColor) errors["author.hairColor"] = "Цвет волос обязателен";
    let weight: number | null = null;
    if (state.weight.trim()) {
      const parsed = integer(state.weight);
      if (parsed === null || parsed <= 0) errors["author.weight"] = "Вес должен быть целым числом больше 0";
      else weight = parsed;
    }
    let location: PersonInput["location"] = null;
    if (state.locMode === "none") location = null;
    else if (state.locMode === "existing" && !state.locDirty) {
      if (!state.locId) errors["author.location"] = "Выберите локацию";
      else location = { id: Number(state.locId) };
    } else {
      const x = integer(state.locX);
      const y = integer(state.locY);
      const z = decimal(state.locZ);
      if (x === null) errors["author.location.x"] = "Укажите целое x";
      if (y === null) errors["author.location.y"] = "Укажите целое y";
      if (z === null) errors["author.location.z"] = "Укажите число z";
      if (x !== null && y !== null && z !== null) {
        location = state.locMode === "existing" ? { id: Number(state.locId), x, y, z } : { x, y, z };
      }
    }
    const personInvalid = !state.authorName.trim() || !state.hairColor || errors["author.weight"] || errors["author.location"]
      || errors["author.location.x"] || errors["author.location.y"] || errors["author.location.z"];
    if (!personInvalid && location !== undefined) {
      author = {
        ...(state.authorMode === "existing" ? { id: Number(state.authorId) } : {}),
        name: state.authorName.trim(),
        eyeColor: state.eyeColor || null,
        hairColor: state.hairColor,
        weight,
        nationality: state.nationality || null,
        location,
      };
    }
  }

  if (!coordinates || !discipline || author === undefined || Object.keys(errors).length > 0 || minimalPoint === null || minimalPoint <= 0) {
    return { errors };
  }
  return {
    errors,
    body: {
      name,
      description: state.description.trim() ? state.description : null,
      difficulty: state.difficulty || null,
      minimalPoint,
      tunedInWorks: tuned,
      coordinates,
      discipline,
      author,
    },
  };
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {error ? <small>{error}</small> : null}
    </label>
  );
}

export function LabWorkForm(props: {
  initial?: LabWork;
  catalogs: Catalogs;
  onCatalogsChanged: () => Promise<void> | void;
  onSubmit: (body: LabWorkInput) => Promise<void>;
  onCancel: () => void;
}) {
  const [state, setState] = useState<FormState>(props.initial ? fromLabWork(props.initial) : emptyState);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState<Pending>(null);
  const [deleteError, setDeleteError] = useState("");

  function patch(partial: Partial<FormState>) {
    setState((current) => ({ ...current, ...partial }));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const result = build(state);
    setErrors(result.errors);
    setMessage("");
    if (!result.body) return;
    setBusy(true);
    try {
      await props.onSubmit(result.body);
    } catch (err) {
      if (err instanceof ApiError) {
        setMessage(err.message);
        setErrors(err.fields ?? {});
      } else {
        setMessage("Не удалось сохранить");
      }
    } finally {
      setBusy(false);
    }
  }

  async function askDelete(kind: CatalogKind, id: string) {
    if (!id) return;
    setDeleteError("");
    try {
      const info = await api.catalogInfo(kind, Number(id));
      setPending({ kind, id: Number(id), message: info.message, requiresReplacement: info.requiresReplacement });
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Не удалось проверить связи");
    }
  }

  function optionsFor(kind: CatalogKind, id: number) {
    if (kind === "coordinates") {
      return props.catalogs.coordinates.filter((item) => item.id !== id)
        .map((item) => ({ id: String(item.id), label: `#${item.id}: x=${item.x}; y=${item.y}` }));
    }
    if (kind === "disciplines") {
      return props.catalogs.disciplines.filter((item) => item.id !== id)
        .map((item) => ({ id: String(item.id), label: `${item.name} (#${item.id})` }));
    }
    if (kind === "persons") {
      return props.catalogs.persons.filter((item) => item.id !== id)
        .map((item) => ({ id: String(item.id), label: `${item.name} (#${item.id})` }));
    }
    return props.catalogs.locations.filter((item) => item.id !== id)
      .map((item) => ({ id: String(item.id), label: `#${item.id}: (${item.x}, ${item.y}, ${item.z})` }));
  }

  function retarget(current: FormState, kind: CatalogKind, deleted: string, replacement: string): FormState {
    if (kind === "coordinates" && current.coordId === deleted) {
      const item = props.catalogs.coordinates.find((entry) => String(entry.id) === replacement);
      if (!item) return { ...current, coordMode: "new", coordId: "", coordDirty: true };
      return { ...current, coordMode: "existing", coordId: replacement, coordX: String(item.x), coordY: String(item.y), coordDirty: false };
    }
    if (kind === "disciplines" && current.discId === deleted) {
      const item = props.catalogs.disciplines.find((entry) => String(entry.id) === replacement);
      if (!item) return { ...current, discMode: "new", discId: "", discDirty: true };
      return {
        ...current,
        discMode: "existing",
        discId: replacement,
        discName: item.name,
        lectureHours: String(item.lectureHours),
        selfStudyHours: String(item.selfStudyHours),
        labsCount: String(item.labsCount),
        discDirty: false,
      };
    }
    if (kind === "persons" && current.authorId === deleted) {
      const item = props.catalogs.persons.find((entry) => String(entry.id) === replacement);
      if (!item) return { ...current, authorMode: "new", authorId: "", authorDirty: true };
      return {
        ...current,
        authorMode: "existing",
        authorId: replacement,
        authorName: item.name,
        eyeColor: item.eyeColor ?? "",
        hairColor: item.hairColor,
        weight: item.weight == null ? "" : String(item.weight),
        nationality: item.nationality ?? "",
        authorDirty: false,
        locMode: item.location ? "existing" : "none",
        locId: item.location ? String(item.location.id) : "",
        locX: item.location ? String(item.location.x) : "",
        locY: item.location ? String(item.location.y) : "",
        locZ: item.location ? String(item.location.z) : "",
        locDirty: false,
      };
    }
    if (kind === "locations" && current.locId === deleted) {
      const item = props.catalogs.locations.find((entry) => String(entry.id) === replacement);
      if (!item) return { ...current, locMode: "new", locId: "", locDirty: true };
      return { ...current, locMode: "existing", locId: replacement, locX: String(item.x), locY: String(item.y), locZ: String(item.z), locDirty: false };
    }
    return current;
  }

  return (
    <form onSubmit={(event) => { void submit(event); }}>
      {message ? <p className="error">{message}</p> : null}
      <div className="grid">
        <Field label="Название" error={errors.name}>
          <input autoFocus value={state.name} onChange={(event) => patch({ name: event.target.value })} />
        </Field>
        <Field label="minimalPoint (> 0)" error={errors.minimalPoint}>
          <input inputMode="numeric" value={state.minimalPoint} onChange={(event) => patch({ minimalPoint: event.target.value })} />
        </Field>
        <Field label="tunedInWorks" error={errors.tunedInWorks}>
          <input inputMode="numeric" value={state.tunedInWorks} onChange={(event) => patch({ tunedInWorks: event.target.value })} />
        </Field>
        <Field label="Сложность" error={errors.difficulty}>
          <select value={state.difficulty} onChange={(event) => patch({ difficulty: event.target.value })}>
            <option value="">не задана</option>
            {DIFFICULTIES.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </Field>
        <Field label="Описание (до 2960)" error={errors.description}>
          <textarea maxLength={2960} rows={3} value={state.description} onChange={(event) => patch({ description: event.target.value })} />
        </Field>
      </div>

      <section>
        <header>
          <h3>Координаты</h3>
          <ModeSwitch value={state.coordMode} onChange={(coordMode) => patch({ coordMode, coordDirty: coordMode === "new" })} />
        </header>
        {state.coordMode === "existing" ? (
          <div className="inline">
            <select value={state.coordId} onChange={(event) => {
              const item = props.catalogs.coordinates.find((entry) => String(entry.id) === event.target.value);
              patch({ coordId: event.target.value, coordDirty: false, coordX: item ? String(item.x) : "", coordY: item ? String(item.y) : "" });
            }}>
              <option value="">выберите</option>
              {props.catalogs.coordinates.map((item) => (
                <option key={item.id} value={item.id}>#{item.id}: x={item.x}; y={item.y}</option>
              ))}
            </select>
            <button type="button" className="ghost" disabled={!state.coordId} onClick={() => { void askDelete("coordinates", state.coordId); }}>Удалить выбранные</button>
          </div>
        ) : null}
        <div className="grid">
          <Field label="x (не больше 139)" error={errors["coordinates.x"]}>
            <input inputMode="numeric" value={state.coordX} onChange={(event) => patch({ coordX: event.target.value, coordDirty: true })} />
          </Field>
          <Field label="y" error={errors["coordinates.y"]}>
            <input inputMode="decimal" value={state.coordY} onChange={(event) => patch({ coordY: event.target.value, coordDirty: true })} />
          </Field>
        </div>
        {errors.coordinates ? <p className="error">{errors.coordinates}</p> : null}
        {state.coordMode === "existing" && state.coordDirty ? <p className="hint">Если эти координаты используются ещё где-то, для этой работы будет создана копия.</p> : null}
      </section>

      <section>
        <header>
          <h3>Дисциплина</h3>
          <ModeSwitch value={state.discMode} onChange={(discMode) => patch({ discMode, discDirty: discMode === "new" })} />
        </header>
        {state.discMode === "existing" ? (
          <div className="inline">
            <select value={state.discId} onChange={(event) => {
              const item = props.catalogs.disciplines.find((entry) => String(entry.id) === event.target.value);
              patch({
                discId: event.target.value,
                discDirty: false,
                discName: item?.name ?? "",
                lectureHours: item ? String(item.lectureHours) : "",
                selfStudyHours: item ? String(item.selfStudyHours) : "",
                labsCount: item ? String(item.labsCount) : "",
              });
            }}>
              <option value="">выберите</option>
              {props.catalogs.disciplines.map((item) => <option key={item.id} value={item.id}>{item.name} (#{item.id})</option>)}
            </select>
            <button type="button" className="ghost" disabled={!state.discId} onClick={() => { void askDelete("disciplines", state.discId); }}>Удалить выбранную</button>
          </div>
        ) : null}
        <div className="grid">
          <Field label="Название" error={errors["discipline.name"]}>
            <input value={state.discName} onChange={(event) => patch({ discName: event.target.value, discDirty: true })} />
          </Field>
          <Field label="Лекционные часы" error={errors["discipline.lectureHours"]}>
            <input inputMode="numeric" value={state.lectureHours} onChange={(event) => patch({ lectureHours: event.target.value, discDirty: true })} />
          </Field>
          <Field label="Самостоятельная работа" error={errors["discipline.selfStudyHours"]}>
            <input inputMode="numeric" value={state.selfStudyHours} onChange={(event) => patch({ selfStudyHours: event.target.value, discDirty: true })} />
          </Field>
          <Field label="Число лабораторных" error={errors["discipline.labsCount"]}>
            <input inputMode="numeric" value={state.labsCount} onChange={(event) => patch({ labsCount: event.target.value, discDirty: true })} />
          </Field>
        </div>
        {errors.discipline ? <p className="error">{errors.discipline}</p> : null}
        {state.discMode === "existing" && state.discDirty ? <p className="hint">Если дисциплину используют другие работы, изменится только связь этой работы: будет создана копия.</p> : null}
      </section>

      <section>
        <header>
          <h3>Автор</h3>
          <div className="switch">
            {(["none", "existing", "new"] as AuthorMode[]).map((mode) => (
              <button type="button" key={mode} className={state.authorMode === mode ? "on" : ""} onClick={() => patch({ authorMode: mode, authorDirty: mode === "new" })}>
                {mode === "none" ? "нет" : mode === "existing" ? "существующий" : "новый"}
              </button>
            ))}
          </div>
        </header>
        {state.authorMode === "existing" ? (
          <div className="inline">
            <select value={state.authorId} onChange={(event) => {
              const item = props.catalogs.persons.find((entry) => String(entry.id) === event.target.value);
              patch({
                authorId: event.target.value,
                authorDirty: false,
                authorName: item?.name ?? "",
                eyeColor: item?.eyeColor ?? "",
                hairColor: item?.hairColor ?? "",
                weight: item?.weight == null ? "" : String(item.weight),
                nationality: item?.nationality ?? "",
                locMode: item?.location ? "existing" : "none",
                locId: item?.location ? String(item.location.id) : "",
                locX: item?.location ? String(item.location.x) : "",
                locY: item?.location ? String(item.location.y) : "",
                locZ: item?.location ? String(item.location.z) : "",
                locDirty: false,
              });
            }}>
              <option value="">выберите</option>
              {props.catalogs.persons.map((item) => <option key={item.id} value={item.id}>{item.name} (#{item.id})</option>)}
            </select>
            <button type="button" className="ghost" disabled={!state.authorId} onClick={() => { void askDelete("persons", state.authorId); }}>Удалить выбранного</button>
          </div>
        ) : null}
        {state.authorMode !== "none" ? (
          <>
            <div className="grid">
              <Field label="Имя" error={errors["author.name"]}>
                <input value={state.authorName} onChange={(event) => patch({ authorName: event.target.value, authorDirty: true })} />
              </Field>
              <Field label="Цвет глаз" error={errors["author.eyeColor"]}>
                <select value={state.eyeColor} onChange={(event) => patch({ eyeColor: event.target.value, authorDirty: true })}>
                  <option value="">не задан</option>
                  {COLORS.map((item) => <option key={item}>{item}</option>)}
                </select>
              </Field>
              <Field label="Цвет волос" error={errors["author.hairColor"]}>
                <select value={state.hairColor} onChange={(event) => patch({ hairColor: event.target.value, authorDirty: true })}>
                  <option value="">выберите</option>
                  {COLORS.map((item) => <option key={item}>{item}</option>)}
                </select>
              </Field>
              <Field label="Вес" error={errors["author.weight"]}>
                <input inputMode="numeric" value={state.weight} onChange={(event) => patch({ weight: event.target.value, authorDirty: true })} />
              </Field>
              <Field label="Страна" error={errors["author.nationality"]}>
                <select value={state.nationality} onChange={(event) => patch({ nationality: event.target.value, authorDirty: true })}>
                  <option value="">не задана</option>
                  {COUNTRIES.map((item) => <option key={item}>{item}</option>)}
                </select>
              </Field>
            </div>
            <header>
              <h3>Локация автора</h3>
              <div className="switch">
                {(["none", "existing", "new"] as LocMode[]).map((mode) => (
                  <button type="button" key={mode} className={state.locMode === mode ? "on" : ""} onClick={() => patch({ locMode: mode, locDirty: mode === "new", authorDirty: true })}>
                    {mode === "none" ? "нет" : mode === "existing" ? "существующая" : "новая"}
                  </button>
                ))}
              </div>
            </header>
            {state.locMode === "existing" ? (
              <div className="inline">
                <select value={state.locId} onChange={(event) => {
                  const item = props.catalogs.locations.find((entry) => String(entry.id) === event.target.value);
                  patch({ locId: event.target.value, locDirty: false, authorDirty: true, locX: item ? String(item.x) : "", locY: item ? String(item.y) : "", locZ: item ? String(item.z) : "" });
                }}>
                  <option value="">выберите</option>
                  {props.catalogs.locations.map((item) => (
                    <option key={item.id} value={item.id}>#{item.id}: ({item.x}, {item.y}, {item.z})</option>
                  ))}
                </select>
                <button type="button" className="ghost" disabled={!state.locId} onClick={() => { void askDelete("locations", state.locId); }}>Удалить выбранную</button>
              </div>
            ) : null}
            {state.locMode !== "none" ? (
              <div className="grid">
                <Field label="x" error={errors["author.location.x"]}>
                  <input inputMode="numeric" value={state.locX} onChange={(event) => patch({ locX: event.target.value, locDirty: true, authorDirty: true })} />
                </Field>
                <Field label="y" error={errors["author.location.y"]}>
                  <input inputMode="numeric" value={state.locY} onChange={(event) => patch({ locY: event.target.value, locDirty: true, authorDirty: true })} />
                </Field>
                <Field label="z" error={errors["author.location.z"]}>
                  <input inputMode="decimal" value={state.locZ} onChange={(event) => patch({ locZ: event.target.value, locDirty: true, authorDirty: true })} />
                </Field>
              </div>
            ) : null}
            {errors["author.location"] ? <p className="error">{errors["author.location"]}</p> : null}
          </>
        ) : null}
        {errors.author ? <p className="error">{errors.author}</p> : null}
      </section>

      <div className="actions">
        <button type="button" className="ghost" onClick={props.onCancel}>Отмена</button>
        <button type="submit" disabled={busy}>{props.initial ? "Сохранить" : "Создать"}</button>
      </div>

      {pending ? (
        <RelinkDialog
          title="Удаление связанного объекта"
          message={pending.message}
          requiresReplacement={pending.requiresReplacement}
          options={optionsFor(pending.kind, pending.id)}
          busy={busy}
          error={deleteError}
          onCancel={() => { setPending(null); setDeleteError(""); }}
          onConfirm={(replacementId) => {
            setBusy(true);
            setDeleteError("");
            void api.catalogRemove(pending.kind, pending.id, replacementId ? Number(replacementId) : undefined)
              .then(async () => {
                setState((current) => retarget(current, pending.kind, String(pending.id), replacementId ?? ""));
                setPending(null);
                await props.onCatalogsChanged();
              })
              .catch((err: unknown) => {
                setDeleteError(err instanceof ApiError ? err.message : "Не удалось удалить");
              })
              .finally(() => setBusy(false));
          }}
        />
      ) : null}
    </form>
  );
}

function ModeSwitch({ value, onChange }: { value: Mode; onChange: (mode: Mode) => void }) {
  return (
    <div className="switch">
      <button type="button" className={value === "existing" ? "on" : ""} onClick={() => onChange("existing")}>существующие</button>
      <button type="button" className={value === "new" ? "on" : ""} onClick={() => onChange("new")}>новые</button>
    </div>
  );
}
