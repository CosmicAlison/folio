import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";

type Theme = "light" | "dark";

function initialTheme(): Theme {
  const current = document.documentElement.dataset.theme;
  if (current === "light" || current === "dark") return current;
  try {
    const saved = localStorage.getItem("folio-theme");
    if (saved === "light" || saved === "dark") return saved;
  } catch {
    /* Storage can be disabled; themes still work for this visit. */
  }
  return window.matchMedia?.("(prefers-color-scheme: light)").matches
    ? "light"
    : "dark";
}

export default function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(initialTheme);
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
  }, [theme]);
  function select(next: Theme) {
    setTheme(next);
    try {
      localStorage.setItem("folio-theme", next);
    } catch {
      /* Optional persistence. */
    }
  }
  return (
    <div className="theme-toggle" role="group" aria-label="Appearance">
      <button
        type="button"
        aria-label="Light theme"
        aria-pressed={theme === "light"}
        onClick={() => select("light")}
      >
        <Sun size={16} />
        <span>Light</span>
      </button>
      <button
        type="button"
        aria-label="Dark theme"
        aria-pressed={theme === "dark"}
        onClick={() => select("dark")}
      >
        <Moon size={16} />
        <span>Dark</span>
      </button>
    </div>
  );
}
