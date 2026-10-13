// Genera evidencia HTTP reproducible para el informe del Avance 2.
// Cada fila muestra la petición, los datos enviados, el código esperado y el código real, con un resumen de la respuesta.
// Uso (backend en marcha):  node docs/avance-2/evidencia-api.js http://localhost:8080 mysql
//                           node docs/avance-2/evidencia-api.js http://localhost:8080 demo
const fs = require('fs');
const path = require('path');

const base = process.argv[2] || 'http://localhost:8080';
const perfil = process.argv[3] || 'desconocido';
const filas = [];

// Fecha de Lima (UTC-5, sin horario de verano)
const hoyLima = () => new Date(Date.now() - 5 * 3600 * 1000).toISOString().slice(0, 10);
const sumarDias = (n) => new Date(Date.now() - 5 * 3600 * 1000 + n * 86400000).toISOString().slice(0, 10);

function resumir(json, texto) {
  if (json === null) return texto.slice(0, 120);
  if (Array.isArray(json)) return `lista de ${json.length} elemento(s)`;
  const claves = Object.keys(json).slice(0, 6).join(', ');
  return `objeto {${claves}${Object.keys(json).length > 6 ? ', ...' : ''}}`;
}

async function llamar(titulo, metodo, ruta, { token, cuerpo, formulario, esperado, extraer, enviado } = {}) {
  const headers = { Accept: 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  let body;
  if (formulario) {
    body = formulario; // fetch pone el Content-Type multipart con el límite
  } else if (cuerpo !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(cuerpo);
  }
  const r = await fetch(base + ruta, { method: metodo, headers, body });
  const texto = await r.text();
  let json = null;
  try { json = texto ? JSON.parse(texto) : null; } catch { json = null; }
  const resumen = extraer && json ? extraer(json) : resumir(json, texto);
  filas.push({ titulo, peticion: `${metodo} ${ruta}`, enviado: enviado || '—', esperado, obtenido: r.status, ok: r.status === esperado, resumen });
  return { status: r.status, json, texto, extra: extraer ? extraer(json) : undefined };
}

// Promesa creada por esta evidencia (identificada por fecha y monto de compromiso)
const buscarPromesaNueva = (json, fecha, monto) =>
  json.promesas.find((x) => x.fechaCompromiso === fecha && Number(x.monto) === Number(monto));
const estadoPromesaNueva = (fecha, monto) => (json) => {
  const p = buscarPromesaNueva(json, fecha, monto);
  return p ? `promesa nueva ${fecha} por ${monto}: ${p.estado}` : 'promesa nueva no encontrada';
};

async function login(usuario, password) {
  const r = await llamar(`Login de ${usuario}`, 'POST', '/api/auth/login', {
    cuerpo: { usuario, password }, esperado: 200, enviado: `usuario ${usuario}`,
  });
  return r.json && r.json.token;
}

async function main() {
  // Espera a que el backend esté listo: el arranque crea los datos iniciales después de abrir el puerto
  let listo = false;
  for (let intento = 0; intento < 60 && !listo; intento++) {
    try {
      const r = await fetch(base + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ usuario: 'admin', password: 'Admin12345' }),
      });
      listo = r.status === 200;
    } catch {
      // aún no hay conexión
    }
    if (!listo) await new Promise((resolver) => setTimeout(resolver, 2000));
  }
  if (!listo) {
    console.error(`El backend en ${base} no respondió con el administrador. Revisa el arranque y el archivo .env.`);
    process.exit(2);
  }

  // ---------------------------------------------------------- 1. Seguridad (autenticación y roles)
  await llamar('Sin token: el acceso a la API queda bloqueado', 'GET', '/api/cartera', { esperado: 401, enviado: 'sin cabecera Authorization' });
  await llamar('Clave incorrecta: no se emite token', 'POST', '/api/auth/login', {
    cuerpo: { usuario: 'admin', password: 'incorrecta' }, esperado: 401, enviado: 'usuario admin, clave incorrecta',
  });
  const tokenAdmin = await login('admin', 'Admin12345');
  const tokenAsesor = await login('asesor1', 'Clave12345');
  const tokenSupervisor = await login('sgarcia', 'Clave12345');
  const tokenAsesor3 = await login('asesor3', 'Clave12345');

  await llamar('Asesor intenta leer la auditoría (solo administrador)', 'GET', '/api/admin/auditoria', { token: tokenAsesor, esperado: 403 });
  await llamar('Asesor intenta ver el dashboard (supervisor y administrador)', 'GET', '/api/dashboard?periodo=MES', { token: tokenAsesor, esperado: 403, enviado: 'periodo = MES' });
  await llamar('Supervisor consulta el dashboard de su equipo', 'GET', '/api/dashboard?periodo=MES', { token: tokenSupervisor, esperado: 200, enviado: 'periodo = MES' });
  await llamar('Administrador consulta la auditoría', 'GET', '/api/admin/auditoria', { token: tokenAdmin, esperado: 200 });

  // ---------------------------------------------------------- 2. Alcance por cartera (consulta filtrada)
  const cartera1 = await llamar('Asesor 1 consulta su cartera', 'GET', '/api/cartera', { token: tokenAsesor, esperado: 200 });
  const cuentaPropia = cartera1.json[0];
  const clientesPropios = new Set(cartera1.json.map((c) => c.clienteId));
  const cartera3 = await llamar('Asesor 3 consulta su cartera', 'GET', '/api/cartera', { token: tokenAsesor3, esperado: 200 });
  const clienteAjeno = cartera3.json.map((c) => c.clienteId).find((id) => !clientesPropios.has(id));
  await llamar('Asesor 1 pide un cliente que no está en su cartera', 'GET', `/api/clientes/${clienteAjeno}`, { token: tokenAsesor, esperado: 404, enviado: `id ${clienteAjeno}` });
  const detalleInicial = await llamar('Asesor 1 consulta el detalle de un cliente de su cartera', 'GET', `/api/clientes/${cuentaPropia.clienteId}`, { token: tokenAsesor, esperado: 200, enviado: `id ${cuentaPropia.clienteId}` });
  const documento = detalleInicial.json.documento;

  // ---------------------------------------------------------- 3. Consultas JPQL
  await llamar('Búsqueda JPQL por documento (filtro por cartera del asesor)', 'GET', `/api/clientes?q=${documento}`, { token: tokenAsesor, esperado: 200, enviado: `q = ${documento}` });
  await llamar('Reporte JPQL con joins y filtros opcionales (gestiones del mes)', 'GET', '/api/reportes/gestiones?periodo=MES', { token: tokenSupervisor, esperado: 200, enviado: 'periodo = MES' });

  // ---------------------------------------------------------- 4. Transacción: gestión + promesa + teléfono
  const fechaCompromiso = sumarDias(10);
  const montoCompromiso = 100;
  const gestion = await llamar('Registrar gestión con promesa (gestión, promesa y teléfono en una sola transacción)', 'POST', '/api/gestiones', {
    token: tokenAsesor, esperado: 201,
    enviado: `cuenta ${cuentaPropia.id}; tipificación N2 1; compromiso ${fechaCompromiso} por ${montoCompromiso}`,
    cuerpo: {
      cuentaId: cuentaPropia.id,
      telefono: '987654321',
      tipificacionN2Id: 1,
      observacion: 'Evidencia: cliente acepta pagar',
      fechaCompromiso,
      montoCompromiso,
    },
  });
  const detalleTrasGestion = await llamar('Se verifica que la promesa quedó registrada', 'GET', `/api/clientes/${cuentaPropia.clienteId}`, {
    token: tokenAsesor, esperado: 200, extraer: estadoPromesaNueva(fechaCompromiso, montoCompromiso),
  });
  const promesaPendiente = buscarPromesaNueva(detalleTrasGestion.json, fechaCompromiso, montoCompromiso);
  if (!promesaPendiente) {
    filas.push({ titulo: 'Promesa PENDIENTE creada por la gestión', peticion: 'verificación', enviado: '—', esperado: 'PENDIENTE', obtenido: 'no encontrada', ok: false, resumen: '' });
  }

  // ---------------------------------------------------------- 5. Transacción: carga de pagos que cumple la promesa
  const csv = `numero_operacion;fecha_pago;monto;referencia\n${cuentaPropia.numeroOperacion};${hoyLima()};50.00;EVIDENCIA-OK\nOP-NO-EXISTE;${hoyLima()};10;EVIDENCIA-X\n`;
  const formulario = new FormData();
  formulario.append('archivo', new Blob([csv], { type: 'text/csv' }), 'pagos-evidencia.csv');
  const carga = await llamar('Carga de pagos: 1 fila válida y 1 inexistente (transacción por archivo)', 'POST', '/api/admin/integracion/pagos', {
    token: tokenAdmin, esperado: 200, formulario,
    enviado: 'archivo pagos-evidencia.csv: 1 fila válida y 1 inexistente',
    extraer: (j) => `${j.aceptados} aceptado(s), ${j.rechazados} rechazado(s)`,
  });
  if (carga.json && (carga.json.aceptados !== 1 || carga.json.rechazados !== 1)) {
    filas.push({ titulo: 'Resultado de la carga', peticion: 'verificación', enviado: '—', esperado: '1 aceptado y 1 rechazado', obtenido: `${carga.json.aceptados} aceptado(s), ${carga.json.rechazados} rechazado(s)`, ok: false, resumen: '' });
  }
  const detalleTrasPago = await llamar('Se verifica que el pago cumplió la promesa (CUMPLIDA)', 'GET', `/api/clientes/${cuentaPropia.clienteId}`, {
    token: tokenAsesor, esperado: 200, extraer: estadoPromesaNueva(fechaCompromiso, montoCompromiso),
  });
  const promesaCumplida = buscarPromesaNueva(detalleTrasPago.json, fechaCompromiso, montoCompromiso);
  const cumplida = promesaCumplida && promesaCumplida.estado === 'CUMPLIDA';
  if (!cumplida) {
    filas.push({ titulo: 'Promesa marcada CUMPLIDA tras el pago', peticion: 'verificación', enviado: '—', esperado: 'CUMPLIDA', obtenido: 'sin cambio', ok: false, resumen: '' });
  }

  // ---------------------------------------------------------- 6. CRUD de clientes (POST, GET, PUT, DELETE)
  const documentoNuevo = `EV${Date.now()}`.slice(0, 15);
  const creado = await llamar('CRUD - POST: alta de cliente (administrador)', 'POST', '/api/clientes', {
    token: tokenAdmin, esperado: 201,
    enviado: `documento ${documentoNuevo}; nombres; correo; dirección`,
    cuerpo: { documento: documentoNuevo, nombres: 'Cliente de evidencia', email: 'evidencia@escall.pe', direccion: 'Av. Evidencia 1' },
  });
  const idNuevo = creado.json && creado.json.id;
  await llamar('CRUD - GET: consulta del cliente creado', 'GET', `/api/clientes/${idNuevo}`, { token: tokenAdmin, esperado: 200, enviado: `id ${idNuevo}` });
  await llamar('CRUD - PUT: actualización de datos de contacto', 'PUT', `/api/clientes/${idNuevo}`, {
    token: tokenAdmin, esperado: 200,
    enviado: 'nombres editados; dirección nueva',
    cuerpo: { nombres: 'Cliente de evidencia editado', email: 'evidencia@escall.pe', direccion: 'Av. Evidencia 2' },
  });
  await llamar('Regla: no se elimina un cliente que tiene cuentas', 'DELETE', `/api/clientes/${cuentaPropia.clienteId}`, { token: tokenAdmin, esperado: 409, enviado: `id ${cuentaPropia.clienteId}` });
  await llamar('CRUD - DELETE: baja de cliente sin cuentas', 'DELETE', `/api/clientes/${idNuevo}`, { token: tokenAdmin, esperado: 204, enviado: `id ${idNuevo}` });
  await llamar('CRUD - GET tras la baja: el cliente ya no existe', 'GET', `/api/clientes/${idNuevo}`, { token: tokenAdmin, esperado: 404, enviado: `id ${idNuevo}` });

  // ---------------------------------------------------------- 7. Consulta de la agenda de promesas (al final para no alterar la numeración)
  await llamar('Agenda de promesas pendientes del asesor (JPQL por estado)', 'GET', '/api/cartera/agenda', { token: tokenAsesor, esperado: 200, enviado: 'token de asesor1' });

  // ---------------------------------------------------------- Salida
  const fallos = filas.filter((f) => !f.ok).length;
  const cabecera = [
    `# Evidencia HTTP del backend (perfil ${perfil})`,
    '',
    `- Generada: ${new Date().toISOString()}`,
    `- Base de la API: ${base}`,
    `- Resultado: ${filas.length - fallos} de ${filas.length} comprobaciones correctas`,
    '',
    '| # | Prueba | Petición | Datos enviados | Esperado | Obtenido | Respuesta |',
    '|---|---|---|---|---|---|---|',
  ];
  const cuerpo = filas.map((f, i) =>
    `| ${i + 1} | ${f.titulo} | \`${f.peticion}\` | ${f.enviado} | ${f.esperado} | ${f.obtenido} ${f.ok ? '✅' : '❌'} | ${f.resumen} |`);
  const salida = path.join(__dirname, `evidencia-api-${perfil}.md`);
  fs.writeFileSync(salida, [...cabecera, ...cuerpo, ''].join('\n'), 'utf8');
  console.log(`Evidencia escrita en ${salida}`);
  console.log(`${filas.length - fallos} de ${filas.length} comprobaciones correctas`);
  process.exitCode = fallos > 0 ? 1 : 0;
}

main().catch((e) => { console.error(e); process.exit(1); });
