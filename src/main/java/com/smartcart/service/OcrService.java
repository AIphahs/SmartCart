package com.smartcart.service;

import com.smartcart.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class OcrService {

    @Value("${ocr.tessdata-path}")
    private String tessdataPath;

    @Value("${ocr.language:fra+eng}")
    private String language;

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    public String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath.toFile());

        log.info("Saved receipt image: {}", filePath);
        return filePath.toString();
    }

    public String extractText(File imageFile) {
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage(language);
            tesseract.setPageSegMode(6);

            String text = tesseract.doOCR(imageFile);
            log.debug("OCR extracted {} chars from {}", text.length(), imageFile.getName());
            return text;
        } catch (TesseractException e) {
            log.error("OCR failed for {}", imageFile.getName(), e);
            throw new OcrProcessingException("OCR processing failed: " + e.getMessage(), e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
