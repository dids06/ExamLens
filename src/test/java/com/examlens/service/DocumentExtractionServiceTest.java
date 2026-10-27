package com.examlens.service;

import com.examlens.config.AppConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentExtractionServiceTest {

    private final DocumentExtractionService extractor =
            new DocumentExtractionService(AppConfig.of(Map.of("TESSERACT_DATA_PATH", "")));

    @Test
    void extractsPlainTextWithoutInventingPageNumbers(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("notes.txt");
        Files.writeString(file, "Unit 1: Algebra\n\nPolynomials and factorisation form the first unit.", StandardCharsets.UTF_8);

        var extraction = extractor.extract(file, "notes.txt");

        assertEquals("TXT", extraction.method());
        assertTrue(extraction.readable());
        assertFalse(extraction.pageLabelled(), "plain text has no page numbers");
        assertTrue(extraction.chars() > 20);
        assertTrue(extraction.blocks().stream().allMatch(b -> b.getPageNumber() == null));
        assertTrue(extraction.warnings().stream().anyMatch(w -> w.toLowerCase().contains("page")));
    }

    @Test
    void extractsPdfTextWithRealPageNumbers(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("syllabus.pdf");
        writePdf(file, "Unit 1: Algebra, polynomials, factorisation and quadratic equations.");

        var extraction = extractor.extract(file, "syllabus.pdf");

        assertEquals("PDFBOX", extraction.method());
        assertTrue(extraction.readable());
        assertTrue(extraction.pageLabelled());
        assertEquals(1, extraction.pageCount());
        assertFalse(extraction.blocks().isEmpty());
        assertEquals(1, extraction.blocks().get(0).getPageNumber());
        assertTrue(extraction.blocks().get(0).getBlockText().contains("Algebra"));
    }

    @Test
    void extractsDocxTextAndReportsThatPagesAreUnavailable(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("outline.docx");
        writeDocx(file, "Unit 1: Algebra", "Polynomials, factorisation and quadratic equations.");

        var extraction = extractor.extract(file, "outline.docx");

        assertEquals("POI", extraction.method());
        assertTrue(extraction.readable());
        assertFalse(extraction.pageLabelled());
        assertTrue(extraction.blocks().get(0).getBlockText().contains("Polynomials"));
        assertTrue(extraction.warnings().stream().anyMatch(w -> w.toLowerCase().contains("page")));
    }

    @Test
    void scannedPdfWithoutTextIsReportedAsUnreadableInsteadOfInventingContent(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("scan.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file.toFile());
        }

        var extraction = extractor.extract(file, "scan.pdf");

        assertFalse(extraction.readable(), "a blank/scanned PDF must not be reported as readable");
        assertTrue(extraction.blocks().isEmpty());
        assertTrue(extraction.warnings().stream()
                        .anyMatch(w -> w.toLowerCase().contains("ocr") || w.toLowerCase().contains("scan")),
                "the honesty warning about OCR/scans must be present");
    }

    @Test
    void unsupportedExtensionIsRejectedClearly(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("data.csv");
        Files.writeString(file, "a,b,c", StandardCharsets.UTF_8);

        var extraction = extractor.extract(file, "data.csv");

        assertFalse(extraction.readable());
        assertEquals("UNSUPPORTED", extraction.method());
        assertFalse(extraction.warnings().isEmpty());
    }

    @Test
    void allowedExtensionsMatchTheDocumentedUploadTypes() {
        assertTrue(DocumentExtractionService.ALLOWED_EXTENSIONS.containsAll(
                java.util.Set.of("pdf", "docx", "txt", "png", "jpg", "jpeg")));
    }

    private static void writePdf(Path file, String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            document.save(file.toFile());
        }
    }

    private static void writeDocx(Path file, String... paragraphs) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            for (String text : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(text);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                document.write(out);
            }
        }
    }
}
