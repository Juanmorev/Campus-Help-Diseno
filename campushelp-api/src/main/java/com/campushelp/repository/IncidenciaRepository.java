package com.campushelp.repository;

import com.campushelp.model.Incidencia;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IncidenciaRepository extends MongoRepository<Incidencia, String>, IncidenciaRepositoryCustom {

    Optional<Incidencia> findByCodigo(String codigo);

    List<Incidencia> findByResponsableId(String responsableId);

    List<Incidencia> findByLugarId(String lugarId);
}
