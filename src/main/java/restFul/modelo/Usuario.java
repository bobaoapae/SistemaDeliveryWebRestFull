package restFul.modelo;


import sistemaDelivery.modelo.Estabelecimento;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class Usuario {
    private UUID uuid, uuid_usuario_indicacao;
    private String usuario, senha;
    private TipoUsuario tipoUsuario;
    private LocalDate dataRegistro;
    private boolean ativo;
    private List<Estabelecimento> estabelecimentos;
    private int maxEstabelecimentos;

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public int getMaxEstabelecimentos() {
        return maxEstabelecimentos;
    }

    public void setMaxEstabelecimentos(int maxEstabelecimentos) {
        this.maxEstabelecimentos = maxEstabelecimentos;
    }

    public List<Estabelecimento> getEstabelecimentos() {
        return estabelecimentos;
    }

    public void setEstabelecimentos(List<Estabelecimento> estabelecimentos) {
        this.estabelecimentos = Collections.synchronizedList(estabelecimentos);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid_usuario_indicacao() {
        return uuid_usuario_indicacao;
    }

    public void setUuid_usuario_indicacao(UUID uuid_usuario_indicacao) {
        this.uuid_usuario_indicacao = uuid_usuario_indicacao;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
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
        Usuario usuario = (Usuario) o;
        return Objects.equals(uuid, usuario.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
