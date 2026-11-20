import { useCallback, useState } from "react";
import { useWatch } from "react-hook-form";
import { Stepper } from "../components/Stepper";
import { SummaryCard } from "../components/SummaryCard";
import { RegistrationForm } from "../features/registration-form/RegistrationForm";
import { UploadSection } from "../features/upload/UploadSection";
import { useApplicantForm } from "../hooks/useApplicantForm";
import type { ApplicantFormValues } from "../types/applicant";
import { Icon } from "../components/Icon";
import type { ApplicantExtractionResponse } from "../services/api";
import {
  hasEmployerCertificate as extractionHasEmployerCertificate,
  mapExtractionToApplicantForm,
} from "../services/applicantMapper";

export function RegistrationPage() {
  const [detectedEmployerCertificate, setDetectedEmployerCertificate] =
    useState(false);
  const hasEmployerCertificate = detectedEmployerCertificate;
  const form = useApplicantForm(hasEmployerCertificate);
  const [injectedFields, setInjectedFields] = useState<
    Set<keyof ApplicantFormValues>
  >(new Set());
  const [message, setMessage] = useState("");
  const [activeStep, setActiveStep] = useState(0);
  useWatch({ control: form.control });
  const values = form.getValues();

  const handleAnalysisResult = useCallback(
    (result: ApplicantExtractionResponse) => {
      const patch = mapExtractionToApplicantForm(result);
      form.reset({ ...form.getValues(), ...patch });
      setInjectedFields(
        (current) =>
          new Set([
            ...current,
            ...(Object.keys(patch) as (keyof ApplicantFormValues)[]),
          ]),
      );
      setDetectedEmployerCertificate(extractionHasEmployerCertificate(result));
      setActiveStep(1);
      setMessage(
        Object.keys(patch).length
          ? `${Object.keys(patch).length} champ(s) ont été pré-rempli(s) à partir des documents. Vérifiez-les avant de continuer.`
          : "L’analyse est terminée, mais aucune information n’a pu être extraite. Vous pouvez compléter le formulaire manuellement.",
      );
    },
    [form],
  );

  function handleSubmit() {
    setActiveStep(2);
    setMessage(
      "Les informations sont vérifiées dans le formulaire. L’API actuelle ne propose pas encore de route pour soumettre le dossier.",
    );
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function clearInjected(field: keyof ApplicantFormValues) {
    setInjectedFields((current) => {
      const next = new Set(current);
      next.delete(field);
      return next;
    });
  }

  return (
    <div className="content-wrap">
      <Stepper activeStep={activeStep} />
      {message && (
        <div className="notice-success" role="status">
          <span className="notice-check">
            <Icon name="check" />
          </span>
          <p>{message}</p>
          <button
            type="button"
            className="icon-button"
            aria-label="Fermer le message"
            onClick={() => setMessage("")}
          >
            <Icon name="close" />
          </button>
        </div>
      )}
      <div className="content-grid">
        <div className="main-column">
          <UploadSection onAnalysisResult={handleAnalysisResult} />
          <RegistrationForm
            form={form}
            injectedFields={injectedFields}
            onSubmit={handleSubmit}
            onClearInjected={clearInjected}
            hasEmployerCertificate={hasEmployerCertificate}
          />
        </div>
        <div className="side-column">
          <SummaryCard
            values={values}
            completion={form.completion}
            loading={false}
          />
        </div>
      </div>
      <p className="footer-note">
        Un doute sur une information ? Vous pourrez la modifier à tout moment
        avec un conseiller.
      </p>
    </div>
  );
}
