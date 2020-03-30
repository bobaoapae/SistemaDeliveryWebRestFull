/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import org.apache.commons.collections4.CollectionUtils;
import utils.Ignorar;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * @author jvbor
 */
public class Pedido {

    @Ignorar
    private UUID uuid, uuid_estabelecimento, uuid_cliente, uuid_tipoEntrega, uuid_horarioFuncionamentoPedido;
    private String nomeCliente;
    private boolean entrega, cartao, impresso;
    private double troco, desconto, pgCreditos, subTotal, taxaEntrega, total, valorPago, totalRemovido;
    private int numeroMesa;
    private String comentarioPedido;
    private LocalDateTime dataPedido;
    private EstadoPedido estadoPedido;
    private String celular, fixo;
    private LocalTime horaAgendamento;
    private long cod;
    @Ignorar
    private List<ItemPedido> produtos;
    @Ignorar
    private Cliente cliente;
    @Ignorar
    private transient Estabelecimento estabelecimento;
    private Endereco endereco;
    @Ignorar
    private TipoEntrega tipoEntrega;
    @Ignorar
    private HorarioFuncionamento horarioFuncionamentoPedido;

    public Pedido(Cliente cliente, Estabelecimento estabelecimento) {
        produtos = Collections.synchronizedList(new ArrayList<>());
        dataPedido = estabelecimento.getDataComHoraAtual();
        horarioFuncionamentoPedido = estabelecimento.nextOrCurrentHorarioAbertoOfDay();
        estadoPedido = EstadoPedido.Novo;
        celular = "";
        fixo = "";
        comentarioPedido = "";
        this.estabelecimento = estabelecimento;
        this.cliente = cliente;
    }

    public Pedido() {
    }

    public UUID getUuid_tipoEntrega() {
        return uuid_tipoEntrega;
    }

    public void setUuid_tipoEntrega(UUID uuid_tipoEntrega) {
        this.uuid_tipoEntrega = uuid_tipoEntrega;
    }

    public TipoEntrega getTipoEntrega() {
        return tipoEntrega;
    }

    public void setTipoEntrega(TipoEntrega tipoEntrega) {
        this.tipoEntrega = tipoEntrega;
    }

    public long getCod() {
        return cod;
    }

    public void setCod(long cod) {
        this.cod = cod;
    }

    public double getValorPago() {
        return valorPago;
    }

    public void setValorPago(double valorPago) {
        this.valorPago = valorPago;
    }

    public double getTotalRemovido() {
        return totalRemovido;
    }

