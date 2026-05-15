package com.smartcart.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses raw Tesseract OCR text into structured receipt data.
 *
 * Handles the three most common French receipt item formats:
 *   [1] PRODUCT NAME          PRICE [TAX_CODE]
 *   [2] QTY PRODUCT NAME      x UNIT_PRICE   TOTAL [TAX_CODE]
 *   [3] PRODUCT NAME          QTYxUNIT_PRICE [= TOTAL] [TAX_CODE]
 *
 * Also handles barcodes/article codes at the start of lines (6-13 digits).
 */
@Service
@Slf4j
public class ParsingService {

    // ── Item patterns ──────────────────────────────────────────────────────────

    // [1] Most common: "PRODUCT NAME   2,99 [A/B/E/*]"
    //     Also handles leading barcode: "0123456 PRODUCT   2,99"
    private static final Pattern P_SIMPLE = Pattern.compile(
            "^(?:\\d{6,13}\\s+)?(.+?)\\s{2,}(\\d{1,4}[.,]\\d{2})(?:\\s+[A-Z0-9!*/])?\\s*$"
    );

    // [2] Qty first: "2 PRODUCT NAME   x 1,89   3,78 [A]"
    private static final Pattern P_QTY_UNIT_TOTAL = Pattern.compile(
            "^(\\d+(?:[.,]\\d+)?)\\s+(.+?)\\s{2,}[xX*]\\s*(\\d+[.,]\\d{2})\\s+(\\d+[.,]\\d{2})(?:\\s+[A-Z])?\\s*$"
    );

    // [3] Inline qty: "PRODUCT NAME   3x1,29 [= 3,87] [A]"
    private static final Pattern P_INLINE_QTY = Pattern.compile(
            "^(?:\\d{6,13}\\s+)?(.+?)\\s+(\\d+)[xX*](\\d+[.,]\\d{2})(?:\\s*=?\\s*(\\d+[.,]\\d{2}))?(?:\\s+[A-Z!*])?\\s*$"
    );

    // ── Total patterns (priority: TTC > generic) ───────────────────────────────

