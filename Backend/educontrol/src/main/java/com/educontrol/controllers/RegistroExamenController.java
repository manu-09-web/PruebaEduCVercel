package com.educontrol.controllers;

import com.educontrol.dao.ConfigExamenDAO;
import com.educontrol.dao.DetalleExamenDAO;
import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.dao.RegistroExamenDAO;
import com.educontrol.modelos.ConfigExamen;
import com.educontrol.modelos.DetalleExamen;
import com.educontrol.modelos.Grupo;
import com.educontrol.modelos.RegistroExamen;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

public class RegistroExamenController {

    private static final RegistroExamenDAO dao = new RegistroExamenDAO();
    private static final DetalleExamenDAO detalleExamenDAO = new DetalleExamenDAO();
    private static final GrupoDAO grupoDAO = new GrupoDAO();
    private static final ConfigExamenDAO configExamenDAO = new ConfigExamenDAO();
    private static final PeriodoDAO periodoDAO = new PeriodoDAO();

    public static void registrarRutas(Javalin app) {

        app.get("/registro-examen", ctx -> {
            try {
                ctx.json(dao.listarTodos());
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.get("/registro-examen/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroExamen registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de examen no encontrado");
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

        // Body esperado: { matricula, idCampoFormativo, nombreExamen, totalPreguntas, aciertos }
        // idExamen, idPeriodo, idUsuario y fecha se resuelven automaticamente en el backend.
        app.post("/registro-examen", ctx -> {
            try {
                Integer idUsuarioSesion = ctx.sessionAttribute("idUsuario");
                if (idUsuarioSesion == null) {
                    ctx.status(401).result("No autorizado. Debes iniciar sesion.");
                    return;
                }

                Map<String, Object> body = ctx.bodyAsClass(Map.class);

                int matricula = ((Number) body.get("matricula")).intValue();
                int idCampoFormativo = ((Number) body.get("idCampoFormativo")).intValue();
                String nombreExamen = (String) body.get("nombreExamen");
                int totalPreguntas = ((Number) body.get("totalPreguntas")).intValue();
                float aciertos = ((Number) body.get("aciertos")).floatValue();

                if (totalPreguntas <= 0) {
                    ctx.status(400).result("El total de preguntas debe ser mayor a 0");
                    return;
                }
                if (aciertos < 0 || aciertos > totalPreguntas) {
                    ctx.status(400).result("Los aciertos no pueden ser negativos ni mayores al total de preguntas");
                    return;
                }

                if (!SeguridadUtil.alumnoPerteneceAGrupoDocente(ctx, matricula)) {
                    ctx.status(403).result("No tienes permiso para registrar examenes de este alumno");
                    return;
                }

                Integer idGrupoDocente = SeguridadUtil.obtenerGrupoDelDocente(ctx);
                if (idGrupoDocente == null) {
                    ctx.status(403).result("Solo un Docente con grupo asignado puede registrar examenes");
                    return;
                }

                Grupo grupo = grupoDAO.obtenerPorId(idGrupoDocente);
                if (grupo == null) {
                    ctx.status(500).result("No se pudo determinar el grado del grupo");
                    return;
                }

                ConfigExamen configExamen = configExamenDAO.obtenerPorUsuario(idUsuarioSesion);
                if (configExamen == null) {
                    ctx.status(400).result("Primero debes configurar tu criterio de Examen en el modulo de Evaluacion");
                    return;
                }

                int idPeriodo = PeriodoUtil.obtenerIdPeriodoParaMateria(idGrupoDocente, grupo.getGrado(), idCampoFormativo);

                RegistroExamen registro = new RegistroExamen(
                    0,
                    LocalDate.now(),
                    nombreExamen,
                    matricula,
                    configExamen.getIdExamen(),
                    idUsuarioSesion,
                    idCampoFormativo,
                    idPeriodo
                );

                dao.crear(registro);

                // Recuperamos el registro recien creado para obtener su idRegistroExamen autogenerado
                // (buscamos el ultimo de este alumno+campo+periodo, ya que acabamos de insertarlo)
                java.util.List<RegistroExamen> insertados = dao.listarPorAlumnoCampoPeriodo(matricula, idCampoFormativo, idPeriodo);
                RegistroExamen recienCreado = insertados.get(insertados.size() - 1);

                DetalleExamen detalle = new DetalleExamen(0, recienCreado.getIdRegistroExamen(), totalPreguntas, aciertos);
                detalleExamenDAO.crear(detalle);

                ctx.status(201).result("Registro de examen creado correctamente");
            } catch (SQLException e) {
                ctx.status(400).result("Error: " + e.getMessage());
            } catch (Exception e) {
                ctx.status(400).result("Datos invalidos: " + e.getMessage());
            }
        });

        app.put("/registro-examen/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroExamen existente = dao.obtenerPorId(id);
                if (existente == null) {
                    ctx.status(404).result("Registro de examen no encontrado");
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
                String nombreExamen = (String) body.get("nombreExamen");

                existente.setNombreExamen(nombreExamen);
                dao.actualizar(existente);

                if (body.get("totalPreguntas") != null && body.get("aciertos") != null) {
                    int totalPreguntas = ((Number) body.get("totalPreguntas")).intValue();
                    float aciertos = ((Number) body.get("aciertos")).floatValue();

                    if (totalPreguntas <= 0) {
                        ctx.status(400).result("El total de preguntas debe ser mayor a 0");
                        return;
                    }
                    if (aciertos < 0 || aciertos > totalPreguntas) {
                        ctx.status(400).result("Los aciertos no pueden ser negativos ni mayores al total de preguntas");
                        return;
                    }

                    DetalleExamen detalle = detalleExamenDAO.obtenerPorRegistroExamen(existente.getIdRegistroExamen());
                    if (detalle == null) {
                        detalleExamenDAO.crear(new DetalleExamen(0, existente.getIdRegistroExamen(), totalPreguntas, aciertos));
                    } else {
                        detalle.setTotalPreguntas(totalPreguntas);
                        detalle.setAciertos(aciertos);
                        detalleExamenDAO.actualizar(detalle);
                    }
                }

                ctx.result("Registro de examen actualizado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.delete("/registro-examen/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                RegistroExamen registro = dao.obtenerPorId(id);

                if (registro == null) {
                    ctx.status(404).result("Registro de examen no encontrado");
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
                ctx.result("Registro de examen eliminado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }
}