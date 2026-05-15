package com.smartcart.service;

import com.smartcart.dto.CategorySpendingDto;
import com.smartcart.dto.MonthlySpendingDto;
import com.smartcart.dto.PriceComparisonDto;
import com.smartcart.dto.SpendingAnalyticsDto;
import com.smartcart.repository.ReceiptItemRepository;
import com.smartcart.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;

    public SpendingAnalyticsDto getSpendingStats(LocalDate from, LocalDate to) {
        Object[] row = receiptRepository.findSpendingStats(from, to);
        SpendingAnalyticsDto dto = new SpendingAnalyticsDto();

        if (row != null && row[0] != null) {
            BigDecimal total = (BigDecimal) row[0];
            long count = ((Number) row[1]).longValue();
            dto.setTotalSpending(total);
            dto.setReceiptCount(count);
            dto.setAveragePerReceipt(count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            dto.setFirstReceiptDate(toLocalDate(row[2]));
            dto.setLastReceiptDate(toLocalDate(row[3]));
        } else {
            dto.setTotalSpending(BigDecimal.ZERO);
            dto.setReceiptCount(0);
            dto.setAveragePerReceipt(BigDecimal.ZERO);
        }
        return dto;
    }

    public List<MonthlySpendingDto> getMonthlySpending(int months) {
        LocalDate from = LocalDate.now().minusMonths(months);
        return receiptRepository.findMonthlySpending(from).stream()
                .map(row -> {
                    MonthlySpendingDto dto = new MonthlySpendingDto();
                    dto.setYear(((Number) row[0]).intValue());
                    dto.setMonth(((Number) row[1]).intValue());
                    dto.setMonthLabel(Month.of(dto.getMonth()).getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + dto.getYear());
                    dto.setTotal(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
                    dto.setReceiptCount(((Number) row[3]).longValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<CategorySpendingDto> getCategoryBreakdown(LocalDate from, LocalDate to) {
        List<Object[]> rows = receiptItemRepository.findCategorySpending(from, to);

        BigDecimal grandTotal = rows.stream()
                .map(r -> r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream().map(row -> {
            CategorySpendingDto dto = new CategorySpendingDto();
            dto.setCategory((String) row[0]);
            BigDecimal total = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            dto.setTotal(total);
            dto.setItemCount(((Number) row[2]).longValue());
            dto.setPercentage(grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? total.divide(grandTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0);
            return dto;
        }).collect(Collectors.toList());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof Date d) return d.toLocalDate();
        return null;
    }

    public List<PriceComparisonDto> comparePrices(String product) {
        if (product == null || product.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        return receiptItemRepository.findPriceComparisonByProduct(product.trim()).stream()
                .map(row -> {
                    PriceComparisonDto dto = new PriceComparisonDto();
                    dto.setStoreName((String) row[0]);
                    dto.setAveragePrice(row[1] != null ? ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP) : null);
                    dto.setMinPrice(row[2] != null ? (BigDecimal) row[2] : null);
                    dto.setMaxPrice(row[3] != null ? (BigDecimal) row[3] : null);
                    dto.setOccurrences(((Number) row[4]).longValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
