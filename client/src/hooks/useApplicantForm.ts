import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { createApplicantSchema } from "../features/registration-form/applicantSchema";
import type { ApplicantFormValues } from "../types/applicant";

const emptyApplicant: ApplicantFormValues = {
  lastName: "",
  birthName: "",
  firstName: "",
  gender: "",
  birthDate: "",
  nationality: "",
  address: "",
  addressComplement: "",
  postalCode: "",
  city: "",
  phone: "",
  email: "",
  socialSecurityNumber: "",
  identityDocumentType: "",
  identityDocumentNumber: "",
  lastJob: "",
  employer: "",
  jobStartDate: "",
  jobEndDate: "",
  contractType: "",
  highestDiploma: "",
  educationLevel: "",
  hasDrivingLicense: false,
  hasVehicle: false,
  mobilityRadius: "",
  skills: [],
};

export function useApplicantForm(hasEmployerCertificate: boolean) {
  const form = useForm<ApplicantFormValues>({
    resolver: zodResolver(createApplicantSchema(hasEmployerCertificate)),
    defaultValues: emptyApplicant,
    mode: "onTouched",
  });

  const watchedValues = useWatch({ control: form.control });
  const trackableFields = Object.entries(watchedValues).filter(
    ([key]) => key !== "hasDrivingLicense" && key !== "hasVehicle",
  );
  const completedFields = trackableFields.filter(([key, value]) => {
    if (key === "skills")
      return (
        Array.isArray(value) && value.some((skill) => (skill.name ?? "").trim())
      );
    return typeof value === "string" && value.trim().length > 0;
  }).length;
  const completion = Math.round(
    (completedFields / trackableFields.length) * 100,
  );

  return { ...form, completion };
}
