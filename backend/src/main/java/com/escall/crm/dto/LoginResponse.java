package com.escall.crm.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEn, UsuarioDto usuario) {
}
