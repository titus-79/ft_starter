import type { ApplicantFormValues } from "../types/applicant";

export const mockEmploymentDetails = {
  lastJob: "Conseiller clientèle",
  employer: "Atelier & Co",
  jobStartDate: "2021-03-01",
  jobEndDate: "2025-01-31",
  contractType: "CDI",
} satisfies Pick<
  ApplicantFormValues,
  "lastJob" | "employer" | "jobStartDate" | "jobEndDate" | "contractType"
>;

export const mockApplicant: ApplicantFormValues = {
  lastName: "Dupont",
  birthName: "Martin",
  firstName: "Jean",
  gender: "Homme",
  birthDate: "1989-08-15",
  nationality: "Française",
  address: "12 rue des Lilas",
  addressComplement: "Bâtiment B, appartement 14",
  postalCode: "75011",
  city: "Paris",
  phone: "06 12 34 56 78",
  email: "jean.dupont@email.fr",
  socialSecurityNumber: "1 89 08 75 123 456 78",
  identityDocumentType: "Carte nationale d’identité",
  identityDocumentNumber: "12AB34567",
  ...mockEmploymentDetails,
  highestDiploma: "BTS",
  educationLevel: "Bac +2",
  hasDrivingLicense: true,
  hasVehicle: false,
  mobilityRadius: "30",
  degrees: [
    {
      degreeTitle: "BTS",
      graduationYear: "2010",
      degreeLevel: "Bac +2",
    },
  ],
  skills: [
    { name: "Relation client" },
    { name: "Gestion de projet" },
    { name: "Pack Office" },
  ],
};
