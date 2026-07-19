const API_URL = 'https://despliegueeduc.duckdns.org';
const UMBRAL_APROBATORIO = 6;

let rolActual = null;
let idGrupoSeleccionado = null;
let chartPromedio = null;
let chartDistribucion = null;

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = await obtenerSesion();
    if (!sesion) return;
    rolActual = sesion.rol;

    if (rolActual === 'Director') {
        await inicializarComoDirector();
    } else {
        await inicializarComoDocente();
    }

    const select = document.getElementById('select-periodo');
    if (select) {
        select.addEventListener('change', (e) => {
            cargarRendimiento(e.target.value);
        });
    }
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
    idGrupoSeleccionado = 'mi-grupo';
    await cargarRendimiento('1');
}

async function inicializarComoDirector() {
    const contenedorFiltros = document.getElementById('select-periodo').closest('.item-filtro-mini').parentElement;

    const contenedorSelect = document.createElement('div');
    contenedorSelect.className = 'item-filtro-mini';
    contenedorSelect.style.cssText = 'display:flex; align-items:center; gap:6px; margin-right: 16px;';
    contenedorSelect.innerHTML = `
        <label style="margin-bottom:0; font-family:'Poppins',sans-serif; font-weight:600; color:#0b2545; font-size:14px;">Grupo:</label>
    `;

    const selectGrupos = document.createElement('select');
    selectGrupos.id = 'select-grupo-director';
    selectGrupos.className = 'select-pildora';
    selectGrupos.style.cssText = 'width: auto; padding: 4px 25px 4px 12px;';
    selectGrupos.innerHTML = '<option value="">Selecciona un grupo...</option>';
    contenedorSelect.appendChild(selectGrupos);

    contenedorFiltros.insertBefore(contenedorSelect, contenedorFiltros.firstChild);

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
            cargarRendimiento(document.getElementById('select-periodo').value);
        } else {
            actualizarVista([]);
        }
    });
}

async function cargarRendimiento(periodo) {
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
        actualizarVista(alumnos);
    } catch (error) {
        console.error('Error cargando rendimiento:', error);
    }
}

function calcularPromedioPorAlumno(datos) {
    const alumnosMap = new Map();

    datos.forEach(item => {
        if (!alumnosMap.has(item.matricula)) {
            alumnosMap.set(item.matricula, []);
        }
        if (item.promedioFinal !== null && item.promedioFinal !== undefined) {
            alumnosMap.get(item.matricula).push(item.promedioFinal);
        }
    });

    const resultado = [];
    alumnosMap.forEach((valores) => {
        if (valores.length === 0) return;
        const promedio = valores.reduce((a, b) => a + b, 0) / valores.length;
        resultado.push(Math.round(promedio * 100) / 100);
    });

    return resultado;
}

function actualizarVista(promediosPorAlumno) {
    const totalAlumnos = promediosPorAlumno.length;
    const aprobados = promediosPorAlumno.filter(p => p >= UMBRAL_APROBATORIO).length;
    const reprobados = totalAlumnos - aprobados;
    const promedioGrupal = totalAlumnos > 0
        ? Math.round((promediosPorAlumno.reduce((a, b) => a + b, 0) / totalAlumnos) * 100) / 100
        : 0;
    const porcentajeAprobacion = totalAlumnos > 0 ? Math.round((aprobados / totalAlumnos) * 1000) / 10 : 0;

    document.getElementById('numTotalAlumnos').textContent = totalAlumnos;
    document.getElementById('numAprobados').textContent = aprobados;
    document.getElementById('numReprobados').textContent = reprobados;
    document.getElementById('txtPorcentajeAprobacion').textContent = porcentajeAprobacion + '%';
    document.getElementById('txtPromedio').textContent = promedioGrupal.toFixed(1);

    dibujarGraficaPromedio(promedioGrupal);
    dibujarGraficaDistribucion(promediosPorAlumno);
}

function dibujarGraficaPromedio(promedio) {
    const ctx = document.getElementById('graficaPromedio');
    if (!ctx) return;

    const restante = Math.max(0, 10 - promedio);

    if (chartPromedio) chartPromedio.destroy();
    chartPromedio = new Chart(ctx, {
        type: 'doughnut',
        data: {
            datasets: [{
                data: [promedio, restante],
                backgroundColor: ['#102A43', '#f1f5f9'],
                borderWidth: 0
            }]
        },
        options: {
            cutout: '85%',
            plugins: { tooltip: { enabled: false } },
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

function dibujarGraficaDistribucion(promedios) {
    const ctx = document.getElementById('graficaDistribucion');
    if (!ctx) return;

    const buckets = { 5: 0, 6: 0, 7: 0, 8: 0, 9: 0, 10: 0 };
    promedios.forEach(p => {
        const redondeado = Math.min(10, Math.max(5, Math.round(p)));
        buckets[redondeado]++;
    });

    if (chartDistribucion) chartDistribucion.destroy();
    chartDistribucion = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['5', '6', '7', '8', '9', '10'],
            datasets: [{
                data: [buckets[5], buckets[6], buckets[7], buckets[8], buckets[9], buckets[10]],
                backgroundColor: ['#d9381e', '#9b51e0', '#f2e205', '#d645ac', '#62dc36', '#38bdf8'],
                borderRadius: 3,
                barPercentage: 0.6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { display: false } },
                y: { beginAtZero: true, ticks: { stepSize: 1 } }
            }
        }
    });
}