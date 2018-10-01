package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.*;

public class GrupoAdicional {

    @Ignore
    private UUID uuid, uuid_categoria, uuid_produto;
    private String nomeGrupo, descricaoGrupo;
    private int qtdMin, qtdMax;
    private boolean ativo;
    @Ignore
    private transient Categoria categoria;
    @Ignore
    private transient Produto produto;
    private FormaCobranca formaCobranca;
    @Ignore
    private List<AdicionalProduto> adicionais;

    public List<AdicionalProduto> getAdicionais() {
        if (adicionais == null) {
            setAdicionais(new ArrayList<>());
        }
        return adicionais;
    }

    public void setAdicionais(List<AdicionalProduto> adicionais) {
        this.adicionais = Collections.synchronizedList(adicionais);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid_categoria() {
        return uuid_categoria;
    }

    public void setUuid_categoria(UUID uuid_categoria) {
        this.uuid_categoria = uuid_categoria;
    }

    public UUID getUuid_produto() {
        return uuid_produto;
    }

    public void setUuid_produto(UUID uuid_produto) {
        this.uuid_produto = uuid_produto;
    }

    public String getNomeGrupo() {
        if (nomeGrupo == null) {
            return "";
        }
        return nomeGrupo;
    }

    public void setNomeGrupo(String nomeGrupo) {
        this.nomeGrupo = nomeGrupo;
    }

    public String getDescricaoGrupo() {
        if (descricaoGrupo == null) {
            return "";
        }
        return descricaoGrupo;
    }

    public void setDescricaoGrupo(String descricaoGrupo) {
        this.descricaoGrupo = descricaoGrupo;
    }

    public int getQtdMin() {
        return qtdMin;
    }

    public void setQtdMin(int qtdMin) {
        this.qtdMin = qtdMin;
    }

    public int getQtdMax() {
        return qtdMax;
    }

    public void setQtdMax(int qtdMax) {
        this.qtdMax = qtdMax;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public FormaCobranca getFormaCobranca() {
        return formaCobranca;
    }

    public void setFormaCobranca(FormaCobranca formaCobranca) {
        this.formaCobranca = formaCobranca;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrupoAdicional that = (GrupoAdicional) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public enum FormaCobranca {
        SOMA, MEDIA, MAIOR_VALOR
    }
}
