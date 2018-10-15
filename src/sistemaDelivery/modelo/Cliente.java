/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;
import sistemaDelivery.controle.ControlePedidos;
import sistemaDelivery.controle.ControleRecargas;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author jvbor
 */
public class Cliente {

    @Ignore
    private UUID uuid;
    private String nome, chatId, telefoneMovel, telefoneFixo;
    private java.sql.Date dataAniversario;
    private java.sql.Date dataCadastro;
    private Timestamp dataUltimaCompra;
    private boolean cadastroRealizado;
    private Endereco endereco;

    public Cliente(String chatId) {
        this();
        this.chatId = chatId;
    }

    public Cliente() {
        this.dataCadastro = new java.sql.Date(new Date().getTime());
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void realizarRecarga(Estabelecimento e, double valorRecarga, TipoRecarga tipoRecarga) {
        ControleRecargas.getInstance().salvarRecarga(new RecargaCliente(e, this, valorRecarga, tipoRecarga));
    }

    public List<RecargaCliente> getRegargas(Estabelecimento e) {
        return ControleRecargas.getInstance().getRecargasCliente(this, e);
    }


    public List<SeloFidelidade> getSelosFidelidade(Estabelecimento e) {
        return null;
    }

    public double getCreditosDisponiveis(Estabelecimento e) {
        double valor = 0;
        for (RecargaCliente recargaCliente : this.getRegargas(e)) {
            if (recargaCliente.getTipoRecarga() == TipoRecarga.DEPOSITO) {
                valor += recargaCliente.getValor();
            } else if (recargaCliente.getTipoRecarga() == TipoRecarga.SAQUE) {
                valor -= recargaCliente.getValor();
            }
        }
        return valor;

    }

    public String getTelefoneMovel() {
        if (telefoneMovel == null) {
            return "";
        }
        return telefoneMovel;
    }

    public void setTelefoneMovel(String telefoneMovel) {
        this.telefoneMovel = telefoneMovel;
    }

    public String getTelefoneFixo() {
        if (telefoneFixo == null) {
            return "";
        }
        return telefoneFixo;
    }

    public void setTelefoneFixo(String telefoneFixo) {
        this.telefoneFixo = telefoneFixo;
    }

    public boolean isCadastroRealizado() {
        return cadastroRealizado;
    }

    public void setCadastroRealizado(boolean cadastroRealizado) {
        this.cadastroRealizado = cadastroRealizado;
    }

    public java.sql.Date getDataAniversario() {
        return dataAniversario;
    }

    public void setDataAniversario(java.sql.Date dataAniversario) {
        this.dataAniversario = dataAniversario;
    }

    public String getNome() {
        if (nome == null) {
            return "";
        }
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getChatId() {
        if (chatId == null) {
            return "";
        }
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }


    public java.sql.Date getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(java.sql.Date dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Timestamp getDataUltimaCompra() {
        return dataUltimaCompra;
    }

    public void setDataUltimaCompra(Timestamp dataUltimaCompra) {
        this.dataUltimaCompra = dataUltimaCompra;
    }

    public List<Pedido> getPedidosCliente(Estabelecimento estabelecimento) {
        return ControlePedidos.getInstance().getPedidosCliente(this, estabelecimento);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(uuid, cliente.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
