package com.educontrol.dao;

import com.educontrol.Main;
import com.educontrol.modelos.RegistroTarea;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegistroTareaDAO {

    public List<RegistroTarea> listarTodos() throws SQLException {
        List<RegistroTarea> lista = new ArrayList<>();
        String sql = "SELECT * FROM registro_tarea";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public RegistroTarea obtenerPorId(int idRegistroTarea) throws SQLException {
        String sql = "SELECT * FROM registro_tarea WHERE idRegistroTarea = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idRegistroTarea);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
        }
        return null;
    }

    public void crear(RegistroTarea registro) throws SQLException {
        String sql = "INSERT INTO registro_tarea (Nombre, Observaciones, puntaje, estatus, Matricula, idTarea, idUsuario) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, registro.getNombre());
            stmt.setString(2, registro.getObservaciones());
            stmt.setFloat(3, registro.getPuntaje());
            stmt.setBoolean(4, registro.isEstatus());
            stmt.setInt(5, registro.getMatricula());
            stmt.setInt(6, registro.getIdTarea());
            stmt.setInt(7, registro.getIdUsuario());

            stmt.executeUpdate();
        }
    }

    public void actualizar(RegistroTarea registro) throws SQLException {
        String sql = "UPDATE registro_tarea SET Nombre = ?, Observaciones = ?, puntaje = ?, estatus = ?, " +
                     "Matricula = ?, idTarea = ?, idUsuario = ? WHERE idRegistroTarea = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, registro.getNombre());
            stmt.setString(2, registro.getObservaciones());
            stmt.setFloat(3, registro.getPuntaje());
            stmt.setBoolean(4, registro.isEstatus());
            stmt.setInt(5, registro.getMatricula());
            stmt.setInt(6, registro.getIdTarea());
            stmt.setInt(7, registro.getIdUsuario());
            stmt.setInt(8, registro.getIdRegistroTarea());

            stmt.executeUpdate();
        }
    }

    public void eliminar(int idRegistroTarea) throws SQLException {
        String sql = "DELETE FROM registro_tarea WHERE idRegistroTarea = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idRegistroTarea);
            stmt.executeUpdate();
        }
    }

    private RegistroTarea mapear(ResultSet rs) throws SQLException {
        return new RegistroTarea(
            rs.getInt("idRegistroTarea"),
            rs.getString("Nombre"),
            rs.getString("Observaciones"),
            rs.getFloat("puntaje"),
            rs.getBoolean("estatus"),
            rs.getInt("Matricula"),
            rs.getInt("idTarea"),
            rs.getInt("idUsuario")
        );
    }
}