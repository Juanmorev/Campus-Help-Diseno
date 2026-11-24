package com.campushelp.service;

import com.campushelp.exception.ArchivoInvalidoException;
import com.campushelp.exception.RecursoNoEncontradoException;
import com.campushelp.model.Evidencia;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/** Guarda y entrega las fotografias de evidencia usando MongoDB GridFS. */
@Service
public class EvidenciaService {

    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final GridFsTemplate gridFs;

    public EvidenciaService(GridFsTemplate gridFs) {
        this.gridFs = gridFs;
    }

    public Evidencia guardar(MultipartFile archivo, String autorId) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Adjunta una fotografía (campo 'archivo')");
        }
        String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        if (!TIPOS.contains(tipo)) {
            throw new ArchivoInvalidoException("La evidencia debe ser una imagen JPG, PNG, WEBP o GIF (llegó: "
                    + (tipo.isBlank() ? "desconocido" : tipo) + ")");
        }
        String nombre = archivo.getOriginalFilename() == null ? "foto" : archivo.getOriginalFilename();
        try (InputStream in = archivo.getInputStream()) {
            return guardar(in, nombre, tipo, archivo.getSize(), autorId);
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer la fotografía");
        }
    }

    public Evidencia guardar(InputStream contenido, String nombre, String tipo, long tamano, String autorId) {
        ObjectId id = gridFs.store(contenido, nombre, tipo);
        return new Evidencia(id.toHexString(), nombre, tipo, tamano, autorId);
    }

    public GridFsResource cargar(String id) {
        if (!ObjectId.isValid(id)) throw new RecursoNoEncontradoException("Fotografía", id);
        GridFSFile archivo = gridFs.findOne(Query.query(Criteria.where("_id").is(new ObjectId(id))));
        if (archivo == null) throw new RecursoNoEncontradoException("Fotografía", id);
        return gridFs.getResource(archivo);
    }
}
