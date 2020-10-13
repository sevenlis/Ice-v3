package by.ingman.sevenlis.ice_v3.classes;

import java.util.Date;

public class Answer {
    private String orderId;
    private String description;
    private int result;
    private Date unloadTime;

    public Answer(String orderId) {
        this.orderId = orderId;
        this.description = "";
        this.result = -1;
        this.unloadTime = new Date();
    }

    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String toStringForViewing() {
        return description;
    }
    
    public Date getUnloadTime() {
        return unloadTime;
    }
    
    public void setUnloadTime(Date unloadTime) {
        this.unloadTime = unloadTime;
    }
    
    public int getResult() {
        return result;
    }
    
    public void setResult(int result) {
        this.result = result;
    }
}
