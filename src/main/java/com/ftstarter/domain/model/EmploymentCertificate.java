package com.ftstarter.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmploymentCertificate(
        String employerName,
        String siret,
        String jobTitle,
        String contractType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal grossSalary,
        String workingTime,
        String reasonForTermination,
        String country,
        String city) {}
