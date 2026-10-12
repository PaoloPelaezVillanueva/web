package com.escall.crm.dto;

import com.escall.crm.modelo.Rol;
import com.escall.crm.modelo.Usuario;

import java.time.Instant;

/** Datos públicos de un usuario (nunca se expone el hash de la contraseña). */
public record UsuarioDto(Long id, String usuario, String nombre, String email, Rol rol,
                         Long supervisorId, String supervisorNombre, boolean activo, Instant creadoEn) {

    public static UsuarioDto desde(Usuario u) {
        Usuario s = u.getSupervisor();
        return new UsuarioDto(u.getId(), u.getUsuario(), u.getNombre(), u.getEmail(), u.getRol(),
                s != null ? s.getId() : null, s != null ? s.getNombre() : null, u.isActivo(), u.getCreadoEn());
    }
}
