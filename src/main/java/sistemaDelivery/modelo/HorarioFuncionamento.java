package sistemaDelivery.modelo;

import adapters.ExposeGetter;
import utils.Ignorar;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@ExposeGetter(methodName = "diaDaSemanaTraduzido", nameExpose = "diaDaSemanaTraduzido")
public class HorarioFuncionamento {

    @Ignorar
    private UUID uuid, uuid_estabelecimento;
    private DayOfWeek diaDaSemana;
    private LocalTime horaAbrir, horaFechar;
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

    public LocalTime getHoraAbrir() {
        return horaAbrir;
    }

    public void setHoraAbrir(LocalTime horaAbrir) {
        this.horaAbrir = horaAbrir;
    }

    public LocalTime getHoraFechar() {
        return horaFechar;
    }

    public void setHoraFechar(LocalTime horaFechar) {
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
