/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import java.util.Date;

/**
 * @author SYSTEM
 */
public class SeloFidelidade {

    private Date dataSelo;

    public SeloFidelidade() {
        this.dataSelo = new Date();
    }

    public Date getDataSelo() {
        return dataSelo;
    }

    public void setDataSelo(Date dataSelo) {
        this.dataSelo = dataSelo;
    }

}
