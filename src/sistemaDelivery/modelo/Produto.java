package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.*;

public class Produto implements Comparable<Produto> {

    @Ignore
    private UUID uuid, uuid_categoria;
    @Ignore
    private transient Categoria categoria;
    private String nome, descricao, foto;
    private double valor;
    private boolean onlyLocal, ativo, visivel;
    private RestricaoVisibilidade restricaoVisibilidade;
    @Ignore
    private List<GrupoAdicional> gruposAdicionais;

    public String getFoto() {
        if (foto == null) {
            return "";
        }
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public boolean isVisivel() {
        return visivel;
    }

    public void setVisivel(boolean visivel) {
        this.visivel = visivel;
    }

    public List<GrupoAdicional> getGruposAdicionais() {
        return gruposAdicionais;
    }

    public void setGruposAdicionais(List<GrupoAdicional> gruposAdicionais) {
        this.gruposAdicionais = Collections.synchronizedList(gruposAdicionais);
    }

    public List<GrupoAdicional> getAllGruposAdicionais() {
        List<GrupoAdicional> adicionais = new ArrayList<>();
        List<Categoria> categorias = new ArrayList<>();
        Categoria catAtual = this.getCategoria();
        while (true) {
            categorias.add(catAtual);
            if (catAtual.getCategoriaPai() != null) {
                catAtual = catAtual.getCategoriaPai();
            } else {
                break;
            }
        }
        Collections.reverse(categorias);
        for (Categoria c : categorias) {
            adicionais.addAll(c.getGruposAdicionais());
        }
        adicionais.addAll(this.getGruposAdicionais());
        return adicionais;
    }

    public RestricaoVisibilidade getRestricaoVisibilidade() {
        return restricaoVisibilidade;
    }

    public void setRestricaoVisibilidade(RestricaoVisibilidade restricaoVisibilidade) {
        this.restricaoVisibilidade = restricaoVisibilidade;
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

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
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

    public boolean isOnlyLocal() {
        return onlyLocal;
    }

    public void setOnlyLocal(boolean onlyLocal) {
        this.onlyLocal = onlyLocal;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public int sequenceNr() {
        if (this.getCategoria() != null) {
            return this.getCategoria().getOrdemExibicao();
        } else {
            return 999999999;
        }
    }

    @Override
    public int compareTo(Produto t) {
        return Integer.compare(this.sequenceNr(), t.sequenceNr());
    }

    public String getNomeWithCategories() {
        List<Categoria> categorias = new ArrayList<>();
        Categoria catAtual = this.getCategoria();
        while (true) {
            categorias.add(catAtual);
            if (catAtual.getCategoriaPai() != null) {
                catAtual = catAtual.getCategoriaPai();
            } else {
                break;
            }
        }
        Collections.reverse(categorias);
        String cats = "";
        for (Categoria c : categorias) {
            cats += c.getNomeCategoria() + " - ";
        }
        return cats + this.getNome();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Produto produto = (Produto) o;
        return Objects.equals(uuid, produto.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
