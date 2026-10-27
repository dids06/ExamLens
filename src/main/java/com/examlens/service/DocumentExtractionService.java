package com.examlens.service;

import com.examlens.config.AppConfig;
import com.examlens.model.DocumentBlock;
import com.examlens.util.Text;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Extracts text and page provenance from uploaded documents.
 *
 * <p>Supported: PDF (PDFBox text layer), DOCX (Apache POI), TXT, and PNG/JPG/JPEG images.
 * When a PDF has no text layer, or for images, OCR is attempted through Tesseract if it is
 * installed. When OCR is unavailable the extraction reports that honestly instead of
 * inventing text. Page numbers are only produced when the format genuinely provides them.</p>
 */
public class DocumentExtractionService {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt", "png", "jpg", "jpeg");

    private static final int MIN_TEXT_CHARS = 40;

    private final AppConfig config;
    private volatile Boolean ocrAvailable;

    public DocumentExtractionService(AppConfig config) {
        this.config = config;
    }

    /** Result of extracting one document. {@code readable=false} means no trustworthy text was obtained. */
    public record Extraction(String method, Integer pageCount, boolean pageLabelled,
                             List<DocumentBlock> blocks, List<String> warnings, int chars, boolean readable) {
    }

    public Extraction extract(Path file, String originalFilename) {
        String ext = StorageService.extension(originalFilename);
        return switch (ext) {
            case "pdf" -> extractPdf(file);
            case "docx" -> extractDocx(file);
            case "txt" -> extractTxt(file);
            case "png", "jpg", "jpeg" -> extractImage(file);
            default -> new Extraction("UNSUPPORTED", null, false, List.of(),
                    List.of("The file type \"." + ext + "\" is not supported for extraction."), 0, false);
        };
    }

    // ------------------------------------------------------------------ PDF

    private Extraction extractPdf(Path file) {
        List<String> warnings = new ArrayList<>();
        try (PDDocument document = PDDocument.load(file.toFile())) {
            int pages = document.getNumberOfPages();
            List<String> perPage = new ArrayList<>();
            for (int i = 1; i <= pages; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                perPage.add(Text.normalizeWhitespace(stripper.getText(document)));
            }
            int total = perPage.stream().mapToInt(String::length).sum();

            if (total < MIN_TEXT_CHARS) {
                warnings.add("The PDF contained little or no embedded text (possible scan). "
                        + "OCR was attempted per page.");
                if (ocrAvailable()) {
                    List<DocumentBlock> blocks = new ArrayList<>();
                    int index = 1;
                    for (int i = 0; i < pages; i++) {
                        String text = ocrPage(document, i, warnings);
                        if (!text.isBlank()) {
                            blocks.add(block("b" + index++, i + 1, "TESSERACT", text, 0.6, false));
                        }
                    }
                    int chars = blocks.stream().mapToInt(b -> b.getBlockText().length()).sum();
                    if (chars == 0) {
                        warnings.add("OCR produced no readable text. Upload a clearer scan or a text-based file.");
                        return new Extraction("TESSERACT", pages, true, List.of(), warnings, 0, false);
                    }
                    warnings.add("Text was obtained with OCR, so recognition errors are possible. Review carefully.");
                    return new Extraction("TESSERACT", pages, true, blocks, warnings, chars, true);
                }
                warnings.add("OCR is not available on this server (Tesseract is not installed), so no text could be read. "
                        + "Upload a text-based PDF or a clearer document.");
                return new Extraction("PDFBOX", pages, true, List.of(), warnings, 0, false);
            }

            List<DocumentBlock> blocks = new ArrayList<>();
            int index = 1;
            for (int i = 0; i < pages; i++) {
                String text = perPage.get(i);
                if (text.isBlank()) {
                    continue;
                }
                blocks.add(block("b" + (index++), i + 1, "PDFBOX", text, 0.95, false));
            }
            return new Extraction("PDFBOX", pages, true, blocks, warnings, total, true);
        } catch (IOException e) {
            return new Extraction("PDFBOX", null, false, List.of(),
                    List.of("The PDF could not be read: " + safe(e.getMessage())), 0, false);
        }
    }