    // Highest priority: "TOTAL TTC", "NET À PAYER", "MONTANT TOTAL"
    private static final Pattern P_TOTAL_TTC = Pattern.compile(
            "(?:total\\s*ttc|net\\s*[àa]\\s*payer|montant\\s*total|total\\s*[àa]\\s*pay)[\\s:=]*(\\d+[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    // Fallback: lone "TOTAL" or "TOTAL :" at start of line
    private static final Pattern P_TOTAL_GENERIC = Pattern.compile(
            "^total[\\s:=]*(\\d+[.,]\\d{2})\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // ── Date ──────────────────────────────────────────────────────────────────

    private static final Pattern P_DATE = Pattern.compile(
            "\\b(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{2,4})\\b"
    );

    // ── Lines to skip completely ───────────────────────────────────────────────

    private static final List<String> SKIP_KEYWORDS = List.of(
            "sous-total", "sub-total", "tva ", "tva:", " vat", "remise", "discount",
            "avoir", "rendu monnaie", "rendu ", "espèces", "carte bancaire",
            "visa", "mastercard", "cb ", "chèque", "cheque", "paiement",
            "caissier", "caisse", "siret", "www.", "http",
            "merci", "bienvenue", "fidélité", "points cumulés",
            "solde carte", "ticket"
    );

    // Separator-only lines (dashes, equals, underscores, stars)
    private static final Pattern P_SEPARATOR = Pattern.compile("^[\\s\\-=_.*]{3,}$");

    // ── Public API ─────────────────────────────────────────────────────────────

    public ParsedReceipt parse(String rawText) {
        ParsedReceipt result = new ParsedReceipt();
        if (rawText == null || rawText.isBlank()) return result;

        String normalized = normalizeOcr(rawText);

        List<String> lines = Arrays.stream(normalized.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .collect(Collectors.toList());

        extractStoreName(lines, result);
        extractDate(lines, result);
        extractItemsAndTotals(lines, result);
        computeValidation(result);

        log.debug("Parsed: store='{}' date={} items={} total={} validation={}",
                result.getStoreName(), result.getDate(),
                result.getItems().size(), result.getTotal(), result.getValidationStatus());

        return result;
    }

    // ── OCR normalization ──────────────────────────────────────────────────────

    /**
     * Fixes the most common Tesseract OCR artifacts before any regex parsing.
     */
    private String normalizeOcr(String text) {
        return text
                // Normalize line endings
                .replace("\r\n", "\n").replace("\r", "\n")
                // Common single-char substitutions in price context: "l,29" → "1,29"
                .replaceAll("(?<=[\\s(])l([,.]\\d{2})", "1$1")
                .replaceAll("(?<=[\\s(])I([,.]\\d{2})", "1$1")
                // Em/en dash to hyphen
                .replace('–', '-').replace('—', '-')
                // Typographic quotes
                .replace('’', '\'').replace('‘', '\'')
                // Form feeds
                .replace('\f', '\n');
    }

    /**
     * Cleans a price string extracted by regex to handle remaining OCR noise.
     * e.g. "l,29" → "1.29", "1O,50" → "10.50"
     */
    private String sanitizePrice(String raw) {
        StringBuilder sb = new StringBuilder(raw.trim());
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == 'l' || c == 'I') sb.setCharAt(i, '1');
            else if (c == 'O') sb.setCharAt(i, '0');
            else if (c == 'S' && i == 0) sb.setCharAt(i, '5');
            else if (c == ',') sb.setCharAt(i, '.');
        }
        return sb.toString();
    }

    // ── Store name ─────────────────────────────────────────────────────────────

    private void extractStoreName(List<String> lines, ParsedReceipt result) {
        for (String line : lines) {
            if (P_DATE.matcher(line).find()) break;
            // Skip postal-code lines
            if (line.matches(".*\\b\\d{5}\\b.*")) break;
            // Skip phone/fax lines
            if (line.matches("(?i).*(?:tél|tel|fax|phone)[.:\\s].*")) break;

            long letters = line.chars().filter(Character::isLetter).count();
            if (letters >= 3 && (double) letters / line.length() > 0.45) {
                result.setStoreName(line.replaceAll("\\s{2,}", " ").trim());
                return;
            }
        }
    }

    // ── Date ───────────────────────────────────────────────────────────────────

    private void extractDate(List<String> lines, ParsedReceipt result) {
        for (String line : lines) {
            Matcher m = P_DATE.matcher(line);
            if (m.find()) {
                try {
                    int d = Integer.parseInt(m.group(1));
                    int mo = Integer.parseInt(m.group(2));
                    String yrStr = m.group(3);
                    int y = Integer.parseInt(yrStr);
                    if (yrStr.length() == 2) y += 2000;
                    if (d >= 1 && d <= 31 && mo >= 1 && mo <= 12 && y >= 2000 && y <= 2100) {
                        result.setDate(LocalDate.of(y, mo, d));
                        return;
                    }
                } catch (DateTimeException | NumberFormatException ignored) {
                }
            }
        }
    }

    // ── Items & totals ─────────────────────────────────────────────────────────

    private void extractItemsAndTotals(List<String> lines, ParsedReceipt result) {
        BigDecimal ttcTotal = null;
        BigDecimal genericTotal = null;

        for (String line : lines) {

            // ── 1. TTC / NET À PAYER (highest priority) ──
            Matcher mTtc = P_TOTAL_TTC.matcher(line);
            if (mTtc.find()) {
                BigDecimal v = parseDecimal(mTtc.group(1));
                if (v != null) ttcTotal = v;
                continue;
            }

            // ── 2. Generic TOTAL ──
            Matcher mTot = P_TOTAL_GENERIC.matcher(line);
            if (mTot.matches()) {
                BigDecimal v = parseDecimal(mTot.group(1));
                if (v != null) genericTotal = v;
                continue;
            }

            // ── 3. Skip non-item lines ──
            String lower = line.toLowerCase();
            if (SKIP_KEYWORDS.stream().anyMatch(lower::contains)) continue;
            if (P_SEPARATOR.matcher(line).matches()) continue;
            // Skip discount lines (negative amounts)
            if (line.matches(".*-\\s*\\d+[.,]\\d{2}.*") && lower.contains("remis")) continue;

            // ── 4. Try all item patterns ──
            ParsedItem item = tryParseItem(line);
            if (item != null) {
                result.getItems().add(item);
            }
        }

        // Prefer TTC total, fall back to generic
        result.setTotal(ttcTotal != null ? ttcTotal : genericTotal);
    }

    private ParsedItem tryParseItem(String line) {
        // Pattern [2]: "2 PRODUCT   x 1,89   3,78"
        Matcher m2 = P_QTY_UNIT_TOTAL.matcher(line);
        if (m2.matches()) {
            BigDecimal qty = parseDecimal(m2.group(1));
            String name = normalizeProductName(m2.group(2));
            BigDecimal unit = parseDecimal(m2.group(3));
            BigDecimal total = parseDecimal(m2.group(4));
            if (isValidItem(name, total)) {
                return buildItem(name, qty, unit, total);
            }
        }

        // Pattern [3]: "PRODUCT   3x1,29 [= 3,87]"
        Matcher m3 = P_INLINE_QTY.matcher(line);
        if (m3.matches()) {
            String name = normalizeProductName(m3.group(1));
            BigDecimal qty = parseDecimal(m3.group(2));
            BigDecimal unit = parseDecimal(m3.group(3));
            BigDecimal total = m3.group(4) != null
                    ? parseDecimal(m3.group(4))
                    : (unit != null && qty != null ? unit.multiply(qty).setScale(2, RoundingMode.HALF_UP) : null);
            if (isValidItem(name, total)) {
                return buildItem(name, qty, unit, total);
            }
        }

        // Pattern [1]: "PRODUCT   2,99"  (most common, try last to avoid false positives)
        Matcher m1 = P_SIMPLE.matcher(line);
        if (m1.matches()) {
            String name = normalizeProductName(m1.group(1));
            BigDecimal price = parseDecimal(m1.group(2));
            if (isValidItem(name, price)) {
                return buildItem(name, BigDecimal.ONE, price, price);
            }
        }

        return null;
    }

    private ParsedItem buildItem(String name, BigDecimal qty, BigDecimal unit, BigDecimal total) {
        ParsedItem item = new ParsedItem();
        item.setName(name);
        item.setQuantity(qty != null ? qty : BigDecimal.ONE);
        item.setUnitPrice(unit);
        item.setTotalPrice(total);
        return item;
    }

    private boolean isValidItem(String name, BigDecimal price) {
        return name != null && name.length() >= 2
                && price != null
                && price.compareTo(BigDecimal.ZERO) > 0
                && price.compareTo(new BigDecimal("500")) < 0;
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    /**
     * Compares the sum of extracted items against the detected receipt total.
     * Tolerance: 0.05 € to absorb minor rounding differences.
     */
    private void computeValidation(ParsedReceipt result) {
        if (result.getItems().isEmpty()) {
            result.setValidationStatus("NO_ITEMS");
            return;
        }

        BigDecimal itemsSum = result.getItems().stream()
                .map(ParsedItem::getTotalPrice)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        result.setItemsTotal(itemsSum);

        if (result.getTotal() == null) {
            result.setValidationStatus("NO_TOTAL");
            return;
        }

        BigDecimal diff = result.getTotal().subtract(itemsSum).abs();
        if (diff.compareTo(new BigDecimal("0.05")) <= 0) {
            result.setValidationStatus("OK");
        } else {
            result.setValidationStatus("MISMATCH");
            result.setTotalDifference(result.getTotal().subtract(itemsSum).setScale(2, RoundingMode.HALF_UP));
            log.warn("Total mismatch: detected={} items_sum={} diff={}",
                    result.getTotal(), itemsSum, diff);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(sanitizePrice(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeProductName(String name) {
        if (name == null) return null;
        return name
                // Strip leading non-alphanumeric (codes, dots, dashes)
                .replaceAll("^[^a-zA-Z0-9àâäéèêëïîôöùûüçÀÂÄÉÈÊËÏÎÔÖÙÛÜÇ]+", "")
                // Strip trailing noise
                .replaceAll("[^a-zA-Z0-9àâäéèêëïîôöùûüçÀÂÄÉÈÊËÏÎÔÖÙÛÜÇ()%.,/'-]+$", "")
                // Collapse multiple spaces
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    // ── Categorization ─────────────────────────────────────────────────────────

    public String categorize(String productName) {
        if (productName == null) return "Autres";
        String lower = productName.toLowerCase();

        if (matches(lower, "lait", "beurre", "fromage", "yaourt", "crème fraîche", "crème",
                "emmental", "camembert", "brie", "gouda", "comté", "gruyère", "mozzarella"))
            return "Produits laitiers";

        if (matches(lower, "pain", "baguette", "croissant", "brioche", "gâteau", "cake",
                "biscuit", "galette", "madeleine", "viennoiserie", "tarte", "éclair"))
            return "Boulangerie & Pâtisserie";

        if (matches(lower, "poulet", "bœuf", "boeuf", "porc", "veau", "jambon", "saucisse",
                "steak", "escalope", "côtelette", "filet", "rôti", "lardons",
                "poisson", "saumon", "thon", "cabillaud", "crevette", "moule"))
            return "Viandes & Poissons";

        if (matches(lower, "pomme", "banane", "orange", "poire", "kiwi", "fraise", "raisin",
                "tomate", "carotte", "salade", "oignon", "poireau", "courgette",
                "haricot", "épinard", "brocoli", "poivron", "avocat", "champignon"))
            return "Fruits & Légumes";

        if (matches(lower, "eau", "jus", "café", "thé", "bière", "vin", "soda",
                "coca", "pepsi", "limonade", "sirop", "cidre", "lemonade"))
            return "Boissons";

        if (matches(lower, "surgelé", "congelé", "pizza", "nugget", "frite"))
            return "Surgelés";

        if (matches(lower, "pâtes", "riz", "farine", "sucre", "sel", "huile", "vinaigre",
                "sauce", "ketchup", "moutarde", "conserve", "thon en boîte",
                "céréales", "muesli", "confiture", "miel", "chocolat"))
            return "Épicerie";

        if (matches(lower, "shampoo", "shampoing", "savon", "gel douche", "dentifrice",
                "déodorant", "rasoir", "coton", "crème hydratante", "maquillage"))
            return "Hygiène & Beauté";

        if (matches(lower, "lessive", "liquide vaisselle", "nettoyant", "éponge",
                "papier toilette", "essuie-tout", "sac poubelle", "désinfectant"))
            return "Entretien";

        return "Autres";
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ── DTOs ───────────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    public static class ParsedReceipt {
        private String storeName;
        private LocalDate date;
        private BigDecimal total;
        private BigDecimal itemsTotal;
        private String validationStatus;
        private BigDecimal totalDifference;
        private List<ParsedItem> items = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class ParsedItem {
        private String name;
        private BigDecimal quantity = BigDecimal.ONE;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
