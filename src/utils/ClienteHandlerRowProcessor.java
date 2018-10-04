package utils;

import org.apache.commons.dbutils.BasicRowProcessor;
import sistemaDelivery.modelo.Cliente;
import sistemaDelivery.modelo.Endereco;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ClienteHandlerRowProcessor extends BasicRowProcessor {
    @Override
    public <T> T toBean(ResultSet rs, Class<? extends T> type) throws SQLException {
        Cliente cliente = (Cliente) super.toBean(rs, type);
        Endereco endereco = super.toBean(rs, Endereco.class);
        cliente.setEndereco(endereco);
        return (T) cliente;
    }
}
