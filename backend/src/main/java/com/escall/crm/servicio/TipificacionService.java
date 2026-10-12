package com.escall.crm.servicio;

import com.escall.crm.dto.TipificacionDto;
import com.escall.crm.dto.TipificacionN1Request;
import com.escall.crm.dto.TipificacionN2Request;
import com.escall.crm.excepcion.RecursoNoEncontradoException;
import com.escall.crm.excepcion.ReglaNegocioException;
import com.escall.crm.modelo.TipificacionN1;
import com.escall.crm.modelo.TipificacionN2;
import com.escall.crm.repositorio.TipificacionN1Repository;
import com.escall.crm.repositorio.TipificacionN2Repository;
import com.escall.crm.seguridad.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Árbol de tipificaciones (RF09 y RF22). Nada se borra: se desactiva para conservar el historial. */
@Service
@RequiredArgsConstructor
public class TipificacionService {

    private final TipificacionN1Repository n1Repo;
    private final TipificacionN2Repository n2Repo;
    private final UsuarioActual usuarioActual;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<TipificacionDto> arbol(boolean soloActivos) {
        return n1Repo.findAllByOrderByOrdenAscNombreAsc().stream()
                .filter(n1 -> !soloActivos || n1.isActivo())
                .map(n1 -> TipificacionDto.desde(n1, soloActivos))
                .toList();
    }

    @Transactional
    public TipificacionDto guardarN1(Long id, TipificacionN1Request req) {
        String nombre = req.nombre().trim();
        boolean duplicado = id == null ? n1Repo.existsByNombreIgnoreCase(nombre)
                : n1Repo.existsByNombreIgnoreCaseAndIdNot(nombre, id);
        if (duplicado) {
            throw new ReglaNegocioException("Ya existe un nivel 1 con ese nombre");
        }
        TipificacionN1 n1 = id == null ? new TipificacionN1()
                : n1Repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Tipificación no encontrada"));
        n1.setNombre(nombre);
        n1.setEsContacto(req.esContacto());
        n1.setOrden(req.orden());
        n1.setActivo(req.activo());
        n1 = n1Repo.save(n1);
        auditoria.registrar(usuarioActual.obtener().getUsuario(), id == null ? "CREAR" : "EDITAR",
                "TipificacionN1", n1.getId(), nombre + (req.activo() ? "" : " (inactiva)"));
        return TipificacionDto.desde(n1, false);
    }

    @Transactional
    public TipificacionDto.Resultado guardarN2(Long id, TipificacionN2Request req) {
        String nombre = req.nombre().trim();
        TipificacionN1 n1 = n1Repo.findById(req.n1Id())
                .orElseThrow(() -> new RecursoNoEncontradoException("El nivel 1 no existe"));
        boolean duplicado = id == null ? n2Repo.existsByN1IdAndNombreIgnoreCase(n1.getId(), nombre)
                : n2Repo.existsByN1IdAndNombreIgnoreCaseAndIdNot(n1.getId(), nombre, id);
        if (duplicado) {
            throw new ReglaNegocioException("Ese resultado ya existe dentro de \"" + n1.getNombre() + "\"");
        }
        TipificacionN2 n2 = id == null ? new TipificacionN2()
                : n2Repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Tipificación no encontrada"));
        n2.setN1(n1);
        n2.setNombre(nombre);
        n2.setRequiereCompromiso(req.requiereCompromiso());
        n2.setOrden(req.orden());
        n2.setActivo(req.activo());
        n2 = n2Repo.save(n2);
        auditoria.registrar(usuarioActual.obtener().getUsuario(), id == null ? "CREAR" : "EDITAR",
                "TipificacionN2", n2.getId(), n1.getNombre() + " > " + nombre);
        return TipificacionDto.Resultado.desde(n2);
    }

    /** Borrado lógico: la tipificación queda inactiva y sigue visible en el historial de gestiones. */
    @Transactional
    public void desactivarN1(Long id) {
        TipificacionN1 n1 = n1Repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipificación no encontrada"));
        n1.setActivo(false);
        n1Repo.save(n1);
        auditoria.registrar(usuarioActual.obtener().getUsuario(), "DESACTIVAR",
                "TipificacionN1", n1.getId(), n1.getNombre());
    }

    /** Borrado lógico de un resultado de nivel 2 (ver desactivarN1). */
    @Transactional
    public void desactivarN2(Long id) {
        TipificacionN2 n2 = n2Repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipificación no encontrada"));
        n2.setActivo(false);
        n2Repo.save(n2);
        auditoria.registrar(usuarioActual.obtener().getUsuario(), "DESACTIVAR",
                "TipificacionN2", n2.getId(), n2.getN1().getNombre() + " > " + n2.getNombre());
    }
}
