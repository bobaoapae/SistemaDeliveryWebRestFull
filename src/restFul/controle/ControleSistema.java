package restFul.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;

import java.sql.SQLException;
import java.util.logging.Logger;

public class ControleSistema {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleSistema instance;
    private Logger logger;

    public static ControleSistema getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleSistema();
            }
            return instance;
        }
    }

    public String getSecurePass() throws SQLException {
        try {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            String securePass = queryRunner.query("select * from \"SecurePass\"", resultSet -> {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                } else {
                    return "";
                }
            });
            return securePass;
        } catch (SQLException e) {
            throw e;
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
