package com.smartcart.service;

import com.smartcart.dto.ReceiptDto;
import com.smartcart.dto.ReceiptItemDto;
import com.smartcart.dto.StoreDto;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.model.Receipt;
import com.smartcart.model.ReceiptItem;
import com.smartcart.model.Store;
import com.smartcart.repository.ReceiptRepository;
import com.smartcart.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final StoreRepository storeRepository;
    private final OcrService ocrService;
    private final ParsingService parsingService;

    @Transactional
    public ReceiptDto processReceipt(MultipartFile file, String overrideStoreName) throws IOException {
        String imagePath = ocrService.saveFile(file);
        String rawText = ocrService.extractText(new File(imagePath));

        ParsingService.ParsedReceipt parsed = parsingService.parse(rawText);

        String storeName = overrideStoreName != null && !overrideStoreName.isBlank()
                ? overrideStoreName.trim()
                : (parsed.getStoreName() != null ? parsed.getStoreName() : "Unknown Store");

        Store store = storeRepository.findByNameIgnoreCase(storeName).orElseGet(() -> {
            Store s = new Store();
            s.setName(storeName);
            return storeRepository.save(s);
        });

        Receipt receipt = new Receipt();
        receipt.setStore(store);
        receipt.setPurchaseDate(parsed.getDate() != null ? parsed.getDate() : LocalDate.now());
        receipt.setTotalAmount(parsed.getTotal());
        receipt.setImagePath(imagePath);
        receipt.setRawText(rawText);

        List<ReceiptItem> items = parsed.getItems().stream().map(pi -> {
            ReceiptItem item = new ReceiptItem();
            item.setReceipt(receipt);
            item.setProductName(pi.getName());
            item.setQuantity(pi.getQuantity());
            item.setUnitPrice(pi.getUnitPrice());
            item.setTotalPrice(pi.getTotalPrice());
            item.setCategory(parsingService.categorize(pi.getName()));
            return item;
        }).collect(Collectors.toList());

        receipt.setItems(items);

        if (receipt.getTotalAmount() == null && !items.isEmpty()) {
            BigDecimal computed = items.stream()
                    .map(ReceiptItem::getTotalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            receipt.setTotalAmount(computed);
        }

        Receipt saved = receiptRepository.save(receipt);
        log.info("Receipt {} saved: store={}, items={}, total={}", saved.getId(), storeName, items.size(), saved.getTotalAmount());
        return toDto(saved);
    }

    public Page<ReceiptDto> getAllReceipts(Pageable pageable) {
        return receiptRepository.findAll(pageable).map(this::toDto);
    }

    public ReceiptDto getReceiptById(Long id) {
        return receiptRepository.findByIdWithDetails(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + id));
    }

    public List<ReceiptDto> getReceiptsByStore(Long storeId) {
        return receiptRepository.findByStoreId(storeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReceipt(Long id) {
        if (!receiptRepository.existsById(id)) {
            throw new ResourceNotFoundException("Receipt not found: " + id);
        }
        receiptRepository.deleteById(id);
    }

    private ReceiptDto toDto(Receipt r) {
        ReceiptDto dto = new ReceiptDto();
        dto.setId(r.getId());
        dto.setPurchaseDate(r.getPurchaseDate());
        dto.setTotalAmount(r.getTotalAmount());
        dto.setCurrency(r.getCurrency());
        dto.setImagePath(r.getImagePath());
        dto.setCreatedAt(r.getCreatedAt());

        if (r.getStore() != null) {
            StoreDto sd = new StoreDto();
            sd.setId(r.getStore().getId());
            sd.setName(r.getStore().getName());
            sd.setAddress(r.getStore().getAddress());
            dto.setStore(sd);
        }

        if (r.getItems() != null) {
            dto.setItems(r.getItems().stream().map(this::toItemDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private ReceiptItemDto toItemDto(ReceiptItem i) {
        ReceiptItemDto dto = new ReceiptItemDto();
        dto.setId(i.getId());
        dto.setProductName(i.getProductName());
        dto.setQuantity(i.getQuantity());
        dto.setUnitPrice(i.getUnitPrice());
        dto.setTotalPrice(i.getTotalPrice());
        dto.setCategory(i.getCategory());
        return dto;
    }
}
