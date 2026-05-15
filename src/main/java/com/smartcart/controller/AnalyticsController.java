package com.smartcart.controller;

import com.smartcart.dto.CategorySpendingDto;
import com.smartcart.dto.MonthlySpendingDto;
import com.smartcart.dto.PriceComparisonDto;
import com.smartcart.dto.SpendingAnalyticsDto;
import com.smartcart.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Spending analytics and price comparison")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/spending/total")
    @Operation(summary = "Get overall spending statistics")
    public ResponseEntity<SpendingAnalyticsDto> totalSpending(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getSpendingStats(from, to));
    }

    @GetMapping("/spending/monthly")
    @Operation(summary = "Get monthly spending breakdown")
    public ResponseEntity<List<MonthlySpendingDto>> monthlySpending(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(analyticsService.getMonthlySpending(months));
    }

    @GetMapping("/spending/categories")
    @Operation(summary = "Get spending breakdown by product category")
    public ResponseEntity<List<CategorySpendingDto>> categoryBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(from, to));
    }

    @GetMapping("/prices/compare")
    @Operation(summary = "Compare the price of a product across stores")
    public ResponseEntity<List<PriceComparisonDto>> comparePrices(
            @RequestParam String product) {
        return ResponseEntity.ok(analyticsService.comparePrices(product));
    }
}
