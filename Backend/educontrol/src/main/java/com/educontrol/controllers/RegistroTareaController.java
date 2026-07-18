package com.educontrol.controllers;

import com.educontrol.dao.ConfigTareaDAO;
import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.dao.RegistroTareaDAO;
import com.educontrol.modelos.ConfigTarea;
import com.educontrol.modelos.Grupo;
import com.educontrol.modelos.RegistroTarea;
import io.javalin.Javalin;

import java.sql.SQLException;

public class RegistroTareaController {

    private static final RegistroTareaDAO dao = new RegistroTareaDAO();
    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final ConfigTareaDAO configTareaDAO = new ConfigTareaDAO();
    private static final PeriodoDAO periodoDAO = new PeriodoDAO();

    public static void registrarRutas(Javalin app) {

        app.get("/registro-tarea", ctx -> {
            try {
                ctx.json(dao.listarTodos());
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.get("/registro-tarea/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroTarea registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de tarea no encontrado");
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

        // El frontend YA NO manda idPeriodo ni idTarea: el backend los resuelve automaticamente
        // segun el grupo del docente logueado y su propia configuracion de criterios.
        app.post("/registro-tarea", ctx -> {
            try {
                Integer idUsuarioSesion = ctx.sessionAttribute("idUsuario");
                if (idUsuarioSesion == null) {
                    ctx.status(401).result("No autorizado. Debes iniciar sesion.");
                    return;
                }

                RegistroTarea registro = ctx.bodyAsClass(RegistroTarea.class);

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, registro.getMatricula())) {
                    ctx.status(403).result("No tienes permiso para registrar tareas de este alumno");
                    return;
                }

                Integer idGrupoDocente = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupoDocente == null) {
                    ctx.status(403).result("Solo un Docente con grupo asignado puede registrar tareas");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupoDocente);
                if (grupo == null) {
                    ctx.status(500).result("No se pudo determinar el grado del grupo");
                    return;
                }

                ConfigTarea configTarea = configTareaDAO.obtenerPorUsuario(idUsuarioSesion);
                if (configTarea == null) {
                    ctx.status(400).result("Primero debes configurar tu criterio de Tareas en el modulo de Evaluacion");
                    return;
                }

                int idPeriodo = PeriodoUtil.obtenerIdPeriodoParaMateria(idGrupoDocente, grupo.getGrado(), registro.getIdCampoFormativo());
                registro.setIdPeriodo(idPeriodo);
                registro.setIdUsuario(idUsuarioSesion);
                registro.setIdTarea(configTarea.getIdTarea());

                dao.crear(registro);
                ctx.status(201).result("Registro de tarea creado correctamente");
            } catch (SQLException e) {
                ctx.status(400).result("Error: " + e.getMessage());
            }
        });

        app.put("/registro-tarea/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroTarea registro = ctx.bodyAsClass(RegistroTarea.class);
                registro.setIdRegistroTarea(id);

                RegistroTarea existente = dao.obtenerPorId(id);
                if (existente == null) {
                    ctx.status(404).result("Registro de tarea no encontrado");
                    return;
                }

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, registro.getMatricula())) {
                    ctx.status(403).result("No tienes permiso para editar este registro");
                    return;
                }

                if (!periodoDAO.estaAbierto(existente.getIdPeriodo())) {
                    ctx.status(403).result("El periodo ya esta cerrado. No se puede editar este registro.");
                    return;
                }

                // El periodo, la tarea configurada y el docente NO se pueden cambiar desde edicion
                registro.setIdPeriodo(existente.getIdPeriodo());
                registro.setIdUsuario(existente.getIdUsuario());
                registro.setIdTarea(existente.getIdTarea());

                dao.actualizar(registro);
                ctx.result("Registro de tarea actualizado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.delete("/registro-tarea/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroTarea registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de tarea no encontrado");
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
                ctx.result("Registro de tarea eliminado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }
}