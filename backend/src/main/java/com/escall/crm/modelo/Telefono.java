package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "telefonos")
@Getter
@Setter
public class Telefono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false, length = 20)
    private String numero;

    /** CELULAR, FIJO, REFERENCIA... */
    @Column(nullable = false, length = 20)
    private String tipo = "CELULAR";

    @Column(nullable = false)
    private boolean activo = true;
}
