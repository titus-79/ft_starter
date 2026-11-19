import type { ApplicantExtractionResponse } from "./api";
import type { ApplicantFormValues } from "../types/applicant";

type ApplicantFormPatch = Partial<ApplicantFormValues>;

function firstValue(
  ...values: (string | null | undefined)[]
): string | undefined {
  for (const value of values) {
    if (value?.trim()) return value;
  }
  return undefined;
}

export function mapExtractionToApplicantForm(
  extraction: ApplicantExtractionResponse,
): ApplicantFormPatch {
  const applicant = extraction.applicant;
  const certificate = applicant.employmentCertificates
    ?.slice()
    .sort((left, right) =>
      (right.endDate ?? right.startDate ?? "").localeCompare(
        left.endDate ?? left.startDate ?? "",
      ),
    )[0];

  const patch: ApplicantFormPatch = {};
  const stringFields: [keyof ApplicantFormValues, string | null | undefined][] =
    [
      ["lastName", applicant.lastName],
      ["birthName", applicant.birthName],
      ["firstName", applicant.firstName],
      ["gender", applicant.gender],
      ["birthDate", applicant.birthDate],
      ["nationality", applicant.nationality],
      ["address", applicant.address],
      ["addressComplement", applicant.addressLine2],
      ["postalCode", applicant.zipCode],
      ["city", applicant.city],
      ["phone", applicant.phone],
      ["email", applicant.email],
      ["socialSecurityNumber", applicant.socialSecurityNumber],
      ["identityDocumentType", applicant.identityDocumentType],
      ["identityDocumentNumber", applicant.identityDocumentNumber],
      ["lastJob", firstValue(applicant.lastJobTitle, certificate?.jobTitle)],
      [
        "employer",
        firstValue(applicant.lastEmployer, certificate?.employerName),
      ],
      [
        "jobStartDate",
        firstValue(applicant.employmentStartDate, certificate?.startDate),
      ],
      [
        "jobEndDate",
        firstValue(applicant.employmentEndDate, certificate?.endDate),
      ],
      [
        "contractType",
        firstValue(applicant.contractType, certificate?.contractType),
      ],
      ["highestDiploma", applicant.highestDegree],
      ["educationLevel", applicant.educationLevel],
    ];

  stringFields.forEach(([field, value]) => {
    if (typeof value === "string" && value.trim()) {
      Object.assign(patch, { [field]: value });
    }
  });

  if (typeof applicant.hasDrivingLicense === "boolean") {
    patch.hasDrivingLicense = applicant.hasDrivingLicense;
  }
  if (typeof applicant.hasPersonalVehicle === "boolean") {
    patch.hasVehicle = applicant.hasPersonalVehicle;
  }
  if (typeof applicant.mobilityRadius === "number") {
    patch.mobilityRadius = String(applicant.mobilityRadius);
  }
  if (applicant.skills?.length) {
    patch.skills = applicant.skills
      .filter((skill) => skill.trim())
      .map((name) => ({ name }));
  }

  return patch;
}

export function hasEmployerCertificate(
  extraction: ApplicantExtractionResponse,
): boolean {
  return (
    (extraction.applicant.employmentCertificates?.length ?? 0) > 0 ||
    extraction.documentsDetected.some((document) =>
      document.toUpperCase().includes("ATTESTATION"),
    )
  );
}
