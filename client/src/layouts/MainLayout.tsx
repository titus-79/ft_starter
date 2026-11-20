import type { ReactNode } from "react";
import { Icon } from "../components/Icon";

export function MainLayout({
  theme,
  onCycleTheme,
  children,
}: {
  theme: "light" | "dark" | "rainbow";
  onCycleTheme: () => void;
  children: ReactNode;
}) {
  const nextThemeLabel =
    theme === "light"
      ? "Activer le mode sombre"
      : theme === "dark"
        ? "Activer le mode arc-en-ciel"
        : "Revenir au mode clair";
  const themeLabel =
    theme === "light"
      ? "Mode sombre"
      : theme === "dark"
        ? "Mode arc-en-ciel"
        : "Mode clair";
  const themeIcon =
    theme === "light" ? "moon" : theme === "dark" ? "rainbow" : "sun";

  return (
    <div className={`app-shell${theme === "light" ? "" : ` ${theme}-mode`}`}>
      <header className="site-header">
        <div className="header-inner">
          <a className="brand" href="#top" aria-label="France Travail, accueil">
            <img className="brand-logo" src="/france-travail-logo.svg" alt="" />
          </a>
          <div className="header-actions">
            <span className="demo-pill">
              <span /> Démonstration
            </span>
            <button
              className="theme-toggle"
              type="button"
              onClick={onCycleTheme}
              aria-label={nextThemeLabel}
            >
              <Icon name={themeIcon} />
              <span>{themeLabel}</span>
            </button>
          </div>
        </div>
      </header>
      <main id="top">
        <section className="hero-section">
          <div className="hero-inner">
            <p className="eyebrow">VOTRE INSCRIPTION, EN TOUTE SIMPLICITÉ</p>
            <h1>
              Pré-remplissez votre inscription
              <br className="desktop-break" /> France Travail
            </h1>
            <p className="hero-description">
              Déposez vos documents justificatifs afin de pré-remplir
              automatiquement votre dossier d'inscription.
            </p>
            <p className="hero-disclaimer">
              Démonstration · Les documents déposés sont transmis au backend
              local
            </p>
          </div>
        </section>
        {children}
      </main>
      <footer className="site-footer">
        <div className="footer-inner">
          <span>© France Travail · Maquette de démonstration</span>
          <a
            href="https://www.francetravail.fr/candidat/vos-services-en-ligne.html"
            target="_blank"
            rel="noreferrer"
          >
            France Travail <span className="external-indicator">↗</span>
          </a>
        </div>
      </footer>
    </div>
  );
}
