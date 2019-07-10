package utils;

import org.apache.commons.dbutils.BasicRowProcessor;
import sistemaDelivery.modelo.Endereco;
import sistemaDelivery.modelo.Pedido;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PedidoHandlerRowProcessor extends BasicRowProcessor {
    @Override
    public <T> T toBean(ResultSet rs, Class<? extends T> type) throws SQLException {
        Pedido pedido = (Pedido) super.toBean(rs, type);
        Endereco endereco = super.toBean(rs, Endereco.class);
        pedido.setEndereco(endereco);
        return (T) pedido;
    }
}
