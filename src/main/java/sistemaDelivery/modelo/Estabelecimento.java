package sistemaDelivery.modelo;

import utils.Ignorar;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

public class Estabelecimento {

    @Ignorar
    private UUID uuid;
    private String nomeEstabelecimento, nomeBot, numeroAviso, webHookNovoPedido, webHookNovaReserva, logo, endereco;
    private int tempoMedioRetirada, tempoMedioEntrega;
    private boolean openPedidos, openChatBot, reservas, reservasComPedidosFechados, abrirFecharPedidosAutomatico;
    private boolean agendamentoDePedidos, ativo, iniciarAutomaticamente;
    @Ignorar
    private Date horaAberturaPedidos;
    private Time horaInicioReservas;
    private double valorSelo;
    @Ignorar
    private transient List<Categoria> categorias;
    @Ignorar
    private transient List<Rodizio> rodizios;
    @Ignorar
    private List<TipoEntrega> tiposEntregas;
    private int maximoSeloPorCompra, validadeSeloFidelidade;
    private String timeZone;
    @Ignorar
    private Map<DayOfWeek, List<HorarioFuncionamento>> horariosFuncionamento;

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public void addHorarioFuncionamento(HorarioFuncionamento horarioFuncionamento) {
        List<HorarioFuncionamento> horarios = getHorariosFuncionamento(horarioFuncionamento.getDiaDaSemana());
        synchronized (horarios) {
            horarios.add(horarioFuncionamento);
        }
    }

    public List<HorarioFuncionamento> getHorariosFuncionamento(DayOfWeek dayOfWeek) {
        synchronized (horariosFuncionamento) {
            if (!horariosFuncionamento.containsKey(dayOfWeek)) {
                horariosFuncionamento.put(dayOfWeek, Collections.synchronizedList(new ArrayList<>()));
            }
            return horariosFuncionamento.get(dayOfWeek);
        }
    }

    public Map<DayOfWeek, List<HorarioFuncionamento>> getHorariosFuncionamento() {
        return horariosFuncionamento;
    }

    public void setHorariosFuncionamento(Map<DayOfWeek, List<HorarioFuncionamento>> horariosFuncionamento) {
        this.horariosFuncionamento = Collections.synchronizedMap(horariosFuncionamento);
    }

    public LocalTime getHoraAtual() {
        return LocalTime.now(getTimeZoneObject().toZoneId());
    }

    public LocalDateTime getDataComHoraAtual() {
        return LocalDateTime.now(getTimeZoneObject().toZoneId());
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZoneObject() {
        if (timeZone == null) {
            timeZone = TimeZone.getDefault().toZoneId().getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("pt-BR"));
        }
        return TimeZone.getTimeZone(timeZone);
    }

    public String getTiposEntregasConcatenados() {
        synchronized (getTiposEntregas()) {
            String formasRetiradas = "";
            for (TipoEntrega tipoEntrega : getTiposEntregas()) {
                formasRetiradas += tipoEntrega.getNome() + ", ";
            }
            formasRetiradas = formasRetiradas.trim().substring(0, formasRetiradas.lastIndexOf(","));
            if (formasRetiradas.contains(", ")) {
                formasRetiradas = formasRetiradas.substring(0, formasRetiradas.lastIndexOf(",")) + " ou" + formasRetiradas.substring(formasRetiradas.lastIndexOf(",") + 1);
            }
            return formasRetiradas;
        }
    }

    public List<TipoEntrega> getTiposEntregas() {
        return tiposEntregas;
    }

    public void setTiposEntregas(List<TipoEntrega> tiposEntregas) {
        this.tiposEntregas = Collections.synchronizedList(tiposEntregas);
    }

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

    public boolean isIniciarAutomaticamente() {
        return iniciarAutomaticamente;
    }

    public void setIniciarAutomaticamente(boolean iniciarAutomaticamente) {
        this.iniciarAutomaticamente = iniciarAutomaticamente;
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
            this.horaAberturaPedidos = Calendar.getInstance(getTimeZoneObject()).getTime();
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

    public boolean isTimeBeetwenHorarioFuncionamento(LocalTime horaInformada, DayOfWeek dayOfWeek) {
        List<HorarioFuncionamento> horarioFuncionamentos = getHorariosFuncionamento(dayOfWeek);
        synchronized (horarioFuncionamentos) {
            if (horarioFuncionamentos.isEmpty()) {
                return false;
            } else {
                for (HorarioFuncionamento horarioFuncionamento : horarioFuncionamentos) {
                    if (!horarioFuncionamento.isAtivo()) {
                        continue;
                    }
                    if (horaInformada.isAfter(horarioFuncionamento.getHoraAbrir().toLocalTime()) && horaInformada.isBefore(horarioFuncionamento.getHoraFechar().toLocalTime())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkTemHorarioFuncionamentoHoje() {
        return getHorariosFuncionamento(getDataComHoraAtual().getDayOfWeek()).size() > 0;
    }

    public HorarioFuncionamento nextOrCurrentHorarioAbertoOfDay() {
        List<HorarioFuncionamento> horarioFuncionamentos = getHorariosFuncionamento(getDataComHoraAtual().getDayOfWeek());
        synchronized (horarioFuncionamentos) {
            if (horarioFuncionamentos.isEmpty()) {
                return null;
            } else {
                for (HorarioFuncionamento horarioFuncionamento : horarioFuncionamentos) {
                    if (!horarioFuncionamento.isAtivo()) {
                        continue;
                    }
                    if ((getDataComHoraAtual().toLocalTime().isAfter(horarioFuncionamento.getHoraAbrir().toLocalTime()) || getDataComHoraAtual().toLocalTime().withSecond(0).withNano(0).equals(horarioFuncionamento.getHoraAbrir().toLocalTime())) && (getDataComHoraAtual().toLocalTime().isBefore(horarioFuncionamento.getHoraFechar().toLocalTime()) || getDataComHoraAtual().toLocalTime().withSecond(0).withNano(0).equals(horarioFuncionamento.getHoraFechar().toLocalTime()))) {
                        return horarioFuncionamento;
                    } else if (getDataComHoraAtual().toLocalTime().isBefore(horarioFuncionamento.getHoraAbrir().toLocalTime())) {
                        return horarioFuncionamento;
                    }
                }
            }
        }
        return null;
    }

    public boolean possuiEntrega() {
        synchronized (getTiposEntregas()) {
            for (TipoEntrega tipoEntrega : getTiposEntregas()) {
                if (tipoEntrega.isSolicitarEndereco()) {
                    return true;
                }
            }
            return false;
        }
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
