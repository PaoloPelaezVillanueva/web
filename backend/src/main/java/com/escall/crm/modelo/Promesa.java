package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Compromiso de pago que nace de una gestión. */
@Entity
@Table(name = "promesas")
@Getter
@Setter
public class Promesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestion_id", unique = true)
    private Gestion gestion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asesor_id")
    private Usuario asesor;

    @Column(nullable = false)
    private LocalDate fechaCompromiso;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPromesa estado = EstadoPromesa.PENDIENTE;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();
}
