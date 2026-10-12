package com.escall.crm.repositorio;

import com.escall.crm.modelo.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroOperacion(String numeroOperacion);

    List<Cuenta> findByClienteIdOrderByNumeroOperacionAsc(Long clienteId);

    boolean existsByClienteId(Long clienteId);

    @Query("select distinct c.cartera from Cuenta c order by c.cartera")
    List<String> carteras();

    @Query("select distinct c.cosecha from Cuenta c order by c.cosecha desc")
    List<String> cosechas();
}
