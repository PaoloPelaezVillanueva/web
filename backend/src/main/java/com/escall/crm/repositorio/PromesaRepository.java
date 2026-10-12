package com.escall.crm.repositorio;

import com.escall.crm.modelo.EstadoPromesa;
import com.escall.crm.modelo.Promesa;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PromesaRepository extends JpaRepository<Promesa, Long> {

    @Query("""
            select p from Promesa p join fetch p.cuenta join fetch p.asesor join fetch p.gestion
            where p.cuenta.cliente.id = :clienteId order by p.fechaCompromiso desc""")
    List<Promesa> delCliente(@Param("clienteId") Long clienteId);

    /** Promesas pendientes del asesor (para su agenda de seguimiento). */
    @Query("""
            select p from Promesa p join fetch p.cuenta c join fetch c.cliente join fetch p.gestion
            where p.asesor.id = :asesorId and p.estado = :estado order by p.fechaCompromiso""")
    List<Promesa> delAsesorPorEstado(@Param("asesorId") Long asesorId, @Param("estado") EstadoPromesa estado);

    /** Promesas generadas en un periodo (fecha de la gestión que las originó). */
    @Query("""
            select p from Promesa p
            join fetch p.cuenta c join fetch c.cliente join fetch p.asesor join fetch p.gestion g
            where g.fechaHora >= :desde and g.fechaHora < :hasta
              and p.asesor.id in :asesores
              and (:cartera is null or c.cartera = :cartera)
              and (:cosecha is null or c.cosecha = :cosecha)
            order by g.fechaHora desc""")
    List<Promesa> delPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                             @Param("asesores") Collection<Long> asesores, @Param("cartera") String cartera,
                             @Param("cosecha") String cosecha, Pageable limite);

    /** Un pago cumple las promesas pendientes de la cuenta hechas hasta la fecha del pago. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Promesa p set p.estado = com.escall.crm.modelo.EstadoPromesa.CUMPLIDA
            where p.cuenta.id = :cuentaId and p.estado = com.escall.crm.modelo.EstadoPromesa.PENDIENTE
              and p.gestion.id in (select g.id from Gestion g where g.fechaHora < :limite)""")
    int marcarCumplidas(@Param("cuentaId") Long cuentaId, @Param("limite") LocalDateTime limite);

    long countByAsesorIdAndEstadoAndFechaCompromisoLessThan(Long asesorId, EstadoPromesa estado, LocalDate fecha);
}
