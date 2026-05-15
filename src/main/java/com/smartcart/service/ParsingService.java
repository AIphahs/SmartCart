package com.smartcart.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ParsingService {

    // "PRODUCT NAME   2.99" or "PRODUCT NAME  2,99 A"
    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "^(.+?)\\s{2,}(\\d{1,4}[.,]\\d{2})(?:\\s+[A-Z])?\\s*$"
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{2,4})\\b"
    );

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "(?:total\\s*ttc|net\\s*[àa]\\s*payer|montant\\s*total|total)[\\s:]*([\\d]+[.,][\\d]{2})",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> SKIP_KEYWORDS = List.of(
            "sous-total", "subtotal", "tva", " vat", "remise", "discount",
            "avoir", "rendu", "espèces", "carte bancaire", "visa", "mastercard",
            "chèque", "cheque", "paiement", "caissier", "caisse", "siret",
            "www.", "http", "merci", "bienvenue", "fidélité"
    );

    public ParsedReceipt parse(String rawText) {
        ParsedReceipt result = new ParsedReceipt();
        if (rawText == null || rawText.isBlank()) {
            return result;
        }

        List<String> lines = new ArrayList<>();
        for (String line : rawText.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }

        extractStoreName(lines, result);
        extractDate(lines, result);
        extractItemsAndTotal(lines, result);

        log.debug("Parsed receipt: store={}, date={}, items={}, total={}",
                result.getStoreName(), result.getDate(), result.getItems().size(), result.getTotal());
        return result;
    }

    private void extractStoreName(List<String> lines, ParsedReceipt result) {
        StringBuilder storeName = new StringBuilder();
        int used = 0;
        for (String line : lines) {
            if (used >= 3) break;
            if (DATE_PATTERN.matcher(line).find()) break;
            if (line.matches(".*\\d{5}.*")) break; // postal code

            long letters = line.chars().filter(Character::isLetter).count();
            if (letters > line.length() * 0.5 && letters > 3) {
                if (storeName.length() > 0) storeName.append(" ");
                storeName.append(line);
                used++;
            }
        }
        if (storeName.length() > 0) {
            result.setStoreName(storeName.toString().trim());
        }
    }

    private void extractDate(List<String> lines, ParsedReceipt result) {
        for (String line : lines) {
            Matcher m = DATE_PATTERN.matcher(line);
            if (m.find()) {
                try {
                    int day = Integer.parseInt(m.group(1));
                    int month = Integer.parseInt(m.group(2));
                    String yearStr = m.group(3);
                    int year = Integer.parseInt(yearStr);
                    if (yearStr.length() == 2) year += 2000;
                    if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
                        result.setDate(LocalDate.of(year, month, day));
                        return;
                    }
                } catch (DateTimeException | NumberFormatException ignored) {
                }
            }
        }
    }

    private void extractItemsAndTotal(List<String> lines, ParsedReceipt result) {
        for (String line : lines) {
            Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
            if (totalMatcher.find() && result.getTotal() == null) {
                result.setTotal(parseDecimal(totalMatcher.group(1)));
                continue;
            }

            String lower = line.toLowerCase();
            if (SKIP_KEYWORDS.stream().anyMatch(lower::contains)) continue;

            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String name = normalizeProductName(itemMatcher.group(1));
                BigDecimal price = parseDecimal(itemMatcher.group(2));

                if (name.length() >= 2 && price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    ParsedItem item = new ParsedItem();
                    item.setName(name);
                    item.setTotalPrice(price);
                    item.setUnitPrice(price);
                    item.setQuantity(BigDecimal.ONE);
                    result.getItems().add(item);
                }
            }
        }
    }

    private BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeProductName(String name) {
        return name
                .replaceAll("^[^a-zA-Z0-9àâäéèêëïîôöùûüçÀÂÄÉÈÊËÏÎÔÖÙÛÜÇ]+", "")
                .replaceAll("[^a-zA-Z0-9àâäéèêëïîôöùûüçÀÂÄÉÈÊËÏÎÔÖÙÛÜÇ\\s]+$", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    public String categorize(String productName) {
        String lower = productName.toLowerCase();

        if (matches(lower, "lait", "beurre", "fromage", "yaourt", "crème", "cheese", "milk", "cream", "butter", "emmental", "camembert", "brie"))
            return "Produits laitiers";
        if (matches(lower, "pain", "baguette", "croissant", "brioche", "gâteau", "cake", "bread", "biscuit", "galette", "viennoiserie"))
            return "Boulangerie & Pâtisserie";
        if (matches(lower, "poulet", "bœuf", "boeuf", "porc", "veau", "jambon", "saucisse", "steak", "escalope", "côte", "filet", "poisson", "saumon", "thon", "crevette"))
            return "Viandes & Poissons";
        if (matches(lower, "pomme", "banane", "orange", "tomate", "carotte", "salade", "fruit", "légume", "oignon", "poireau", "courgette", "haricot"))
            return "Fruits & Légumes";
        if (matches(lower, "eau", "jus", "café", "thé", "bière", "vin", "soda", "coca", "pepsi", "limonade", "sirop", "cidre"))
            return "Boissons";
        if (matches(lower, "surgelé", "congelé", "frozen"))
            return "Surgelés";
        if (matches(lower, "pâtes", "riz", "farine", "sucre", "sel", "huile", "vinaigre", "sauce", "conserve"))
            return "Épicerie";
        if (matches(lower, "shampoo", "shampoing", "savon", "gel douche", "dentifrice", "déodorant", "rasoir"))
            return "Hygiène & Beauté";
        if (matches(lower, "lessive", "vaisselle", "nettoyant", "éponge", "papier toilette", "essuie", "sac poubelle"))
            return "Entretien";

        return "Autres";
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    @Data
    @NoArgsConstructor
    public static class ParsedReceipt {
        private String storeName;
        private LocalDate date;
        private BigDecimal total;
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
