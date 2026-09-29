import { useEffect, useState } from "react";
import { ApiError, api } from "../api";
import { useAuth } from "../auth";
import type { Discipline, LabWork, LabWorkBrief } from "../types";
import { dash } from "../types";

export function SpecialPage() {
  const admin = useAuth().role === "ADMIN";
  const [average, setAverage] = useState<string>("");
  const [substring, setSubstring] = useState("алгоритм");
  const [found, setFound] = useState<LabWork[] | null>(null);
  const [points, setPoints] = useState<number[] | null>(null);
  const [briefs, setBriefs] = useState<LabWorkBrief[]>([]);
  const [disciplines, setDisciplines] = useState<Discipline[]>([]);
  const [labWorkId, setLabWorkId] = useState("");
  const [steps, setSteps] = useState("1");
  const [disciplineId, setDisciplineId] = useState("");
  const [decreaseText, setDecreaseText] = useState("");
  const [hardestText, setHardestText] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api.briefs(), api.disciplines()])
      .then(([works, discs]) => {
        setBriefs(works);
        setDisciplines(discs);
        setLabWorkId(works[0] ? String(works[0].id) : "");
        setDisciplineId(discs[0] ? String(discs[0].id) : "");
      })
      .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Не удалось загрузить списки"));
  }, []);

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Специальные операции</h1>
          <p>Каждая операция выполняется функцией PostgreSQL, сервер только вызывает её.</p>
        </div>
      </header>
      {error ? <p className="error">{error}</p> : null}
      <div className="ops">
        <article>
          <h2>Среднее minimalPoint</h2>
          <button type="button" onClick={() => {
            setError("");
            void api.average().then((result) => setAverage(result.average == null ? "нет данных" : String(result.average)))
              .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Ошибка"));
          }}>Рассчитать</button>
          {average ? <p className="result">{average}</p> : null}
        </article>
        <article>
          <h2>Описание содержит подстроку</h2>
          <p className="hint">Это не фильтр таблицы: здесь ищется вхождение, а не полное совпадение.</p>
          <div className="inline">
            <input value={substring} onChange={(event) => setSubstring(event.target.value)} />
            <button type="button" onClick={() => {
              setError("");
              void api.byDescription(substring).then(setFound)
                .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Ошибка"));
            }}>Найти</button>
          </div>
          {found ? (
            found.length === 0 ? <p>Ничего не найдено.</p> : (
              <ul>{found.map((item) => <li key={item.id}>#{item.id} {item.name} — {dash(item.description)}</li>)}</ul>
            )
          ) : null}
        </article>
        <article>
          <h2>Уникальные minimalPoint</h2>
          <button type="button" onClick={() => {
            setError("");
            void api.uniquePoints().then((result) => setPoints(result.values))
              .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Ошибка"));
          }}>Показать</button>
          {points ? <p className="result">{points.length ? points.join(", ") : "нет данных"}</p> : null}
        </article>
        <article>
          <h2>Понизить сложность</h2>
          <p className="hint">Порядок от сложной к простой: TERRIBLE, IMPOSSIBLE, VERY_HARD, HARD, NORMAL. Ниже NORMAL опуститься нельзя, пустая сложность не понижается.</p>
          <div className="inline">
            <select value={labWorkId} onChange={(event) => setLabWorkId(event.target.value)} disabled={!admin}>
              {briefs.map((item) => <option key={item.id} value={item.id}>#{item.id} {item.name} ({item.difficulty ?? "нет"})</option>)}
            </select>
            <input inputMode="numeric" value={steps} onChange={(event) => setSteps(event.target.value)} disabled={!admin} />
            <button type="button" disabled={!admin} onClick={() => {
              setError("");
              setDecreaseText("");
              void api.decrease(Number(labWorkId), Number(steps))
                .then((lab) => setDecreaseText(`Работа #${lab.id}: теперь ${lab.difficulty}`))
                .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Ошибка"));
            }}>Понизить</button>
          </div>
          {!admin ? <p className="hint">Доступно администратору.</p> : null}
          {decreaseText ? <p className="result">{decreaseText}</p> : null}
        </article>
        <article>
          <h2>10 самых сложных в программу</h2>
          <p className="hint">Берутся работы с заполненной сложностью. При равной сложности выше та, у которой больше minimalPoint. Уже входящие в программу повторно не добавляются.</p>
          <div className="inline">
            <select value={disciplineId} onChange={(event) => setDisciplineId(event.target.value)} disabled={!admin}>
              {disciplines.map((item) => <option key={item.id} value={item.id}>{item.name} (#{item.id})</option>)}
            </select>
            <button type="button" disabled={!admin} onClick={() => {
              setError("");
              setHardestText("");
              void api.addHardest(Number(disciplineId))
                .then((result) => setHardestText(`В десятке: ${result.labWorkIds.join(", ") || "—"}. Добавлено новых: ${result.inserted}.`))
                .catch((err: unknown) => setError(err instanceof ApiError ? err.message : "Ошибка"));
            }}>Добавить</button>
          </div>
          {!admin ? <p className="hint">Доступно администратору.</p> : null}
          {hardestText ? <p className="result">{hardestText}</p> : null}
        </article>
      </div>
    </div>
  );
}
