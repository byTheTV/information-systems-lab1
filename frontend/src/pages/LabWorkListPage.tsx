import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api";
import { useAuth } from "../auth";
import { DeleteLabWorkDialog } from "../components/DeleteLabWorkDialog";
import { LabWorkDialog } from "../components/LabWorkDialog";
import { useLive } from "../live";
import type { LabWork } from "../types";
import { dash, formatDate } from "../types";

type Filters = {
  name: string;
  description: string;
  difficulty: string;
  disciplineName: string;
  authorName: string;
};

const emptyFilters: Filters = { name: "", description: "", difficulty: "", disciplineName: "", authorName: "" };
const sorts = ["name", "description", "difficulty", "disciplineName", "authorName"] as const;
type SortKey = (typeof sorts)[number];

const titles: Record<SortKey, string> = {
  name: "Название",
  description: "Описание",
  difficulty: "Сложность",
  disciplineName: "Дисциплина",
  authorName: "Автор",
};

export function LabWorkListPage() {
  const auth = useAuth();
  const admin = auth.role === "ADMIN";
  const [draft, setDraft] = useState<Filters>(emptyFilters);
  const [applied, setApplied] = useState<Filters>(emptyFilters);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const [sort, setSort] = useState<SortKey | "">("");
  const [order, setOrder] = useState<"asc" | "desc">("asc");
  const [items, setItems] = useState<LabWork[]>([]);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<number | null>(null);
  const [deleting, setDeleting] = useState<LabWork | null>(null);

  const load = useCallback(async () => {
    try {
      const result = await api.list({ page, size, ...applied, sort: sort || undefined, order: sort ? order : undefined });
      setItems(result.items);
      setTotal(result.total);
      setError("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось загрузить список");
    } finally {
      setLoading(false);
    }
  }, [page, size, applied, sort, order]);

  useEffect(() => {
    void load();
  }, [load]);

  useLive(() => {
    void load();
  });

  function apply() {
    setPage(0);
    setApplied(draft);
  }

  function Sortable(props: { label: SortKey; sort: SortKey | ""; order: "asc" | "desc"; onSort: (key: SortKey) => void }) {
  return (
    <th>
      <button type="button" className="sort" onClick={() => props.onSort(props.label)}>
        {titles[props.label]}{props.sort === props.label ? (props.order === "asc" ? " ↑" : " ↓") : ""}
      </button>
    </th>
  );
}

function toggleSort(key: SortKey) {
    setPage(0);
    if (sort !== key) {
      setSort(key);
      setOrder("asc");
      return;
    }
    setOrder((current) => (current === "asc" ? "desc" : "asc"));
  }

  const from = total === 0 ? 0 : page * size + 1;
  const to = Math.min(total, page * size + items.length);
  const pages = Math.max(1, Math.ceil(total / size));

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Лабораторные работы</h1>
          <p>Фильтр строковых колонок — полное совпадение, с учётом регистра. Пустое поле фильтр не включает. Сортировка — по заголовку.</p>
        </div>
        <button type="button" className="ghost" onClick={() => { setDraft(emptyFilters); setApplied(emptyFilters); setPage(0); setSort(""); }}>Сбросить фильтр</button>
      </header>
      {!admin ? <p className="note">Режим просмотра. Создавать и менять записи может администратор.</p> : null}
      {error ? <p className="error">{error}</p> : null}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>id</th>
              <Sortable label="name" sort={sort} order={order} onSort={toggleSort} />
              <th>Координаты</th>
              <th>Создана</th>
              <Sortable label="description" sort={sort} order={order} onSort={toggleSort} />
              <Sortable label="difficulty" sort={sort} order={order} onSort={toggleSort} />
              <Sortable label="disciplineName" sort={sort} order={order} onSort={toggleSort} />
              <th>minimalPoint</th>
              <th>tunedInWorks</th>
              <Sortable label="authorName" sort={sort} order={order} onSort={toggleSort} />
              <th></th>
            </tr>
            <tr className="filters">
              <th></th>
              <th><input aria-label="Фильтр названия" value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} onKeyDown={(event) => { if (event.key === "Enter") apply(); }} /></th>
              <th></th>
              <th></th>
              <th><input aria-label="Фильтр описания" value={draft.description} onChange={(event) => setDraft({ ...draft, description: event.target.value })} onKeyDown={(event) => { if (event.key === "Enter") apply(); }} /></th>
              <th>
                <select aria-label="Фильтр сложности" value={draft.difficulty} onChange={(event) => setDraft({ ...draft, difficulty: event.target.value })}>
                  <option value="">все</option>
                  <option>NORMAL</option>
                  <option>HARD</option>
                  <option>VERY_HARD</option>
                  <option>IMPOSSIBLE</option>
                  <option>TERRIBLE</option>
                </select>
              </th>
              <th><input aria-label="Фильтр дисциплины" value={draft.disciplineName} onChange={(event) => setDraft({ ...draft, disciplineName: event.target.value })} onKeyDown={(event) => { if (event.key === "Enter") apply(); }} /></th>
              <th></th>
              <th></th>
              <th><input aria-label="Фильтр автора" value={draft.authorName} onChange={(event) => setDraft({ ...draft, authorName: event.target.value })} onKeyDown={(event) => { if (event.key === "Enter") apply(); }} /></th>
              <th>
                <button type="button" onClick={apply}>Применить</button>
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td><Link to={`/labworks/${item.id}`}>{item.name}</Link></td>
                <td>{item.coordinates.x}; {item.coordinates.y}</td>
                <td>{formatDate(item.creationDate)}</td>
                <td className="clip" title={item.description ?? ""}>{dash(item.description)}</td>
                <td>{dash(item.difficulty)}</td>
                <td>{item.discipline.name}</td>
                <td>{item.minimalPoint}</td>
                <td>{dash(item.tunedInWorks)}</td>
                <td>{dash(item.author?.name)}</td>
                <td className="row-actions">
                  {admin ? <button type="button" className="ghost" onClick={() => setEditing(item.id)}>Изменить</button> : null}
                  {admin ? <button type="button" className="ghost" onClick={() => setDeleting(item)}>Удалить</button> : null}
                </td>
              </tr>
            ))}
            {!loading && items.length === 0 ? (
              <tr><td colSpan={11}>Нет объектов по текущему фильтру.</td></tr>
            ) : null}
          </tbody>
        </table>
      </div>
      <footer className="pager">
        <span>Показаны {from}–{to} из {total}</span>
        <label>
          на странице
          <select value={size} onChange={(event) => { setSize(Number(event.target.value)); setPage(0); }}>
            <option value={5}>5</option>
            <option value={10}>10</option>
            <option value={20}>20</option>
          </select>
        </label>
        <button type="button" className="ghost" disabled={page <= 0} onClick={() => setPage((value) => value - 1)}>Назад</button>
        <span>{page + 1} / {pages}</span>
        <button type="button" className="ghost" disabled={page + 1 >= pages} onClick={() => setPage((value) => value + 1)}>Дальше</button>
      </footer>
      {editing != null ? <LabWorkDialog id={editing} onClose={() => setEditing(null)} onSaved={() => setEditing(null)} /> : null}
      {deleting ? (
        <DeleteLabWorkDialog
          id={deleting.id}
          name={deleting.name}
          onClose={() => setDeleting(null)}
          onDeleted={() => setDeleting(null)}
        />
      ) : null}
    </div>
  );
}
