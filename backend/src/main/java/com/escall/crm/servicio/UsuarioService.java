package com.escall.crm.servicio;

import com.escall.crm.dto.UsuarioDto;
import com.escall.crm.dto.UsuarioRequest;
import com.escall.crm.excepcion.RecursoNoEncontradoException;
import com.escall.crm.excepcion.ReglaNegocioException;
import com.escall.crm.modelo.Rol;
import com.escall.crm.modelo.Usuario;
import com.escall.crm.repositorio.UsuarioRepository;
import com.escall.crm.seguridad.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Administración de usuarios.
 *  - RF16: el SUPERVISOR crea asesores, edita nombre/correo, cambia contraseña y activa/desactiva
 *          SOLO a los asesores de su equipo.
 *  - RF17: el ADMINISTRADOR gestiona supervisores y asesores (crear, editar, activar, restablecer).
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int LONGITUD_MINIMA = 8;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        Usuario yo = usuarioActual.obtener();
        List<Usuario> lista = yo.getRol() == Rol.ADMIN
                ? usuarios.findAllByOrderByRolAscNombreAsc()
                : usuarios.findBySupervisorIdOrderByNombreAsc(yo.getId());
        return lista.stream().map(UsuarioDto::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> supervisores() {
        return usuarios.findByRolOrderByNombreAsc(Rol.SUPERVISOR).stream().filter(Usuario::isActivo)
                .map(UsuarioDto::desde).toList();
    }

    @Transactional
    public UsuarioDto crear(UsuarioRequest req) {
        Usuario yo = usuarioActual.obtener();
        String usuario = req.usuario().trim().toLowerCase(Locale.ROOT);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByUsuarioIgnoreCase(usuario)) {
            throw new ReglaNegocioException("Ya existe el usuario \"" + usuario + "\"");
        }
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        validarPassword(req.password());

        Usuario u = new Usuario();
        u.setUsuario(usuario);
        u.setNombre(req.nombre().trim());
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        if (yo.getRol() == Rol.SUPERVISOR) {
            // El supervisor solo crea asesores, y quedan en SU equipo
            u.setRol(Rol.ASESOR);
            u.setSupervisor(yo);
        } else {
            u.setRol(req.rol());
            u.setSupervisor(supervisorPara(req));
        }
        usuarios.save(u);
        auditoria.registrar(yo.getUsuario(), "CREAR", "Usuario", u.getId(), u.getUsuario() + " (" + u.getRol() + ")");
        return UsuarioDto.desde(u);
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioRequest req) {
        Usuario yo = usuarioActual.obtener();
        Usuario u = buscarGestionable(yo, id);
        String usuario = req.usuario().trim().toLowerCase(Locale.ROOT);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (!u.getUsuario().equalsIgnoreCase(usuario) && usuarios.existsByUsuarioIgnoreCase(usuario)) {
            throw new ReglaNegocioException("Ya existe el usuario \"" + usuario + "\"");
        }
        if (!u.getEmail().equalsIgnoreCase(email) && usuarios.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo");
        }
        u.setNombre(req.nombre().trim());
        u.setEmail(email);
        if (yo.getRol() == Rol.ADMIN) {
            // Solo el administrador cambia usuario, rol y supervisor
            if (u.getId().equals(yo.getId()) && req.rol() != Rol.ADMIN) {
                throw new ReglaNegocioException("No puedes quitarte el rol de administrador");
            }
            u.setUsuario(usuario);
            u.setRol(req.rol());
            u.setSupervisor(supervisorPara(req));
        }
        auditoria.registrar(yo.getUsuario(), "EDITAR", "Usuario", u.getId(), u.getUsuario());
        return UsuarioDto.desde(u);
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, boolean activo) {
        Usuario yo = usuarioActual.obtener();
        Usuario u = buscarGestionable(yo, id);
        if (u.getId().equals(yo.getId())) {
            throw new ReglaNegocioException("No puedes desactivar tu propio usuario");
        }
        u.setActivo(activo);
        auditoria.registrar(yo.getUsuario(), activo ? "ACTIVAR" : "DESACTIVAR", "Usuario", u.getId(), u.getUsuario());
        return UsuarioDto.desde(u);
    }

    @Transactional
    public void cambiarPassword(Long id, String password) {
        Usuario yo = usuarioActual.obtener();
        Usuario u = buscarGestionable(yo, id);
        validarPassword(password);
        u.setPasswordHash(passwordEncoder.encode(password));
        auditoria.registrar(yo.getUsuario(), "CAMBIAR_PASSWORD", "Usuario", u.getId(), u.getUsuario());
    }

    // ---------------------------------------------------------------- reglas

    /** El admin gestiona a cualquiera; el supervisor solo a los asesores de su equipo. */
    private Usuario buscarGestionable(Usuario yo, Long id) {
        Usuario u = usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (yo.getRol() == Rol.ADMIN) {
            return u;
        }
        boolean esDeSuEquipo = u.getRol() == Rol.ASESOR && u.getSupervisor() != null
                && u.getSupervisor().getId().equals(yo.getId());
        if (!esDeSuEquipo) {
            throw new RecursoNoEncontradoException("Usuario no encontrado");
        }
        return u;
    }

    private Usuario supervisorPara(UsuarioRequest req) {
        if (req.rol() != Rol.ASESOR) {
            return null;
        }
        if (req.supervisorId() == null) {
            throw new ReglaNegocioException("Todo asesor debe pertenecer al equipo de un supervisor");
        }
        return usuarios.findById(req.supervisorId())
                .filter(s -> s.getRol() == Rol.SUPERVISOR && s.isActivo())
                .orElseThrow(() -> new ReglaNegocioException("El supervisor elegido no existe o está inactivo"));
    }

    private static void validarPassword(String password) {
        if (password == null || password.length() < LONGITUD_MINIMA) {
            throw new ReglaNegocioException("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
    }
}
