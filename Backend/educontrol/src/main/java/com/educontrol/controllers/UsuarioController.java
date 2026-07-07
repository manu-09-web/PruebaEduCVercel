package com.educontrol.controllers;

import com.educontrol.dao.UsuarioDAO;
import com.educontrol.modelos.Usuario;
import io.javalin.Javalin;

import java.sql.SQLException;

public class UsuarioController {

    private static final UsuarioDAO dao = new UsuarioDAO();

    public static void registrarRutas(Javalin app) {

        app.get("/usuarios", ctx -> {
            try {
                ctx.json(dao.listarTodos());
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.get("/usuarios/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Usuario usuario = dao.obtenerPorId(id);

                if (usuario != null) {
                    ctx.json(usuario);
                } else {
                    ctx.status(404).result("Usuario no encontrado");
                }
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.post("/usuarios", ctx -> {
            try {
                Usuario usuario = ctx.bodyAsClass(Usuario.class);
                dao.crear(usuario);
                ctx.status(201).result("Usuario creado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.put("/usuarios/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Usuario usuario = ctx.bodyAsClass(Usuario.class);
                usuario.setIdUsuario(id);
                dao.actualizar(usuario);
                ctx.result("Usuario actualizado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });

        app.delete("/usuarios/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                dao.eliminar(id);
                ctx.result("Usuario eliminado correctamente");
            } catch (SQLException e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
    }
}