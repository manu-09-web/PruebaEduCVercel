package com.educontrol.controllers;

import com.educontrol.dao.CampoFormativoDAO;
import com.educontrol.dao.PeriodoDAO;
import com.educontrol.modelos.CampoFormativo;

import java.sql.SQLException;
import java.util.List;

public class PeriodoUtil {

    private static final PeriodoDAO periodoDAO = new PeriodoDAO();
    private static final CampoFormativoDAO campoFormativoDAO = new CampoFormativoDAO();

    // Da el idPeriodo que corresponde usar AHORA MISMO para un registro de Tarea/Examen/Participacion
    // (especifico de esa materia). Si el grupo nunca ha tenido un periodo, lo inicializa en "1".
    public static int obtenerIdPeriodoParaMateria(int idGrupo, int grado, int idCampoFormativo) throws SQLException {
        asegurarPeriodoInicializado(idGrupo, grado);

        Integer idPeriodo = periodoDAO.obtenerIdPeriodoAbiertoPorCampo(idGrupo, idCampoFormativo);
        if (idPeriodo == null) {
            throw new SQLException("El periodo esta cerrado para este grupo. No se pueden agregar registros hasta iniciar el siguiente periodo.");
        }
        return idPeriodo;
    }

    // Da el idPeriodo "ancla" para Asistencia/Disciplina (no varian por materia)
    public static int obtenerIdPeriodoParaAsistenciaODisciplina(int idGrupo, int grado) throws SQLException {
        asegurarPeriodoInicializado(idGrupo, grado);

        List<com.educontrol.modelos.Periodo> abiertos = periodoDAO.listarAbiertosPorGrupo(idGrupo);
        if (abiertos.isEmpty()) {
            throw new SQLException("El periodo esta cerrado para este grupo. No se pueden agregar registros hasta iniciar el siguiente periodo.");
        }
        String periodoTexto = abiertos.get(0).getPeriodo();
        Integer idAncla = periodoDAO.obtenerIdPeriodoAncla(idGrupo, periodoTexto);
        if (idAncla == null) {
            throw new SQLException("No se pudo resolver el periodo ancla para el grupo " + idGrupo);
        }
        return idAncla;
    }

    // Si el grupo nunca ha tenido ninguna fila de PERIODO, crea el Periodo "1" (una fila por cada
    // Campo Formativo de su grado). No hace nada si ya existen filas (abiertas o cerradas).
    private static void asegurarPeriodoInicializado(int idGrupo, int grado) throws SQLException {
        int totalExistente = periodoDAO.contarPorGrupo(idGrupo);
        if (totalExistente == 0) {
            List<CampoFormativo> campos = campoFormativoDAO.listarPorGrado(grado);
            if (campos.isEmpty()) {
                throw new SQLException("No hay Campos Formativos registrados para el grado " + grado);
            }
            periodoDAO.crearFilasParaPeriodo(idGrupo, "1", campos);
        }
    }
}