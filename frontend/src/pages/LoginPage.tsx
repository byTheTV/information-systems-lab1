import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../api";
import { useAuth } from "../auth";

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin");
  const [error, setError] = useState("");

  return (
    <div className="gate">
      <form onSubmit={(event) => {
        event.preventDefault();
        setError("");
        void auth.login(username, password).then(() => navigate("/")).catch((err: unknown) => {
          setError(err instanceof ApiError ? err.message : "Не удалось войти");
        });
      }}>
        <span className="mark">LW</span>
        <h1>Вход</h1>
        <label className="field">Имя<input value={username} onChange={(event) => setUsername(event.target.value)} /></label>
        <label className="field">Пароль<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        {error ? <p className="error">{error}</p> : null}
        <button type="submit">Войти</button>
        <p>Нет учётной записи? <Link to="/register">Регистрация</Link></p>
        <p className="hint">Администратор: admin / admin. Просмотр: viewer / viewer.</p>
      </form>
    </div>
  );
}
