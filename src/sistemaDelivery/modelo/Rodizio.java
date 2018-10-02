/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.sql.Time;
import java.util.UUID;

/**
 * @author SYSTEM
 */
public class Rodizio {

    @Ignore
    private UUID uuid, uuid_estabelecimento;
    private String nome, descricao;
    private double valor;
    private boolean ativo;
    private Time horaInicio;
    private boolean diasSemana[];
    @Ignore
    private transient Estabelecimento estabelecimento;


    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid_estabelecimento() {
        return uuid_estabelecimento;
    }

    public void setUuid_estabelecimento(UUID uuid_estabelecimento) {
        this.uuid_estabelecimento = uuid_estabelecimento;
    }

    public String getDescricao() {
        if (descricao == null) {
            return "";
        }
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public String getNome() {
        if (nome == null) {
            return "";
        }
        if (!nome.contains("Rodizio")) {
            return "Rodizio - " + nome; //To change body of generated methods, choose Tools | Templates.
        } else {
            return nome;
        }
    }

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

    public void setNome(String nome) {
        this.nome = nome;
    }

}
