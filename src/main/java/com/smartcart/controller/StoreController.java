package com.smartcart.controller;

import com.smartcart.dto.ReceiptDto;
import com.smartcart.dto.StoreDto;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.model.Store;
import com.smartcart.repository.StoreRepository;
import com.smartcart.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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

    private StoreDto toDto(Store s) {
        StoreDto dto = new StoreDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setAddress(s.getAddress());
        dto.setReceiptCount(s.getReceipts() != null ? s.getReceipts().size() : 0);
        return dto;
    }
}
