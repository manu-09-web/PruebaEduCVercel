package com.educontrol.controllers;

import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.dao.RegistroAsistenciaDAO;
import com.educontrol.modelos.Grupo;
import com.educontrol.modelos.RegistroAsistencia;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteController {

    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final PeriodoDAO periodoDAO = new PeriodoDAO();
    private static final RegistroAsistenciaDAO registroAsistenciaDAO = new RegistroAsistenciaDAO();

    public static void registrarRutas(Javalin app) {

        // % real de asistencia (sin ponderar) del grupo del Docente logueado, para un periodo
        app.get("/reportes/asistencia", ctx -> {
            try {
                Integer idUsuario = ctx.sessionAttribute("idUsuario");
                if (idUsuario == null) {
                    ctx.status(401).result("No autorizado. Debes iniciar sesion.");
                    return;
                }

                Integer idGrupo = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupo == null) {
                    ctx.status(403).result("Este endpoint es solo para Docentes con grupo asignado");
                    return;
                }

                String periodoTexto = ctx.queryParam("periodo");
                if (periodoTexto == null || periodoTexto.isBlank()) periodoTexto = "1";

                ctx.json(calcularAsistencia(idGrupo, periodoTexto));

            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        // Igual, pero para que el Director consulte cualquier grupo
        app.get("/reportes/asistencia-grupo", ctx -> {
            try {
                Integer idUsuario = ctx.sessionAttribute("idUsuario");
                String rol = ctx.sessionAttribute("rol");
                if (idUsuario == null) {
                    ctx.status(401).result("No autorizado. Debes iniciar sesion.");
                    return;
                }
                if (!"Director".equals(rol)) {
                    ctx.status(403).result("Solo el Director puede consultar la asistencia de cualquier grupo");
                    return;
                }

                String idGrupoParam = ctx.queryParam("idGrupo");
                if (idGrupoParam == null || idGrupoParam.isBlank()) {
                    ctx.status(400).result("Falta el parametro idGrupo");
                    return;
                }
                int idGrupo = Integer.parseInt(idGrupoParam);

                String periodoTexto = ctx.queryParam("periodo");
                if (periodoTexto == null || periodoTexto.isBlank()) periodoTexto = "1";

                ctx.json(calcularAsistencia(idGrupo, periodoTexto));

            } catch (NumberFormatException e) {
                ctx.status(400).result("idGrupo invalido");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }

    private static Map<String, Object> calcularAsistencia(int idGrupo, String periodoTexto) throws SQLException {
        Grupo grupo = grupoDAO.obtenerPorId(idGrupo);
        if (grupo == null) {
            throw new SQLException("No se encontro el grupo " + idGrupo);
        }

        Integer idPeriodoAncla = periodoDAO.obtenerIdPeriodoAncla(idGrupo, periodoTexto);

        Map<String, Object> resultado = new HashMap<>();

        if (idPeriodoAncla == null) {
            resultado.put("porcentajeAsistencia", 0.0);
            resultado.put("totalRegistros", 0);
            return resultado;
        }

        List<RegistroAsistencia> registros = registroAsistenciaDAO.listarPorPeriodo(idPeriodoAncla);

        if (registros.isEmpty()) {
            resultado.put("porcentajeAsistencia", 0.0);
            resultado.put("totalRegistros", 0);
            return resultado;
        }

        long presentes = registros.stream().filter(r -> "Asistencia".equals(r.getEstado())).count();
        double porcentaje = Math.round((double) presentes / registros.size() * 1000) / 10.0;

        resultado.put("porcentajeAsistencia", porcentaje);
        resultado.put("totalRegistros", registros.size());
        return resultado;
    }
}