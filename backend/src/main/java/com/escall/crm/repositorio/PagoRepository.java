package com.escall.crm.repositorio;

import com.escall.crm.modelo.Pago;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("select p from Pago p join fetch p.cuenta where p.cuenta.cliente.id = :clienteId order by p.fechaPago desc")
    List<Pago> delCliente(@Param("clienteId") Long clienteId);

    /** Total pagado por cuenta: filas [cuentaId, suma]. */
    @Query("select p.cuenta.id, sum(p.monto) from Pago p where p.cuenta.id in :cuentas group by p.cuenta.id")
    List<Object[]> totalesPorCuenta(@Param("cuentas") Collection<Long> cuentas);

    boolean existsByCuentaIdAndFechaPagoAndMontoAndReferencia(Long cuentaId, LocalDate fecha, BigDecimal monto,
                                                              String referencia);

    /**
     * Pagos del periodo junto con el asesor que tiene asignada la cuenta (puede ser null).
     * Devuelve filas [Pago, Usuario]. Si "todos" es true no se filtra por asesor (vista del administrador).
     */
    @Query("""
            select p, a.asesor from Pago p
            join fetch p.cuenta c join fetch c.cliente
            left join Asignacion a on a.cuenta = c and a.activa = true
            where p.fechaPago >= :desde and p.fechaPago <= :hasta
              and (:todos = true or a.asesor.id in :asesores)
              and (:cartera is null or c.cartera = :cartera)
              and (:cosecha is null or c.cosecha = :cosecha)
            order by p.fechaPago desc""")
    List<Object[]> delPeriodo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                              @Param("todos") boolean todos, @Param("asesores") Collection<Long> asesores,
                              @Param("cartera") String cartera, @Param("cosecha") String cosecha,
                              Pageable limite);
}
