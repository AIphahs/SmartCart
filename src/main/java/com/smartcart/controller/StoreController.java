package com.smartcart.controller;

import com.smartcart.dto.ReceiptDto;
import com.smartcart.dto.StoreDto;
import com.smartcart.dto.StoreHourDto;
import com.smartcart.dto.StoreUpdateRequest;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.model.Store;
import com.smartcart.model.StoreHours;
import com.smartcart.repository.StoreRepository;
import com.smartcart.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Manage stores")
public class StoreController {

    private final StoreRepository storeRepository;
    private final ReceiptService receiptService;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List all stores")
    public ResponseEntity<List<StoreDto>> getAll() {
        List<StoreDto> stores = storeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get store by ID")
    public ResponseEntity<StoreDto> getById(@PathVariable Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));
        return ResponseEntity.ok(toDto(store));
    }

    @GetMapping("/{id}/receipts")
    @Operation(summary = "Get all receipts from a specific store")
    public ResponseEntity<List<ReceiptDto>> getReceipts(@PathVariable Long id) {
        if (!storeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Store not found: " + id);
        }
        return ResponseEntity.ok(receiptService.getReceiptsByStore(id));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update store details (address, phone, website, opening hours)")
    public ResponseEntity<StoreDto> update(@PathVariable Long id, @Valid @RequestBody StoreUpdateRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));

        store.setAddress(request.getAddress());
        store.setPhone(request.getPhone());
        store.setWebsite(request.getWebsite());

        store.getHours().clear();
        if (request.getHours() != null) {
            for (StoreHourDto h : request.getHours()) {
                StoreHours entity = new StoreHours();
                entity.setStore(store);
                entity.setDayOfWeek(h.getDayOfWeek());
                entity.setOpenTime(h.getOpenTime());
                entity.setCloseTime(h.getCloseTime());
                entity.setClosed(h.isClosed());
                store.getHours().add(entity);
            }
        }

        return ResponseEntity.ok(toDto(storeRepository.save(store)));
    }

    private StoreDto toDto(Store s) {
        StoreDto dto = new StoreDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setAddress(s.getAddress());
        dto.setPhone(s.getPhone());
        dto.setWebsite(s.getWebsite());
        dto.setReceiptCount(s.getReceipts() != null ? s.getReceipts().size() : 0);
        dto.setHours(s.getHours() != null
                ? s.getHours().stream().map(this::toHourDto).collect(Collectors.toList())
                : List.of());
        dto.setOpenNow(isOpenNow(s));
        return dto;
    }

    private StoreHourDto toHourDto(StoreHours h) {
        StoreHourDto dto = new StoreHourDto();
        dto.setDayOfWeek(h.getDayOfWeek());
        dto.setOpenTime(h.getOpenTime());
        dto.setCloseTime(h.getCloseTime());
        dto.setClosed(h.isClosed());
        return dto;
    }

    private boolean isOpenNow(Store s) {
        if (s.getHours() == null) return false;
        LocalDateTime now = LocalDateTime.now();
        int today = now.getDayOfWeek().getValue();
        LocalTime time = now.toLocalTime();
        return s.getHours().stream()
                .filter(h -> h.getDayOfWeek() == today && !h.isClosed())
                .anyMatch(h -> h.getOpenTime() != null && h.getCloseTime() != null
                        && !time.isBefore(h.getOpenTime()) && !time.isAfter(h.getCloseTime()));
    }
}
