package com.smartcart.controller;

import com.smartcart.dto.ReceiptDto;
import com.smartcart.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Upload and manage grocery receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a receipt image", description = "Runs OCR on the image and extracts items automatically")
    public ResponseEntity<ReceiptDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "storeName", required = false) String storeName) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ReceiptDto receipt = receiptService.processReceipt(file, storeName);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }

    @GetMapping
    @Operation(summary = "List all receipts (paginated)")
    public ResponseEntity<Page<ReceiptDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(receiptService.getAllReceipts(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a receipt by ID with all items")
    public ResponseEntity<ReceiptDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a receipt")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        receiptService.deleteReceipt(id);
        return ResponseEntity.noContent().build();
    }
}
