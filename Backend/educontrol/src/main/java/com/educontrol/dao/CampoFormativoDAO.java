package com.educontrol.dao;

import com.educontrol.Main;
import com.educontrol.modelos.CampoFormativo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CampoFormativoDAO {

    public List<CampoFormativo> listarTodos() throws SQLException {
        List<CampoFormativo> lista = new ArrayList<>();
        String sql = "SELECT * FROM campo_formativo";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public CampoFormativo obtenerPorId(int idCampoFormativo) throws SQLException {
        String sql = "SELECT * FROM campo_formativo WHERE idCampoFormativo = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCampoFormativo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
        }
        return null;
    }

    public void crear(CampoFormativo campo) throws SQLException {
        String sql = "INSERT INTO campo_formativo (Nombre, Grado) VALUES (?, ?)";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, campo.getNombre());
            if (campo.getGrado() != null) {
                stmt.setInt(2, campo.getGrado());
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }

            stmt.executeUpdate();
        }
    }

    public void actualizar(CampoFormativo campo) throws SQLException {
        String sql = "UPDATE campo_formativo SET Nombre = ?, Grado = ? WHERE idCampoFormativo = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, campo.getNombre());
            if (campo.getGrado() != null) {
                stmt.setInt(2, campo.getGrado());
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setInt(3, campo.getIdCampoFormativo());

            stmt.executeUpdate();
        }
    }

    public void eliminar(int idCampoFormativo) throws SQLException {
        String sql = "DELETE FROM campo_formativo WHERE idCampoFormativo = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCampoFormativo);
            stmt.executeUpdate();
        }
    }

    private CampoFormativo mapear(ResultSet rs) throws SQLException {
        int grado = rs.getInt("Grado");
        Integer gradoObj = rs.wasNull() ? null : grado;

        return new CampoFormativo(
            rs.getInt("idCampoFormativo"),
            rs.getString("Nombre"),
            gradoObj
        );
    }
}