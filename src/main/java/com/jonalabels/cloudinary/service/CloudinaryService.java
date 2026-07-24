package com.jonalabels.cloudinary.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
@ConditionalOnBean(Cloudinary.class)
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String subirArchivo(MultipartFile archivo) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload_", "_" + archivo.getOriginalFilename());
            archivo.transferTo(tempFile);

            @SuppressWarnings("unchecked")
            var resultado = (Map<String, Object>) cloudinary.uploader().upload(tempFile, Map.of());

            return (String) resultado.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Cloudinary", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
