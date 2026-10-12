package com.escall.crm.dto;

import com.escall.crm.modelo.TipificacionN1;
import com.escall.crm.modelo.TipificacionN2;

import java.util.List;

/** Nodo de nivel 1 del árbol con sus resultados de nivel 2 (RF09). */
public record TipificacionDto(Long id, String nombre, boolean esContacto, int orden, boolean activo,
                              List<Resultado> resultados) {

    public record Resultado(Long id, Long n1Id, String nombre, boolean requiereCompromiso, int orden, boolean activo) {

        public static Resultado desde(TipificacionN2 t) {
            return new Resultado(t.getId(), t.getN1().getId(), t.getNombre(), t.isRequiereCompromiso(),
                    t.getOrden(), t.isActivo());
        }
    }

    public static TipificacionDto desde(TipificacionN1 n1, boolean soloActivos) {
        List<Resultado> resultados = n1.getResultados().stream()
                .filter(n2 -> !soloActivos || n2.isActivo())
                .map(Resultado::desde)
                .toList();
        return new TipificacionDto(n1.getId(), n1.getNombre(), n1.isEsContacto(), n1.getOrden(), n1.isActivo(),
                resultados);
    }
}
