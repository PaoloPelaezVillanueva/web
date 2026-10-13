# Evidencia HTTP del backend (perfil demo)

- Generada: 2026-10-13T17:03:20.036Z
- Base de la API: http://localhost:8080
- Resultado: 27 de 27 comprobaciones correctas

| # | Prueba | Petición | Datos enviados | Esperado | Obtenido | Respuesta |
|---|---|---|---|---|---|---|
| 1 | Sin token: el acceso a la API queda bloqueado | `GET /api/cartera` | sin cabecera Authorization | 401 | 401 ✅ |  |
| 2 | Clave incorrecta: no se emite token | `POST /api/auth/login` | usuario admin, clave incorrecta | 401 | 401 ✅ | objeto {estado, mensaje, errores, fecha} |
| 3 | Login de admin | `POST /api/auth/login` | usuario admin | 200 | 200 ✅ | objeto {token, expiraEn, usuario} |
| 4 | Login de asesor1 | `POST /api/auth/login` | usuario asesor1 | 200 | 200 ✅ | objeto {token, expiraEn, usuario} |
| 5 | Login de sgarcia | `POST /api/auth/login` | usuario sgarcia | 200 | 200 ✅ | objeto {token, expiraEn, usuario} |
| 6 | Login de asesor3 | `POST /api/auth/login` | usuario asesor3 | 200 | 200 ✅ | objeto {token, expiraEn, usuario} |
| 7 | Asesor intenta leer la auditoría (solo administrador) | `GET /api/admin/auditoria` | — | 403 | 403 ✅ | objeto {estado, mensaje, errores, fecha} |
| 8 | Asesor intenta ver el dashboard (supervisor y administrador) | `GET /api/dashboard?periodo=MES` | periodo = MES | 403 | 403 ✅ | objeto {estado, mensaje, errores, fecha} |
| 9 | Supervisor consulta el dashboard de su equipo | `GET /api/dashboard?periodo=MES` | periodo = MES | 200 | 200 ✅ | objeto {desde, hasta, gestiones, contactos, tasaContacto, promesas, ...} |
| 10 | Administrador consulta la auditoría | `GET /api/admin/auditoria` | — | 200 | 200 ✅ | objeto {contenido, pagina, tamano, totalElementos, totalPaginas} |
| 11 | Asesor 1 consulta su cartera | `GET /api/cartera` | — | 200 | 200 ✅ | lista de 5 elemento(s) |
| 12 | Asesor 3 consulta su cartera | `GET /api/cartera` | — | 200 | 200 ✅ | lista de 5 elemento(s) |
| 13 | Asesor 1 pide un cliente que no está en su cartera | `GET /api/clientes/4` | id 4 | 404 | 404 ✅ | objeto {estado, mensaje, errores, fecha} |
| 14 | Asesor 1 consulta el detalle de un cliente de su cartera | `GET /api/clientes/7` | id 7 | 200 | 200 ✅ | objeto {id, tipoDocumento, documento, nombres, email, direccion, ...} |
| 15 | Búsqueda JPQL por documento (filtro por cartera del asesor) | `GET /api/clientes?q=43698521` | q = 43698521 | 200 | 200 ✅ | lista de 1 elemento(s) |
| 16 | Reporte JPQL con joins y filtros opcionales (gestiones del mes) | `GET /api/reportes/gestiones?periodo=MES` | periodo = MES | 200 | 200 ✅ | lista de 34 elemento(s) |
| 17 | Registrar gestión con promesa (gestión, promesa y teléfono en una sola transacción) | `POST /api/gestiones` | cuenta 9; tipificación N2 1; compromiso 2026-10-23 por 100 | 201 | 201 ✅ | objeto {id, cuentaId, numeroOperacion, cartera, fechaHora, telefono, ...} |
| 18 | Se verifica que la promesa quedó registrada | `GET /api/clientes/7` | — | 200 | 200 ✅ | promesa nueva 2026-10-23 por 100: PENDIENTE |
| 19 | Carga de pagos: 1 fila válida y 1 inexistente (transacción por archivo) | `POST /api/admin/integracion/pagos` | archivo pagos-evidencia.csv: 1 fila válida y 1 inexistente | 200 | 200 ✅ | 1 aceptado(s), 1 rechazado(s) |
| 20 | Se verifica que el pago cumplió la promesa (CUMPLIDA) | `GET /api/clientes/7` | — | 200 | 200 ✅ | promesa nueva 2026-10-23 por 100: CUMPLIDA |
| 21 | CRUD - POST: alta de cliente (administrador) | `POST /api/clientes` | documento EV1791910999941; nombres; correo; dirección | 201 | 201 ✅ | objeto {id, tipoDocumento, documento, nombres, email, direccion, ...} |
| 22 | CRUD - GET: consulta del cliente creado | `GET /api/clientes/13` | id 13 | 200 | 200 ✅ | objeto {id, tipoDocumento, documento, nombres, email, direccion, ...} |
| 23 | CRUD - PUT: actualización de datos de contacto | `PUT /api/clientes/13` | nombres editados; dirección nueva | 200 | 200 ✅ | objeto {id, tipoDocumento, documento, nombres, email, direccion, ...} |
| 24 | Regla: no se elimina un cliente que tiene cuentas | `DELETE /api/clientes/7` | id 7 | 409 | 409 ✅ | objeto {estado, mensaje, errores, fecha} |
| 25 | CRUD - DELETE: baja de cliente sin cuentas | `DELETE /api/clientes/13` | id 13 | 204 | 204 ✅ |  |
| 26 | CRUD - GET tras la baja: el cliente ya no existe | `GET /api/clientes/13` | id 13 | 404 | 404 ✅ | objeto {estado, mensaje, errores, fecha} |
| 27 | Agenda de promesas pendientes del asesor (JPQL por estado) | `GET /api/cartera/agenda` | token de asesor1 | 200 | 200 ✅ | lista de 1 elemento(s) |
