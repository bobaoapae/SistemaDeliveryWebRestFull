package sistemaDelivery.modelo;

import utils.Ignorar;

import java.sql.Time;
import java.util.Objects;
import java.util.UUID;

public class RestricaoVisibilidade {

    @Ignorar
    private UUID uuid, uuid_categoria, uuid_produto;
    private Time horarioDe, horarioAte;
    private boolean restricaoHorario, restricaoDia;
    private boolean[] diasSemana;
    @Ignorar
    private transient Produto produto;
    @Ignorar
    private transient Categoria categoria;

    public RestricaoVisibilidade(Produto produto) {
        diasSemana = new boolean[]{false, false, false, false, false, false, false};
        this.produto = produto;
    }

    public RestricaoVisibilidade(Categoria categoria) {
        diasSemana = new boolean[]{false, false, false, false, false, false, false};
        this.categoria = categoria;
    }

    public RestricaoVisibilidade() {
        diasSemana = new boolean[]{false, false, false, false, false, false, false};
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
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

    public boolean[] getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(boolean[] diasSemana) {
        this.diasSemana = diasSemana;
    }

    public Time getHorarioDe() {
        return horarioDe;
    }

    public void setHorarioDe(Time horarioDe) {
        this.horarioDe = horarioDe;
    }

    public Time getHorarioAte() {
        return horarioAte;
    }

    public void setHorarioAte(Time horarioAte) {
        this.horarioAte = horarioAte;
    }

    public boolean isRestricaoHorario() {
        return restricaoHorario;
    }

    public void setRestricaoHorario(boolean restricaoHorario) {
        this.restricaoHorario = restricaoHorario;
    }

    public boolean isRestricaoDia() {
        return restricaoDia;
    }

    public void setRestricaoDia(boolean restricaoDia) {
        this.restricaoDia = restricaoDia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestricaoVisibilidade that = (RestricaoVisibilidade) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
