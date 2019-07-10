package sistemaDelivery.modelo;

public class Endereco {
    private String logradouro, bairro, numero, referencia;

    public String getLogradouro() {
        if (logradouro == null) {
            return "";
        }
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getBairro() {
        if (bairro == null) {
            return "";
        }
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getNumero() {
        if (numero == null) {
            return "";
        }
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getReferencia() {
        if (referencia == null) {
            return "";
        }
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    @Override
    public String toString() {
        if ((getBairro().isEmpty() || getNumero().isEmpty()) && !getLogradouro().isEmpty()) {
            return getLogradouro();
        } else {
            return "Bairro:" + getBairro() + "\nLogradouro: " + getLogradouro() + "\nNumero:" + getNumero() + "\nReferencia:" + getReferencia();
        }
    }
}
