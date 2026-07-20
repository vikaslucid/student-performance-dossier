package com.vikas.studentperformancedossier.controller;

import com.vikas.studentperformancedossier.dto.DossierResponse;
import com.vikas.studentperformancedossier.pdf.DossierPdfGenerator;
import com.vikas.studentperformancedossier.service.DossierService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dossier")
public class DossierController {

    private final DossierService dossierService;
    private final DossierPdfGenerator dossierPdfGenerator;

    public DossierController(DossierService dossierService, DossierPdfGenerator dossierPdfGenerator) {
        this.dossierService = dossierService;
        this.dossierPdfGenerator = dossierPdfGenerator;
    }

    @GetMapping("/{studentId}")
    public DossierResponse getDossier(@PathVariable Long studentId) {
        return dossierService.getDossier(studentId);
    }

    @GetMapping("/{studentId}/pdf")
    public ResponseEntity<byte[]> getDossierPdf(@PathVariable Long studentId) {
        DossierResponse dossier = dossierService.getDossier(studentId);
        byte[] pdfBytes = dossierPdfGenerator.generate(dossier);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dossier-" + studentId + ".pdf\"")
                .body(pdfBytes);
    }
}
