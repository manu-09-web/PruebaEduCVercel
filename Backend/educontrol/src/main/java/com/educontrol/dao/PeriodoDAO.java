package com.educontrol.dao;

import com.educontrol.Main;
import com.educontrol.modelos.Periodo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PeriodoDAO {

    public List<Periodo> listarTodos() throws SQLException {
        List<Periodo> lista = new ArrayList<>();
        String sql = "SELECT * FROM periodo";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Periodo obtenerPorId(int idPeriodo) throws SQLException {
        String sql = "SELECT * FROM periodo WHERE idPeriodo = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPeriodo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapear(rs);
            }
        }
        return null;
    }

    public void crear(Periodo periodo) throws SQLException {
        String sql = "INSERT INTO periodo (periodo, idGrupo, idCampoFormativo) VALUES (?, ?, ?)";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, periodo.getPeriodo());
            stmt.setInt(2, periodo.getIdGrupo());
            stmt.setInt(3, periodo.getIdCampoFormativo());

            stmt.executeUpdate();
        }
    }

    public void actualizar(Periodo periodo) throws SQLException {
        String sql = "UPDATE periodo SET periodo = ?, idGrupo = ?, idCampoFormativo = ? WHERE idPeriodo = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, periodo.getPeriodo());
            stmt.setInt(2, periodo.getIdGrupo());
            stmt.setInt(3, periodo.getIdCampoFormativo());
            stmt.setInt(4, periodo.getIdPeriodo());

            stmt.executeUpdate();
        }
    }

    public void eliminar(int idPeriodo) throws SQLException {
        String sql = "DELETE FROM periodo WHERE idPeriodo = ?";

        try (Connection conn = Main.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPeriodo);
            stmt.executeUpdate();
        }
    }

    private Periodo mapear(ResultSet rs) throws SQLException {
        return new Periodo(
            rs.getInt("idPeriodo"),
            rs.getString("periodo"),
            rs.getInt("idGrupo"),
            rs.getInt("idCampoFormativo")
        );
    }
}