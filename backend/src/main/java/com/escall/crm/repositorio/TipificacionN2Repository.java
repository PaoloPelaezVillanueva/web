package com.escall.crm.repositorio;

import com.escall.crm.modelo.TipificacionN2;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipificacionN2Repository extends JpaRepository<TipificacionN2, Long> {

    @EntityGraph(attributePaths = "n1")
    Optional<TipificacionN2> findWithN1ById(Long id);

    boolean existsByN1IdAndNombreIgnoreCase(Long n1Id, String nombre);

    boolean existsByN1IdAndNombreIgnoreCaseAndIdNot(Long n1Id, String nombre, Long id);
}
