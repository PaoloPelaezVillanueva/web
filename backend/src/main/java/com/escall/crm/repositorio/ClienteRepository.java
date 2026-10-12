package com.escall.crm.repositorio;

import com.escall.crm.modelo.Cliente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    /** Búsqueda por documento, nombre o número de operación (todas las carteras). */
    @Query("""
            select c from Cliente c
            where c.documento like :patron
               or lower(c.nombres) like :patron
               or exists (select 1 from Cuenta cu where cu.cliente = c and cu.numeroOperacion like :patron)
            order by c.nombres""")
    List<Cliente> buscar(@Param("patron") String patron, Pageable limite);

    /** Igual que buscar(), pero solo clientes con alguna cuenta asignada a los asesores indicados. */
    @Query("""
            select c from Cliente c
            where (c.documento like :patron
                   or lower(c.nombres) like :patron
                   or exists (select 1 from Cuenta cu where cu.cliente = c and cu.numeroOperacion like :patron))
              and exists (select 1 from Asignacion a
                          where a.cuenta.cliente = c and a.activa = true and a.asesor.id in :asesores)
            order by c.nombres""")
    List<Cliente> buscarEnCartera(@Param("patron") String patron, @Param("asesores") Collection<Long> asesores,
                                  Pageable limite);

    @Query("""
            select count(a) > 0 from Asignacion a
            where a.cuenta.cliente.id = :clienteId and a.activa = true and a.asesor.id in :asesores""")
    boolean esVisiblePara(@Param("clienteId") Long clienteId, @Param("asesores") Collection<Long> asesores);
}
