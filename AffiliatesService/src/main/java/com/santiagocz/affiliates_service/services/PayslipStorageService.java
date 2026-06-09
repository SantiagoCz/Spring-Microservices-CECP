package com.santiagocz.affiliates_service.services;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;

@Service
public class PayslipStorageService {

    @Value("${storage.affiliates.payslips-root}")
    private String storageRoot;

    public String save(MultipartFile file, Long payslipId) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("No se pudo determinar el tipo del archivo");
        }

        byte[] pdfBytes;
        if (contentType.equals("application/pdf")) {
            try {
                pdfBytes = file.getBytes();
            } catch (IOException e) {
                throw new RuntimeException("Error leyendo el archivo: " + e.getMessage(), e);
            }
        } else if (contentType.startsWith("image/")) {
            pdfBytes = convertImageToPdf(file);
        } else {
            throw new IllegalArgumentException(
                    "Tipo de archivo no soportado: " + contentType + ". Se acepta PDF o imagen.");
        }

        String fileName = payslipId + ".pdf";
        Path fullPath = Paths.get(storageRoot, fileName);

        try {
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, pdfBytes);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando el archivo: " + e.getMessage(), e);
        }

        return fileName;
    }

    public byte[] read(String fileName) {
        Path path = Paths.get(storageRoot, fileName);
        if (!Files.exists(path)) {
            throw new RuntimeException("Archivo no encontrado: " + fileName);
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo el archivo: " + e.getMessage(), e);
        }
    }

    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) return;
        Path path = Paths.get(storageRoot, fileName);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Error borrando el archivo: " + e.getMessage(), e);
        }
    }

    private byte[] convertImageToPdf(MultipartFile imageFile) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(baos)) {

            PdfDocument pdf = new PdfDocument(writer);
            try (Document document = new Document(pdf)) {
                ImageData imageData = ImageDataFactory.create(imageFile.getBytes());
                Image image = new Image(imageData);
                image.setAutoScale(true);
                document.add(image);
            }
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error convirtiendo imagen a PDF: " + e.getMessage(), e);
        }
    }
}