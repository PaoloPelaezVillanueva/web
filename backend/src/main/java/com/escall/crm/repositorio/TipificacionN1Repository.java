package com.escall.crm.repositorio;

import com.escall.crm.modelo.TipificacionN1;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipificacionN1Repository extends JpaRepository<TipificacionN1, Long> {

    @EntityGraph(attributePaths = "resultados")
    List<TipificacionN1> findAllByOrderByOrdenAscNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
