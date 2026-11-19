import type { ReactNode } from "react";
import { Icon } from "../components/Icon";

export function MainLayout({
  darkMode,
  onToggleTheme,
  children,
}: {
  darkMode: boolean;
  onToggleTheme: () => void;
  children: ReactNode;
}) {
  return (
    <div className={`app-shell${darkMode ? " dark-mode" : ""}`}>
      <header className="site-header">
        <div className="header-inner">
          <a className="brand" href="#top" aria-label="France Travail, accueil">
            <span className="brand-mark" aria-hidden="true">
              <i />
              <i />
              <i />
              <i />
            </span>
            <span className="brand-name">
              france<span>travail</span>
            </span>
          </a>
          <div className="header-actions">
            <span className="demo-pill">
              <span /> Démonstration
            </span>
            <button
              className="theme-toggle"
              type="button"
              onClick={onToggleTheme}
              aria-label={
                darkMode ? "Activer le thème clair" : "Activer le thème sombre"
              }
            >
              <Icon name={darkMode ? "sun" : "moon"} />
              <span>{darkMode ? "Mode clair" : "Mode sombre"}</span>
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
