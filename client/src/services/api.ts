import axios, { isAxiosError } from "axios";

export type UploadResponse = {
  uploadId: string;
  status: string;
  documentCount: number;
};

export type EmploymentCertificateResponse = {
  employerName?: string | null;
  jobTitle?: string | null;
  contractType?: string | null;
  startDate?: string | null;
  endDate?: string | null;
};

export type ExtractedApplicant = {
  lastName?: string | null;
  birthName?: string | null;
  firstName?: string | null;
  gender?: string | null;
  birthDate?: string | null;
  nationality?: string | null;
  address?: string | null;
  addressLine2?: string | null;
  zipCode?: string | null;
  city?: string | null;
  phone?: string | null;
  email?: string | null;
  socialSecurityNumber?: string | null;
  identityDocumentType?: string | null;
  identityDocumentNumber?: string | null;
  lastJobTitle?: string | null;
  lastEmployer?: string | null;
  employmentStartDate?: string | null;
  employmentEndDate?: string | null;
  contractType?: string | null;
  highestDegree?: string | null;
  educationLevel?: string | null;
  hasDrivingLicense?: boolean | null;
  hasPersonalVehicle?: boolean | null;
  mobilityRadius?: number | null;
  skills?: string[] | null;
  employmentCertificates?: EmploymentCertificateResponse[] | null;
};

export type ApplicantExtractionResponse = {
  confidenceScore: number;
  documentsDetected: string[];
  applicant: ExtractedApplicant;
};

type ApiErrorResponse = {
  code?: string;
  message?: string;
  details?: string[];
};

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  timeout: 120_000,
});

export async function uploadDocuments(
  files: File[],
  onProgress?: (progress: number) => void,
): Promise<UploadResponse> {
  const formData = new FormData();
  files.forEach((file) => formData.append("files[]", file));
  const { data } = await apiClient.post<UploadResponse>(
    "/v1/documents/upload",
    formData,
    {
      onUploadProgress: (event) => {
        if (event.total)
          onProgress?.(Math.round((event.loaded / event.total) * 60));
      },
    },
  );
  return data;
}

export async function analyzeDocuments(
  uploadId: string,
): Promise<ApplicantExtractionResponse> {
  const { data } = await apiClient.post<ApplicantExtractionResponse>(
    `/v1/analysis/${encodeURIComponent(uploadId)}`,
  );
  return data;
}

export async function getAnalysisResult(
  uploadId: string,
): Promise<ApplicantExtractionResponse> {
  const { data } = await apiClient.get<ApplicantExtractionResponse>(
    `/v1/analysis/${encodeURIComponent(uploadId)}`,
  );
  return data;
}

export function getApiErrorMessage(error: unknown): string {
  if (isAxiosError<ApiErrorResponse>(error)) {
    const response = error.response;
    if (response) {
      const { code, message, details } = response.data ?? {};
      const description = message ?? "La requête a échoué.";
      const extra = details?.filter(Boolean).join(" ");
      return [code, description, extra].filter(Boolean).join(" — ");
    }

    if (error.code === "ECONNABORTED" || error.code === "ETIMEDOUT") {
      return "Le délai d’attente du serveur est dépassé. Vous pouvez réessayer.";
    }
    return "Impossible de joindre le serveur. Vérifiez que le backend est démarré puis réessayez.";
  }

  return error instanceof Error
    ? error.message
    : "Une erreur inattendue est survenue.";
}
