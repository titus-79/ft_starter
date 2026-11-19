package com.ftstarter.infrastructure.mapper;

import com.ftstarter.domain.model.Applicant;
import com.ftstarter.domain.model.ApplicantExtractionResponse;
import com.ftstarter.domain.model.EmploymentCertificate;
import com.ftstarter.infrastructure.n8n.N8nAnalysisResponse;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ApplicantExtractionMapper {

    public ApplicantExtractionResponse normalize(N8nAnalysisResponse response) {
        double confidence = response.confidenceScore() == null
                || !Double.isFinite(response.confidenceScore())
                ? 0
                : Math.min(1, Math.max(0, response.confidenceScore()));
        List<String> documents = response.documentsDetected() == null
                ? List.of()
                : response.documentsDetected().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .map(value -> value.toUpperCase(Locale.ROOT)
                                .replaceAll("[^A-Z0-9]+", "_")
                                .replaceAll("^_|_$", ""))
                        .distinct()
                        .toList();
        return new ApplicantExtractionResponse(
                confidence,
                documents,
                normalizeApplicant(response.applicant()));
    }

    private Applicant normalizeApplicant(Applicant applicant) {
        if (applicant == null) return Applicant.empty();
        List<String> skills = applicant.skills().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        List<EmploymentCertificate> certificates = applicant.employmentCertificates()
                .stream()
                .filter(Objects::nonNull)
                .map(this::normalizeCertificate)
                .toList();
        return new Applicant(
                trim(applicant.lastName()),
                trim(applicant.birthName()),
                trim(applicant.firstName()),
                trim(applicant.gender()),
                applicant.birthDate(),
                trim(applicant.nationality()),
                trim(applicant.address()),
                trim(applicant.addressLine2()),
                trim(applicant.zipCode()),
                trim(applicant.city()),
                trim(applicant.phone()),
                trim(applicant.email()),
                trim(applicant.socialSecurityNumber()),
                trim(applicant.identityDocumentType()),
                trim(applicant.identityDocumentNumber()),
                trim(applicant.lastJobTitle()),
                trim(applicant.lastEmployer()),
                applicant.employmentStartDate(),
                applicant.employmentEndDate(),
                trim(applicant.contractType()),
                trim(applicant.highestDegree()),
                trim(applicant.educationLevel()),
                applicant.hasDrivingLicense(),
                applicant.hasPersonalVehicle(),
                applicant.mobilityRadius(),
                skills,
                certificates);
    }

    private EmploymentCertificate normalizeCertificate(EmploymentCertificate certificate) {
        return new EmploymentCertificate(
                trim(certificate.employerName()),
                trim(certificate.siret()),
                trim(certificate.jobTitle()),
                trim(certificate.contractType()),
                certificate.startDate(),
                certificate.endDate(),
                certificate.grossSalary(),
                trim(certificate.workingTime()),
                trim(certificate.reasonForTermination()),
                trim(certificate.country()),
                trim(certificate.city()));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
