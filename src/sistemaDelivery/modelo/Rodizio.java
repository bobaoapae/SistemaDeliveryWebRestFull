/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import java.sql.Time;

/**
 * @author SYSTEM
 */
public class Rodizio extends Produto {

    private Time horaInicio;
    private boolean diasSemana[];

    public Rodizio() {
        this.diasSemana = new boolean[]{false, false, false, false, false, false, false};
    }

    public boolean isDomingo() {
        return this.diasSemana[0];
    }

    public void setDomingo(boolean flag) {
        this.diasSemana[0] = flag;
    }

    public boolean isSegunda() {
        return this.diasSemana[1];
    }

    public void setSegunda(boolean flag) {
        this.diasSemana[1] = flag;
    }

    public boolean isTerca() {
        return this.diasSemana[2];
    }

    public void setTerca(boolean flag) {
        this.diasSemana[2] = flag;
    }

    public boolean isQuarta() {
        return this.diasSemana[3];
    }

    public void setQuarta(boolean flag) {
        this.diasSemana[3] = flag;
    }

    public boolean isQuinta() {
        return this.diasSemana[4];
    }

    public void setQuinta(boolean flag) {
        this.diasSemana[4] = flag;
    }

    public boolean isSexta() {
        return this.diasSemana[5];
    }

    public void setSexta(boolean flag) {
        this.diasSemana[5] = flag;
    }

    public boolean isSabado() {
        return this.diasSemana[6];
    }

    public void setSabado(boolean flag) {
        this.diasSemana[6] = flag;
    }

    public Time getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(Time horaInicio) {
        this.horaInicio = horaInicio;
    }

    public boolean[] getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(boolean[] diasSemana) {
        this.diasSemana = diasSemana;
    }

    @Override
    public String getNome() {
        if (!getNome().contains("Rodizio")) {
            return "Rodizio - " + super.getNome(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return super.getNome();
        }
    }

}
