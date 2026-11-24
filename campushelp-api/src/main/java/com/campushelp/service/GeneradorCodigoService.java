package com.campushelp.service;

import com.campushelp.model.Contador;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/** Genera codigos consecutivos (CH-000001) con un contador atomico en MongoDB. */
@Service
public class GeneradorCodigoService {

    private final MongoTemplate mongoTemplate;

    public GeneradorCodigoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String siguienteCodigo() {
        Contador c = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is("incidencias")),
                new Update().inc("valor", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Contador.class);
        return String.format("CH-%06d", c.getValor());
    }
}
