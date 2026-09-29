import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError } from "../api";
import { useAuth } from "../auth";

export function RegisterPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [repeat, setRepeat] = useState("");
  const [error, setError] = useState("");

  return (
    <div className="gate">
      <form onSubmit={(event) => {
        event.preventDefault();
        if (password !== repeat) {
          setError("Пароли не совпадают");
          return;
        }
        setError("");
        void auth.register(username, password).then(() => navigate("/")).catch((err: unknown) => {
          setError(err instanceof ApiError ? err.message : "Не удалось зарегистрироваться");
        });
      }}>
        <span className="mark">LW</span>
        <h1>Регистрация</h1>
        <p className="hint">Новый пользователь получает роль просмотра. Менять объекты может только администратор.</p>
        <label className="field">Имя<input value={username} onChange={(event) => setUsername(event.target.value)} /></label>
        <label className="field">Пароль<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <label className="field">Ещё раз<input type="password" value={repeat} onChange={(event) => setRepeat(event.target.value)} /></label>
        {error ? <p className="error">{error}</p> : null}
        <button type="submit">Создать</button>
        <p>Уже есть вход? <Link to="/login">Войти</Link></p>
      </form>
    </div>
  );
}
