package com.educontrol.controllers;

import com.educontrol.dao.ConfigDisciplinaDAO;
import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.dao.RegistroDisciplinaDAO;
import com.educontrol.modelos.ConfigDisciplina;
import com.educontrol.modelos.Grupo;
import com.educontrol.modelos.RegistroDisciplina;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

public class RegistroDisciplinaController {

    private static final RegistroDisciplinaDAO dao = new RegistroDisciplinaDAO();
    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final ConfigDisciplinaDAO configDisciplinaDAO = new ConfigDisciplinaDAO();
    private static final PeriodoDAO periodoDAO = new PeriodoDAO();

    public static void registrarRutas(Javalin app) {

        app.get("/registro-disciplina", ctx -> {
            try {
                ctx.json(dao.listarTodos());
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.get("/registro-disciplina/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroDisciplina registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de disciplina no encontrado");
                    return;
                }

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, registro.getMatricula())) {
                    ctx.status(403).result("No tienes permiso para ver este registro");
                    return;
                }

                ctx.json(registro);
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        // Body esperado: { matricula, puntosMenos, observaciones }
        // NO se manda idCampoFormativo: Disciplina aplica igual a todas las materias del grupo.
        app.post("/registro-disciplina", ctx -> {
            try {
                Integer idUsuarioSesion = ctx.sessionAttribute("idUsuario");
                if (idUsuarioSesion == null) {
                    ctx.status(401).result("No autorizado. Debes iniciar sesion.");
                    return;
                }

                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int matricula = ((Number) body.get("matricula")).intValue();
                int puntosMenos = ((Number) body.get("puntosMenos")).intValue();
                String observaciones = (String) body.get("observaciones");

                if (puntosMenos <= 0 || puntosMenos > 10) {
                    ctx.status(400).result("Los puntos menos deben estar entre 1 y 10 (solo se registra si hay una incidencia real)");
                    return;
                }

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, matricula)) {
                    ctx.status(403).result("No tienes permiso para registrar disciplina de este alumno");
                    return;
                }

                Integer idGrupoDocente = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupoDocente == null) {
                    ctx.status(403).result("Solo un Docente con grupo asignado puede registrar disciplina");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupoDocente);
                if (grupo == null) {
                    ctx.status(500).result("No se pudo determinar el grado del grupo");
                    return;
                }

                ConfigDisciplina configDisciplina = configDisciplinaDAO.obtenerPorUsuario(idUsuarioSesion);
                if (configDisciplina == null) {
                    ctx.status(400).result("Primero debes configurar tu criterio de Disciplina en el modulo de Evaluacion");
                    return;
                }

                int idPeriodo = PeriodoUtil.obtenerIdPeriodoParaAsistenciaODisciplina(idGrupoDocente, grupo.getGrado());

                RegistroDisciplina registro = new RegistroDisciplina(
                    0,
                    observaciones,
                    puntosMenos,
                    LocalDate.now(),
                    matricula,
                    configDisciplina.getIdConfigDisciplina(),
                    idUsuarioSesion,
                    idPeriodo
                );

                dao.crear(registro);
                ctx.status(201).result("Registro de disciplina creado correctamente");
            } catch (SQLException e) {
                ctx.status(400).result("Error: " + e.getMessage());
            } catch (Exception e) {
                ctx.status(400).result("Datos invalidos: " + e.getMessage());
            }
        });

        app.put("/registro-disciplina/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroDisciplina existente = dao.obtenerPorId(id);
                if (existente == null) {
                    ctx.status(404).result("Registro de disciplina no encontrado");
                    return;
                }

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, existente.getMatricula())) {
                    ctx.status(403).result("No tienes permiso para editar este registro");
                    return;
                }

                if (!periodoDAO.estaAbierto(existente.getIdPeriodo())) {
                    ctx.status(403).result("El periodo ya esta cerrado. No se puede editar este registro.");
                    return;
                }

                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int puntosMenos = ((Number) body.get("puntosMenos")).intValue();
                String observaciones = (String) body.get("observaciones");

                existente.setPuntosMenos(puntosMenos);
                existente.setObservaciones(observaciones);

                dao.actualizar(existente);
                ctx.result("Registro de disciplina actualizado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.delete("/registro-disciplina/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroDisciplina registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de disciplina no encontrado");
                    return;
                }

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, registro.getMatricula())) {
                    ctx.status(403).result("No tienes permiso para eliminar este registro");
                    return;
                }

                if (!periodoDAO.estaAbierto(registro.getIdPeriodo())) {
                    ctx.status(403).result("El periodo ya esta cerrado. No se puede eliminar este registro.");
                    return;
                }

                dao.eliminar(id);
                ctx.result("Registro de disciplina eliminado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }
}