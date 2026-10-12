package com.escall.crm.repositorio;

import com.escall.crm.modelo.Gestion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface GestionRepository extends JpaRepository<Gestion, Long> {

    /** Historial de gestiones de todas las cuentas de un cliente. */
    @Query("""
            select g from Gestion g
            join fetch g.cuenta join fetch g.asesor join fetch g.tipificacionN1 join fetch g.tipificacionN2
            left join fetch g.promesa
            where g.cuenta.cliente.id = :clienteId
            order by g.fechaHora desc""")
    List<Gestion> historialDelCliente(@Param("clienteId") Long clienteId);

    /** Gestiones de un periodo para dashboard y reportes (cartera y cosecha son opcionales). */
    @Query("""
            select g from Gestion g
            join fetch g.cuenta c join fetch c.cliente join fetch g.asesor
            join fetch g.tipificacionN1 join fetch g.tipificacionN2 left join fetch g.promesa
            where g.fechaHora >= :desde and g.fechaHora < :hasta
              and g.asesor.id in :asesores
              and (:cartera is null or c.cartera = :cartera)
              and (:cosecha is null or c.cosecha = :cosecha)
            order by g.fechaHora desc""")
    List<Gestion> delPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                             @Param("asesores") Collection<Long> asesores, @Param("cartera") String cartera,
                             @Param("cosecha") String cosecha, Pageable limite);
}
