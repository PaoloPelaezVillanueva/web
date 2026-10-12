package com.escall.crm.excepcion;

import com.escall.crm.dto.ErrorRespuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte cualquier error en una respuesta JSON clara y en español.
 * Así el frontend puede mostrar el mensaje directamente al usuario.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ErrorRespuesta> credenciales(CredencialesInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        String mensaje = errores.isEmpty() ? "Datos inválidos" : errores.values().iterator().next();
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta(400, mensaje, errores, Instant.now()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorRespuesta> formatoInvalido(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "El formato de los datos enviados no es válido");
    }

    /** Falta un archivo o un parámetro obligatorio en la petición (400, no 500). */
    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErrorRespuesta> archivoFaltante(MissingServletRequestPartException e) {
        return responder(HttpStatus.BAD_REQUEST, "Debes adjuntar el archivo (campo \"" + e.getRequestPartName() + "\")");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorRespuesta> parametroFaltante(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el dato obligatorio \"" + e.getParameterName() + "\"");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes (¿registro duplicado?)");
    }

    /** @PreAuthorize rechazó la acción: el rol del usuario no tiene permiso (403). */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorRespuesta> sinPermiso(AccessDeniedException e) {
        return responder(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorRespuesta> archivoGrande(MaxUploadSizeExceededException e) {
        return responder(HttpStatus.BAD_REQUEST, "El archivo supera el tamaño máximo permitido (10 MB)");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorRespuesta> rutaNoExiste(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta nuevamente.");
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(ErrorRespuesta.de(estado.value(), mensaje));
    }
}
