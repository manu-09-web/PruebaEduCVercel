const API_URL = 'https://despliegueeduc.duckdns.org';

let rolActual = null;
let idGrupoSeleccionado = null;
let datosOriginales = [];

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = await obtenerSesion();
    if (!sesion) return;
    rolActual = sesion.rol;

    if (rolActual === 'Director') {
        await inicializarComoDirector();
    } else {
        await inicializarComoDocente();
    }

    document.getElementById('select-periodo').addEventListener('change', (e) => {
        cargarPromedios(e.target.value);
    });

    document.getElementById('input-buscar-alumno').addEventListener('input', (e) => {
        filtrarTabla(e.target.value);
    });
});

async function obtenerSesion() {
    try {
        const response = await fetch(`${API_URL}/session`, { credentials: 'include' });
        if (response.status === 401) {
            window.location.href = '../../index.html';
            return null;
        }
        return await response.json();
    } catch (error) {
        console.error('Error obteniendo sesión:', error);
        return null;
    }
}

async function inicializarComoDocente() {
    try {
        const response = await fetch(`${API_URL}/mi-grupo`, { credentials: 'include' });
        if (!response.ok) return;
        const grupo = await response.json();
        idGrupoSeleccionado = grupo.idGrupo;
        document.getElementById('input-grado').value = grupo.grado + '°';
        document.getElementById('input-grupo').value = grupo.grupo;
        await cargarPromedios('1');
    } catch (error) {
        console.error('Error cargando grupo:', error);
    }
}

async function inicializarComoDirector() {
    const contenedorGrado = document.getElementById('input-grado').closest('.item-filtro-mini');
    const contenedorGrupo = document.getElementById('input-grupo').closest('.item-filtro-mini');

    const selectGrupos = document.createElement('select');
    selectGrupos.id = 'select-grupo-director';
    selectGrupos.className = 'select-pildora';
    selectGrupos.style.cssText = 'width: auto; padding: 4px 25px 4px 12px;';
    selectGrupos.innerHTML = '<option value="">Selecciona un grupo...</option>';

    const nuevaEtiqueta = document.createElement('label');
    nuevaEtiqueta.textContent = 'Grupo:';
    nuevaEtiqueta.style.cssText = "margin-bottom:0; font-family:'Poppins',sans-serif; font-weight:600; color:#0b2545; font-size:14px;";

    contenedorGrado.remove();
    contenedorGrupo.innerHTML = '';
    contenedorGrupo.appendChild(nuevaEtiqueta);
    contenedorGrupo.appendChild(selectGrupos);

    try {
        const response = await fetch(`${API_URL}/grupos`, { credentials: 'include' });
        const grupos = await response.json();
        grupos.forEach(g => {
            const opt = document.createElement('option');
            opt.value = g.idGrupo;
            opt.textContent = `${g.grado}° ${g.grupo}`;
            selectGrupos.appendChild(opt);
        });
    } catch (error) {
        console.error('Error cargando grupos:', error);
    }

    selectGrupos.addEventListener('change', () => {
        idGrupoSeleccionado = selectGrupos.value ? parseInt(selectGrupos.value) : null;
        if (idGrupoSeleccionado) {
            cargarPromedios(document.getElementById('select-periodo').value);
        } else {
            document.getElementById('tabla-cuerpo').innerHTML = '<tr><td>Selecciona un grupo para ver sus calificaciones.</td></tr>';
        }
    });

    document.getElementById('tabla-cuerpo').innerHTML = '<tr><td>Selecciona un grupo para ver sus calificaciones.</td></tr>';
}

async function cargarPromedios(periodo) {
    if (!idGrupoSeleccionado) return;

    const endpoint = rolActual === 'Director'
        ? `${API_URL}/promedio/grupo?idGrupo=${idGrupoSeleccionado}&periodo=${periodo}`
        : `${API_URL}/promedio/mi-grupo?periodo=${periodo}`;

    try {
        const response = await fetch(endpoint, { credentials: 'include' });
        if (!response.ok) {
            console.error('Error al cargar promedios');
            return;
        }
        datosOriginales = await response.json();
        renderizarTabla(datosOriginales);
    } catch (error) {
        console.error('Error cargando promedios:', error);
    }
}

function renderizarTabla(datos) {
    const camposMap = new Map();
    const alumnosMap = new Map();

    datos.forEach(item => {
        camposMap.set(item.idCampoFormativo, item.nombreCampoFormativo);

        if (!alumnosMap.has(item.matricula)) {
            alumnosMap.set(item.matricula, { nombreAlumno: item.nombreAlumno, campos: {} });
        }
        alumnosMap.get(item.matricula).campos[item.idCampoFormativo] = item.promedioFinal;
    });

    const camposOrdenados = Array.from(camposMap.entries()).sort((a, b) => a[0] - b[0]);

    const theadRow = document.getElementById('encabezados-campos');
    theadRow.innerHTML = '<th style="width: 32%; text-align: left; padding-left: 20px;">Alumno</th>';
    camposOrdenados.forEach(([idCampo, nombre]) => {
        const th = document.createElement('th');
        th.style.width = (68 / camposOrdenados.length) + '%';
        th.style.textAlign = 'center';
        th.textContent = nombre;
        theadRow.appendChild(th);
    });

    const tbody = document.getElementById('tabla-cuerpo');
    tbody.innerHTML = '';

    if (alumnosMap.size === 0) {
        tbody.innerHTML = '<tr><td colspan="5">No hay alumnos en este grupo.</td></tr>';
        return;
    }

    alumnosMap.forEach((info, matricula) => {
        const fila = document.createElement('tr');
        fila.dataset.nombre = info.nombreAlumno.toLowerCase();

        let celdas = `<td class="celda-nombre-alumno">${info.nombreAlumno}</td>`;
        camposOrdenados.forEach(([idCampo]) => {
            const valor = info.campos[idCampo];
            celdas += `<td class="celda-calificacion-bd">${valor !== null && valor !== undefined ? valor : '—'}</td>`;
        });

        fila.innerHTML = celdas;
        tbody.appendChild(fila);
    });
}

function filtrarTabla(texto) {
    const filas = document.querySelectorAll('#tabla-cuerpo tr');
    const textoLower = texto.toLowerCase();
    filas.forEach(fila => {
        if (!fila.dataset.nombre) return;
        fila.style.display = fila.dataset.nombre.includes(textoLower) ? '' : 'none';
    });
}