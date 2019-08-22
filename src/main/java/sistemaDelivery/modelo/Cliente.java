/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import sistemaDelivery.controle.ControlePedidos;
import sistemaDelivery.controle.ControleRecargas;
import utils.Ignorar;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author jvbor
 */
public class Cliente {

    @Ignorar
    private UUID uuid, uuid_estabelecimento;
    private String nome, chatId, telefoneMovel, telefoneFixo;
    private LocalDate dataAniversario;
    private LocalDate dataCadastro;
    private LocalDateTime dataUltimaCompra;
    private boolean cadastroRealizado;
    private Endereco endereco;
    @Ignorar
    private transient Estabelecimento estabelecimento;

    public Cliente(String chatId, Estabelecimento estabelecimento) {
        this();
        this.chatId = chatId;
        this.estabelecimento = estabelecimento;
    }

    public Cliente() {
        this.dataCadastro = LocalDate.now();
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

    public void realizarRecarga(Estabelecimento e, double valorRecarga, TipoRecarga tipoRecarga) throws SQLException {
        ControleRecargas.getInstance().salvarRecarga(new RecargaCliente(e, this, valorRecarga, tipoRecarga));
    }

    public List<RecargaCliente> getRegargas() throws SQLException {
        return ControleRecargas.getInstance().getRecargasCliente(this);
    }

    public double getCreditosDisponiveis() throws SQLException {
        double valor = 0;
        for (RecargaCliente recargaCliente : this.getRegargas()) {
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

    public LocalDate getDataAniversario() {
        return dataAniversario;
    }

    public void setDataAniversario(LocalDate dataAniversario) {
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


    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public LocalDateTime getDataUltimaCompra() {
        return dataUltimaCompra;
    }

    public void setDataUltimaCompra(LocalDateTime dataUltimaCompra) {
        this.dataUltimaCompra = dataUltimaCompra;
    }

    public List<Pedido> getPedidosCliente() throws SQLException {
        return ControlePedidos.getInstance().getPedidosCliente(this);
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
