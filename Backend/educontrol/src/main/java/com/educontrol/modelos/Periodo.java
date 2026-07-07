package com.educontrol.modelos;

public class Periodo {
    private int idPeriodo;
    private String periodo;
    private int idGrupo;
    private int idCampoFormativo;

    public Periodo() {
    }

    public Periodo(int idPeriodo, String periodo, int idGrupo, int idCampoFormativo) {
        this.idPeriodo = idPeriodo;
        this.periodo = periodo;
        this.idGrupo = idGrupo;
        this.idCampoFormativo = idCampoFormativo;
    }

    public int getIdPeriodo() {
        return idPeriodo;
    }

    public void setIdPeriodo(int idPeriodo) {
        this.idPeriodo = idPeriodo;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public int getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(int idGrupo) {
        this.idGrupo = idGrupo;
    }

    public int getIdCampoFormativo() {
        return idCampoFormativo;
    }

    public void setIdCampoFormativo(int idCampoFormativo) {
        this.idCampoFormativo = idCampoFormativo;
    }
}