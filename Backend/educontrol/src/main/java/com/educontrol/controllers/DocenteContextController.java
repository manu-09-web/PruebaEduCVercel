package com.educontrol.controllers;

import com.educontrol.dao.CampoFormativoDAO;
import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.modelos.CampoFormativo;
import com.educontrol.modelos.Grupo;
import com.educontrol.modelos.Periodo;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocenteContextController {

    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final CampoFormativoDAO campoFormativoDAO = new CampoFormativoDAO();
    private static final PeriodoDAO periodoDAO = new PeriodoDAO();

    public static void registrarRutas(Javalin app) {

        // Da el grupo del Docente logueado (Grado, Grupo) para mostrarlo bloqueado en los formularios
        app.get("/mi-grupo", ctx -> {
            try {
                Integer idGrupo = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupo == null) {
                    ctx.status(403).result("Este endpoint es solo para Docentes con grupo asignado");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupo);
                if (grupo == null) {
                    ctx.status(404).result("Grupo no encontrado");
                    return;
                }

                Map<String, Object> resultado = new HashMap<>();
                resultado.put("idGrupo", grupo.getIdGrupo());
                resultado.put("grado", grupo.getGrado());
                resultado.put("grupo", grupo.getGrupo());

                ctx.json(resultado);
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        // Da el periodo actualmente abierto para el grupo del Docente logueado
        // (lo inicializa en "1" automaticamente si el grupo nunca ha registrado nada)
        app.get("/mi-periodo-actual", ctx -> {
            try {
                Integer idGrupo = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupo == null) {
                    ctx.status(403).result("Este endpoint es solo para Docentes con grupo asignado");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupo);
                if (grupo == null) {
                    ctx.status(404).result("Grupo no encontrado");
                    return;
                }

                // Forzamos la inicializacion del Periodo 1 si nunca se ha usado (mismo bootstrap que usan los registros)
                PeriodoUtil.obtenerIdPeriodoParaAsistenciaODisciplina(idGrupo, grupo.getGrado());

                List<Periodo> abiertos = periodoDAO.listarAbiertosPorGrupo(idGrupo);

                Map<String, Object> resultado = new HashMap<>();
                if (abiertos.isEmpty()) {
                    resultado.put("periodo", null);
                    resultado.put("estado", "SinPeriodoAbierto");
                    resultado.put("mensaje", "El ciclo escolar esta completo. Ve a Fin de Periodo para ver el promedio anual.");
                } else {
                    resultado.put("periodo", abiertos.get(0).getPeriodo());
                    resultado.put("estado", "Abierto");
                }

                // Tomamos el CicloEscolar de cualquier Campo Formativo de ese grado (todos comparten el mismo)
                List<CampoFormativo> campos = campoFormativoDAO.listarPorGrado(grupo.getGrado());
                resultado.put("cicloEscolar", campos.isEmpty() ? null : campos.get(0).getCicloEscolar());

                ctx.json(resultado);
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        // Da los Campos Formativos que le corresponden al Docente logueado (segun el grado de su grupo)
        app.get("/mis-campos-formativos", ctx -> {
            try {
                Integer idGrupo = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupo == null) {
                    ctx.status(403).result("Este endpoint es solo para Docentes con grupo asignado");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupo);
                if (grupo == null) {
                    ctx.status(404).result("Grupo no encontrado");
                    return;
                }

                ctx.json(campoFormativoDAO.listarPorGrado(grupo.getGrado()));
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }
}