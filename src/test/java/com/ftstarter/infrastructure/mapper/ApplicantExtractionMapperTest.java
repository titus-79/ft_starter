package com.ftstarter.infrastructure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ftstarter.domain.model.Applicant;
import com.ftstarter.domain.model.EmploymentCertificate;
import com.ftstarter.infrastructure.n8n.N8nAnalysisResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicantExtractionMapperTest {

    private final ApplicantExtractionMapper mapper = new ApplicantExtractionMapper();

    @Test
    void clampsConfidenceAndNormalizesExtractedDocumentAndApplicantValues() {
        Applicant applicant = new Applicant(
                " DUPONT ",
                null,
                " Jean ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Arrays.asList(" Java ", "", "Java", null),
                List.of(new EmploymentCertificate(
                        " Capgemini ",
                        " 38012986600014 ",
                        " Développeur Java ",
                        " CDI ",
                        LocalDate.parse("2022-01-01"),
                        LocalDate.parse("2025-01-31"),
                        new BigDecimal("38000"),
                        "Temps plein",
                        null,
                        "France",
                        "Paris")));
        N8nAnalysisResponse source = new N8nAnalysisResponse(
                1.4,
                Arrays.asList(" cni ", "Attestation employeur", "CNI", null, " "),
                applicant);

        var result = mapper.normalize(source);

        assertEquals(1, result.confidenceScore());
        assertEquals(List.of("CNI", "ATTESTATION_EMPLOYEUR"), result.documentsDetected());
        assertEquals("DUPONT", result.applicant().lastName());
        assertEquals(List.of("Java"), result.applicant().skills());
        assertEquals("Capgemini", result.applicant().employmentCertificates()
                .getFirst().employerName());
        assertEquals("38012986600014", result.applicant().employmentCertificates()
                .getFirst().siret());
    }

    @Test
    void suppliesSafeDefaultsForMissingN8nData() {
        var result = mapper.normalize(new N8nAnalysisResponse(null, null, null));

        assertEquals(0, result.confidenceScore());
        assertEquals(List.of(), result.documentsDetected());
        assertEquals(List.of(), result.applicant().employmentCertificates());
    }
}
