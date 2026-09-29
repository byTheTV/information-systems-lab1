import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth";

export function Layout() {
  const auth = useAuth();
  const navigate = useNavigate();
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">
          <span className="mark">LW</span>
          <div>
            <strong>LabWork</strong>
            <small>информационная система</small>
          </div>
        </div>
        <nav>
          <NavLink to="/" end>Список работ</NavLink>
          {auth.role === "ADMIN" ? <NavLink to="/labworks/new">Новая работа</NavLink> : null}
          <NavLink to="/special">Специальные операции</NavLink>
        </nav>
        <div className="account">
          <span>{auth.username}</span>
          <small>{auth.role === "ADMIN" ? "администратор" : "только просмотр"}</small>
          <button
            type="button"
            className="ghost"
            onClick={() => {
              void auth.logout().then(() => navigate("/login"));
            }}
          >
            Выйти
          </button>
        </div>
      </aside>
      <main>
        <Outlet />
      </main>
    </div>
  );
}
