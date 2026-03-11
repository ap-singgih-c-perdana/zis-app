package com.zakat.controller;

import com.zakat.service.ReportService;
import com.zakat.service.dto.KwitansiReportResponse;
import com.zakat.service.dto.MuzakkiDetailReportResponse;
import com.zakat.service.dto.RekapZisReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/rekap-zis")
    public RekapZisReportResponse rekapZis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        try {
            return reportService.rekapZis(from, to);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/muzakki-detail")
    public MuzakkiDetailReportResponse muzakkiDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        try {
            return reportService.muzakkiDetail(from, to);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping(value = "/muzakki-detail.csv", produces = "text/csv")
    public ResponseEntity<byte[]> muzakkiDetailCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        try {
            MuzakkiDetailReportResponse report = reportService.muzakkiDetail(from, to);
            String csv = reportService.muzakkiDetailCsv(report);

            String filename = String.format("muzakki-detail_%s_%s.csv", from, to);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/kwitansi/{paymentId}")
    public KwitansiReportResponse kwitansi(@PathVariable UUID paymentId) {
        try {
            return reportService.kwitansi(paymentId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping(value = "/kwitansi/{paymentId}/template.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> kwitansiTemplatePdf(
            @PathVariable UUID paymentId,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(defaultValue = "false") boolean debugSave
    ) {
        byte[] bytes = reportService.kwitansiTemplatePdf(paymentId);
        String debugPath = null;
        if (debugSave) {
            debugPath = saveDebugPdf(paymentId, bytes);
        }
        System.out.println("debugPath = " + debugPath);
        String disposition = (download ? "attachment" : "inline")
                + "; filename=\"kwitansi-" + paymentId + ".pdf\"";
        var response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
        if (debugPath != null) {
            return ResponseEntity.status(response.getStatusCode())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .header("X-Debug-Pdf-Path", debugPath)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        }
        return response;
    }

    @GetMapping(value = "/kwitansi/{paymentId}/template/print", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> kwitansiTemplatePrint(@PathVariable UUID paymentId) {
        String src = "/api/reports/kwitansi/" + paymentId + "/template.pdf?debugSave=true";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html lang="id">
                <head>
                  <meta charset="utf-8">
                  <title>Print Kwitansi Template PDF</title>
                  <style>
                    html, body { margin: 0; height: 100%; background: #111; }
                    iframe { border: 0; width: 100%; height: 100%; }
                  </style>
                </head>
                <body>
                  <iframe id="pdfFrame" src="%s"></iframe>
                  <script>
                    const frame = document.getElementById('pdfFrame');
                    frame.addEventListener('load', function () {
                      setTimeout(function () {
                        try {
                          frame.contentWindow.focus();
                          frame.contentWindow.print();
                        } catch (e) {
                          window.print();
                        }
                      }, 300);
                    });
                  </script>
                </body>
                </html>
                """;
        String html = htmlTemplate
                .replace("src=\"%s\"", "src=\"__PDF_SRC__\"")
                .replace("__PDF_SRC__", src);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(html);
    }

    private static String saveDebugPdf(UUID paymentId, byte[] bytes) {
        try {
            Path dir = Path.of(System.getProperty("user.dir"), "target", "debug-pdf");
            Files.createDirectories(dir);
            Path path = dir.resolve("kwitansi-debug-" + paymentId + ".pdf");
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return path.toString();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal simpan debug PDF ke target/debug-pdf", e);
        }
    }
}
