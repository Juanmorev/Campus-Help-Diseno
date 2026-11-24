package com.campushelp.repository;

import com.campushelp.model.Lugar;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LugarRepository extends MongoRepository<Lugar, String> {

    Optional<Lugar> findByCodigo(String codigo);

    List<Lugar> findByActivoTrueOrderByEdificioAscPisoAscCodigoAsc();
}
