package com.educontrol.modelos;

public class CampoFormativo {
    private int idCampoFormativo;
    private String nombre;
    private Integer grado; // Integer (no int) porque puede ser null

    public CampoFormativo() {
    }

    public CampoFormativo(int idCampoFormativo, String nombre, Integer grado) {
        this.idCampoFormativo = idCampoFormativo;
        this.nombre = nombre;
        this.grado = grado;
    }

    public int getIdCampoFormativo() {
        return idCampoFormativo;
    }

    public void setIdCampoFormativo(int idCampoFormativo) {
        this.idCampoFormativo = idCampoFormativo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getGrado() {
        return grado;
    }

    public void setGrado(Integer grado) {
        this.grado = grado;
    }
}