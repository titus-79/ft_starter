import { applicantFieldLabels } from "../constants/formOptions";
import { Icon } from "./Icon";
import type { ApplicantFormValues } from "../types/applicant";

export function SummaryCard({
  values,
  completion,
  loading = false,
}: {
  values: ApplicantFormValues;
  completion: number;
  loading?: boolean;
}) {
  const summary = [
    {
      key: "firstName",
      value: [values.firstName, values.lastName].filter(Boolean).join(" "),
    },
    { key: "email", value: values.email },
    { key: "phone", value: values.phone },
    {
      key: "city",
      value: [values.postalCode, values.city].filter(Boolean).join(" "),
    },
  ];
  const completeCount = summary.filter((item) => item.value).length;
  return (
    <aside
      className="card summary-card"
      aria-labelledby="summary-title"
      aria-busy={loading}
    >
      <div className="summary-title-row">
        <h2 id="summary-title">Votre dossier</h2>
        <span className="summary-icon">
          <Icon name="file" />
        </span>
      </div>
      <div className="completion-label">
        <span>Complétion du formulaire</span>
        <strong>{loading ? "…" : `${completion}%`}</strong>
      </div>
      <div
        className="completion-track"
        role="progressbar"
        aria-label="Complétion du formulaire"
        aria-valuenow={completion}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <span style={{ width: `${completion}%` }} />
      </div>
      <p className="summary-hint">
        {completeCount} information{completeCount > 1 ? "s" : ""} de contact sur
        4 renseignée{completeCount > 1 ? "s" : ""}
      </p>
      <div className="summary-divider" />
      <h3>Vos coordonnées</h3>
      <dl className="summary-list">
        {summary.map((item) => (
          <div key={item.key}>
            <dt>{applicantFieldLabels[item.key]}</dt>
            <dd>
              {loading ? (
                <span className="skeleton-line" aria-label="Chargement" />
              ) : (
                item.value || <span className="not-provided">À compléter</span>
              )}
            </dd>
          </div>
        ))}
      </dl>
      <div className="privacy-note">
        <Icon name="info" />
        <p>
          Vos informations restent dans cette démonstration et ne sont envoyées
          à aucun serveur.
        </p>
      </div>
    </aside>
  );
}
