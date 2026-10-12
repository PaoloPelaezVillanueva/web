package com.escall.crm.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String tipoDocumento = "DNI";

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(nullable = false, length = 150)
    private String nombres;

    @Column(length = 150)
    private String email;

    @Column(length = 300)
    private String direccion;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Telefono> telefonos = new ArrayList<>();

    /** Agrega el teléfono solo si el cliente aún no lo tiene. */
    public void agregarTelefono(String numero, String tipo) {
        if (numero == null || numero.isBlank()) {
            return;
        }
        boolean existe = telefonos.stream().anyMatch(t -> t.getNumero().equals(numero.trim()));
        if (!existe) {
            Telefono t = new Telefono();
            t.setCliente(this);
            t.setNumero(numero.trim());
            t.setTipo(tipo);
            telefonos.add(t);
        }
    }
}
