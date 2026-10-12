package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Nivel 1 del árbol: tipo de contacto (directo, indirecto, no contacto, ilocalizado). */
@Entity
@Table(name = "tipificaciones_n1")
@Getter
@Setter
public class TipificacionN1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    /** true si cuenta como "contacto" para los indicadores del dashboard. */
    @Column(nullable = false)
    private boolean esContacto;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "n1")
    @OrderBy("orden, nombre")
    private List<TipificacionN2> resultados = new ArrayList<>();
}