    public void setTotalRemovido(double totalRemovido) {
        this.totalRemovido = totalRemovido;
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

    public double getTaxaEntrega() {
        return taxaEntrega;
    }

    public void setTaxaEntrega(double taxaEntrega) {
        this.taxaEntrega = taxaEntrega;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public String getNomeCliente() {
        if (nomeCliente == null) {
            return "";
        }
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public boolean isEntrega() {
        return entrega;
    }

    public void setEntrega(boolean entrega) {
        this.entrega = entrega;
    }

    public boolean isCartao() {
        return cartao;
    }

    public void setCartao(boolean cartao) {
        this.cartao = cartao;
    }

    public boolean isImpresso() {
        return impresso;
    }

    public void setImpresso(boolean impresso) {
        this.impresso = impresso;
    }

    public double getTroco() {
        return troco;
    }

    public void setTroco(double troco) {
        this.troco = troco;
    }

    public double getDesconto() {
        return desconto;
    }

    public void setDesconto(double desconto) {
        this.desconto = desconto;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public int getNumeroMesa() {
        return numeroMesa;
    }

    public void setNumeroMesa(int numeroMesa) {
        this.numeroMesa = numeroMesa;
    }

    public String getComentarioPedido() {
        if (comentarioPedido == null) {
            return "";
        }
        return comentarioPedido;
    }

    public void setComentarioPedido(String comentarioPedido) {
        this.comentarioPedido = comentarioPedido;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDateTime dataPedido) {
        this.dataPedido = dataPedido;
    }

    public EstadoPedido getEstadoPedido() {
        return estadoPedido;
    }

    public void setEstadoPedido(EstadoPedido estadoPedido) {
        this.estadoPedido = estadoPedido;
    }

    public String getCelular() {
        if (celular == null) {
            return "";
        }
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getFixo() {
        if (fixo == null) {
            return "";
        }
        return fixo;
    }

    public void setFixo(String fixo) {
        this.fixo = fixo;
    }

    public LocalTime getHoraAgendamento() {
        return horaAgendamento;
    }

    public void setHoraAgendamento(LocalTime horaAgendamento) {
        this.horaAgendamento = horaAgendamento;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    /*public void atualizarDesconto() {
        HashMap<Produto, Integer> hashMap = new HashMap<>();
        desconto = 0;
        synchronized (this.getProdutos()) {
            for (ItemPedido iitem : Collections.unmodifiableList(this.getProdutos())) {
                ArrayList<Promocao> promocoes = ControlePromocoes.getInstance(Db4oGenerico.getInstance("banco")).promocoesProduto(iitem.getProduto());
                for (Promocao promo : promocoes) {
                    for (CategoriaPromocao catPro : promo.getCategoriasPromocao()) {
                        if (catPro == CategoriaPromocao.POR_QUANTIDADE) {
                            if (!hashMap.containsKey(iitem.getProduto())) {
                                hashMap.put(iitem.getProduto(), iitem.getQtd());
                            } else {
                                hashMap.put(iitem.getProduto(), hashMap.get(iitem.getProduto()) + iitem.getQtd());
                            }
                            while (hashMap.get(iitem.getProduto()) >= promo.getQtd()) {
                                desconto += promo.getValor();
                                hashMap.put(iitem.getProduto(), hashMap.get(iitem.getProduto()) - promo.getQtd());
                            }
                        }
                    }
                }
            }
        }
    }*/

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public void calcularValor() {
        double subTotal = 0;
        double totalRemovido = 0;
        double valorPago = 0;
        synchronized (getProdutos()) {
            for (ItemPedido p : getProdutos()) {
                p.calcularValor();
                if (p.isRemovido()) {
                    totalRemovido += p.getSubTotal();
                } else {
                    valorPago += p.getValorPago();
                }
                subTotal += p.getSubTotal();
            }
        }
        if (this.tipoEntrega != null && this.tipoEntrega.getValor() > 0) {
            boolean cobrarTaxa = true;
            for (ItemPedido itemPedido : this.getProdutos()) {
                if (itemPedido.getProduto().isEntregaGratis() || itemPedido.getProduto().getCategoria().getRootCategoria().isEntregaGratis()) {
                    cobrarTaxa = false;
                    break;
                }
            }
            if (cobrarTaxa) {
                this.taxaEntrega = this.tipoEntrega.getValor();
            }
        }
        this.totalRemovido = totalRemovido;
        this.subTotal = subTotal;
        this.valorPago = valorPago;
        this.total = subTotal + taxaEntrega - desconto - pgCreditos - totalRemovido;
    }

    public void addItemPedido(ItemPedido item) {
        synchronized (getProdutos()) {
            Iterator<ItemPedido> iguais = getProdutos().stream().filter(o -> o.getProduto().equals(item.getProduto()) && item.getComentario().equals(((ItemPedido) o).getComentario()) && CollectionUtils.isEqualCollection(((ItemPedido) o).getAdicionais(), item.getAdicionais())).iterator();
            if (iguais.hasNext()) {
                ItemPedido itemBase = iguais.next();
                itemBase.setQtd(itemBase.getQtd() + item.getQtd());
            } else {
                getProdutos().add(item);
            }
        }
        //atualizarDesconto();
    }

    public double getPgCreditos() {
        return pgCreditos;
    }

    public void setPgCreditos(double pgCreditos) {
        this.pgCreditos = pgCreditos;
    }

    public List<ItemPedido> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<ItemPedido> produtos) {
        this.produtos = Collections.synchronizedList(produtos);
    }

    public List<ItemPedido> getProdutos(Categoria c) {
        synchronized (produtos) {
            List<ItemPedido> lista = new ArrayList<>();
            for (ItemPedido i : getProdutos()) {
                if (i.getProduto().getCategoria().equals(c) || i.getProduto().getCategoria().getRootCategoria().equals(c)) {
                    lista.add(i);
                }
            }
            return lista;
        }
    }

    public HorarioFuncionamento getHorarioFuncionamentoPedido() {
        return horarioFuncionamentoPedido;
    }

    public void setHorarioFuncionamentoPedido(HorarioFuncionamento horarioFuncionamentoPedido) {
        this.horarioFuncionamentoPedido = horarioFuncionamentoPedido;
    }

    public UUID getUuid_horarioFuncionamentoPedido() {
        return uuid_horarioFuncionamentoPedido;
    }

    public void setUuid_horarioFuncionamentoPedido(UUID uuid_horarioFuncionamentoPedido) {
        this.uuid_horarioFuncionamentoPedido = uuid_horarioFuncionamentoPedido;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(uuid, pedido.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
