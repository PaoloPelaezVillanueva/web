package com.escall.crm.repositorio;

import com.escall.crm.modelo.Asignacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    Optional<Asignacion> findByCuentaIdAndActivaTrue(Long cuentaId);

    @EntityGraph(attributePaths = "asesor")
    List<Asignacion> findByCuentaIdInAndActivaTrue(Collection<Long> cuentaIds);

    /** Cartera actual de un asesor, con cuenta y cliente cargados en una sola consulta. */
    @EntityGraph(attributePaths = {"cuenta", "cuenta.cliente"})
    List<Asignacion> findByAsesorIdAndActivaTrueOrderByCuentaDiasMoraDesc(Long asesorId);

    long countByAsesorIdAndActivaTrue(Long asesorId);
}
