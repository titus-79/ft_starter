import type { documentTypes } from "../constants/formOptions";

export type ApplicantFormValues = {
  lastName: string;
  birthName: string;
  firstName: string;
  gender: string;
  birthDate: string;
  nationality: string;
  address: string;
  addressComplement: string;
  postalCode: string;
  city: string;
  phone: string;
  email: string;
  socialSecurityNumber: string;
  identityDocumentType: string;
  identityDocumentNumber: string;
  lastJob: string;
  employer: string;
  jobStartDate: string;
  jobEndDate: string;
  contractType: string;
  highestDiploma: string;
  educationLevel: string;
  hasDrivingLicense: boolean;
  hasVehicle: boolean;
  mobilityRadius: string;
  degrees: {
    degreeTitle: string;
    graduationYear: string;
    degreeLevel: string;
  }[];
  skills: { name: string }[];
};

export type ApplicantField = keyof ApplicantFormValues;
export type DocumentType = (typeof documentTypes)[number];

export type UploadedDocument = {
  id: string;
  name: string;
  size: number;
  type: string;
  file: File;
  status: "uploading" | "success" | "error";
  progress: number;
  documentType: DocumentType | "";
  uploadId?: string;
  errorMessage?: string;
};
