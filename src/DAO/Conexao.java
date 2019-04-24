package DAO;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Conexao {


    private static DataSource dataSource;

    public static Connection getConnection() {
        try {
            return Conexao.getDataSource().getConnection();
        } catch (SQLException e) {
            Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            InitialContext ctx = null;
            try {
                ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup("java:jboss/datasources/DeliverySistem");
            } catch (NamingException e) {
                Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return dataSource;
    }
}
