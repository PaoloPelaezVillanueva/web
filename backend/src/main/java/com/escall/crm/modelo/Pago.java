package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Pago de una cuenta, cargado desde Integración de Datos. */
@Entity
@Table(name = "pagos")
@Getter
@Setter
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

    @Column(nullable = false)
    private LocalDate fechaPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Número de voucher u operación bancaria ("" si no se informa). */
    @Column(nullable = false, length = 60)
    private String referencia = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carga_id")
    private Carga carga;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();
}
