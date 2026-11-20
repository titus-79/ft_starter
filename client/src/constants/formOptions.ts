export const documentTypes = [
  "Carte nationale d’identité",
  "Passeport",
  "CV",
  "Justificatif de domicile",
  "Relevé d’identité bancaire",
  "Attestation employeur",
  "Attestation de travail",
] as const;

export const identityDocumentOptions = [
  "Carte nationale d’identité",
  "Passeport",
  "Titre de séjour",
] as const;

export const applicantFieldLabels: Record<string, string> = {
  lastName: "Nom",
  birthName: "Nom de naissance",
  firstName: "Prénom",
  gender: "Sexe",
  birthDate: "Date de naissance",
  nationality: "Nationalité",
  address: "Adresse",
  addressComplement: "Complément d'adresse",
  postalCode: "Code postal",
  city: "Ville",
  phone: "Téléphone",
  email: "Adresse e-mail",
  socialSecurityNumber: "Numéro de sécurité sociale",
  identityDocumentType: "Type de document d'identité",
  identityDocumentNumber: "Numéro du document",
  lastJob: "Dernier emploi occupé",
  employer: "Employeur",
  jobStartDate: "Date de début",
  jobEndDate: "Date de fin",
  contractType: "Type de contrat",
  highestDiploma: "Diplôme le plus élevé obtenu",
  educationLevel: "Niveau d'étude",
  hasDrivingLicense: "Permis B",
  hasVehicle: "Véhicule personnel",
  mobilityRadius: "Rayon de mobilité",
  skills: "Compétences",
};

export const uploadAccept = ".pdf,.jpg,.jpeg,.png";
export const uploadExtensions = ["pdf", "jpg", "jpeg", "png"];
export const maxFileSize = 20 * 1024 * 1024;
export const maxFileCount = 20;
