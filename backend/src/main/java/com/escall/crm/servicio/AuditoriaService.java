package com.escall.crm.servicio;

import com.escall.crm.dto.AuditoriaDto;
import com.escall.crm.dto.Pagina;
import com.escall.crm.modelo.Auditoria;
import com.escall.crm.repositorio.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra las acciones críticas (RF23): quién, qué, sobre qué y cuándo. */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository repositorio;

    @Transactional
    public void registrar(String usuario, String accion, String entidad, Object entidadId, String detalle) {
        Auditoria a = new Auditoria();
        a.setUsuario(usuario);
        a.setAccion(accion);
        a.setEntidad(entidad);
        a.setEntidadId(entidadId != null ? entidadId.toString() : null);
        a.setDetalle(detalle != null && detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle);
        repositorio.save(a);
    }

    @Transactional(readOnly = true)
    public Pagina<AuditoriaDto> listar(int pagina, int tamano) {
        return Pagina.desde(repositorio.findAllByOrderByFechaDesc(PageRequest.of(Math.max(pagina, 0),
                Math.min(Math.max(tamano, 1), 100))), AuditoriaDto::desde);
    }
}
