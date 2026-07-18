package com.educontrol.controllers;

import com.educontrol.dao.AlumnoGrupoDAO;
import com.educontrol.dao.AsignarGrupoDAO;
import com.educontrol.dao.GrupoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.modelos.AlumnoGrupo;
import com.educontrol.modelos.AsignarGrupo;
import com.educontrol.modelos.Grupo;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.util.List;

public class GrupoController {

    private static final GrupoDAO dao = new GrupoDAO();
    private static final AsignarGrupoDAO asignarGrupoDAO = new AsignarGrupoDAO();
    private static final AlumnoGrupoDAO alumnoGrupoDAO = new AlumnoGrupoDAO();
    private static final PeriodoDAO periodoDAO = new PeriodoDAO();

    public static void registrarRutas(Javalin app) {

        app.get("/grupos", ctx -> {
            try {
                ctx.json(dao.listarTodos());
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.get("/grupos/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Grupo grupo = dao.obtenerPorId(id);

                if (grupo != null) {
                    ctx.json(grupo);
                } else {
                    ctx.status(404).result("Grupo no encontrado");
                }
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.post("/grupos", ctx -> {
            try {
                Grupo grupo = ctx.bodyAsClass(Grupo.class);

                if (existeGradoGrupo(grupo.getGrado(), grupo.getGrupo(), null)) {
                    ctx.status(409).result("Ya existe un grupo " + grupo.getGrado() + "° " + grupo.getGrupo());
                    return;
                }

                int idGenerado = dao.crear(grupo);
                grupo.setIdGrupo(idGenerado);
                ctx.status(201).json(grupo);
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.put("/grupos/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Grupo grupo = ctx.bodyAsClass(Grupo.class);
                grupo.setIdGrupo(id);

                if (existeGradoGrupo(grupo.getGrado(), grupo.getGrupo(), id)) {
                    ctx.status(409).result("Ya existe otro grupo " + grupo.getGrado() + "° " + grupo.getGrupo());
                    return;
                }

                dao.actualizar(grupo);
                ctx.result("Grupo actualizado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.delete("/grupos/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));

                List<AlumnoGrupo> alumnos = alumnoGrupoDAO.listarPorGrupo(id);
                if (!alumnos.isEmpty()) {
                    ctx.status(409).result("No puedes eliminar este grupo: tiene " + alumnos.size() + " alumno(s) asignado(s). Reasigna o elimina a esos alumnos primero.");
                    return;
                }

                if (periodoDAO.contarPorGrupo(id) > 0) {
                    ctx.status(409).result("No puedes eliminar este grupo: ya tiene historial de Registro/Calificaciones (periodos abiertos o cerrados). Este grupo debe conservarse por integridad académica.");
                    return;
                }

                // Desasignamos al docente (si tenia uno) antes de borrar el grupo, para no violar la FK
                AsignarGrupo asignacion = asignarGrupoDAO.obtenerPorGrupo(id);
                if (asignacion != null) {
                    asignarGrupoDAO.eliminar(asignacion.getIdUsuario());
                }

                dao.eliminar(id);
                ctx.result("Grupo eliminado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }

    private static boolean existeGradoGrupo(int grado, String grupoLetra, Integer idExcluir) throws SQLException {
        List<Grupo> todos = dao.listarTodos();
        return todos.stream().anyMatch(g ->
            g.getGrado() == grado
            && g.getGrupo().equalsIgnoreCase(grupoLetra)
            && (idExcluir == null || g.getIdGrupo() != idExcluir)
        );
    }
}