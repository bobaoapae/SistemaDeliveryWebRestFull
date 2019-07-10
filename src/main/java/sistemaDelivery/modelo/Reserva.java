package sistemaDelivery.modelo;

import utils.Ignorar;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

public class Reserva {

    @Ignorar
    private UUID uuid, uuid_estabelecimento, uuid_cliente;
    private String telefoneContato, nomeContato, comentario;
    private Timestamp dataReserva;
    private int qtdPessoas;
    private long cod;
    private boolean impresso;
    @Ignorar
    private transient Estabelecimento estabelecimento;
    @Ignorar
    private transient Cliente cliente;

    public Reserva() {
        impresso = false;
    }

    public long getCod() {
        return cod;
    }

    public void setCod(long cod) {
        this.cod = cod;
    }

    public String getTelefoneContato() {
        if (telefoneContato == null) {
            return "";
        }
        return telefoneContato;
    }

    public void setTelefoneContato(String telefoneContato) {
        this.telefoneContato = telefoneContato;
    }

    public String getNomeContato() {
        if (nomeContato == null) {
            return "";
        }
        return nomeContato;
    }

    public void setNomeContato(String nomeContato) {
        this.nomeContato = nomeContato;
    }

    public String getComentario() {
        if (comentario == null) {
            return "";
        }
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
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

    public boolean isImpresso() {
        return impresso;
    }

    public void setImpresso(boolean impresso) {
        this.impresso = impresso;
    }

    public Timestamp getDataReserva() {
        return dataReserva;
    }

    public void setDataReserva(Timestamp dataReserva) {
        this.dataReserva = dataReserva;
    }

    public int getQtdPessoas() {
        return qtdPessoas;
    }

    public void setQtdPessoas(int qtdPessoas) {
        this.qtdPessoas = qtdPessoas;
    }

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

    public UUID getUuid_cliente() {
        return uuid_cliente;
    }

    public void setUuid_cliente(UUID uuid_cliente) {
        this.uuid_cliente = uuid_cliente;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reserva reserva = (Reserva) o;
        return Objects.equals(uuid, reserva.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
