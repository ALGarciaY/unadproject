package com.unad.project_video_platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final int SIGNATURE_BYTES = 12;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String storeImage(MultipartFile file) {
        requireNotEmpty(file);
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 2 MB");
        }
        String extension = detectImageExtension(file);
        if (extension == null) {
            throw new IllegalArgumentException("Solo se permiten imágenes JPG, PNG o WEBP");
        }
        return doStore(file, extension);
    }

    /**
     * Borra un archivo guardado por este servicio a partir de su ruta pública
     * (/uploads/nombre). Ignora rutas que no sean de ese formato.
     */
    public void delete(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/uploads/")) {
            return;
        }
        String filename = publicPath.substring("/uploads/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(uploadPath().resolve(filename).normalize());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo borrar el archivo anterior: " + e.getMessage());
        }
    }

    private void requireNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
    }

    private String detectImageExtension(MultipartFile file) {
        byte[] head = readHead(file);
        if (matches(head, 0, 0xFF, 0xD8, 0xFF)) {
            return ".jpg";
        }
        if (matches(head, 0, 0x89, 0x50, 0x4E, 0x47)) {
            return ".png";
        }
        if (matches(head, 0, 'R', 'I', 'F', 'F') && matches(head, 8, 'W', 'E', 'B', 'P')) {
            return ".webp";
        }
        return null;
    }

    private byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(SIGNATURE_BYTES);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    private boolean matches(byte[] head, int offset, int... expected) {
        if (head.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((head[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private String doStore(MultipartFile file, String extension) {
        String filename = UUID.randomUUID() + extension;
        try {
            Path dir = uploadPath();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    private Path uploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
}
