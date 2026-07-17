package com.educontrol.controllers;

import com.educontrol.dao.ConfigAsistenciaDAO;
import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.dao.RegistroAsistenciaDAO;
import com.educontrol.modelos.ConfigAsistencia;
import com.educontrol.modelos.Grupo;
import com.educontrol.modelos.RegistroAsistencia;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

public class RegistroAsistenciaController {

    private static final RegistroAsistenciaDAO dao = new RegistroAsistenciaDAO();
    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final ConfigAsistenciaDAO configAsistenciaDAO = new ConfigAsistenciaDAO();
    private static final PeriodoDAO periodoDAO = new PeriodoDAO();

    public static void registrarRutas(Javalin app) {

        app.get("/registro-asistencia", ctx -> {
            try {
                ctx.json(dao.listarTodos());
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.get("/registro-asistencia/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroAsistencia registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de asistencia no encontrado");
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

        // Body esperado: { matricula, estado } (estado: 'Asistencia' | 'Falta' | 'Permiso')
        // NO se manda idCampoFormativo: Asistencia aplica igual a todas las materias del grupo.
        app.post("/registro-asistencia", ctx -> {
            try {
                Integer idUsuarioSesion = ctx.sessionAttribute("idUsuario");
                if (idUsuarioSesion == null) {
                    ctx.status(401).result("No autorizado. Debes iniciar sesion.");
                    return;
                }

                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int matricula = ((Number) body.get("matricula")).intValue();
                String estado = (String) body.get("estado");

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, matricula)) {
                    ctx.status(403).result("No tienes permiso para registrar asistencia de este alumno");
                    return;
                }

                Integer idGrupoDocente = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupoDocente == null) {
                    ctx.status(403).result("Solo un Docente con grupo asignado puede registrar asistencia");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupoDocente);
                if (grupo == null) {
                    ctx.status(500).result("No se pudo determinar el grado del grupo");
                    return;
                }

                ConfigAsistencia configAsistencia = configAsistenciaDAO.obtenerPorUsuario(idUsuarioSesion);
                if (configAsistencia == null) {
                    ctx.status(400).result("Primero debes configurar tu criterio de Asistencia en el modulo de Evaluacion");
                    return;
                }

                int idPeriodo = PeriodoUtil.obtenerIdPeriodoParaAsistenciaODisciplina(idGrupoDocente, grupo.getGrado());

                RegistroAsistencia registro = new RegistroAsistencia(
                    0,
                    estado,
                    LocalDate.now(),
                    matricula,
                    configAsistencia.getIdConfigAsistencia(),
                    idUsuarioSesion,
                    idPeriodo
                );

                dao.crear(registro);
                ctx.status(201).result("Registro de asistencia creado correctamente");
            } catch (SQLException e) {
                ctx.status(400).result("Error: " + e.getMessage());
            } catch (Exception e) {
                ctx.status(400).result("Datos invalidos: " + e.getMessage());
            }
        });

        app.put("/registro-asistencia/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroAsistencia existente = dao.obtenerPorId(id);
                if (existente == null) {
                    ctx.status(404).result("Registro de asistencia no encontrado");
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
                String estado = (String) body.get("estado");
                existente.setEstado(estado);

                dao.actualizar(existente);
                ctx.result("Registro de asistencia actualizado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.delete("/registro-asistencia/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroAsistencia registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de asistencia no encontrado");
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
                ctx.result("Registro de asistencia eliminado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }
}