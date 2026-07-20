package com.jonalabels.archivo.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String guardar(MultipartFile archivo);

    Resource cargarComoRecurso(String nombreArchivo);
}
