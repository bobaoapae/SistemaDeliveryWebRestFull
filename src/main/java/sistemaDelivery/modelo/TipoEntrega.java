package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.Objects;
import java.util.UUID;

public class TipoEntrega {
    @Ignore
    private UUID uuid, uuid_estabelecimento;
    @Ignore
    private transient Estabelecimento estabelecimento;
    private String nome;
    private boolean solicitarEndereco;
    private double valor;

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

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isSolicitarEndereco() {
        return solicitarEndereco;
    }

    public void setSolicitarEndereco(boolean solicitarEndereco) {
        this.solicitarEndereco = solicitarEndereco;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TipoEntrega that = (TipoEntrega) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
