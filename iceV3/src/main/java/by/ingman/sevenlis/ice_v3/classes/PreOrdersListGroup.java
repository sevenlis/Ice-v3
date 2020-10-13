package by.ingman.sevenlis.ice_v3.classes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PreOrdersListGroup {
    Date date;
    List<Order> orderList;

    public PreOrdersListGroup(Date date, List<Order> orderList) {
        this.date = date;
        this.orderList = orderList;
    }

    public int getOrdersCount() {
        return orderList.size();
    }

    public Order geOrder(int position) {
        return orderList.get(position);
    }

    public List<Order> getOrderList() {
        return orderList;
    }

    public Date getDate() {
        return date;
    }

    public int getPointsCount() {
        List<String> containsList = new ArrayList<>();
        for (Order order : orderList) {
            Point point = order.getPoint();
            String containsString = point.getCode() + "_" + point.getName();
            if (!containsList.contains(containsString))
                containsList.add(containsString);
        }
        return containsList.size();
    }

    public int getContragentsCount() {
        List<String> containsList = new ArrayList<>();
        for (Order order : orderList) {
            Contragent contragent = order.getContragent();
            String containsString = contragent.getCode() + "_" + contragent.getName();
            if (!containsList.contains(containsString))
                containsList.add(containsString);
        }
        return containsList.size();
    }

    public double getPacks() {
        double packs = 0D;
        for (Order order : orderList) {
            packs += order.getPacks();
        }
        return packs;
    }

    public double getWeight() {
        double packs = 0D;
        for (Order order : orderList) {
            packs += order.getWeight();
        }
        return packs;
    }

}
