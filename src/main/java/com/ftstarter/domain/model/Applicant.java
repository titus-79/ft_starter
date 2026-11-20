package com.ftstarter.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record Applicant(
        String lastName,
        String birthName,
        String firstName,
        String gender,
        LocalDate birthDate,
        String nationality,
        String address,
        String addressLine2,
        String zipCode,
        String city,
        String phone,
        String email,
        String socialSecurityNumber,
        String identityDocumentType,
        String identityDocumentNumber,
        String lastJobTitle,
        String lastEmployer,
        LocalDate employmentStartDate,
        LocalDate employmentEndDate,
        String contractType,
        String highestDegree,
        String educationLevel,
        Boolean hasDrivingLicense,
        Boolean hasPersonalVehicle,
        Integer mobilityRadius,
        List<String> skills,
        List<EmploymentCertificate> employmentCertificates,
        List<ApplicantDegree> degrees) {

    public Applicant {
        skills = skills == null
                ? List.of()
                : skills.stream().filter(Objects::nonNull).toList();
        employmentCertificates = employmentCertificates == null
                ? List.of()
                : employmentCertificates.stream().filter(Objects::nonNull).toList();
        degrees = degrees == null
                ? List.of()
                : degrees.stream().filter(Objects::nonNull).toList();
    }

    public static Applicant empty() {
        return new Applicant(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, List.of(), List.of(), List.of());
    }
}
