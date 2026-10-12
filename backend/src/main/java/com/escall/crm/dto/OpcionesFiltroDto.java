package com.escall.crm.dto;

import java.util.List;

/** Valores para los desplegables de filtros, según lo que el usuario puede ver. */
public record OpcionesFiltroDto(List<String> carteras, List<String> cosechas, List<Opcion> asesores) {

    public record Opcion(Long id, String nombre) {
    }
}
