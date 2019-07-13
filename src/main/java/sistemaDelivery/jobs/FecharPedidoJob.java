package sistemaDelivery.jobs;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import sistemaDelivery.SistemaDelivery;

import java.sql.SQLException;
import java.util.logging.Level;

public class FecharPedidoJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        SistemaDelivery sistemaDelivery = (SistemaDelivery) dataMap.get("sistemaDelivery");
        try {
            sistemaDelivery.fecharPedidos();
        } catch (SQLException e) {
            sistemaDelivery.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }
}
