package com.campushelp.repository;

import com.campushelp.dto.FiltroIncidencias;
import com.campushelp.model.Incidencia;

import java.util.List;

/** Consultas con filtros dinamicos (MongoTemplate). */
public interface IncidenciaRepositoryCustom {

    List<Incidencia> buscar(FiltroIncidencias filtro);
}
