package by.ingman.sevenlis.ice_v3.local.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import by.ingman.sevenlis.ice_v3.classes.Answer;
import by.ingman.sevenlis.ice_v3.classes.Contragent;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.classes.Point;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.classes.Storehouse;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class DBLocal {
    static final String TABLE_ORDERS = "orders";
    static final String TABLE_ANSWERS = "answers";
    public static final String TABLE_RESTS = "rests";
    public static final String TABLE_DEBTS = "debts";
    public static final String TABLE_CONTRAGENTS = "contragents";

    private DBHelper dbHelper;
    private Context ctx;

    public DBLocal(Context context) {
        this.ctx = context;
        this.dbHelper = new DBHelper(context);
    }

    public ArrayList<Order> getOrdersList(Calendar dateCal) {
        Calendar dayStart = (Calendar) dateCal.clone();
        FormatsUtils.roundDayToStart(dayStart);

        Calendar dayEnd = (Calendar) dateCal.clone();
        FormatsUtils.roundDayToEnd(dayEnd);

        Long dayStartMillis = dayStart.getTimeInMillis();
        Long dayEndMillis = dayEnd.getTimeInMillis();

        ArrayList<String> ordersUids = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true,TABLE_ORDERS,new String[]{"order_id"},"order_date >= ? AND order_date <= ?",new String[]{String.valueOf(dayStartMillis), String.valueOf(dayEndMillis)},null,null,null,null);
        while (cursor.moveToNext())
            ordersUids.add(cursor.getString(cursor.getColumnIndex("order_id")));
        cursor.close();
        db.close();

        ArrayList<Order> ordersList = new ArrayList<>();
        for (String orderUid : ordersUids) {
            ordersList.add(getOrder(orderUid));
        }
        return ordersList;
    }

    public ArrayList<Order> getUnsentOrdersList() {
        ArrayList<String> ordersUids = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true,TABLE_ORDERS,new String[]{"order_id"},"sent = 0 AND status = 0 AND processed = 0",null,null,null,null,null);
        while (cursor.moveToNext())
            ordersUids.add(cursor.getString(cursor.getColumnIndex("order_id")));
        cursor.close();
        db.close();

        ArrayList<Order> ordersList = new ArrayList<>();
        for (String orderUid : ordersUids) {
            ordersList.add(getOrder(orderUid));
        }
        return ordersList;
    }

    public ArrayList<String> getUnansweredOrdersUids() {
        ArrayList<String> ordersUids = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true,TABLE_ORDERS,new String[]{"order_id"},"sent = 1 AND status = 1 AND processed = 0",null,null,null,null,null);
        while (cursor.moveToNext())
            ordersUids.add(cursor.getString(cursor.getColumnIndex("order_id")));
        cursor.close();
        db.close();
        return ordersUids;
    }

    public Order getOrder(String orderUid) {
        Order mOrder = new Order(ctx);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true,TABLE_ORDERS,null,"order_id = ?",new String[]{orderUid},null,null,null,"1");
        if (cursor.moveToFirst()) {
            mOrder.orderUid = cursor.getString(cursor.getColumnIndex("order_id"));
            mOrder.orderDate = new Date(cursor.getLong(cursor.getColumnIndex("order_date")));
            mOrder.comment = cursor.getString(cursor.getColumnIndex("comments"));
            mOrder.setContragent(new Contragent(cursor.getString(cursor.getColumnIndex("code_k")), cursor.getString(cursor.getColumnIndex("name_k"))));
            mOrder.setPoint(new Point(cursor.getString(cursor.getColumnIndex("code_r")), cursor.getString(cursor.getColumnIndex("name_r"))));
            mOrder.setStorehouse(new Storehouse(cursor.getString(cursor.getColumnIndex("code_s")), cursor.getString(cursor.getColumnIndex("name_s"))));
            mOrder.isAdvertising = cursor.getInt(cursor.getColumnIndex("is_advertising")) == 1;
            mOrder.advType = cursor.getInt(cursor.getColumnIndex("adv_type"));
            mOrder.dateUnload = cursor.getLong(cursor.getColumnIndex("date_unload"));
            mOrder.status = cursor.getInt(cursor.getColumnIndex("status"));
        }
        cursor.close();
        db.close();

        mOrder.setOrderItems(getOrderItems(orderUid));

        return mOrder;
    }

    private ArrayList<OrderItem> getOrderItems(String orderUid) {
        ArrayList<OrderItem> orderItems = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ORDERS,null,"order_id = ?",new String[]{orderUid},null, null, null, null);
        while (cursor.moveToNext()) {
            Product product = new Product(cursor.getString(cursor.getColumnIndex("code_p")), cursor.getString(cursor.getColumnIndex("name_p")),
                    cursor.getDouble(cursor.getColumnIndex("weight_p")), cursor.getDouble(cursor.getColumnIndex("price_p")),
                    cursor.getDouble(cursor.getColumnIndex("num_in_pack_p")));
            orderItems.add(new OrderItem(product, cursor.getDouble(cursor.getColumnIndex("amount"))));
        }
        cursor.close();
        db.close();

        return orderItems;
    }

    public ArrayList<Contragent> getContragents(String condition, String[] conditionArgs) {
        ArrayList<Contragent> contragentArrayList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        if (condition.isEmpty()) {
            cursor = db.query(true,TABLE_CONTRAGENTS,new String[]{"code_k", "name_k"},null,null,null,null,null,null);
        } else {
            cursor = db.query(true,TABLE_CONTRAGENTS,new String[]{"code_k", "name_k"},condition,conditionArgs,null,null,null,null);
        }

        while (cursor.moveToNext()) {
            Contragent contragent = new Contragent(cursor.getString(cursor.getColumnIndex("code_k")), cursor.getString(cursor.getColumnIndex("name_k")));
            contragentArrayList.add(contragent);
        }
        cursor.close();
        db.close();
        return contragentArrayList;
    }


    public ArrayList<Contragent> getRecentContragents() {
        ArrayList<Contragent> contragentArrayList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true,TABLE_ORDERS,new String[]{"code_k", "name_k"},null,null,null,null,"name_k ASC",null);
        while (cursor.moveToNext()) {
            Contragent contragent = new Contragent(cursor.getString(cursor.getColumnIndex("code_k")), cursor.getString(cursor.getColumnIndex("name_k")));
            contragentArrayList.add(contragent);
        }
        cursor.close();
        db.close();
        return contragentArrayList;
    }

    public ArrayList<Contragent> getContragents(String condition) {
        ArrayList<Contragent> contragentArrayList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(condition,null);
        while (cursor.moveToNext()) {
            Contragent contragent = new Contragent(cursor.getString(cursor.getColumnIndex("code_k")), cursor.getString(cursor.getColumnIndex("name_k")));
            contragentArrayList.add(contragent);
        }
        cursor.close();
        db.close();
        return contragentArrayList;
    }

    public ArrayList<Product> getProducts(String condition, String[] conditionArgs) {
        ArrayList<Product> productArrayList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        if (condition.isEmpty()) {
            cursor = db.query(true,TABLE_RESTS,new String[]{"code_p", "name_p", "price", "gross_weight", "amt_in_pack"},null,null,null,null,null,null);
        } else {
            cursor = db.query(true,TABLE_RESTS,new String[]{"code_p", "name_p", "price", "gross_weight", "amt_in_pack"},condition,conditionArgs,null,null,null,null);
        }
        while (cursor.moveToNext()) {
            Product product = new Product(
                    cursor.getString(cursor.getColumnIndex("code_p")),
                    cursor.getString(cursor.getColumnIndex("name_p")),
                    cursor.getDouble(cursor.getColumnIndex("gross_weight")),
                    cursor.getDouble(cursor.getColumnIndex("price")),
                    cursor.getDouble(cursor.getColumnIndex("amt_in_pack"))
                                );
            productArrayList.add(product);
        }
        cursor.close();
        db.close();
        return productArrayList;
    }

    public ArrayList<Point> getPoints(String condition, String[] conditionArgs) {
        ArrayList<Point> pointArrayList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true,TABLE_CONTRAGENTS,new String[]{"code_r", "name_r"},condition,conditionArgs,null,null,null,null);
        while (cursor.moveToNext()) {
            Point point = new Point(cursor.getString(cursor.getColumnIndex("code_r")), cursor.getString(cursor.getColumnIndex("name_r")));
            pointArrayList.add(point);
        }
        cursor.close();
        db.close();
        return pointArrayList;
    }

    public String getContragentRating(Contragent contragent) {
        String value = "";
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBLocal.TABLE_DEBTS, null, "code_k = ? AND name_k = ?", new String[]{contragent.getCode(), contragent.getName()}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndex("rating"));
        }
        cursor.close();
        db.close();
        return value;
    }

    public double getContragentDebt(Contragent contragent) {
        double value = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBLocal.TABLE_DEBTS, null, "code_k = ? AND name_k = ?", new String[]{contragent.getCode(), contragent.getName()}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("debt"));
        }
        cursor.close();
        db.close();
        return value;
    }

    public double getContragentOverdue(Contragent contragent) throws SQLiteException {
        double value = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBLocal.TABLE_DEBTS, null, "code_k = ? AND name_k = ?", new String[]{contragent.getCode(), contragent.getName()}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("overdue"));
        }
        cursor.close();
        db.close();
        return value;
    }

    public double getProductRestAmount(Product product) {
        double value = 0;
        String code_s = SettingsUtils.Settings.getDefaultStoreHouseCode(ctx);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBLocal.TABLE_RESTS, null, "code_s = ? AND code_p = ? AND name_p = ?", new String[]{code_s, product.code, product.name}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("amount"));
        }
        cursor.close();
        db.close();
        return value;
    }

    public double getProductBlockAmount(Product product) {
        double value = 0;
        String code_s = SettingsUtils.Settings.getDefaultStoreHouseCode(ctx);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBLocal.TABLE_ORDERS, new String[]{"amount"}, "code_s = ? AND code_p = ? AND name_p = ? AND status <> 2", new String[]{code_s, product.code, product.name}, null, null, null, null);
        if (cursor.moveToFirst()) {
            value += cursor.getDouble(cursor.getColumnIndex("amount"));
        }
        cursor.close();
        db.close();
        return value;
    }

    public double getProductRestPacks(Product product) {
        Cursor cursor;
        double value = 0;
        String code_s = SettingsUtils.Settings.getDefaultStoreHouseCode(ctx);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        cursor = db.query(DBLocal.TABLE_RESTS, null, "code_s = ? AND code_p = ? AND name_p = ?", new String[]{code_s, product.code, product.name}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("packs"));
        }
        cursor.close();
        db.close();

        return value;
    }

    public double getProductBlockPacks(Product product) {
        Cursor cursor;
        double value = 0;
        String code_s = SettingsUtils.Settings.getDefaultStoreHouseCode(ctx);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        cursor = db.query(DBLocal.TABLE_ORDERS, new String[]{"amt_packs"}, "code_s = ? AND code_p = ? AND name_p = ? AND status <> 2", new String[]{code_s, product.code, product.name}, null, null, null, null);
        while (cursor.moveToNext()) {
            value += cursor.getDouble(cursor.getColumnIndex("amt_packs"));
        }
        cursor.close();
        db.close();

        return value;
    }

    public Storehouse getDefaultStorehouse() {
        Storehouse value = null;
        String code_s = SettingsUtils.Settings.getDefaultStoreHouseCode(ctx);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(true,TABLE_RESTS,new String[]{"code_s", "name_s"},"code_s = ?",new String[]{code_s},null,null,null,null);
        if (cursor.moveToFirst()) {
            value = new Storehouse(cursor.getString(cursor.getColumnIndex("code_s")), cursor.getString(cursor.getColumnIndex("name_s")));
        }
        cursor.close();
        db.close();
        return value;
    }

    public String addWildcards(String filter) {
        String filterWildcards = TextUtils.isEmpty(filter) ? "" : filter.trim().toUpperCase();
        filterWildcards = String.format("%%%s%%", filterWildcards);
        return filterWildcards;
    }

    public void saveOrder(Order order) {
        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        long nowMillis = now.getTime();
        ArrayList<ContentValues> orderData = new ArrayList<>();
        for (OrderItem oi : order.orderItems) {
            ContentValues cv = new ContentValues();
            cv.put("order_id",order.orderUid);
            cv.put("name_m",SettingsUtils.Settings.getManagerName(ctx));
            cv.put("order_date",order.orderDate.getTime());
            cv.put("is_advertising",order.getIsAdvInteger());
            cv.put("adv_type",order.advType);
            cv.put("code_k",order.contragentCode);
            cv.put("name_k",order.contragentName);
            cv.put("code_r",order.pointCode);
            cv.put("name_r",order.pointName);
            cv.put("code_s",order.storehouseCode);
            cv.put("name_s",order.storehouseName);
            cv.put("comments",order.comment);
            cv.put("processed",0);
            cv.put("status",0);
            cv.put("sent",0);
            cv.put("date_unload",nowMillis);

            cv.put("code_p",oi.product.code);
            cv.put("name_p",oi.product.name);
            cv.put("weight_p",oi.product.weight);
            cv.put("price_p",oi.product.price);
            cv.put("num_in_pack_p",oi.product.num_in_pack);
            cv.put("amount",oi.quantity);
            cv.put("amt_packs",oi.packs);
            cv.put("weight",oi.weight);
            cv.put("price",oi.product.price);
            cv.put("summa",oi.summa);

            orderData.add(cv);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            db.beginTransaction();
            db.delete(DBLocal.TABLE_ORDERS, "order_id = ?", new String[]{order.orderUid});
            for (ContentValues cv : orderData) {
                db.insert(DBLocal.TABLE_ORDERS,null,cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void deleteOrder(Order order) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            db.beginTransaction();
            db.delete(DBLocal.TABLE_ORDERS, "order_id = ?", new String[]{order.orderUid});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void setUnsetOrdersAsSent(ArrayList<Order> ordersList) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        for (Order order : ordersList) {
            ContentValues cv = new ContentValues();
            cv.put("status",1);
            cv.put("sent",1);
            cv.put("processed",0);
            try {
                db.beginTransaction();
                db.update(DBLocal.TABLE_ORDERS, cv, "order_id = ?", new String[]{order.orderUid});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        db.close();
    }

    private void setOrderAsAnswered(String orderUid) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status"     ,2);
        cv.put("sent"       ,1);
        cv.put("processed"  ,1);
        try {
            db.beginTransaction();
            db.update(DBLocal.TABLE_ORDERS, cv, "order_id = ?", new String[]{orderUid});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    public void updateAnswer(Answer answer) {
        boolean result = false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("order_id",answer.getOrderId());
        cv.put("description",answer.getDescription());
        cv.put("result",answer.getResult());
        cv.put("date_unload",answer.getUnloadTime().getTime());
        try {
            db.beginTransaction();
            db.delete(DBLocal.TABLE_ANSWERS, "order_id = ?", new String[]{answer.getOrderId()});
            db.insert(DBLocal.TABLE_ANSWERS, null, cv);
            db.setTransactionSuccessful();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        db.close();

        if (result) {
            setOrderAsAnswered(answer.getOrderId());
        }
    }

    public Answer getAnswer(String orderUid) {
        Answer answer = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBLocal.TABLE_ANSWERS, null, "order_id = ?", new String[]{orderUid}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            answer = new Answer();
            answer.setOrderId(cursor.getString(cursor.getColumnIndex("order_id")));
            answer.setDescription(cursor.getString(cursor.getColumnIndex("description")));
            answer.setResult(cursor.getInt(cursor.getColumnIndex("result")));
            answer.setUnloadTime(new Date(cursor.getLong(cursor.getColumnIndex("date_unload"))));
        }
        cursor.close();
        db.close();
        return answer;
    }
}
