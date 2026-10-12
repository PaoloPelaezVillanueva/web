package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Nivel 2 del árbol: resultado de la gestión. Depende de un nivel 1. */
@Entity
@Table(name = "tipificaciones_n2")
@Getter
@Setter
public class TipificacionN2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "n1_id")
    private TipificacionN1 n1;

    @Column(nullable = false, length = 80)
    private String nombre;

    /** Si es true, la gestión exige fecha y monto de compromiso (genera una promesa). */
    @Column(nullable = false)
    private boolean requiereCompromiso;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;
}
