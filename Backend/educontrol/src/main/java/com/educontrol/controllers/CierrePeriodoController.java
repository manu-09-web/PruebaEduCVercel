package com.educontrol.controllers;

import com.educontrol.dao.AlumnoGrupoDAO;
import com.educontrol.dao.CampoFormativoDAO;
import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.dao.PromedioDAO;
import com.educontrol.modelos.AlumnoGrupo;
import com.educontrol.modelos.CampoFormativo;
import com.educontrol.modelos.Grupo;
import com.educontrol.modelos.Periodo;
import com.educontrol.modelos.Promedio;
import io.javalin.Javalin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CierrePeriodoController {

    private static final PeriodoDAO periodoDAO = new PeriodoDAO();
    private static final CampoFormativoDAO campoFormativoDAO = new CampoFormativoDAO();
    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final AlumnoGrupoDAO alumnoGrupoDAO = new AlumnoGrupoDAO();
    private static final PromedioDAO promedioDAO = new PromedioDAO();

    public static void registrarRutas(Javalin app) {

        // El Docente presiona "Fin de Periodo" sobre SU PROPIO grupo (no recibe idGrupo por parametro,
        // se resuelve de la sesion para que nadie pueda cerrar el periodo de otro grupo).
        app.post("/periodos/finalizar", ctx -> {
            try {
                Integer idUsuario = ctx.sessionAttribute("idUsuario");
                String rol = ctx.sessionAttribute("rol");

                if (idUsuario == null) {
                    ctx.status(401).result("No autorizado. Debes iniciar sesion.");
                    return;
                }
                if (!"Docente".equals(rol)) {
                    ctx.status(403).result("Solo un Docente puede finalizar el periodo de su grupo");
                    return;
                }

                Integer idGrupo = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupo == null) {
                    ctx.status(403).result("No tienes un grupo asignado");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupo);
                if (grupo == null) {
                    ctx.status(500).result("No se pudo determinar el grado del grupo");
                    return;
                }
                int grado = grupo.getGrado();

                List<CampoFormativo> campos = campoFormativoDAO.listarPorGrado(grado);
                if (campos.isEmpty()) {
                    ctx.status(500).result("No hay Campos Formativos configurados para el grado " + grado);
                    return;
                }

                List<Periodo> abiertos = periodoDAO.listarAbiertosPorGrupo(idGrupo);

                // CASO A: nunca se ha registrado nada para este grupo -> inicializa Periodo 1 y no cierra nada aun
                if (abiertos.isEmpty() && periodoDAO.contarPorGrupo(idGrupo) == 0) {
                    periodoDAO.crearFilasParaPeriodo(idGrupo, "1", campos);
                    ctx.json(mensaje("Periodo 1 iniciado. Registra tareas, examenes, participacion, asistencia y disciplina, y vuelve a presionar este boton cuando quieras cerrar el Periodo 1."));
                    return;
                }

                // CASO B: no hay periodo abierto, pero ya existieron periodos antes -> significa que el
                // Periodo 3 ya se cerro (nunca se abre un Periodo 4). Este es el "4to click": promedio anual.
                if (abiertos.isEmpty()) {
                    int maxPeriodo = periodoDAO.obtenerNumeroPeriodoMasAlto(idGrupo);
                    if (maxPeriodo < 3) {
                        // Estado inesperado (no deberia pasar en flujo normal)
                        ctx.status(409).result("El periodo esta cerrado pero no se alcanzo el Periodo 3. Revisa el estado de PERIODO para este grupo.");
                        return;
                    }

                    Map<String, Object> promedioAnual = calcularPromedioAnual(idGrupo, grado, campos);
                    ctx.json(promedioAnual);
                    return;
                }

                // CASO C: hay un periodo abierto (1 o 2 o 3) -> cerrarlo, calcular promedios, abrir el siguiente si aplica
                String periodoTextoActual = abiertos.get(0).getPeriodo();
                int numeroActual = Integer.parseInt(periodoTextoActual);

                List<AlumnoGrupo> alumnosDelGrupo = alumnoGrupoDAO.listarPorGrupo(idGrupo);

                List<Map<String, Object>> promediosCalculados = new ArrayList<>();

                for (AlumnoGrupo ag : alumnosDelGrupo) {
                    for (Periodo periodoDeMateria : abiertos) {
                        BigDecimal promedio = PromedioController.calcularYGuardarPromedio(
                            idUsuario,
                            ag.getMatricula(),
                            periodoDeMateria.getIdCampoFormativo(),
                            periodoDeMateria.getIdPeriodo(),
                            grado
                        );

                        Map<String, Object> item = new HashMap<>();
                        item.put("matricula", ag.getMatricula());
                        item.put("idCampoFormativo", periodoDeMateria.getIdCampoFormativo());
                        item.put("promedioFinal", promedio);
                        promediosCalculados.add(item);
                    }
                }

                // Cerrar TODAS las filas de este grupo+periodo en una sola operacion por lote
                periodoDAO.cerrarPorGrupoYPeriodo(idGrupo, periodoTextoActual);

                Map<String, Object> respuesta = new HashMap<>();

                if (numeroActual < 3) {
                    String siguienteTexto = String.valueOf(numeroActual + 1);
                    periodoDAO.crearFilasParaPeriodo(idGrupo, siguienteTexto, campos);
                    respuesta.put("mensaje", "Periodo " + numeroActual + " cerrado. Promedios calculados. Periodo " + siguienteTexto + " iniciado.");
                } else {
                    respuesta.put("mensaje", "Periodo 3 cerrado. Promedios calculados. El ciclo escolar esta completo: la proxima vez que presiones este boton se calculara el promedio final anual.");
                }

                respuesta.put("periodoCerrado", periodoTextoActual);
                respuesta.put("promedios", promediosCalculados);
                ctx.json(respuesta);

            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                ctx.status(500).result("El texto de periodo guardado en PERIODO no es un numero valido (se esperaba '1', '2' o '3').");
            }
        });
    }

    // Promedio anual = promedio de los 3 PromedioFinal (Periodo 1, 2 y 3) por alumno y Campo Formativo.
    // Se calcula AL VUELO, no se guarda en ninguna tabla (por decision del cliente).
    private static Map<String, Object> calcularPromedioAnual(int idGrupo, int grado, List<CampoFormativo> campos) throws SQLException {
        List<AlumnoGrupo> alumnosDelGrupo = alumnoGrupoDAO.listarPorGrupo(idGrupo);

        List<Periodo> periodo1 = periodoDAO.listarTodos().stream()
            .filter(p -> p.getIdGrupo() == idGrupo && "1".equals(p.getPeriodo()))
            .toList();
        List<Periodo> periodo2 = periodoDAO.listarTodos().stream()
            .filter(p -> p.getIdGrupo() == idGrupo && "2".equals(p.getPeriodo()))
            .toList();
        List<Periodo> periodo3 = periodoDAO.listarTodos().stream()
            .filter(p -> p.getIdGrupo() == idGrupo && "3".equals(p.getPeriodo()))
            .toList();

        List<Map<String, Object>> resultados = new ArrayList<>();

        for (AlumnoGrupo ag : alumnosDelGrupo) {
            for (CampoFormativo campo : campos) {
                BigDecimal suma = BigDecimal.ZERO;
                int encontrados = 0;

                for (List<Periodo> listaPeriodo : List.of(periodo1, periodo2, periodo3)) {
                    Periodo p = listaPeriodo.stream()
                        .filter(x -> x.getIdCampoFormativo() == campo.getIdCampoFormativo())
                        .findFirst().orElse(null);
                    if (p == null) continue;

                    Promedio prom = promedioDAO.obtenerPorMatriculaCampoPeriodo(ag.getMatricula(), campo.getIdCampoFormativo(), p.getIdPeriodo());
                    if (prom != null && prom.getPromedioFinal() != null) {
                        suma = suma.add(prom.getPromedioFinal());
                        encontrados++;
                    }
                }

                if (encontrados > 0) {
                    BigDecimal promedioAnual = suma.divide(BigDecimal.valueOf(encontrados), 2, RoundingMode.HALF_UP);

                    Map<String, Object> item = new HashMap<>();
                    item.put("matricula", ag.getMatricula());
                    item.put("idCampoFormativo", campo.getIdCampoFormativo());
                    item.put("nombreCampoFormativo", campo.getNombre());
                    item.put("promedioAnual", promedioAnual);
                    item.put("periodosConsiderados", encontrados);
                    resultados.add(item);
                }
            }
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Promedio final del ciclo escolar calculado (no se guarda, se calcula al momento de consultarlo).");
        respuesta.put("promedioAnual", resultados);
        return respuesta;
    }

    private static Map<String, Object> mensaje(String texto) {
        Map<String, Object> m = new HashMap<>();
        m.put("mensaje", texto);
        return m;
    }
}