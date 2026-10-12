package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

/** Interacción registrada por un asesor sobre una cuenta y un teléfono. */
@Entity
@Table(name = "gestiones")
@Getter
@Setter
public class Gestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asesor_id")
    private Usuario asesor;

    /** Fecha y hora local de la gestión (Lima). */
    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 20)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipificacion_n1_id")
    private TipificacionN1 tipificacionN1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipificacion_n2_id")
    private TipificacionN2 tipificacionN2;

    @Column(nullable = false, length = 500)
    private String observacion;

    @OneToOne(mappedBy = "gestion", cascade = CascadeType.ALL)
    private Promesa promesa;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();
}
