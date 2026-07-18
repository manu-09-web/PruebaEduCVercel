const API_URL = 'https://despliegueeduc.duckdns.orggueeduc.duckdns.org:7000';

document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('btnFinPeriodo');
    if (btn) {
        btn.addEventListener('click', finalizarPeriodo);
    }
});

async function finalizarPeriodo() {
    mostrarModalConfirmacion();
}

function mostrarModalConfirmacion() {
    let overlay = document.getElementById('modal-fin-periodo');
    if (overlay) overlay.remove();

    overlay = document.createElement('div');
    overlay.id = 'modal-fin-periodo';
    overlay.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(11,37,69,0.6); display: flex; align-items: center;
        justify-content: center; z-index: 9999;
    `;

    const caja = document.createElement('div');
    caja.style.cssText = `
        background: white; border-radius: 16px; padding: 30px; max-width: 480px;
        width: 90%; box-shadow: 0 20px 40px rgba(0,0,0,0.2);
    `;

    caja.innerHTML = `
        <h2 style="color:#0b2545; margin-top:0; font-size: 20px;">¿Estás seguro que deseas finalizar el periodo actual?</h2>
        <p style="color:#64748b; font-size:14px; line-height:1.5;">
            Esto cerrará el periodo, calculará las calificaciones de todos los alumnos de tu grupo
            y no podrás seguir registrando datos para este periodo después.
        </p>
        <div style="text-align:right; margin-top:20px; display:flex; gap:12px; justify-content:flex-end;">
            <button id="btn-cancelar-fin-periodo" style="
                background:white; color:#0b2545; border:2px solid #0b2545; border-radius:20px;
                padding:8px 24px; cursor:pointer; font-size:14px; font-weight:600;
            ">Cancelar</button>
            <button id="btn-confirmar-fin-periodo" style="
                background:#0b2545; color:white; border:none; border-radius:20px;
                padding:8px 24px; cursor:pointer; font-size:14px; font-weight:600;
            ">Confirmar</button>
        </div>
    `;

    overlay.appendChild(caja);
    document.body.appendChild(overlay);

    document.getElementById('btn-cancelar-fin-periodo').addEventListener('click', () => {
        overlay.remove();
    });

    document.getElementById('btn-confirmar-fin-periodo').addEventListener('click', () => {
        overlay.remove();
        ejecutarFinalizarPeriodo();
    });
}

async function ejecutarFinalizarPeriodo() {
    const btn = document.getElementById('btnFinPeriodo');
    btn.disabled = true;

    try {
        const response = await fetch(`${API_URL}/periodos/finalizar`, {
            method: 'POST',
            credentials: 'include'
        });

        if (response.status === 401) {
            window.location.href = '../../index.html';
            return;
        }

        if (!response.ok) {
            const texto = await response.text();
            mostrarModalResultado('Error', texto, true);
            return;
        }

        const resultado = await response.json();
        mostrarResultadoFinPeriodo(resultado);

    } catch (error) {
        console.error('Error al finalizar periodo:', error);
        mostrarModalResultado('Error', 'No se pudo conectar con el servidor', true);
    } finally {
        btn.disabled = false;
    }
}

function mostrarResultadoFinPeriodo(resultado) {
    // Caso: promedio anual (4to click)
    if (resultado.promedioAnual) {
        let contenidoHtml = `<p>${resultado.mensaje}</p>`;
        if (resultado.promedioAnual.length === 0) {
            contenidoHtml += '<p><em>No hay promedios anuales para mostrar.</em></p>';
        } else {
            contenidoHtml += `
                <table style="width:100%; border-collapse: collapse; margin-top: 10px; font-size: 13px;">
                    <thead>
                        <tr style="background:#0b2545; color:white;">
                            <th style="padding:6px; text-align:left;">Matrícula</th>
                            <th style="padding:6px; text-align:left;">Campo Formativo</th>
                            <th style="padding:6px; text-align:left;">Promedio Anual</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${resultado.promedioAnual.map(item => `
                            <tr style="border-bottom:1px solid #e2e8f0;">
                                <td style="padding:6px;">${item.matricula}</td>
                                <td style="padding:6px;">${item.nombreCampoFormativo || item.idCampoFormativo}</td>
                                <td style="padding:6px;">${item.promedioAnual}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }
        mostrarModalResultado('Promedio Final del Ciclo Escolar', contenidoHtml, false, true);
        return;
    }

    // Caso: periodo cerrado con promedios calculados, o periodo 1 recien iniciado
    let contenidoHtml = `<p>${resultado.mensaje}</p>`;
    if (resultado.promedios && resultado.promedios.length > 0) {
        contenidoHtml += `<p>Se calcularon <strong>${resultado.promedios.length}</strong> promedios (alumno × Campo Formativo).</p>`;
    }
    mostrarModalResultado('Fin de Periodo', contenidoHtml, false, true);
}

function mostrarModalResultado(titulo, contenido, esError, esHtml) {
    let overlay = document.getElementById('modal-fin-periodo');
    if (overlay) overlay.remove();

    overlay = document.createElement('div');
    overlay.id = 'modal-fin-periodo';
    overlay.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(11,37,69,0.6); display: flex; align-items: center;
        justify-content: center; z-index: 9999;
    `;

    const caja = document.createElement('div');
    caja.style.cssText = `
        background: white; border-radius: 16px; padding: 30px; max-width: 600px;
        width: 90%; max-height: 80vh; overflow-y: auto; box-shadow: 0 20px 40px rgba(0,0,0,0.2);
    `;

    const tituloColor = esError ? '#c0392b' : '#0b2545';
    caja.innerHTML = `
        <h2 style="color:${tituloColor}; margin-top:0;">${titulo}</h2>
        <div style="color:#334155; font-size:14px; line-height:1.5;">
            ${esHtml ? contenido : `<p>${contenido}</p>`}
        </div>
        <div style="text-align:right; margin-top:20px;">
            <button id="btn-cerrar-modal-fin-periodo" style="
                background:#0b2545; color:white; border:none; border-radius:20px;
                padding:8px 24px; cursor:pointer; font-size:14px;
            ">Cerrar</button>
        </div>
    `;

    overlay.appendChild(caja);
    document.body.appendChild(overlay);

    document.getElementById('btn-cerrar-modal-fin-periodo').addEventListener('click', () => {
        overlay.remove();
    });
}