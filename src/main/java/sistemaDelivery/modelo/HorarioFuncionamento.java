package sistemaDelivery.modelo;

import adapters.ExposeGetter;
import utils.Ignorar;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@ExposeGetter(methodName = "diaDaSemanaTraduzido", nameExpose = "diaDaSemanaTraduzido")
public class HorarioFuncionamento {

    @Ignorar
    private UUID uuid, uuid_estabelecimento;
    private DayOfWeek diaDaSemana;
    private Time horaAbrir, horaFechar;
    private boolean ativo;
    @Ignorar
    private transient Estabelecimento estabelecimento;

    public String diaDaSemanaTraduzido() {
        return diaDaSemana.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("pt-BR"));
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

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public DayOfWeek getDiaDaSemana() {
        return diaDaSemana;
    }

    public void setDiaDaSemana(DayOfWeek diaDaSemana) {
        this.diaDaSemana = diaDaSemana;
    }

    public Time getHoraAbrir() {
        return horaAbrir;
    }

    public void setHoraAbrir(Time horaAbrir) {
        this.horaAbrir = horaAbrir;
    }

    public Time getHoraFechar() {
        return horaFechar;
    }

    public void setHoraFechar(Time horaFechar) {
        this.horaFechar = horaFechar;
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
        HorarioFuncionamento that = (HorarioFuncionamento) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
