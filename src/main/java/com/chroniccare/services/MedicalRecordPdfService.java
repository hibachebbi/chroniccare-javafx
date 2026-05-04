package com.chroniccare.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicalRecordPdfService {

    private static final float COLOR_PAGE_BG_R = 248f / 255f;
    private static final float COLOR_PAGE_BG_G = 250f / 255f;
    private static final float COLOR_PAGE_BG_B = 252f / 255f; // #f8fafc
    private static final float COLOR_HEADER_BG_R = 11f / 255f;
    private static final float COLOR_HEADER_BG_G = 31f / 255f;
    private static final float COLOR_HEADER_BG_B = 58f / 255f; // #0b1f3a
    private static final float COLOR_CARD_BG_R = 1f;
    private static final float COLOR_CARD_BG_G = 1f;
    private static final float COLOR_CARD_BG_B = 1f;           // #ffffff
    private static final float COLOR_SECTION_BG_R = 239f / 255f;
    private static final float COLOR_SECTION_BG_G = 246f / 255f;
    private static final float COLOR_SECTION_BG_B = 255f / 255f; // #eff6ff
    private static final float COLOR_SHADOW_R = 226f / 255f;
    private static final float COLOR_SHADOW_G = 232f / 255f;
    private static final float COLOR_SHADOW_B = 240f / 255f;   // #e2e8f0

    private static final float COLOR_TITLE_R = 15f / 255f;
    private static final float COLOR_TITLE_G = 23f / 255f;
    private static final float COLOR_TITLE_B = 42f / 255f;   // #0f172a
    private static final float COLOR_TEXT_R = 51f / 255f;
    private static final float COLOR_TEXT_G = 65f / 255f;
    private static final float COLOR_TEXT_B = 85f / 255f;    // #334155
    private static final float COLOR_MUTED_R = 100f / 255f;
    private static final float COLOR_MUTED_G = 116f / 255f;
    private static final float COLOR_MUTED_B = 139f / 255f;  // #64748b
    private static final float COLOR_LINE_R = 226f / 255f;
    private static final float COLOR_LINE_G = 232f / 255f;
    private static final float COLOR_LINE_B = 240f / 255f;   // #e2e8f0
    private static final float COLOR_ACCENT_R = 37f / 255f;
    private static final float COLOR_ACCENT_G = 99f / 255f;
    private static final float COLOR_ACCENT_B = 235f / 255f; // #2563eb

    public void exportToPdf(String content, String path) throws IOException {
        exportToPdf(content, path, null, null, List.of());
    }

    public void exportToPdf(
            String content,
            String path,
            String patientName,
            String mainCondition,
            List<String> secondaryConditions
    ) throws IOException {
        String safeContent = sanitize(content);
        String safePatientName = sanitize(patientName);
        String safeMainCondition = sanitize(mainCondition);
        List<String> safeSecondary = new ArrayList<>();
        if (secondaryConditions != null) {
            for (String c : secondaryConditions) {
                if (c != null && !c.isBlank()) safeSecondary.add(sanitize(c.trim()));
            }
        }

        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        float margin = 50f;
        float fontSize = 11f;
        float leading = 14f;
        float headingSize = 13f;
        float titleSize = 13f;
        float bulletIndent = 14f;

        Pattern sectionPattern = Pattern.compile("^(\\s*\\d+\\s*[).\\-]\\s*.+|\\s*Annexe\\b.*)$", Pattern.CASE_INSENSITIVE);
        Pattern bulletPattern = Pattern.compile("^\\s*[-•]\\s*(.*)$");
        Pattern annexPattern = Pattern.compile("^\\s*Annexe\\b.*$", Pattern.CASE_INSENSITIVE);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH));

        try (PDDocument document = new PDDocument()) {
            PageContext ctx = startNewPage(document, 1, true, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);

            float usableWidth = ctx.page.getMediaBox().getWidth() - 2 * margin;
            boolean firstNonEmptyLineHandled = false;
            boolean wroteAnyBodyLine = false;

            for (String rawLine : safeContent.split("\\R")) {
                String line = rawLine == null ? "" : rawLine.stripTrailing();
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    ctx.y -= leading / 2f;
                    if (ctx.y <= margin) {
                        ctx.stream.close();
                        ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                    }
                    continue;
                }

                boolean isSection = sectionPattern.matcher(trimmed).matches();
                Matcher bulletMatcher = bulletPattern.matcher(trimmed);
                boolean isBullet = bulletMatcher.matches();

                if (!firstNonEmptyLineHandled) {
                    firstNonEmptyLineHandled = true;
                    if (!isSection && !isBullet) {
                        boolean looksLikeDocumentTitle = trimmed.toLowerCase(Locale.FRENCH).startsWith("dossier médical")
                                || trimmed.toLowerCase(Locale.FRENCH).startsWith("dossier medical");

                        PDType1Font titleFont = looksLikeDocumentTitle ? fontItalic : fontBold;
                        float size = looksLikeDocumentTitle ? fontSize : (titleSize + 2f);
                        ctx.stream.setNonStrokingColor(looksLikeDocumentTitle ? COLOR_MUTED_R : COLOR_TITLE_R,
                                looksLikeDocumentTitle ? COLOR_MUTED_G : COLOR_TITLE_G,
                                looksLikeDocumentTitle ? COLOR_MUTED_B : COLOR_TITLE_B);

                        for (String w : wrapByWidth(trimmed, titleFont, size, usableWidth)) {
                            if (ctx.y <= margin) {
                                ctx.stream.close();
                                ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                            }
                            drawText(ctx.stream, titleFont, size, margin, ctx.y, w);
                            ctx.y -= leading;
                        }
                        ctx.stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
                        ctx.y -= looksLikeDocumentTitle ? 6f : 10f;
                        wroteAnyBodyLine = true;
                        continue;
                    }
                }

                if (isSection) {
                    if (annexPattern.matcher(trimmed).matches() && wroteAnyBodyLine) {
                        ctx.stream.close();
                        ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                    }

                    float stripeW = 4f;
                    float padX = 10f;
                    float padY = 6f;
                    List<String> headingLines = wrapByWidth(trimmed, fontBold, headingSize, usableWidth - stripeW - 2 * padX);
                    float minSpace = leading * (headingLines.size() + 1.5f);
                    if (ctx.y - minSpace <= margin) {
                        ctx.stream.close();
                        ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                    }

                    ctx.y -= 6f;
                    float textStartY = ctx.y;
                    float lastBaseline = textStartY - (headingLines.size() - 1) * leading;
                    float blockTop = textStartY + headingSize + padY;
                    float blockBottom = lastBaseline - padY - 3f;
                    float blockHeight = blockTop - blockBottom;

                    ctx.stream.setNonStrokingColor(COLOR_SECTION_BG_R, COLOR_SECTION_BG_G, COLOR_SECTION_BG_B);
                    ctx.stream.addRect(margin, blockBottom, usableWidth, blockHeight);
                    ctx.stream.fill();
                    ctx.stream.setNonStrokingColor(COLOR_ACCENT_R, COLOR_ACCENT_G, COLOR_ACCENT_B);
                    ctx.stream.addRect(margin, blockBottom, stripeW, blockHeight);
                    ctx.stream.fill();

                    ctx.stream.setNonStrokingColor(COLOR_TITLE_R, COLOR_TITLE_G, COLOR_TITLE_B);
                    for (String h : headingLines) {
                        if (ctx.y <= margin) {
                            ctx.stream.close();
                            ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                        }
                        drawText(ctx.stream, fontBold, headingSize, margin + stripeW + padX, ctx.y, h);
                        ctx.y -= leading;
                    }

                    ctx.stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
                    ctx.y -= 4f;
                    wroteAnyBodyLine = true;
                    continue;
                }

                if (isBullet) {
                    String body = bulletMatcher.group(1);
                    String bulletBody = body == null ? "" : body.trim();
                    if (!bulletBody.isEmpty()) {
                        List<String> wrapped = wrapByWidth(bulletBody, fontRegular, fontSize, usableWidth - bulletIndent);
                        for (int i = 0; i < wrapped.size(); i++) {
                            if (ctx.y <= margin) {
                                ctx.stream.close();
                                ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                            }
                            if (i == 0) {
                                ctx.stream.setNonStrokingColor(COLOR_ACCENT_R, COLOR_ACCENT_G, COLOR_ACCENT_B);
                                drawText(ctx.stream, fontBold, fontSize, margin, ctx.y, "•");
                                ctx.stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
                            }
                            drawText(ctx.stream, fontRegular, fontSize, margin + bulletIndent, ctx.y, wrapped.get(i));
                            ctx.y -= leading;
                        }
                        wroteAnyBodyLine = true;
                    }
                    continue;
                }

                String lower = trimmed.toLowerCase(Locale.FRENCH);
                PDType1Font paragraphFont = (lower.contains("généré automatiquement") && lower.contains("avis médical"))
                        ? fontItalic
                        : fontRegular;

                List<String> wrapped = wrapByWidth(trimmed, paragraphFont, fontSize, usableWidth);
                for (String w : wrapped) {
                    if (ctx.y <= margin) {
                        ctx.stream.close();
                        ctx = startNewPage(document, ctx.pageNumber + 1, false, margin, fontBold, fontRegular, dateStr, safePatientName, safeMainCondition, safeSecondary);
                    }

                    ctx.stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
                    drawText(ctx.stream, paragraphFont, fontSize, margin, ctx.y, w);
                    ctx.y -= leading;
                }
                wroteAnyBodyLine = true;
            }

            ctx.stream.close();
            document.save(path);
        }
    }

    public String importPdfText(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static class PageContext {
        private final PDPage page;
        private final PDPageContentStream stream;
        private final int pageNumber;
        private float y;

        private PageContext(PDPage page, PDPageContentStream stream, int pageNumber, float y) {
            this.page = page;
            this.stream = stream;
            this.pageNumber = pageNumber;
            this.y = y;
        }
    }

    private PageContext startNewPage(
            PDDocument document,
            int pageNumber,
            boolean firstPage,
            float margin,
            PDType1Font fontBold,
            PDType1Font fontRegular,
            String dateStr,
            String patientName,
            String mainCondition,
            List<String> secondaryConditions
    ) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(document, page);

        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float usableWidth = pageWidth - 2 * margin;
        float y = pageHeight - margin;

        // Page background
        stream.setNonStrokingColor(COLOR_PAGE_BG_R, COLOR_PAGE_BG_G, COLOR_PAGE_BG_B);
        stream.addRect(0, 0, pageWidth, pageHeight);
        stream.fill();

        // Footer (kept inside the bottom margin)
        float footerLineY = margin - 12f;
        float footerTextY = margin - 26f;
        stream.setStrokingColor(COLOR_LINE_R, COLOR_LINE_G, COLOR_LINE_B);
        stream.setLineWidth(0.7f);
        stream.moveTo(margin, footerLineY);
        stream.lineTo(pageWidth - margin, footerLineY);
        stream.stroke();
        stream.setNonStrokingColor(COLOR_MUTED_R, COLOR_MUTED_G, COLOR_MUTED_B);
        drawText(stream, fontRegular, 9f, margin, footerTextY, "ChronicCare");
        drawTextRight(stream, fontRegular, 9f, pageWidth - margin, footerTextY, "Page " + pageNumber);

        if (firstPage) {
            float headerH = 92f;
            float headerY = pageHeight - headerH;

            // Header block
            stream.setNonStrokingColor(COLOR_HEADER_BG_R, COLOR_HEADER_BG_G, COLOR_HEADER_BG_B);
            stream.addRect(0, headerY, pageWidth, headerH);
            stream.fill();
            stream.setNonStrokingColor(COLOR_ACCENT_R, COLOR_ACCENT_G, COLOR_ACCENT_B);
            stream.addRect(0, headerY, pageWidth, 6f);
            stream.fill();

            stream.setNonStrokingColor(1f, 1f, 1f);
            drawText(stream, fontBold, 22f, margin, pageHeight - 40f, "ChronicCare");
            stream.setNonStrokingColor(0.93f, 0.96f, 1f);
            drawText(stream, fontBold, 14f, margin, pageHeight - 64f, "Dossier médical");
            stream.setNonStrokingColor(0.78f, 0.84f, 0.92f);
            drawTextRight(stream, fontRegular, 11f, pageWidth - margin, pageHeight - 54f, dateStr);

            // Patient card
            float cardTop = headerY - 18f;
            float cardX = margin;
            float cardW = pageWidth - 2 * margin;
            float cardPadX = 14f;
            float cardPadY = 12f;
            float labelSize = 9f;
            float valueSize = 11f;
            float cardLeading = 13f;
            float blockGap = 10f;
            float stripeW = 5f;

            String patientDisplay = (patientName == null || patientName.isBlank()) ? "Non renseigné" : patientName;
            String mainDisplay = (mainCondition == null || mainCondition.isBlank()) ? "Non renseignée" : mainCondition;
            String secondaryDisplay = (secondaryConditions == null || secondaryConditions.isEmpty())
                    ? null
                    : String.join(", ", secondaryConditions);

            float cardContentW = cardW - 2 * cardPadX - stripeW;
            List<String> patientLines = wrapByWidth(patientDisplay, fontBold, valueSize + 1f, cardContentW);
            List<String> mainLines = wrapByWidth(mainDisplay, fontRegular, valueSize, cardContentW);
            List<String> secondaryLines = (secondaryDisplay == null) ? List.of() : wrapByWidth(secondaryDisplay, fontRegular, valueSize, cardContentW);

            float blocksH = 0f;
            blocksH += labelSize + 4f + patientLines.size() * cardLeading;
            blocksH += blockGap;
            blocksH += labelSize + 4f + mainLines.size() * cardLeading;
            if (!secondaryLines.isEmpty()) {
                blocksH += blockGap;
                blocksH += labelSize + 4f + secondaryLines.size() * cardLeading;
            }

            float cardH = cardPadY + blocksH + cardPadY;
            float cardBottom = cardTop - cardH;

            stream.setNonStrokingColor(COLOR_SHADOW_R, COLOR_SHADOW_G, COLOR_SHADOW_B);
            stream.addRect(cardX + 2f, cardBottom - 2f, cardW, cardH);
            stream.fill();
            stream.setNonStrokingColor(COLOR_CARD_BG_R, COLOR_CARD_BG_G, COLOR_CARD_BG_B);
            stream.addRect(cardX, cardBottom, cardW, cardH);
            stream.fill();
            stream.setStrokingColor(COLOR_LINE_R, COLOR_LINE_G, COLOR_LINE_B);
            stream.setLineWidth(0.9f);
            stream.addRect(cardX, cardBottom, cardW, cardH);
            stream.stroke();
            stream.setNonStrokingColor(COLOR_ACCENT_R, COLOR_ACCENT_G, COLOR_ACCENT_B);
            stream.addRect(cardX, cardBottom, stripeW, cardH);
            stream.fill();

            float tx = cardX + stripeW + cardPadX;
            float ty = cardTop - cardPadY - labelSize;

            stream.setNonStrokingColor(COLOR_MUTED_R, COLOR_MUTED_G, COLOR_MUTED_B);
            drawText(stream, fontBold, labelSize, tx, ty, "PATIENT");
            ty -= labelSize + 4f;
            stream.setNonStrokingColor(COLOR_TITLE_R, COLOR_TITLE_G, COLOR_TITLE_B);
            for (String l : patientLines) {
                drawText(stream, fontBold, valueSize + 1f, tx, ty, l);
                ty -= cardLeading;
            }

            ty -= blockGap - 2f;
            stream.setNonStrokingColor(COLOR_MUTED_R, COLOR_MUTED_G, COLOR_MUTED_B);
            drawText(stream, fontBold, labelSize, tx, ty, "CONDITION PRINCIPALE");
            ty -= labelSize + 4f;
            stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
            for (String l : mainLines) {
                drawText(stream, fontRegular, valueSize, tx, ty, l);
                ty -= cardLeading;
            }

            if (!secondaryLines.isEmpty()) {
                ty -= blockGap - 2f;
                stream.setNonStrokingColor(COLOR_MUTED_R, COLOR_MUTED_G, COLOR_MUTED_B);
                drawText(stream, fontBold, labelSize, tx, ty, "MALADIES SECONDAIRES");
                ty -= labelSize + 4f;
                stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
                for (String l : secondaryLines) {
                    drawText(stream, fontRegular, valueSize, tx, ty, l);
                    ty -= cardLeading;
                }
            }

            y = cardBottom - 26f;
        } else {
            float headerH = 54f;
            float headerY = pageHeight - headerH;

            stream.setNonStrokingColor(COLOR_CARD_BG_R, COLOR_CARD_BG_G, COLOR_CARD_BG_B);
            stream.addRect(0, headerY, pageWidth, headerH);
            stream.fill();
            stream.setNonStrokingColor(COLOR_ACCENT_R, COLOR_ACCENT_G, COLOR_ACCENT_B);
            stream.addRect(0, headerY, pageWidth, 4f);
            stream.fill();

            float hTextY = pageHeight - 28f;
            stream.setNonStrokingColor(COLOR_TITLE_R, COLOR_TITLE_G, COLOR_TITLE_B);
            drawText(stream, fontBold, 12.5f, margin, hTextY, "Dossier médical");
            stream.setNonStrokingColor(COLOR_MUTED_R, COLOR_MUTED_G, COLOR_MUTED_B);
            drawTextRight(stream, fontRegular, 10.5f, pageWidth - margin, hTextY, dateStr);

            if (patientName != null && !patientName.isBlank()) {
                stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
                drawText(stream, fontRegular, 10.5f, margin, hTextY - 14f, patientName);
            }

            stream.setStrokingColor(COLOR_LINE_R, COLOR_LINE_G, COLOR_LINE_B);
            stream.setLineWidth(0.8f);
            stream.moveTo(margin, headerY);
            stream.lineTo(pageWidth - margin, headerY);
            stream.stroke();

            y = headerY - 22f;
            stream.setNonStrokingColor(COLOR_TEXT_R, COLOR_TEXT_G, COLOR_TEXT_B);
        }

        return new PageContext(page, stream, pageNumber, y);
    }

    private static void drawText(PDPageContentStream stream, PDType1Font font, float fontSize, float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text == null ? "" : text);
        stream.endText();
    }

    private static void drawTextRight(PDPageContentStream stream, PDType1Font font, float fontSize, float rightX, float y, String text) throws IOException {
        String t = text == null ? "" : text;
        float width = font.getStringWidth(t) / 1000f * fontSize;
        drawText(stream, font, fontSize, rightX - width, y, t);
    }

    private static float drawWrappedText(
            PDPageContentStream stream,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            float maxWidth,
            String text
    ) throws IOException {
        float leading = 14f;
        for (String line : wrapByWidth(text, font, fontSize, maxWidth)) {
            drawText(stream, font, fontSize, x, y, line);
            y -= leading;
        }
        return y;
    }

    private static List<String> wrapByWidth(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) return List.of("");

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : s.split("\\s+")) {
            if (word.isEmpty()) continue;

            String candidate = current.length() == 0 ? word : current + " " + word;
            if (textWidth(font, fontSize, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }

            if (textWidth(font, fontSize, word) <= maxWidth) {
                current.append(word);
                continue;
            }

            String remaining = word;
            while (!remaining.isEmpty()) {
                int cut = 1;
                while (cut < remaining.length() && textWidth(font, fontSize, remaining.substring(0, cut + 1)) <= maxWidth) {
                    cut++;
                }
                lines.add(remaining.substring(0, cut));
                remaining = remaining.substring(cut);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private static float textWidth(PDType1Font font, float fontSize, String text) throws IOException {
        String t = text == null ? "" : text;
        return font.getStringWidth(t) / 1000f * fontSize;
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        String normalized = repairMojibakeIfNeeded(input)
                .replace('\u2028', '\n')
                .replace('\u2029', '\n')
                .replace('\u00A0', ' ')
                .replace('−', '-')
                .replace('✓', ' ')
                .replace('✗', ' ')
                .replace('⏳', ' ')
                .replace('\uFFFD', '?');

        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');

        StringBuilder cleaned = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '\n' || ch == '\t' || ch >= 0x20) cleaned.append(ch);
            else cleaned.append(' ');
        }
        normalized = cleaned.toString().trim();

        normalized = normalized.replaceAll("[ \\t]{2,}", " ");

        CharsetEncoder encoder = Charset.forName("windows-1252").newEncoder();
        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            sb.append(encoder.canEncode(ch) ? ch : '?');
        }
        return sb.toString();
    }

    private static String repairMojibakeIfNeeded(String input) {
        if (input == null || input.isBlank()) return input == null ? "" : input;
        if (!looksLikeMojibake(input)) return input;

        try {
            String repaired = new String(input.getBytes(Charset.forName("windows-1252")), StandardCharsets.UTF_8);
            if (repaired.indexOf('\uFFFD') < 0 && mojibakeScore(repaired) < mojibakeScore(input)) {
                return repaired;
            }
        } catch (Exception ignored) {
        }

        try {
            String repaired = new String(input.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            if (repaired.indexOf('\uFFFD') < 0 && mojibakeScore(repaired) < mojibakeScore(input)) {
                return repaired;
            }
        } catch (Exception ignored) {
        }

        return input;
    }

    private static boolean looksLikeMojibake(String s) {
        if (s == null) return false;
        return s.contains("Ã") || s.contains("Â") || s.contains("â€") || s.contains("Å") || s.contains("\uFFFD");
    }

    private static int mojibakeScore(String s) {
        if (s == null || s.isEmpty()) return 0;
        int score = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == 'Ã' || ch == 'Â') score += 3;
            if (ch == '\uFFFD') score += 5;
        }
        int idx = 0;
        while ((idx = s.indexOf("â€", idx)) >= 0) {
            score += 4;
            idx += 2;
        }
        return score;
    }
}
