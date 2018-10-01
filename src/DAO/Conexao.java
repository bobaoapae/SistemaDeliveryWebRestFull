package DAO;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Conexao {


    private static DataSource dataSource;

    public static Connection getConnection() {
        try {
            return Conexao.getDataSource().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
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
                e.printStackTrace();
            }
        }
        return dataSource;
    }
}
