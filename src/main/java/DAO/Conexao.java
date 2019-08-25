package DAO;

import restFul.controle.ControleSistema;
import utils.Propriedades;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class Conexao {


    private static DataSource dataSource;

    public static Connection getConnection() {
        try {
            return Conexao.getDataSource().getConnection();
        } catch (SQLException e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            InitialContext ctx = null;
            try {
                ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup("java:jboss/datasources/" + Propriedades.getDataSource());
            } catch (NamingException e) {
                ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return dataSource;
    }
}
