package com.escall.crm.repositorio;

import com.escall.crm.modelo.Rol;
import com.escall.crm.modelo.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "supervisor")
    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    boolean existsByUsuarioIgnoreCase(String usuario);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "supervisor")
    List<Usuario> findAllByOrderByRolAscNombreAsc();

    @EntityGraph(attributePaths = "supervisor")
    List<Usuario> findBySupervisorIdOrderByNombreAsc(Long supervisorId);

    List<Usuario> findByRolOrderByNombreAsc(Rol rol);

    @Query("select u.id from Usuario u where u.rol = :rol")
    List<Long> idsPorRol(@Param("rol") Rol rol);

    @Query("select u.id from Usuario u where u.supervisor.id = :supervisorId")
    List<Long> idsDelEquipo(@Param("supervisorId") Long supervisorId);
}
