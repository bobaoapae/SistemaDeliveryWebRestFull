/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import java.util.*;

/**
 * @author jvbor
 */
public class ItemPedido implements Comparable<ItemPedido> {

    private UUID uuid, uuid_pedido, uuid_produto;
    private Produto produto;
    private int qtd;
    private String comentario;
    private List<AdicionalProduto> adicionais;
    private boolean removido;
    private double subTotal;

    public ItemPedido() {
        adicionais = new ArrayList<>();
        comentario = "";
        qtd = 1;
    }

    public boolean isRemovido() {
        return removido;
    }

    public void setRemovido(boolean removido) {
        this.removido = removido;
    }

    public int getQtd() {
        return qtd;
    }

    public void setQtd(int qtd) {
        this.qtd = qtd;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public String getComentario() {
        if (comentario == null) {
            comentario = "";
        }
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public double calcularValor() {
        if (produto == null) {
            return 0;
        }
        double retorno = produto.getValor() * qtd;
        HashMap<GrupoAdicional, List<AdicionalProduto>> adicionalListHashMap = new HashMap<>();
        for (AdicionalProduto adicionalProduto : adicionais) {
            if (!adicionalListHashMap.containsKey(adicionalProduto.getGrupoAdicional())) {
                adicionalListHashMap.put(adicionalProduto.getGrupoAdicional(), getAdicionais(adicionalProduto.getGrupoAdicional()));
            }
        }
        for (Map.Entry<GrupoAdicional, List<AdicionalProduto>> entry : adicionalListHashMap.entrySet()) {
            if (entry.getKey().getFormaCobranca() == GrupoAdicional.FormaCobranca.SOMA) {
                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                    retorno += adicionalProduto.getValor() * qtd;
                }
            } else if (entry.getKey().getFormaCobranca() == GrupoAdicional.FormaCobranca.MEDIA) {
                double temp = 0;
                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                    temp += adicionalProduto.getValor() * qtd;
                }
                temp /= entry.getValue().size();
                retorno += temp;
            } else if (entry.getKey().getFormaCobranca() == GrupoAdicional.FormaCobranca.MAIOR_VALOR) {
                double maior = 0;
                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                    if (maior < adicionalProduto.getValor()) {
                        maior = adicionalProduto.getValor();
                    }
                }
                retorno += maior;
            }
        }
        subTotal = retorno;
        return retorno;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid_pedido() {
        return uuid_pedido;
    }

    public void setUuid_pedido(UUID uuid_pedido) {
        this.uuid_pedido = uuid_pedido;
    }

    public UUID getUuid_produto() {
        return uuid_produto;
    }

    public void setUuid_produto(UUID uuid_produto) {
        this.uuid_produto = uuid_produto;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public List<AdicionalProduto> getAdicionais() {
        return adicionais;
    }

    public void setAdicionais(List<AdicionalProduto> adicionais) {
        this.adicionais = adicionais;
    }

    public List<AdicionalProduto> getAdicionais(GrupoAdicional grupoAdicional) {
        List<AdicionalProduto> temp = new ArrayList<>();
        Iterator<AdicionalProduto> it = getAdicionais().stream().filter((o) -> o.getGrupoAdicional().equals(grupoAdicional)).iterator();
        while (it.hasNext()) {
            temp.add(it.next());
        }
        return temp;
    }

    public boolean addAdicional(AdicionalProduto ad) {
        int contTotalGrupo = getAdicionais(ad.getGrupoAdicional()).size();
        if (contTotalGrupo < ad.getGrupoAdicional().getQtdMax()) {
            getAdicionais().add(ad);
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(ItemPedido t) {
        Integer otherCategory = t.getProduto().sequenceNr();
        Integer thisCategory = getProduto().sequenceNr();
        return thisCategory.compareTo(otherCategory);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemPedido that = (ItemPedido) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
