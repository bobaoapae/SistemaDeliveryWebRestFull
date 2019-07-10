/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import adapters.ExposeGetter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import utils.Ignorar;
import utils.Utilitarios;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jvbor
 */
@ExposeGetter(methodName = "getAdicionaisGroupByGrupoJson", nameExpose = "adicionaisPorGrupo")
public class ItemPedido implements Comparable<ItemPedido> {

    @Ignorar
    private UUID uuid, uuid_pedido, uuid_produto;
    @Ignorar
    private Produto produto;
    private int qtd, qtdPago;
    private String comentario;
    @Ignorar
    private List<AdicionalProduto> adicionais;
    private boolean removido;
    private double subTotal, valorPago;
    @Ignorar
    private transient Pedido pedido;

    public ItemPedido() {
        adicionais = new ArrayList<>();
        comentario = "";
        qtd = 1;
    }

    public JsonArray getAdicionaisGroupByGrupoJson() {
        Gson gson = Utilitarios.getDefaultGsonBuilder(null).create();
        synchronized (adicionais) {
            Map<GrupoAdicional, List<AdicionalProduto>> retorno =
                    adicionais.stream().collect(Collectors.groupingBy(w -> w.getGrupoAdicional(), LinkedHashMap::new, Collectors.toList()));
            JsonArray array = new JsonArray();
            for (Map.Entry<GrupoAdicional, List<AdicionalProduto>> entry : retorno.entrySet()) {
                JsonObject object = new JsonObject();
                object.addProperty("nomeGrupo", entry.getKey().getNomeGrupo());
                object.add("adicionais", gson.toJsonTree(entry.getValue()));
                array.add(object);
            }
            return array;
        }
    }

    public Map<GrupoAdicional, List<AdicionalProduto>> getAdicionaisGroupByGrupo() {
        synchronized (adicionais) {
            Map<GrupoAdicional, List<AdicionalProduto>> retorno =
                    adicionais.stream().collect(Collectors.groupingBy(w -> w.getGrupoAdicional(), LinkedHashMap::new, Collectors.toList()));
            return retorno;
        }
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
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

    public void calcularValor() {
        if (produto == null) {
            return;
        }
        double subTotal = produto.getValor() * qtd;
        HashMap<GrupoAdicional, List<AdicionalProduto>> adicionalListHashMap = new HashMap<>();
        for (AdicionalProduto adicionalProduto : adicionais) {
            if (!adicionalListHashMap.containsKey(adicionalProduto.getGrupoAdicional())) {
                adicionalListHashMap.put(adicionalProduto.getGrupoAdicional(), getAdicionais(adicionalProduto.getGrupoAdicional()));
            }
        }
        for (Map.Entry<GrupoAdicional, List<AdicionalProduto>> entry : adicionalListHashMap.entrySet()) {
            if (entry.getKey().getFormaCobranca() == GrupoAdicional.FormaCobranca.SOMA) {
                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                    subTotal += adicionalProduto.getValor() * qtd;
                }
            } else if (entry.getKey().getFormaCobranca() == GrupoAdicional.FormaCobranca.MEDIA) {
                double temp = 0;
                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                    temp += adicionalProduto.getValor() * qtd;
                }
                temp /= entry.getValue().size();
                subTotal += temp;
            } else if (entry.getKey().getFormaCobranca() == GrupoAdicional.FormaCobranca.MAIOR_VALOR) {
                double maior = 0;
                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                    if (adicionalProduto.getValor() > maior) {
                        maior = adicionalProduto.getValor() * qtd;
                    }
                }
                subTotal += maior;
            }
        }
        this.valorPago = (subTotal * qtdPago);
        this.subTotal = subTotal;
    }

    public int getQtdPago() {
        return qtdPago;
    }

    public void setQtdPago(int qtdPago) {
        this.qtdPago = qtdPago;
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

    public double getValorPago() {
        return valorPago;
    }

    public void setValorPago(double valorPago) {
        this.valorPago = valorPago;
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
