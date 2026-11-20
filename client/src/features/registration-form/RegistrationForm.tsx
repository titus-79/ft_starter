import { useState } from "react";
import { useFieldArray, useWatch, type UseFormReturn } from "react-hook-form";
import { Icon } from "../../components/Icon";
import { Field } from "../../components/Field";
import { identityDocumentOptions } from "../../constants/formOptions";
import type { ApplicantFormValues } from "../../types/applicant";

type Props = {
  form: UseFormReturn<ApplicantFormValues>;
  injectedFields: Set<keyof ApplicantFormValues>;
  onSubmit: (values: ApplicantFormValues) => void;
  onClearInjected: (field: keyof ApplicantFormValues) => void;
  hasEmployerCertificate: boolean;
};

export function RegistrationForm({
  form,
  injectedFields,
  onSubmit,
  onClearInjected,
  hasEmployerCertificate,
}: Props) {
  const {
    register,
    control,
    formState: { errors },
  } = form;
  const { fields, append, remove } = useFieldArray({ control, name: "skills" });
  const {
    fields: degreeFields,
    append: appendDegree,
    remove: removeDegree,
  } = useFieldArray({ control, name: "degrees" });
  const watchedDegrees = useWatch({ control, name: "degrees" }) ?? [];
  const sortedDegreeFields = degreeFields
    .map((field, index) => ({
      field,
      index,
      year: Number(watchedDegrees[index]?.graduationYear),
    }))
    .sort((left, right) => {
      const leftHasYear = Number.isInteger(left.year) && left.year > 0;
      const rightHasYear = Number.isInteger(right.year) && right.year > 0;
      if (leftHasYear !== rightHasYear) return leftHasYear ? -1 : 1;
      if (leftHasYear && left.year !== right.year)
        return right.year - left.year;
      return left.index - right.index;
    });
  const [skill, setSkill] = useState("");

  function inputClass(field: keyof ApplicantFormValues, hasError: boolean) {
    return `${injectedFields.has(field) ? "is-prefilled " : ""}${hasError ? "invalid" : ""}`.trim();
  }

  function clearInjected(field: keyof ApplicantFormValues) {
    if (injectedFields.has(field)) onClearInjected(field);
  }

  return (
    <form
      className="card registration-card"
      onSubmit={form.handleSubmit(onSubmit)}
      noValidate
    >
      <div className="section-heading form-heading">
        <div>
          <div className="eyebrow">ÉTAPE 2 · VÉRIFICATION</div>
          <h2>Vérifiez vos informations</h2>
          <p>Vérifiez les informations, puis complétez les champs manquants.</p>
        </div>
      </div>
      {injectedFields.size > 0 && (
        <div className="prefill-legend" role="status">
          <span className="legend-dot" /> Les champs surlignés ont été
          pré-remplis. Vérifiez-les avant de continuer.
        </div>
      )}

      <fieldset className="form-fields">
        <section className="form-section" aria-labelledby="civil-title">
          <h3 id="civil-title">État civil</h3>
          <div className="field-grid">
            <Field
              id="firstName"
              label="Prénom"
              required
              error={errors.firstName}
            >
              <input
                id="firstName"
                autoComplete="given-name"
                className={inputClass("firstName", !!errors.firstName)}
                {...register("firstName", {
                  onChange: () => clearInjected("firstName"),
                })}
              />
            </Field>
            <Field id="lastName" label="Nom" required error={errors.lastName}>
              <input
                id="lastName"
                autoComplete="family-name"
                className={inputClass("lastName", !!errors.lastName)}
                {...register("lastName", {
                  onChange: () => clearInjected("lastName"),
                })}
              />
            </Field>
            <Field id="birthName" label="Nom de naissance">
              <input
                id="birthName"
                className={inputClass("birthName", false)}
                {...register("birthName", {
                  onChange: () => clearInjected("birthName"),
                })}
              />
            </Field>
            <Field id="gender" label="Sexe" required error={errors.gender}>
              <select
                id="gender"
                className={inputClass("gender", !!errors.gender)}
                {...register("gender", {
                  onChange: () => clearInjected("gender"),
                })}
              >
                <option value="">Sélectionnez</option>
                <option>Femme</option>
                <option>Homme</option>
                <option>Ne souhaite pas répondre</option>
              </select>
            </Field>
            <Field
              id="birthDate"
              label="Date de naissance"
              required
              error={errors.birthDate}
            >
              <input
                id="birthDate"
                type="date"
                autoComplete="bday"
                className={inputClass("birthDate", !!errors.birthDate)}
                {...register("birthDate", {
                  onChange: () => clearInjected("birthDate"),
                })}
              />
            </Field>
            <Field
              id="nationality"
              label="Nationalité"
              required
              error={errors.nationality}
            >
              <input
                id="nationality"
                className={inputClass("nationality", !!errors.nationality)}
                {...register("nationality", {
                  onChange: () => clearInjected("nationality"),
                })}
              />
            </Field>
          </div>
        </section>

        <section className="form-section" aria-labelledby="contact-title">
          <h3 id="contact-title">Coordonnées</h3>
          <div className="field-grid">
            <Field id="address" label="Adresse" required error={errors.address}>
              <input
                id="address"
                autoComplete="street-address"
                className={inputClass("address", !!errors.address)}
                {...register("address", {
                  onChange: () => clearInjected("address"),
                })}
              />
            </Field>
            <Field id="addressComplement" label="Complément d’adresse">
              <input
                id="addressComplement"
                className={inputClass("addressComplement", false)}
                {...register("addressComplement", {
                  onChange: () => clearInjected("addressComplement"),
                })}
              />
            </Field>
            <Field
              id="postalCode"
              label="Code postal"
              required
              error={errors.postalCode}
            >
              <input
                id="postalCode"
                autoComplete="postal-code"
                inputMode="numeric"
                className={inputClass("postalCode", !!errors.postalCode)}
                {...register("postalCode", {
                  onChange: () => clearInjected("postalCode"),
                })}
              />
            </Field>
            <Field id="city" label="Ville" required error={errors.city}>
              <input
                id="city"
                autoComplete="address-level2"
                className={inputClass("city", !!errors.city)}
                {...register("city", { onChange: () => clearInjected("city") })}
              />
            </Field>
            <Field id="phone" label="Téléphone" required error={errors.phone}>
              <input
                id="phone"
                type="tel"
                autoComplete="tel"
                className={inputClass("phone", !!errors.phone)}
                {...register("phone", {
                  onChange: () => clearInjected("phone"),
                })}
              />
            </Field>
            <Field
              id="email"
              label="Adresse e-mail"
              required
              error={errors.email}
            >
              <input
                id="email"
                type="email"
                autoComplete="email"
                className={inputClass("email", !!errors.email)}
                {...register("email", {
                  onChange: () => clearInjected("email"),
                })}
              />
            </Field>
          </div>
        </section>

        <section className="form-section" aria-labelledby="admin-title">
          <h3 id="admin-title">Situation administrative</h3>
          <div className="field-grid">
            <Field
              id="socialSecurityNumber"
              label="Numéro de sécurité sociale"
              hint="15 chiffres, si vous le connaissez."
            >
              <input
                id="socialSecurityNumber"
                inputMode="numeric"
                className={inputClass("socialSecurityNumber", false)}
                {...register("socialSecurityNumber", {
                  onChange: () => clearInjected("socialSecurityNumber"),
                })}
              />
            </Field>
            <Field
              id="identityDocumentType"
              label="Type de document d’identité"
              required
              error={errors.identityDocumentType}
            >
              <select
                id="identityDocumentType"
                className={inputClass(
                  "identityDocumentType",
                  !!errors.identityDocumentType,
                )}
                {...register("identityDocumentType", {
                  onChange: () => clearInjected("identityDocumentType"),
                })}
              >
                <option value="">Sélectionnez</option>
                {identityDocumentOptions.map((option) => (
                  <option key={option}>{option}</option>
                ))}
              </select>
            </Field>
            <Field id="identityDocumentNumber" label="Numéro du document">
              <input
                id="identityDocumentNumber"
                className={inputClass("identityDocumentNumber", false)}
                {...register("identityDocumentNumber", {
                  onChange: () => clearInjected("identityDocumentNumber"),
                })}
              />
            </Field>
          </div>
        </section>

        <section className="form-section" aria-labelledby="job-title">
          <h3 id="job-title">Situation professionnelle</h3>
          {hasEmployerCertificate && (
            <p
              className="section-intro employment-certificate-note"
              role="status"
            >
              Les informations de l’attestation ont été pré-remplies à titre de
              démonstration. Vérifiez-les avant de valider votre dossier.
            </p>
          )}
          <div className="field-grid">
            <Field
              id="lastJob"
              label="Dernier emploi occupé"
              required={hasEmployerCertificate}
              error={errors.lastJob}
            >
              <input
                id="lastJob"
                className={inputClass("lastJob", false)}
                {...register("lastJob", {
                  onChange: () => clearInjected("lastJob"),
                })}
              />
            </Field>
            <Field
              id="employer"
              label="Employeur"
              required={hasEmployerCertificate}
              error={errors.employer}
            >
              <input
                id="employer"
                className={inputClass("employer", false)}
                {...register("employer", {
                  onChange: () => clearInjected("employer"),
                })}
              />
            </Field>
            <Field
              id="jobStartDate"
              label="Date de début"
              required={hasEmployerCertificate}
              error={errors.jobStartDate}
            >
              <input
                id="jobStartDate"
                type="date"
                className={inputClass("jobStartDate", false)}
                {...register("jobStartDate", {
                  onChange: () => clearInjected("jobStartDate"),
                })}
              />
            </Field>
            <Field
              id="jobEndDate"
              label="Date de fin"
              required={hasEmployerCertificate}
              error={errors.jobEndDate}
            >
              <input
                id="jobEndDate"
                type="date"
                className={inputClass("jobEndDate", false)}
                {...register("jobEndDate", {
                  onChange: () => clearInjected("jobEndDate"),
                })}
              />
            </Field>
            <Field
              id="contractType"
              label="Type de contrat"
              required={hasEmployerCertificate}
              error={errors.contractType}
            >
              <select
                id="contractType"
                className={inputClass("contractType", false)}
                {...register("contractType", {
                  onChange: () => clearInjected("contractType"),
                })}
              >
                <option value="">Sélectionnez</option>
                <option>CDI</option>
                <option>CDD</option>
                <option>Intérim</option>
                <option>Indépendant</option>
                <option>Autre</option>
              </select>
            </Field>
          </div>
        </section>

        <section className="form-section" aria-labelledby="training-title">
          <h3 id="training-title">Formation</h3>
          <div className="field-grid">
            <Field id="highestDiploma" label="Diplôme le plus élevé (synthèse)">
              <input
                id="highestDiploma"
                list="highest-diploma-options"
                className={inputClass("highestDiploma", false)}
                {...register("highestDiploma", {
                  onChange: () => clearInjected("highestDiploma"),
                })}
                placeholder="Ex. Master Informatique, parcours Génie logiciel"
              />
              <datalist id="highest-diploma-options">
                <option value="Sans diplôme" />
                <option value="CAP / BEP" />
                <option value="Baccalauréat" />
                <option value="BTS" />
                <option value="Licence" />
                <option value="Master" />
                <option value="Doctorat" />
              </datalist>
            </Field>
            <Field id="educationLevel" label="Niveau du diplôme principal">
              <input
                id="educationLevel"
                list="education-level-options"
                className={inputClass("educationLevel", false)}
                {...register("educationLevel", {
                  onChange: () => clearInjected("educationLevel"),
                })}
                placeholder="Ex. Bac+5"
              />
              <datalist id="education-level-options">
                <option value="Sans diplôme" />
                <option value="CAP / BEP" />
                <option value="Bac" />
                <option value="Bac +2" />
                <option value="Bac +3" />
                <option value="Bac +5 ou plus" />
              </datalist>
            </Field>
          </div>
          <div className="degrees-list">
            <div>
              <h4>Tous les diplômes</h4>
              <p className="section-intro">
                Sans limite de nombre, classés de l’année d’obtention la plus
                récente à la plus ancienne.
              </p>
            </div>
            {sortedDegreeFields.length === 0 && (
              <p className="not-provided">Aucun diplôme renseigné.</p>
            )}
            {sortedDegreeFields.map(({ field, index }, position) => (
              <div
                className="degree-entry"
                key={field.id}
                role="group"
                aria-label={`Diplôme ${position + 1}`}
              >
                <div className="degree-fields">
                  <Field
                    id={`degree-title-${field.id}`}
                    label="Intitulé du diplôme"
                  >
                    <input
                      id={`degree-title-${field.id}`}
                      className={inputClass("degrees", false)}
                      {...register(`degrees.${index}.degreeTitle`, {
                        onChange: () => clearInjected("degrees"),
                      })}
                    />
                  </Field>
                  <Field
                    id={`degree-year-${field.id}`}
                    label="Année d’obtention"
                  >
                    <input
                      id={`degree-year-${field.id}`}
                      type="number"
                      min="1900"
                      max="2100"
                      inputMode="numeric"
                      className={inputClass("degrees", false)}
                      {...register(`degrees.${index}.graduationYear`, {
                        onChange: () => clearInjected("degrees"),
                      })}
                    />
                  </Field>
                  <Field
                    id={`degree-level-${field.id}`}
                    label="Niveau du diplôme"
                  >
                    <input
                      id={`degree-level-${field.id}`}
                      list="degree-level-options"
                      className={inputClass("degrees", false)}
                      {...register(`degrees.${index}.degreeLevel`, {
                        onChange: () => clearInjected("degrees"),
                      })}
                    />
                  </Field>
                  <button
                    type="button"
                    className="icon-button degree-remove"
                    aria-label={`Supprimer le diplôme ${position + 1}`}
                    onClick={() => removeDegree(index)}
                  >
                    <Icon name="trash" />
                  </button>
                </div>
              </div>
            ))}
            <datalist id="degree-level-options">
              <option value="Bac+5" />
              <option value="Bac+3" />
              <option value="Bac+2" />
              <option value="Bac" />
              <option value="CAP / BEP" />
            </datalist>
            <button
              type="button"
              className="button button-secondary degree-add"
              onClick={() =>
                appendDegree({
                  degreeTitle: "",
                  graduationYear: "",
                  degreeLevel: "",
                })
              }
            >
              <Icon name="plus" /> Ajouter un diplôme
            </button>
          </div>
        </section>

        <section className="form-section" aria-labelledby="mobility-title">
          <h3 id="mobility-title">Mobilité</h3>
          <div className="field-grid mobility-grid">
            <Field id="hasDrivingLicense" label="Permis B">
              <div className="switch-row">
                <input
                  id="hasDrivingLicense"
                  type="checkbox"
                  className="check-input"
                  {...register("hasDrivingLicense")}
                />
                <span>Je possède le permis B</span>
              </div>
            </Field>
            <Field id="hasVehicle" label="Véhicule personnel">
              <div className="switch-row">
                <input
                  id="hasVehicle"
                  type="checkbox"
                  className="check-input"
                  {...register("hasVehicle")}
                />
                <span>Je dispose d’un véhicule</span>
              </div>
            </Field>
            <Field id="mobilityRadius" label="Rayon de mobilité">
              <div className="input-suffix">
                <input
                  id="mobilityRadius"
                  type="number"
                  min="0"
                  max="500"
                  className={inputClass("mobilityRadius", false)}
                  {...register("mobilityRadius", {
                    onChange: () => clearInjected("mobilityRadius"),
                  })}
                />
                <span>km</span>
              </div>
            </Field>
          </div>
        </section>

        <section
          className="form-section skills-section"
          aria-labelledby="skills-title"
        >
          <h3 id="skills-title">Compétences</h3>
          <p className="section-intro">
            Ajoutez les compétences que vous souhaitez mettre en avant.
          </p>
          <div className="skill-add">
            <label className="visually-hidden" htmlFor="new-skill">
              Nouvelle compétence
            </label>
            <input
              id="new-skill"
              value={skill}
              onChange={(event) => setSkill(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  if (skill.trim()) {
                    append({ name: skill.trim() });
                    setSkill("");
                  }
                }
              }}
              placeholder="Ex. Gestion de projet"
            />
            <button
              type="button"
              className="button button-secondary"
              onClick={() => {
                if (skill.trim()) {
                  append({ name: skill.trim() });
                  setSkill("");
                }
              }}
            >
              <Icon name="plus" /> Ajouter
            </button>
          </div>
          {fields.length > 0 && (
            <ul className="skill-list">
              {fields.map((field, index) => (
                <li key={field.id}>
                  <input
                    aria-label={`Compétence ${index + 1}`}
                    {...register(`skills.${index}.name`)}
                  />
                  <button
                    type="button"
                    className="icon-button"
                    aria-label={`Supprimer la compétence ${index + 1}`}
                    onClick={() => remove(index)}
                  >
                    <Icon name="close" />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <div className="form-footer">
          <span className="required-note">
            <span className="required-mark">*</span> Champs obligatoires
          </span>
          <button className="button button-primary submit-button" type="submit">
            Vérifier mon dossier <Icon name="arrow" />
          </button>
        </div>
      </fieldset>
    </form>
  );
}
