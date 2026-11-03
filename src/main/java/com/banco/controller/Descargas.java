package com.banco.controller;

import com.banco.patterns.proxy.ArchivoReporte;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Arma la respuesta HTTP para descargar un reporte. */
final class Descargas {

    private Descargas() {
    }

    static ResponseEntity<byte[]> archivo(ArchivoReporte archivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .body(archivo.contenido());
    }
}
