const API_URL = 'https://despliegueeduc.duckdns.org';
const UMBRAL_APROBATORIO = 6;

let rolActual = null;
let idGrupoSeleccionado = null;

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

    document.getElementById('input-buscar').addEventListener('input', (e) => {
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
    idGrupoSeleccionado = 'mi-grupo'; // marcador, la ruta real usa /mi-grupo sin idGrupo
    await cargarPromedios('1');
}

async function inicializarComoDirector() {
    const filtros = document.querySelector('.fila-filtros-compacta');

    const contenedorSelect = document.createElement('div');
    contenedorSelect.className = 'item-filtro-mini';
    contenedorSelect.innerHTML = `
        <label style="margin-bottom:0; font-family:'Poppins',sans-serif; font-weight:600; color:#0b2545; font-size:14px;">Grupo:</label>
    `;

    const selectGrupos = document.createElement('select');
    selectGrupos.id = 'select-grupo-director';
    selectGrupos.className = 'select-pildora';
    selectGrupos.style.cssText = 'width: auto; padding: 4px 25px 4px 12px;';
    selectGrupos.innerHTML = '<option value="">Selecciona un grupo...</option>';
    contenedorSelect.appendChild(selectGrupos);

    filtros.insertBefore(contenedorSelect, filtros.firstChild);

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
            document.getElementById('tabla-cuerpo').innerHTML = '<tr><td colspan="5">Selecciona un grupo para ver su estatus.</td></tr>';
        }
    });

    document.getElementById('tabla-cuerpo').innerHTML = '<tr><td colspan="5">Selecciona un grupo para ver su estatus.</td></tr>';
}

async function cargarPromedios(periodo) {
    if (!idGrupoSeleccionado) return;

    const endpoint = rolActual === 'Director'
        ? `${API_URL}/promedio/grupo?idGrupo=${idGrupoSeleccionado}&periodo=${periodo}`
        : `${API_URL}/promedio/mi-grupo?periodo=${periodo}`;

    try {
        const response = await fetch(endpoint, { credentials: 'include' });

        if (response.status === 401) {
            window.location.href = '../../index.html';
            return;
        }
        if (response.status === 403) {
            alert('No tienes permiso para consultar este grupo.');
            return;
        }
        if (!response.ok) {
            console.error('Error al cargar promedios');
            return;
        }

        const datos = await response.json();
        const alumnos = calcularPromedioPorAlumno(datos);
        renderizarTabla(alumnos);
    } catch (error) {
        console.error('Error cargando promedios:', error);
    }
}

function calcularPromedioPorAlumno(datos) {
    const alumnosMap = new Map();

    datos.forEach(item => {
        if (!alumnosMap.has(item.matricula)) {
            alumnosMap.set(item.matricula, { nombreAlumno: item.nombreAlumno, valores: [] });
        }
        if (item.promedioFinal !== null && item.promedioFinal !== undefined) {
            alumnosMap.get(item.matricula).valores.push(item.promedioFinal);
        }
    });

    const resultado = [];
    alumnosMap.forEach((info, matricula) => {
        if (info.valores.length === 0) {
            resultado.push({ matricula, nombreAlumno: info.nombreAlumno, promedio: null, estado: 'Pendiente' });
        } else {
            const suma = info.valores.reduce((a, b) => a + b, 0);
            const promedio = suma / info.valores.length;
            const estado = promedio >= UMBRAL_APROBATORIO ? 'Aprobado' : 'Reprobado';
            resultado.push({ matricula, nombreAlumno: info.nombreAlumno, promedio: Math.round(promedio * 100) / 100, estado });
        }
    });

    return resultado;
}

function renderizarTabla(alumnos) {
    const tbody = document.getElementById('tabla-cuerpo');
    tbody.innerHTML = '';

    if (alumnos.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5">No hay alumnos en este grupo.</td></tr>';
        return;
    }

    alumnos.forEach((alumno, index) => {
        const fila = document.createElement('tr');
        fila.dataset.nombre = alumno.nombreAlumno.toLowerCase();

        const icono = alumno.estado === 'Aprobado'
            ? `<svg class="icono-status" viewBox="0 0 24 24" fill="none" stroke="#1cc88a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`
            : alumno.estado === 'Reprobado'
                ? `<svg class="icono-status" viewBox="0 0 24 24" fill="none" stroke="#e74c3c" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`
                : '—';

        fila.innerHTML = `
            <td class="celda-centro">${index + 1}</td>
            <td class="celda-nombre-alumno">${alumno.nombreAlumno}</td>
            <td class="celda-centro">${alumno.promedio !== null ? alumno.promedio : '—'}</td>
            <td class="celda-centro">${alumno.estado}</td>
            <td class="celda-centro">${icono}</td>
        `;
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