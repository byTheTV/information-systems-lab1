import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { useCatalogs } from "../catalogs";
import { LabWorkForm } from "../components/LabWorkForm";

export function LabWorkCreatePage() {
  const navigate = useNavigate();
  const catalogs = useCatalogs();
  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Новая лабораторная работа</h1>
          <p>Дата создания и id назначаются на сервере. Координаты, дисциплину и автора можно выбрать из уже существующих или создать здесь же.</p>
        </div>
      </header>
      {catalogs.error ? <p className="error">{catalogs.error}</p> : null}
      {catalogs.data ? (
        <LabWorkForm
          catalogs={catalogs.data}
          onCatalogsChanged={catalogs.reload}
          onCancel={() => navigate("/")}
          onSubmit={async (body) => {
            await api.create(body);
            navigate("/");
          }}
        />
      ) : !catalogs.error ? <p>Загрузка справочников…</p> : null}
    </div>
  );
}