    private String ocrPage(PDDocument document, int pageIndex, List<String> warnings) {
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 200, ImageType.GRAY);
            return ocr(image);
        } catch (Exception e) {
            warnings.add("A page could not be rendered for OCR: " + safe(e.getMessage()));
            return "";
        }
    }

    // ------------------------------------------------------------------ DOCX

    private Extraction extractDocx(Path file) {
        List<String> warnings = new ArrayList<>();
        try (InputStream in = Files.newInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {
            List<DocumentBlock> blocks = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            int index = 1;
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        if (current.length() > 0) {
                            current.append('\n');
                        }
                        current.append(text.trim());
                    }
                } else if (element instanceof XWPFTable table) {
                    StringBuilder tableText = new StringBuilder();
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cells.add(cell.getText() == null ? "" : cell.getText().trim());
                        }
                        tableText.append(String.join(" | ", cells)).append('\n');
                    }
                    if (tableText.length() > 0) {
                        if (current.length() > 0) {
                            current.append('\n');
                        }
                        current.append(tableText);
                    }
                }
            }
            if (current.length() > 0) {
                blocks.add(block("b" + index, null, "POI", Text.normalizeWhitespace(current.toString()), 0.9, false));
            }
            int chars = blocks.stream().mapToInt(b -> b.getBlockText().length()).sum();
            if (chars == 0) {
                warnings.add("The DOCX contained no readable paragraph text.");
                return new Extraction("POI", null, false, List.of(), warnings, 0, false);
            }
            warnings.add("DOCX files have no page numbers, so no page references are available.");
            return new Extraction("POI", null, false, blocks, warnings, chars, true);
        } catch (IOException e) {
            return new Extraction("POI", null, false, List.of(),
                    List.of("The DOCX could not be read: " + safe(e.getMessage())), 0, false);
        }
    }

    // ------------------------------------------------------------------ TXT

    private Extraction extractTxt(Path file) {
        List<String> warnings = new ArrayList<>();
        try {
            String raw = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            String text = Text.normalizeWhitespace(raw);
            if (text.length() == 0) {
                warnings.add("The text file is empty.");
                return new Extraction("TXT", null, false, List.of(), warnings, 0, false);
            }
            List<DocumentBlock> blocks = new ArrayList<>();
            int index = 1;
            for (String chunk : text.split("\\n\\s*\\n")) {
                String trimmed = chunk.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                blocks.add(block("b" + (index++), null, "TXT", trimmed, 0.98, false));
            }
            warnings.add("Plain text has no page numbers, so no page references are available.");
            return new Extraction("TXT", null, false, blocks, warnings, text.length(), true);
        } catch (IOException e) {
            return new Extraction("TXT", null, false, List.of(),
                    List.of("The text file could not be read: " + safe(e.getMessage())), 0, false);
        }
    }

    // ------------------------------------------------------------------ images

    private Extraction extractImage(Path file) {
        List<String> warnings = new ArrayList<>();
        if (!ocrAvailable()) {
            warnings.add("This image could not be read because OCR is not available on this server "
                    + "(Tesseract is not installed). Install Tesseract or upload a PDF/DOCX/TXT file.");
            return new Extraction("OCR_UNAVAILABLE", 1, false, List.of(), warnings, 0, false);
        }
        try {
            BufferedImage image = javax.imageio.ImageIO.read(file.toFile());
            if (image == null) {
                return new Extraction("TESSERACT", 1, false, List.of(),
                        List.of("The image format could not be decoded."), 0, false);
            }
            String text = Text.normalizeWhitespace(ocr(image));
            if (text.isBlank()) {
                warnings.add("OCR found no readable text in the image.");
                return new Extraction("TESSERACT", 1, false, List.of(), warnings, 0, false);
            }
            warnings.add("Text was obtained with OCR, so recognition errors are possible. Review carefully.");
            DocumentBlock block = block("b1", 1, "TESSERACT", text, 0.6, true);
            return new Extraction("TESSERACT", 1, false, List.of(block), warnings, text.length(), true);
        } catch (IOException e) {
            return new Extraction("TESSERACT", 1, false, List.of(),
                    List.of("The image could not be read: " + safe(e.getMessage())), 0, false);
        }
    }

    // ------------------------------------------------------------------ OCR

    /** Lazily probes Tesseract once and caches the outcome. Never throws. */
    public boolean ocrAvailable() {
        Boolean cached = ocrAvailable;
        if (cached != null) {
            return cached;
        }
        boolean available;
        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            String dataPath = config.tesseractDataPath();
            if (dataPath != null && !dataPath.isBlank()) {
                tesseract.setDatapath(dataPath);
            }
            tesseract.setLanguage(config.tesseractLanguage());
            available = true;
        } catch (Throwable t) {
            available = false;
        }
        ocrAvailable = available;
        return available;
    }

    private String ocr(BufferedImage image) {
        try {
            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            String dataPath = config.tesseractDataPath();
            if (dataPath != null && !dataPath.isBlank()) {
                tesseract.setDatapath(dataPath);
            }
            tesseract.setLanguage(config.tesseractLanguage());
            String text = tesseract.doOCR(image);
            return text == null ? "" : text;
        } catch (Throwable t) {
            return "";
        }
    }

    // ------------------------------------------------------------------ helpers

    private static DocumentBlock block(String key, Integer page, String method, String text, double confidence, boolean uncertain) {
        DocumentBlock block = new DocumentBlock();
        block.setBlockKey(key);
        block.setPageNumber(page);
        block.setBlockText(text);
        block.setExtractionMethod(method);
        block.setConfidence(confidence);
        block.setUncertain(uncertain);
        return block;
    }

    private static String safe(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    static String normalizeExtension(String name) {
        return StorageService.extension(name).toLowerCase(Locale.ROOT);
    }
}
