package com.escall.crm.controlador;

import com.escall.crm.dto.LoginRequest;
import com.escall.crm.dto.LoginResponse;
import com.escall.crm.dto.UsuarioDto;
import com.escall.crm.servicio.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Módulo Login (RF01, RF02). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        auth.logout();
    }

    @GetMapping("/yo")
    public UsuarioDto yo() {
        return auth.actual();
    }
}
