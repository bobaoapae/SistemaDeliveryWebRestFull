package restFul.modelo;

import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.modelo.Estabelecimento;

import java.security.Principal;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Token implements Principal {

    private UUID uuid_estabelecimento, uuid_usuario;
    private String token;
    private Date validade;
    private Estabelecimento estabelecimento;
    private Usuario usuario;
    private SistemaDelivery sistemaDelivery;

    public SistemaDelivery getSistemaDelivery() {
        return sistemaDelivery;
    }

    public void setSistemaDelivery(SistemaDelivery sistemaDelivery) {
        this.sistemaDelivery = sistemaDelivery;
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public void setEstabelecimento(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
    }

    public UUID getUuid_usuario() {
        return uuid_usuario;
    }

    public void setUuid_usuario(UUID uuid_usuario) {
        this.uuid_usuario = uuid_usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public UUID getUuid_estabelecimento() {
        return uuid_estabelecimento;
    }

    public void setUuid_estabelecimento(UUID uuid_estabelecimento) {
        this.uuid_estabelecimento = uuid_estabelecimento;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getValidade() {
        return validade;
    }

    public void setValidade(Date validade) {
        this.validade = validade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token1 = (Token) o;
        return Objects.equals(token, token1.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }

    @Override
    public String getName() {
        return null;
    }
}
