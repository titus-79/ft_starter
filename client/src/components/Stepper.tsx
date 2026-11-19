import { Icon } from "./Icon";

const steps = ["Dépôt des documents", "Vérification des données", "Validation"];

export function Stepper({ activeStep }: { activeStep: number }) {
  return (
    <nav className="stepper" aria-label="Étapes de votre inscription">
      {steps.map((step, index) => {
        const status =
          index < activeStep
            ? "complete"
            : index === activeStep
              ? "active"
              : "";
        return (
          <div
            className={`step ${status}`}
            key={step}
            aria-current={index === activeStep ? "step" : undefined}
          >
            <span className="step-marker">
              {index < activeStep ? <Icon name="check" /> : `0${index + 1}`}
            </span>
            <span className="step-name">{step}</span>
          </div>
        );
      })}
    </nav>
  );
}
