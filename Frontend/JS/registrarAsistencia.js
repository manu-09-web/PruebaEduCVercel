const API_URL = 'https://despliegueeduc.duckdns.org';

document.addEventListener('DOMContentLoaded', async () => {
    await cargarContexto();
    await cargarAlumnos();
});

async function cargarContexto() {
    try {
        const resGrupo = await fetch(`${API_URL}/mi-grupo`, { credentials: 'include' });
        if (resGrupo.status === 401) {
            window.location.href = '../../index.html';
            return;
        }
        if (resGrupo.status === 403) {
            mostrarError('Este modulo es exclusivo para Docentes con grupo asignado. Redirigiendo...');
            setTimeout(() => { window.location.href = '../Cuenta/Cuenta.html'; }, 2000);
            return;
        }
        const grupo = await resGrupo.json();
        document.getElementById('input-grado').value = grupo.grado + '°';
        document.getElementById('input-grupo').value = grupo.grupo;

        const resPeriodo = await fetch(`${API_URL}/mi-periodo-actual`, { credentials: 'include' });
        const periodoInfo = await resPeriodo.json();

        const select = document.getElementById('select-periodo');
        select.innerHTML = '';
        if (periodoInfo.periodo) {
            const opt = document.createElement('option');
            opt.value = periodoInfo.periodo;
            opt.textContent = periodoInfo.periodo + '° Periodo';
            opt.selected = true;
            select.appendChild(opt);
        } else {
            const opt = document.createElement('option');
            opt.textContent = 'Ciclo escolar completo';
            select.appendChild(opt);
            mostrarError('El ciclo escolar esta completo. Ve al modulo de Registro > Fin de Periodo.');
        }

        document.getElementById('input-ciclo').value = periodoInfo.cicloEscolar || '---';
    } catch (error) {
        console.error('Error cargando contexto:', error);
        mostrarError('No se pudo conectar con el servidor');
    }
}

async function cargarAlumnos() {
    try {
        const response = await fetch(`${API_URL}/alumnos`, { credentials: 'include' });
        const alumnos = await response.json();

        const tbody = document.getElementById('tabla-cuerpo');
        tbody.innerHTML = '';

        alumnos.forEach((alumno, index) => {
            const fila = document.createElement('tr');
            fila.dataset.matricula = alumno.matricula;
            fila.innerHTML = `
                <td>${index + 1}</td>
                <td class="celda-nombre-alumno">${alumno.nombre} ${alumno.apellidoPaterno} ${alumno.apellidoMaterno}</td>
                <td><input type="radio" name="estado_alumno_${alumno.matricula}" value="Asistencia" class="radio-asistencia radio-verde" checked></td>
                <td><input type="radio" name="estado_alumno_${alumno.matricula}" value="Falta" class="radio-asistencia radio-rojo"></td>
                <td><input type="radio" name="estado_alumno_${alumno.matricula}" value="Permiso" class="radio-asistencia radio-amarillo"></td>
            `;
            tbody.appendChild(fila);
        });
    } catch (error) {
        console.error('Error cargando alumnos:', error);
        mostrarError('No se pudo cargar la lista de alumnos');
    }
}

async function guardarRegistro() {
    const filas = document.querySelectorAll('#tabla-cuerpo tr');
    if (filas.length === 0) {
        mostrarError('No hay alumnos para registrar');
        return;
    }

    let huboError = false;

    for (const fila of filas) {
        const matricula = parseInt(fila.dataset.matricula);
        const radioSeleccionado = fila.querySelector(`input[name="estado_alumno_${matricula}"]:checked`);
        const estado = radioSeleccionado ? radioSeleccionado.value : 'Asistencia';

        try {
            const response = await fetch(`${API_URL}/registro-asistencia`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ matricula, estado })
            });

            if (!response.ok) {
                huboError = true;
                const texto = await response.text();
                console.error(`Error con matricula ${matricula}:`, texto);
            }
        } catch (error) {
            huboError = true;
            console.error(`Error de red con matricula ${matricula}:`, error);
        }
    }

    if (huboError) {
        mostrarError('Algunos registros no se pudieron guardar. Revisa la consola.');
    } else {
        mostrarAlertaExito();
    }
}

function mostrarAlertaExito() {
    const alerta = document.getElementById('alerta-exito');
    alerta.classList.add('mostrar-alerta');
    setTimeout(() => { alerta.classList.remove('mostrar-alerta'); }, 3500);
}

function mostrarError(texto) {
    let el = document.getElementById("alerta-error");
    if (!el) {
        el = document.createElement("div");
        el.id = "alerta-error";
        el.className = "alerta-toast";
        el.innerHTML = `
            <div class="icono-check" style="background:#f8d7da;">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 9v4m0 4h.01M10.29 3.86l-8.18 14.18A2 2 0 0 0 3.82 21h16.36a2 2 0 0 0 1.71-3.01L13.71 3.86a2 2 0 0 0-3.42 0z" stroke="#721c24" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </div>
            <div class="texto-alerta" id="texto-alerta-error"></div>
        `;
        document.body.appendChild(el);
    }
    document.getElementById("texto-alerta-error").textContent = texto;
    el.classList.add("mostrar-alerta");
    setTimeout(() => { el.classList.remove("mostrar-alerta"); }, 4500);
}