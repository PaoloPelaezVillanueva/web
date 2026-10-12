package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Bitácora de acciones críticas: quién hizo qué y cuándo (RF23). */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String usuario;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(length = 40)
    private String entidadId;

    @Column(length = 1000)
    private String detalle;

    @Column(nullable = false)
    private Instant fecha = Instant.now();
}
