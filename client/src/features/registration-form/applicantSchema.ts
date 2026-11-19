import { z } from "zod";

export const applicantSchema = z.object({
  lastName: z.string().min(1, "Renseignez votre nom."),
  birthName: z.string(),
  firstName: z.string().min(1, "Renseignez votre prénom."),
  gender: z.string().min(1, "Sélectionnez une option."),
  birthDate: z.string().min(1, "Renseignez votre date de naissance."),
  nationality: z.string().min(1, "Renseignez votre nationalité."),
  address: z.string().min(1, "Renseignez votre adresse."),
  addressComplement: z.string(),
  postalCode: z
    .string()
    .regex(/^\d{5}$/, "Saisissez un code postal à 5 chiffres."),
  city: z.string().min(1, "Renseignez votre ville."),
  phone: z.string().min(1, "Renseignez votre numéro de téléphone."),
  email: z.string().email("Saisissez une adresse e-mail valide."),
  socialSecurityNumber: z.string(),
  identityDocumentType: z.string().min(1, "Sélectionnez un type de document."),
  identityDocumentNumber: z.string(),
  lastJob: z.string(),
  employer: z.string(),
  jobStartDate: z.string(),
  jobEndDate: z.string(),
  contractType: z.string(),
  highestDiploma: z.string(),
  educationLevel: z.string(),
  hasDrivingLicense: z.boolean(),
  hasVehicle: z.boolean(),
  mobilityRadius: z.string(),
  skills: z.array(z.object({ name: z.string() })),
});

const employmentFields = [
  "lastJob",
  "employer",
  "jobStartDate",
  "jobEndDate",
  "contractType",
] as const;

export function createApplicantSchema(hasEmployerCertificate: boolean) {
  return applicantSchema.superRefine((applicant, context) => {
    if (!hasEmployerCertificate) return;

    employmentFields.forEach((field) => {
      if (!applicant[field].trim()) {
        context.addIssue({
          code: "custom",
          path: [field],
          message:
            "Ce champ est requis lorsque vous ajoutez une attestation employeur.",
        });
      }
    });

    if (
      applicant.jobStartDate &&
      applicant.jobEndDate &&
      applicant.jobEndDate < applicant.jobStartDate
    ) {
      context.addIssue({
        code: "custom",
        path: ["jobEndDate"],
        message: "La date de fin doit être postérieure à la date de début.",
      });
    }
  });
}

export type ApplicantSchemaValues = z.infer<typeof applicantSchema>;
