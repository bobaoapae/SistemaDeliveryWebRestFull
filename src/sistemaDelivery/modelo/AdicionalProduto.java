package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.Objects;
import java.util.UUID;

public class AdicionalProduto {

    @Ignore
    private UUID uuid, uuid_grupo_adicional;
    private String nome, descricao;
    private double valor;
    private boolean ativo;
    @Ignore
    private transient GrupoAdicional grupoAdicional;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid_grupo_adicional() {
        return uuid_grupo_adicional;
    }

    public void setUuid_grupo_adicional(UUID uuid_grupo_adicional) {
        this.uuid_grupo_adicional = uuid_grupo_adicional;
    }

    public GrupoAdicional getGrupoAdicional() {
        return grupoAdicional;
    }

    public void setGrupoAdicional(GrupoAdicional grupoAdicional) {
        this.grupoAdicional = grupoAdicional;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdicionalProduto that = (AdicionalProduto) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
