package sistemaDelivery.modelo;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.sql.Time;
import java.time.LocalTime;
import java.util.*;

public class Estabelecimento {

    @Ignore
    private UUID uuid;
    private String nomeEstabelecimento, nomeBot, numeroAviso, webHookNovoPedido, webHookNovaReserva, logo;
    private int tempoMedioRetirada, tempoMedioEntrega;
    private boolean openPedidos, openChatBot, reservas, reservasComPedidosFechados, abrirFecharPedidosAutomatico;
    private boolean agendamentoDePedidos, ativo;
    @Ignore
    private Date horaAberturaPedidos;
    private Time horaAutomaticaFecharPedidos, horaAutomaticaAbrirPedidos, horaInicioReservas;
    private double taxaEntregaFixa, taxaEntregaKm, valorSelo;
    @Ignore
    private transient List<Categoria> categorias;
    @Ignore
    private transient List<Rodizio> rodizios;
    private int maximoSeloPorCompra, validadeSeloFidelidade;

    public List<Rodizio> getRodizios() {
        return rodizios;
    }

    public void setRodizios(List<Rodizio> rodizios) {
        this.rodizios = Collections.synchronizedList(rodizios);
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getNomeEstabelecimento() {
        if (nomeEstabelecimento == null) {
            return "";
        }
        return nomeEstabelecimento;
    }

    public void setNomeEstabelecimento(String nomeEstabelecimento) {
        this.nomeEstabelecimento = nomeEstabelecimento;
    }

    public String getNomeBot() {
        if (nomeBot == null) {
            return "";
        }
        return nomeBot;
    }

    public void setNomeBot(String nomeBot) {
        this.nomeBot = nomeBot;
    }

    public String getNumeroAviso() {
        if (numeroAviso == null) {
            return "";
        }
        return numeroAviso;
    }

    public void setNumeroAviso(String numeroAviso) {
        this.numeroAviso = numeroAviso;
    }

    public String getWebHookNovoPedido() {
        if (webHookNovoPedido == null) {
            return "";
        }
        return webHookNovoPedido;
    }

    public void setWebHookNovoPedido(String webHookNovoPedido) {
        this.webHookNovoPedido = webHookNovoPedido;
    }

    public String getWebHookNovaReserva() {
        if (webHookNovaReserva == null) {
            return "";
        }
        return webHookNovaReserva;
    }

    public void setWebHookNovaReserva(String webHookNovaReserva) {
        this.webHookNovaReserva = webHookNovaReserva;
    }

    public String getLogo() {
        if (logo == null) {
            return "";
        }
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public List<Categoria> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<Categoria> categorias) {
        this.categorias = Collections.synchronizedList(categorias);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getTempoMedioRetirada() {
        return tempoMedioRetirada;
    }

    public void setTempoMedioRetirada(int tempoMedioRetirada) {
        this.tempoMedioRetirada = tempoMedioRetirada;
    }

    public int getTempoMedioEntrega() {
        return tempoMedioEntrega;
    }

    public void setTempoMedioEntrega(int tempoMedioEntrega) {
        this.tempoMedioEntrega = tempoMedioEntrega;
    }

    public boolean isOpenPedidos() {
        return openPedidos;
    }

    public void setOpenPedidos(boolean openPedidos) {
        if (openPedidos && !this.openPedidos) {
            this.horaAberturaPedidos = new Date();
        }
        this.openPedidos = openPedidos;
    }

    public boolean isOpenChatBot() {
        return openChatBot;
    }

    public void setOpenChatBot(boolean openChatBot) {
        this.openChatBot = openChatBot;
    }

    public boolean isReservas() {
        return reservas;
    }

    public void setReservas(boolean reservas) {
        this.reservas = reservas;
    }

    public boolean isReservasComPedidosFechados() {
        return reservasComPedidosFechados;
    }

    public void setReservasComPedidosFechados(boolean reservasComPedidosFechados) {
        this.reservasComPedidosFechados = reservasComPedidosFechados;
    }

    public boolean isAbrirFecharPedidosAutomatico() {
        return abrirFecharPedidosAutomatico;
    }

    public void setAbrirFecharPedidosAutomatico(boolean abrirFecharPedidosAutomatico) {
        this.abrirFecharPedidosAutomatico = abrirFecharPedidosAutomatico;
    }

    public boolean isAgendamentoDePedidos() {
        return agendamentoDePedidos;
    }

    public void setAgendamentoDePedidos(boolean agendamentoDePedidos) {
        this.agendamentoDePedidos = agendamentoDePedidos;
    }

    public Time getHoraAutomaticaFecharPedidos() {
        return horaAutomaticaFecharPedidos;
    }

    public void setHoraAutomaticaFecharPedidos(Time horaAutomaticaFecharPedidos) {
        this.horaAutomaticaFecharPedidos = horaAutomaticaFecharPedidos;
    }

    public Time getHoraAutomaticaAbrirPedidos() {
        return horaAutomaticaAbrirPedidos;
    }

    public void setHoraAutomaticaAbrirPedidos(Time horaAutomaticaAbrirPedidos) {
        this.horaAutomaticaAbrirPedidos = horaAutomaticaAbrirPedidos;
    }

    public Time getHoraInicioReservas() {
        return horaInicioReservas;
    }

    public void setHoraInicioReservas(Time horaInicioReservas) {
        this.horaInicioReservas = horaInicioReservas;
    }

    public double getValorSelo() {
        return valorSelo;
    }

    public void setValorSelo(double valorSelo) {
        this.valorSelo = valorSelo;
    }

    public int getMaximoSeloPorCompra() {
        return maximoSeloPorCompra;
    }

    public void setMaximoSeloPorCompra(int maximoSeloPorCompra) {
        this.maximoSeloPorCompra = maximoSeloPorCompra;
    }

    public int getValidadeSeloFidelidade() {
        return validadeSeloFidelidade;
    }

    public void setValidadeSeloFidelidade(int validadeSeloFidelidade) {
        this.validadeSeloFidelidade = validadeSeloFidelidade;
    }

    public Date getHoraAberturaPedidos() {
        return horaAberturaPedidos;
    }

    public void setHoraAberturaPedidos(Date horaAberturaPedidos) {
        this.horaAberturaPedidos = horaAberturaPedidos;
    }

    public double getTaxaEntregaFixa() {
        return taxaEntregaFixa;
    }

    public void setTaxaEntregaFixa(double taxaEntregaFixa) {
        this.taxaEntregaFixa = taxaEntregaFixa;
    }

    public double getTaxaEntregaKm() {
        return taxaEntregaKm;
    }

    public void setTaxaEntregaKm(double taxaEntregaKm) {
        this.taxaEntregaKm = taxaEntregaKm;
    }

    public boolean isTimeBeetwenHorarioFuncionamento(LocalTime horaInformada) {
        if (this.getHoraAutomaticaAbrirPedidos().toLocalTime().isAfter(this.getHoraAutomaticaFecharPedidos().toLocalTime())) {
            if (!(horaInformada.isBefore(this.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaInformada.isAfter(this.getHoraAutomaticaFecharPedidos().toLocalTime()))) {
                return true;
            }
        } else {
            if (horaInformada.isAfter(this.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaInformada.isBefore(this.getHoraAutomaticaFecharPedidos().toLocalTime())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Estabelecimento that = (Estabelecimento) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
