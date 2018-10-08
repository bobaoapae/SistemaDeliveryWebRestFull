package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.util.*;

public class Categoria {

    @Ignore
    private UUID uuid, uuid_estabelecimento, uuid_categoria_pai;
    private String nomeCategoria, exemplosComentarioPedido;
    private int qtdMinEntrega, ordemExibicao;
    private boolean fazEntrega, precisaPedirOutraCategoria, entregaGratis, ativo, visivel;
    @Ignore
    private List<Categoria> categoriasFilhas;
    @Ignore
    private List<Produto> produtos;
    @Ignore
    private transient Categoria categoriaPai;
    private RestricaoVisibilidade restricaoVisibilidade;
    @Ignore
    private List<GrupoAdicional> gruposAdicionais;
    @Ignore
    private transient Estabelecimento estabelecimento;
    @Ignore
    private List<Categoria> categoriasNecessarias;
    @Ignore
    private List<UUID> uuidsCategoriasNecessarias;

    public List<UUID> getUuidsCategoriasNecessarias() {
        return uuidsCategoriasNecessarias;
    }

    public void setUuidsCategoriasNecessarias(List<UUID> uuidsCategoriasNecessarias) {
        this.uuidsCategoriasNecessarias = uuidsCategoriasNecessarias;
    }

    public boolean isVisivel() {
        return visivel;
    }

    public void setVisivel(boolean visivel) {
        this.visivel = visivel;
    }

    public List<Categoria> getCategoriasNecessarias() {
        if (categoriasNecessarias == null) {
            setCategoriasNecessarias(new ArrayList<>());
        }
        return categoriasNecessarias;
    }

    public void setCategoriasNecessarias(List<Categoria> categoriasNecessarias) {
        this.categoriasNecessarias = Collections.synchronizedList(categoriasNecessarias);
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public List<GrupoAdicional> getGruposAdicionais() {
        return gruposAdicionais;
    }

    public void setGruposAdicionais(List<GrupoAdicional> gruposAdicionais) {
        this.gruposAdicionais = Collections.synchronizedList(gruposAdicionais);
    }

    public RestricaoVisibilidade getRestricaoVisibilidade() {
        return restricaoVisibilidade;
    }

    public void setRestricaoVisibilidade(RestricaoVisibilidade restricaoVisibilidade) {
        this.restricaoVisibilidade = restricaoVisibilidade;
    }

    public List<Produto> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<Produto> produtos) {
        this.produtos = Collections.synchronizedList(produtos);
    }

    public Categoria getRootCategoria() {
        if (this.categoriaPai == null) {
            return this;
        } else {
            Categoria catAtual = this;
            while (true) {
                if (catAtual.getCategoriaPai() != null) {
                    catAtual = catAtual.getCategoriaPai();
                } else {
                    return catAtual;
                }
            }
        }
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

    public UUID getUuid_categoria_pai() {
        return uuid_categoria_pai;
    }

    public void setUuid_categoria_pai(UUID uuid_categoria_pai) {
        this.uuid_categoria_pai = uuid_categoria_pai;
    }

    public String getNomeCategoria() {
        return nomeCategoria;
    }

    public void setNomeCategoria(String nomeCategoria) {
        this.nomeCategoria = nomeCategoria;
    }

    public String getExemplosComentarioPedido() {
        return exemplosComentarioPedido;
    }

    public void setExemplosComentarioPedido(String exemplosComentarioPedido) {
        this.exemplosComentarioPedido = exemplosComentarioPedido;
    }

    public int getQtdMinEntrega() {
        return qtdMinEntrega;
    }

    public void setQtdMinEntrega(int qtdMinEntrega) {
        this.qtdMinEntrega = qtdMinEntrega;
    }

    public int getOrdemExibicao() {
        return ordemExibicao;
    }

    public void setOrdemExibicao(int ordemExibicao) {
        this.ordemExibicao = ordemExibicao;
    }

    public boolean isFazEntrega() {
        return fazEntrega;
    }

    public void setFazEntrega(boolean fazEntrega) {
        this.fazEntrega = fazEntrega;
    }

    public boolean isPrecisaPedirOutraCategoria() {
        return precisaPedirOutraCategoria;
    }

    public void setPrecisaPedirOutraCategoria(boolean precisaPedirOutraCategoria) {
        this.precisaPedirOutraCategoria = precisaPedirOutraCategoria;
    }

    public boolean isEntregaGratis() {
        return entregaGratis;
    }

    public void setEntregaGratis(boolean entregaGratis) {
        this.entregaGratis = entregaGratis;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<Categoria> getCategoriasFilhas() {
        return categoriasFilhas;
    }

    public void setCategoriasFilhas(List<Categoria> categoriasFilhas) {
        this.categoriasFilhas = Collections.synchronizedList(categoriasFilhas);
    }

    public Categoria getCategoriaPai() {
        return categoriaPai;
    }

    public void setCategoriaPai(Categoria categoriaPai) {
        this.categoriaPai = categoriaPai;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categoria categoria = (Categoria) o;
        return Objects.equals(uuid, categoria.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
