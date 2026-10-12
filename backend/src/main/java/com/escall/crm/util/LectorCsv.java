package com.escall.crm.util;

import com.escall.crm.excepcion.ReglaNegocioException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Lector de archivos CSV sencillo (sin librerías externas).
 *  - Acepta separador ";" (Excel en español) o "," (detectado en la cabecera).
 *  - Ignora el BOM que agrega Excel al guardar como "CSV UTF-8".
 *  - Soporta valores entre comillas: "Pérez; Juan".
 * Cada fila se devuelve como un Map columna -> valor (columnas en minúsculas).
 */
public final class LectorCsv {

    public static final int MAX_FILAS = 20_000;

    private LectorCsv() {
    }

    public record Fila(int numero, Map<String, String> valores) {

        public String get(String columna) {
            String v = valores.get(columna);
            return v == null || v.isBlank() ? null : v.trim();
        }
    }

    public static List<Fila> leer(InputStream entrada, Set<String> columnasObligatorias) {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String cabecera = lector.readLine();
            if (cabecera == null || cabecera.isBlank()) {
                throw new ReglaNegocioException("El archivo está vacío");
            }
            cabecera = cabecera.replace("﻿", "");
            char separador = cabecera.chars().filter(c -> c == ';').count() >= cabecera.chars().filter(c -> c == ',').count()
                    ? ';' : ',';
            List<String> columnas = dividir(cabecera, separador).stream()
                    .map(c -> c.trim().toLowerCase(Locale.ROOT).replace(' ', '_'))
                    .toList();

            List<String> faltantes = columnasObligatorias.stream().filter(c -> !columnas.contains(c)).sorted().toList();
            if (!faltantes.isEmpty()) {
                throw new ReglaNegocioException("Estructura inválida. Faltan las columnas: " + String.join(", ", faltantes));
            }

            List<Fila> filas = new ArrayList<>();
            String linea;
            int numero = 1;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (linea.isBlank()) {
                    continue;
                }
                if (filas.size() >= MAX_FILAS) {
                    throw new ReglaNegocioException("El archivo supera el máximo de " + MAX_FILAS + " filas");
                }
                List<String> partes = dividir(linea, separador);
                Map<String, String> valores = new HashMap<>();
                for (int i = 0; i < columnas.size(); i++) {
                    valores.put(columnas.get(i), i < partes.size() ? partes.get(i) : null);
                }
                filas.add(new Fila(numero, valores));
            }
            if (filas.isEmpty()) {
                throw new ReglaNegocioException("El archivo no tiene filas de datos");
            }
            return filas;
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo. Verifica que sea un CSV en UTF-8.");
        }
    }

    private static List<String> dividir(String linea, char separador) {
        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == separador && !entreComillas) {
                partes.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        partes.add(actual.toString());
        return partes;
    }
}
