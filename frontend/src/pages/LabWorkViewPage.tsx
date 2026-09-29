import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, api } from "../api";
import { useAuth } from "../auth";
import { DeleteLabWorkDialog } from "../components/DeleteLabWorkDialog";
import { LabWorkDialog } from "../components/LabWorkDialog";
import { useLive } from "../live";
import type { LabWork } from "../types";
import { dash, formatDate } from "../types";

export function LabWorkViewPage() {
  const params = useParams();
  const id = Number(params.id);
  const auth = useAuth();
  const navigate = useNavigate();
  const admin = auth.role === "ADMIN";
  const [lab, setLab] = useState<LabWork | null>(null);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    if (!Number.isInteger(id)) {
      setError("Некорректный идентификатор");
      return;
    }
    try {
      setLab(await api.get(id));
      setError("");
    } catch (err) {
      setLab(null);
      setError(err instanceof ApiError ? err.message : "Не удалось открыть запись");
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  useLive((event) => {
    if (event.action === "DELETE" && event.id === id) {
      setLab(null);
      setError("Объект удалён");
      setEditing(false);
      return;
    }
    if (event.action === "RELOAD" || event.id === id) void load();
  });

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>{lab ? lab.name : "Карточка объекта"}</h1>
          <p>Работа, связанные координаты, дисциплина, автор и программы, в которые она входит.</p>
        </div>
        <div className="actions">
          <Link className="button ghost" to="/">К списку</Link>
          {admin && lab ? <button type="button" onClick={() => setEditing(true)}>Изменить</button> : null}
          {admin && lab ? <button type="button" className="danger" onClick={() => setDeleting(true)}>Удалить</button> : null}
        </div>
      </header>
      {error ? <p className="error">{error}</p> : null}
      {lab ? (
        <div className="cards">
          <article>
            <h2>Работа</h2>
            <dl>
              <div><dt>id</dt><dd>{lab.id}</dd></div>
              <div><dt>Название</dt><dd>{lab.name}</dd></div>
              <div><dt>Создана</dt><dd>{formatDate(lab.creationDate)}</dd></div>
              <div><dt>Описание</dt><dd>{dash(lab.description)}</dd></div>
              <div><dt>Сложность</dt><dd>{dash(lab.difficulty)}</dd></div>
              <div><dt>minimalPoint</dt><dd>{lab.minimalPoint}</dd></div>
              <div><dt>tunedInWorks</dt><dd>{dash(lab.tunedInWorks)}</dd></div>
            </dl>
          </article>
          <article>
            <h2>Координаты</h2>
            <dl>
              <div><dt>id</dt><dd>{lab.coordinates.id}</dd></div>
              <div><dt>x</dt><dd>{lab.coordinates.x}</dd></div>
              <div><dt>y</dt><dd>{lab.coordinates.y}</dd></div>
            </dl>
          </article>
          <article>
            <h2>Дисциплина</h2>
            <dl>
              <div><dt>id</dt><dd>{lab.discipline.id}</dd></div>
              <div><dt>Название</dt><dd>{lab.discipline.name}</dd></div>
              <div><dt>Лекции</dt><dd>{lab.discipline.lectureHours}</dd></div>
              <div><dt>Самостоятельная работа</dt><dd>{lab.discipline.selfStudyHours}</dd></div>
              <div><dt>Лабораторные</dt><dd>{lab.discipline.labsCount}</dd></div>
            </dl>
          </article>
          <article>
            <h2>Автор</h2>
            {lab.author ? (
              <dl>
                <div><dt>id</dt><dd>{lab.author.id}</dd></div>
                <div><dt>Имя</dt><dd>{lab.author.name}</dd></div>
                <div><dt>Цвет глаз</dt><dd>{dash(lab.author.eyeColor)}</dd></div>
                <div><dt>Цвет волос</dt><dd>{lab.author.hairColor}</dd></div>
                <div><dt>Вес</dt><dd>{dash(lab.author.weight)}</dd></div>
                <div><dt>Страна</dt><dd>{dash(lab.author.nationality)}</dd></div>
              </dl>
            ) : <p>Автор не указан.</p>}
          </article>
          <article>
            <h2>Локация автора</h2>
            {lab.author?.location ? (
              <dl>
                <div><dt>id</dt><dd>{lab.author.location.id}</dd></div>
                <div><dt>x</dt><dd>{lab.author.location.x}</dd></div>
                <div><dt>y</dt><dd>{lab.author.location.y}</dd></div>
                <div><dt>z</dt><dd>{lab.author.location.z}</dd></div>
              </dl>
            ) : <p>Локация не указана.</p>}
          </article>
          <article>
            <h2>В программах дисциплин</h2>
            {lab.programs && lab.programs.length > 0 ? (
              <ul>{lab.programs.map((item) => <li key={item.id}>{item.name} (#{item.id})</li>)}</ul>
            ) : <p>Ни в одну программу не входит.</p>}
          </article>
        </div>
      ) : null}
      {editing ? <LabWorkDialog id={id} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); void load(); }} /> : null}
      {deleting && lab ? (
        <DeleteLabWorkDialog id={lab.id} name={lab.name} onClose={() => setDeleting(false)} onDeleted={() => navigate("/")} />
      ) : null}
    </div>
  );
}
