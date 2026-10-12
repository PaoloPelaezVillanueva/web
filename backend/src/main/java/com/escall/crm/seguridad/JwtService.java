package com.escall.crm.seguridad;

import com.escall.crm.modelo.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Genera los tokens JWT que el panel envía en cada petición (cabecera Authorization: Bearer ...). */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long horasExpiracion;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.horas-expiracion}") long horasExpiracion) {
        this.encoder = encoder;
        this.horasExpiracion = horasExpiracion;
    }

    public TokenGenerado generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(horasExpiracion, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("crm-cobranzas")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(usuario.getUsuario())
                .claim("nombre", usuario.getNombre())
                .claim("roles", List.of(usuario.getRol().name()))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenGenerado(token, expira);
    }

    public record TokenGenerado(String valor, Instant expiraEn) {
    }
}
