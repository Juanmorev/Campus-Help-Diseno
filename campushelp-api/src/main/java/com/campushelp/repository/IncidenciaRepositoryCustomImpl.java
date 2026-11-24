package com.campushelp.repository;

import com.campushelp.dto.FiltroIncidencias;
import com.campushelp.model.Incidencia;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class IncidenciaRepositoryCustomImpl implements IncidenciaRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public IncidenciaRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Incidencia> buscar(FiltroIncidencias f) {
        Query q = new Query();
        if (f.estado() != null) q.addCriteria(Criteria.where("estado").is(f.estado()));
        if (f.tipo() != null) q.addCriteria(Criteria.where("tipo").is(f.tipo()));
        if (f.prioridad() != null) q.addCriteria(Criteria.where("prioridad").is(f.prioridad()));
        if (f.lugarId() != null) q.addCriteria(Criteria.where("lugarId").is(f.lugarId()));
        if (f.reportadaPorId() != null) q.addCriteria(Criteria.where("reportadaPorId").is(f.reportadaPorId()));
        if (f.responsableId() != null) q.addCriteria(Criteria.where("responsableId").is(f.responsableId()));
        q.with(Sort.by(Sort.Direction.DESC, "fechaReporte"));
        return mongoTemplate.find(q, Incidencia.class);
    }
}
