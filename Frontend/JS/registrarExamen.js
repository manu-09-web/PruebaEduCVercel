const API_URL = 'http://localhost:7000';

document.addEventListener('DOMContentLoaded', async () => {
    await cargarContexto();
    await cargarCampos();
    await cargarAlumnos();

    // Recalcula la calificacion (%) en vivo mientras se escriben respuestas correctas
    document.getElementById('tabla-cuerpo').addEventListener('input', (e) => {
        if (e.target.classList.contains('input-respuestas-correctas')) {
            actualizarCalificacionVisual(e.target.closest('tr'));
        }
    });
    document.getElementById('input-total-preguntas').addEventListener('input', () => {
        document.querySelectorAll('#tabla-cuerpo tr').forEach(actualizarCalificacionVisual);
    });
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

async function cargarCampos() {
    try {
        const response = await fetch(`${API_URL}/mis-campos-formativos`, { credentials: 'include' });
        const campos = await response.json();

        const select = document.getElementById('select-campo');
        select.innerHTML = '<option value="" disabled selected>Seleccione un campo...</option>';

        campos.forEach(campo => {
            const opt = document.createElement('option');
            opt.value = campo.idCampoFormativo;
            opt.textContent = campo.nombre;
            select.appendChild(opt);
        });
    } catch (error) {
        console.error('Error cargando campos formativos:', error);
        mostrarError('No se pudieron cargar los Campos Formativos');
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
                <td><input type="number" min="0" class="input-tabla-pildora input-respuestas-correctas"></td>
                <td><input type="text" class="input-tabla-linea input-calificacion-visual" placeholder="————" readonly></td>
            `;
            tbody.appendChild(fila);
        });
    } catch (error) {
        console.error('Error cargando alumnos:', error);
        mostrarError('No se pudo cargar la lista de alumnos');
    }
}

function actualizarCalificacionVisual(fila) {
    const totalPreguntas = parseInt(document.getElementById('input-total-preguntas').value) || 0;
    const respuestasInput = fila.querySelector('.input-respuestas-correctas');
    const calificacionInput = fila.querySelector('.input-calificacion-visual');

    const aciertos = parseInt(respuestasInput.value);
    if (totalPreguntas > 0 && !isNaN(aciertos)) {
        const porcentaje = (aciertos / totalPreguntas) * 100;
        calificacionInput.value = porcentaje.toFixed(1) + '%';
    } else {
        calificacionInput.value = '————';
    }
}

async function guardarRegistro() {
    const idCampoFormativo = document.getElementById('select-campo').value;
    const nombreExamen = document.getElementById('input-examen-nombre').value.trim();
    const totalPreguntas = parseInt(document.getElementById('input-total-preguntas').value);

    if (!idCampoFormativo) {
        mostrarError('Selecciona un Campo Formativo');
        return;
    }
    if (!nombreExamen) {
        mostrarError('Escribe el nombre del examen');
        return;
    }
    if (!totalPreguntas || totalPreguntas <= 0) {
        mostrarError('Escribe el total de preguntas del examen');
        return;
    }

    const filas = document.querySelectorAll('#tabla-cuerpo tr');
    if (filas.length === 0) {
        mostrarError('No hay alumnos para registrar');
        return;
    }

    let huboError = false;
    let huboFueraDeRango = false;

    for (const fila of filas) {
        const matricula = parseInt(fila.dataset.matricula);
        const aciertosInput = fila.querySelector('.input-respuestas-correctas').value;
        const aciertos = parseInt(aciertosInput);

        if (isNaN(aciertos)) {
            continue; // Si no se capturo nada para este alumno, se omite
        }

        if (aciertos > totalPreguntas || aciertos < 0) {
            huboFueraDeRango = true;
            continue;
        }

        try {
            const response = await fetch(`${API_URL}/registro-examen`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({
                    matricula,
                    idCampoFormativo: parseInt(idCampoFormativo),
                    nombreExamen,
                    totalPreguntas,
                    aciertos
                })
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

    if (huboFueraDeRango) {
        mostrarError('Las respuestas correctas no pueden ser mayores al total de preguntas (ni negativas). Revisa los alumnos marcados fuera de rango.');
        return;
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