package com.escall.crm.repositorio;

import com.escall.crm.modelo.Carga;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CargaRepository extends JpaRepository<Carga, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<Carga> findTop20ByOrderByCreadoEnDesc();
}
