import { useState } from "react";
import { BookOpen, ArrowUpRight } from "lucide-react";
import { useAuth } from "../context/AuthContext";
export default function AuthPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const { login, register } = useAuth();
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      if (isLogin) await login(username.trim(), password);
      else await register(username.trim(), email.trim(), password);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Please try again.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="auth-shell">
      <section className="auth-story">
        <a className="brand" href="/">
          <span className="brand-icon">
            <BookOpen size={23} />
          </span>
          folio<span className="brand-dot">.</span>
        </a>
        <h1>
          Good ideas
          <br />
          deserve a home.
        </h1>
        <p>
          A private place for your writing. And a thoughtful assistant to help
          you find what connects it.
        </p>
      </section>
      <section className="auth-form-area">
        <div className="auth-card">
          <h2>{isLogin ? "Welcome back." : "Make yourself at home."}</h2>
          <p>
            {isLogin
              ? "Your next idea is waiting for you."
              : "Start your own collection of thoughts."}
          </p>
          <div className="auth-tabs">
            <button
              className={isLogin ? "selected" : ""}
              disabled={busy}
              onClick={() => {
                setIsLogin(true);
                setError("");
              }}
            >
              Sign in
            </button>
            <button
              className={!isLogin ? "selected" : ""}
              disabled={busy}
              onClick={() => {
                setIsLogin(false);
                setError("");
              }}
            >
              Create account
            </button>
          </div>
          <form onSubmit={submit}>
            {!isLogin && (
              <>
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </>
            )}
            <label htmlFor="username">Username</label>
            <input
              id="username"
              autoComplete="username"
              required
              minLength={3}
              maxLength={50}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete={isLogin ? "current-password" : "new-password"}
              required
              maxLength={72}
              minLength={isLogin ? 1 : 8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {error && (
              <p className="error" role="alert">
                {error}
              </p>
            )}
            <button className="primary" disabled={busy}>
              {busy
                ? "One moment…"
                : isLogin
                  ? "Open your workspace"
                  : "Create your workspace"}
              <ArrowUpRight size={18} />
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}
