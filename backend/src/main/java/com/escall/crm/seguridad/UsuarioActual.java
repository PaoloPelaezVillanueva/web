package com.escall.crm.seguridad;

import com.escall.crm.excepcion.CredencialesInvalidasException;
import com.escall.crm.modelo.Rol;
import com.escall.crm.modelo.Usuario;
import com.escall.crm.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Da acceso al usuario que hizo la petición y define QUÉ ASESORES puede ver:
 *  - ADMIN      -> todos los asesores
 *  - SUPERVISOR -> solo los asesores de su equipo (RF14)
 *  - ASESOR     -> solo él mismo (su cartera)
 * Todos los servicios usan este método para filtrar la información.
 */
@Component
@RequiredArgsConstructor
public class UsuarioActual {

    private final UsuarioRepository usuarios;

    public Usuario obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new CredencialesInvalidasException();
        }
        return usuarios.findByUsuarioIgnoreCase(auth.getName())
                .filter(Usuario::isActivo)
                .orElseThrow(CredencialesInvalidasException::new);
    }

    public List<Long> asesoresVisibles(Usuario u) {
        return switch (u.getRol()) {
            case ADMIN -> usuarios.idsPorRol(Rol.ASESOR);
            case SUPERVISOR -> usuarios.idsDelEquipo(u.getId());
            case ASESOR -> List.of(u.getId());
        };
    }

    /** Las consultas "in (:lista)" no aceptan listas vacías: usamos un id imposible. */
    public static List<Long> noVacia(List<Long> ids) {
        return ids.isEmpty() ? List.of(-1L) : ids;
    }
}
