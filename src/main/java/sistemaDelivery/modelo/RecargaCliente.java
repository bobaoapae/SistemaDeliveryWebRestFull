/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * @author SYSTEM
 */
public class RecargaCliente {

    boolean ativo;
    @Ignore
    private UUID uuid, uuid_cliente, uuid_estabelecimento;
    private Date dataRecarga;
    @Ignore
    private transient Estabelecimento estabelecimento;
    @Ignore
    private transient Cliente cliente;
    private TipoRecarga tipoRecarga;
    private double valor;

    public RecargaCliente(Estabelecimento estabelecimento, Cliente cliente, double valor, TipoRecarga tipoRecarga) {
        this.valor = valor;
        this.estabelecimento = estabelecimento;
        this.cliente = cliente;
        dataRecarga = new Date();
        this.tipoRecarga = tipoRecarga;
    }

    public RecargaCliente() {
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Date getDataRecarga() {
        return dataRecarga;
    }

    public void setDataRecarga(Date dataRecarga) {
        this.dataRecarga = dataRecarga;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid_cliente() {
        return uuid_cliente;
    }

    public void setUuid_cliente(UUID uuid_cliente) {
        this.uuid_cliente = uuid_cliente;
    }

    public UUID getUuid_estabelecimento() {
        return uuid_estabelecimento;
    }

    public void setUuid_estabelecimento(UUID uuid_estabelecimento) {
        this.uuid_estabelecimento = uuid_estabelecimento;
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public TipoRecarga getTipoRecarga() {
        return tipoRecarga;
    }

    public void setTipoRecarga(TipoRecarga tipoRecarga) {
        this.tipoRecarga = tipoRecarga;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecargaCliente that = (RecargaCliente) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }


}
