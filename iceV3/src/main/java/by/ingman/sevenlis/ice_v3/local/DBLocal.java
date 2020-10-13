package by.ingman.sevenlis.ice_v3.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Agreement;
import by.ingman.sevenlis.ice_v3.classes.Answer;
import by.ingman.sevenlis.ice_v3.classes.Contragent;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.classes.Point;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.classes.Storehouse;
import by.ingman.sevenlis.ice_v3.remote.ConnectionFactory;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class DBLocal {
    public static final String TABLE_RESTS = "rests";
    public static final String TABLE_DEBTS = "debts";
    public static final String TABLE_CONTRAGENTS = "contragents";
    public static final String TABLE_AGREEMENTS = "agreements";
    static final String TABLE_ORDERS = "orders";
    static final String TABLE_ANSWERS = "answers";
    static final String TABLE_LOCATION = "location";
    private final Context ctx;
    private String mStorehouseCode;
    
    public DBLocal(Context context) {
        this.ctx = context;
        this.mStorehouseCode = SettingsUtils.Settings.getDefaultStoreHouseCode(ctx);
    }

    public void setStorehouseCode(String mStorehouseCode) {
        this.mStorehouseCode = mStorehouseCode;
    }

    public List<Order> getOrdersList(Calendar dateCal) {
        List<Order> ordersList = new ArrayList<>();

        Calendar dayStart = (Calendar) dateCal.clone();
        FormatsUtils.roundDayToStart(dayStart);
        
        Calendar dayEnd = (Calendar) dateCal.clone();
        FormatsUtils.roundDayToEnd(dayEnd);
        
        Long dayStartMillis = dayStart.getTimeInMillis();
        Long dayEndMillis = dayEnd.getTimeInMillis();
        
        List<String> ordersUids = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return ordersList;
        Cursor cursor = db.query(true, TABLE_ORDERS, new String[]{"order_id"}, "order_type <> -20 AND order_date >= ? AND order_date <= ?", new String[]{String.valueOf(dayStartMillis), String.valueOf(dayEndMillis)}, null, null, null, null);
        while (cursor.moveToNext())
            ordersUids.add(cursor.getString(cursor.getColumnIndex("order_id")));
        cursor.close();

        for (String orderUid : ordersUids) {
            ordersList.add(getOrder(orderUid));
        }
        DBHelper.closeDatabase(db);
        return ordersList;
    }

    public List<Order> getPreOrdersList(Calendar dateCal) {
        List<Order> ordersList = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return ordersList;

        Long dayStartMillis = FormatsUtils.roundDayToStart((Calendar) dateCal.clone()).getTimeInMillis();
        Long dayEndMillis = FormatsUtils.roundDayToEnd((Calendar) dateCal.clone()).getTimeInMillis();

        List<String> ordersUids = new ArrayList<>();
        Cursor cursor = db.query(true, TABLE_ORDERS, new String[]{"order_id"}, "order_type = -20 AND order_date >= ? AND order_date <= ?", new String[]{String.valueOf(dayStartMillis), String.valueOf(dayEndMillis)}, null, null, null, null);
        while (cursor.moveToNext())
            ordersUids.add(cursor.getString(cursor.getColumnIndex("order_id")));
        cursor.close();

        for (String orderUid : ordersUids) {
            ordersList.add(getOrder(orderUid));
        }
        DBHelper.closeDatabase(db);
        return ordersList;
    }

    public List<Date> getPreOrdersDatesDesc() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        List<Date> dates = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return dates;
        Cursor cursor = db.query(true,TABLE_ORDERS + " AS O",new String[]{"strftime('%Y-%m-%d', O.order_date/1000, 'unixepoch', 'localtime') AS s_date"},"order_type = -20",null,null,null,"order_date DESC",null);
        while (cursor.moveToNext()) {
            String sDate = cursor.getString(cursor.getColumnIndexOrThrow("s_date"));
            try {
                Date date = sdf.parse(sDate);
                dates.add(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return dates;
    }

    public List<Order> getUnsentOrdersList() {
        List<Order> ordersList = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return ordersList;

        List<String> ordersUids = new ArrayList<>();
        Cursor cursor = db.query(true, TABLE_ORDERS, new String[]{"order_id"}, "sent = 0 AND status = 0 AND processed = 0 AND order_type <> -20", null, null, null, null, null);
        while (cursor.moveToNext())
            ordersUids.add(cursor.getString(cursor.getColumnIndex("order_id")));
        cursor.close();

        for (String orderUid : ordersUids) {
            ordersList.add(getOrder(orderUid));
        }
        DBHelper.closeDatabase(db);
        return ordersList;
    }
    
    public List<String> getUnansweredOrdersUids() {
        List<String> ordersUids = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return ordersUids;
        Cursor cursor = db.query(true, TABLE_ORDERS, new String[]{"order_id"}, "sent = 1 AND status = 1 AND processed = 0 AND order_type <> -20", null, null, null, null, null);
        while (cursor.moveToNext())
            ordersUids.add(cursor.getString(cursor.getColumnIndex("order_id")));
        cursor.close();
        return ordersUids;
    }

    public Agreement getAgreementById(String id) {
        Agreement agreement = new Agreement("",ctx.getResources().getString(R.string.select_agreement));
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return agreement;
        Cursor cursor = db.query(true, TABLE_AGREEMENTS, new String[]{"id","name"}, "id = ?", new String[]{id == null ? "" : id}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            agreement.setId(cursor.getString(cursor.getColumnIndex("id")));
            agreement.setName(cursor.getString(cursor.getColumnIndex("name")));
        }
        cursor.close();
        DBHelper.closeDatabase(db);
        return agreement;
    }
    
    public Order getOrder(String orderUid) {
        Order mOrder = new Order(ctx);
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return mOrder;
        Cursor cursor = db.query(true, TABLE_ORDERS, null, "order_id = ?", new String[]{orderUid}, null, null, null, "1");
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
            mOrder.setAgreement(getAgreementById(cursor.getString(cursor.getColumnIndex("agreementId"))));
            mOrder.orderType = cursor.getInt(cursor.getColumnIndex("order_type"));
            mOrder.answer = getAnswer(orderUid);
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        mOrder.setOrderItems(getOrderItems(orderUid));
        
        return mOrder;
    }
    
    private List<OrderItem> getOrderItems(String orderUid) {
        List<OrderItem> orderItems = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        Cursor cursor = db.query(false, TABLE_ORDERS, null, "order_id = ?", new String[]{orderUid}, null, null, null, null);
        while (cursor.moveToNext()) {
            Product product = new Product(cursor.getString(cursor.getColumnIndex("code_p")), cursor.getString(cursor.getColumnIndex("name_p")),
                    cursor.getDouble(cursor.getColumnIndex("weight_p")), cursor.getDouble(cursor.getColumnIndex("price_p")),
                    cursor.getDouble(cursor.getColumnIndex("num_in_pack_p")));
            orderItems.add(new OrderItem(product, cursor.getDouble(cursor.getColumnIndex("amount"))));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return orderItems;
    }
    
    public List<Contragent> getContragents(String condition, String[] conditionArgs) {
        List<Contragent> contragentArrayList = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return contragentArrayList;
        Cursor cursor;
        if (condition.isEmpty()) {
            cursor = db.query(true, TABLE_CONTRAGENTS, new String[]{"code_k", "name_k"}, null, null, null, null, null, null);
        } else {
            cursor = db.query(true, TABLE_CONTRAGENTS, new String[]{"code_k", "name_k"}, condition, conditionArgs, null, null, null, null);
        }
        
        while (cursor.moveToNext()) {
            Contragent contragent = new Contragent(cursor.getString(cursor.getColumnIndex("code_k")), cursor.getString(cursor.getColumnIndex("name_k")));
            contragentArrayList.add(contragent);
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return contragentArrayList;
    }
    
    public List<Contragent> getRecentContragents() {
        List<Contragent> contragentArrayList = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return contragentArrayList;
        Cursor cursor = db.query(true, TABLE_ORDERS, new String[]{"code_k", "name_k"}, null, null, null, null, "name_k ASC", null);
        while (cursor.moveToNext()) {
            Contragent contragent = new Contragent(cursor.getString(cursor.getColumnIndex("code_k")), cursor.getString(cursor.getColumnIndex("name_k")));
            contragentArrayList.add(contragent);
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return contragentArrayList;
    }

    public List<Storehouse> getStorehouses() {
        String[] defCodes = SettingsUtils.Settings.getStorehousesCodes(ctx);
        StringBuilder sb = new StringBuilder("'");
        for (String code : defCodes) {
            sb.append(code.replace("'", "''")).append("','");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }

        List<Storehouse> storehouses = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return storehouses;
        Cursor cursor = db.rawQuery("select distinct code_s, name_s from " + TABLE_RESTS + " where code_s in (" + sb.toString() + ")",null);
        while (cursor.moveToNext()) {
            storehouses.add(new Storehouse(cursor.getString(cursor.getColumnIndex("code_s")),cursor.getString(cursor.getColumnIndex("name_s"))));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return storehouses;
    }
    
    public List<Contragent> getContragents(String condition) {
        List<Contragent> contragentArrayList = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return contragentArrayList;
        Cursor cursor = db.rawQuery(condition, null);
        while (cursor.moveToNext()) {
            Contragent contragent = new Contragent(cursor.getString(cursor.getColumnIndex("code_k")), cursor.getString(cursor.getColumnIndex("name_k")));
            contragentArrayList.add(contragent);
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return contragentArrayList;
    }
    
    public List<Product> getProducts(String condition, String[] conditionArgs) {
        List<Product> productArrayList = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return productArrayList;
        Cursor cursor;
        if (condition.isEmpty()) {
            cursor = db.query(true, TABLE_RESTS, new String[]{"code_p", "name_p", "price", "gross_weight", "amt_in_pack"}, null, null, null, null, null, null);
        } else {
            cursor = db.query(true, TABLE_RESTS, new String[]{"code_p", "name_p", "price", "gross_weight", "amt_in_pack"}, condition, conditionArgs, null, null, null, null);
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
        DBHelper.closeDatabase(db);

        return productArrayList;
    }
    
    public List<Point> getPoints(String condition, String[] conditionArgs) {
        List<Point> pointArrayList = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return pointArrayList;
        Cursor cursor = db.query(true, TABLE_CONTRAGENTS, new String[]{"code_r", "name_r"}, condition, conditionArgs, null, null, null, null);
        while (cursor.moveToNext()) {
            Point point = new Point(cursor.getString(cursor.getColumnIndex("code_r")), cursor.getString(cursor.getColumnIndex("name_r")));
            pointArrayList.add(point);
        }
        cursor.close();
        return pointArrayList;
    }

    public List<Agreement> getAgreements(Contragent contragent) {
        List<Agreement> agreements = new ArrayList<>();
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return agreements;
        Cursor cursor = db.query(true, TABLE_AGREEMENTS, new String[]{"id", "name"}, "code_k = ? AND name_k = ?", new String[]{contragent.getCode(), contragent.getName()}, null, null, null, null);
        while (cursor.moveToNext()) {
            agreements.add(new Agreement(cursor.getString(cursor.getColumnIndex("id")),cursor.getString(cursor.getColumnIndex("name"))));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return agreements;
    }
    
    public String getContragentRating(Contragent contragent) {
        String rating = "";
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return rating;
        Cursor cursor = db.query(true, DBLocal.TABLE_DEBTS, new String[]{"rating"}, "code_k = ? AND name_k = ?", new String[]{contragent.getCode(), contragent.getName()}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            rating = cursor.getString(cursor.getColumnIndex("rating"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return rating;
    }
    
    public double getContragentDebt(Contragent contragent) {
        double value = 0d;
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return value;
        Cursor cursor = db.query(true, DBLocal.TABLE_DEBTS, new String[]{"debt"}, "code_k = ? AND name_k = ?", new String[]{contragent.getCode(), contragent.getName()}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("debt"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return value;
    }
    
    public double getContragentOverdue(Contragent contragent) throws SQLiteException {
        double value = 0d;
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return value;
        Cursor cursor = db.query(true, DBLocal.TABLE_DEBTS, new String[]{"overdue"}, "code_k = ? AND name_k = ?", new String[]{contragent.getCode(), contragent.getName()}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("overdue"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return value;
    }
    
    public double getProductRestAmount(Product product) {
        double value = 0d;
        String code_s = mStorehouseCode;
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return value;
        Cursor cursor = db.query(true, DBLocal.TABLE_RESTS, new String[]{"amount"}, "code_s = ? AND code_p = ? AND name_p = ?", new String[]{code_s, product.code, product.name}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("amount"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return value;
    }
    
    public double getProductBlockAmount(Product product) {
        double value = 0d;
        String code_s = mStorehouseCode;
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return value;
        Cursor cursor = db.query(true, DBLocal.TABLE_ORDERS, new String[]{"amount"}, "code_s = ? AND code_p = ? AND name_p = ? AND status <> 2", new String[]{code_s, product.code, product.name}, null, null, null, null);
        if (cursor.moveToFirst()) {
            value += cursor.getDouble(cursor.getColumnIndex("amount"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return value;
    }
    
    public double getProductRestPacks(Product product) {
        Cursor cursor;
        double value = 0d;
        String code_s = mStorehouseCode;
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return value;
        cursor = db.query(true, DBLocal.TABLE_RESTS, new String[]{"packs"}, "code_s = ? AND code_p = ? AND name_p = ?", new String[]{code_s, product.code, product.name}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            value = cursor.getDouble(cursor.getColumnIndex("packs"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return value;
    }
    
    public double getProductBlockPacks(Product product) {
        Cursor cursor;
        double value = 0d;
        String code_s = mStorehouseCode;
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return value;
        cursor = db.query(true, DBLocal.TABLE_ORDERS, new String[]{"amt_packs"}, "code_s = ? AND code_p = ? AND name_p = ? AND status <> 2", new String[]{code_s, product.code, product.name}, null, null, null, null);
        while (cursor.moveToNext()) {
            value += cursor.getDouble(cursor.getColumnIndex("amt_packs"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return value;
    }
    
    public Storehouse getDefaultStorehouse() {
        Storehouse storehouse = new Storehouse("",ctx.getResources().getString(R.string.select_storehouse));
        String code_s = SettingsUtils.Settings.getDefaultStoreHouseCode(ctx);
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return storehouse;
        Cursor cursor = db.query(true, TABLE_RESTS, new String[]{"code_s", "name_s"}, "code_s = ?", new String[]{code_s}, null, null, null, null);
        if (cursor.moveToFirst()) {
            storehouse.code = cursor.getString(cursor.getColumnIndex("code_s"));
            storehouse.name = cursor.getString(cursor.getColumnIndex("name_s"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return storehouse;
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
            cv.put("order_id", order.orderUid);
            cv.put("name_m", SettingsUtils.Settings.getManagerName(ctx));
            cv.put("order_date", order.orderDate.getTime());
            cv.put("is_advertising", order.getIsAdvInteger());
            cv.put("adv_type", order.advType);
            cv.put("code_k", order.contragentCode);
            cv.put("name_k", order.contragentName);
            cv.put("code_r", order.pointCode);
            cv.put("name_r", order.pointName);
            cv.put("code_s", order.storehouseCode);
            cv.put("name_s", order.storehouseName);
            cv.put("agreementId",order.agreement.getId());
            cv.put("order_type",order.orderType);
            cv.put("comments", order.comment);
            cv.put("processed", 0);
            cv.put("status", 0);
            cv.put("sent", 0);
            cv.put("date_unload", nowMillis);
            
            cv.put("code_p", oi.product.code);
            cv.put("name_p", oi.product.name);
            cv.put("weight_p", oi.product.weight);
            cv.put("price_p", oi.product.price);
            cv.put("num_in_pack_p", oi.product.num_in_pack);
            cv.put("amount", oi.quantity);
            cv.put("amt_packs", oi.packs);
            cv.put("weight", oi.weight);
            cv.put("price", oi.product.price);
            cv.put("summa", oi.summa);
            
            orderData.add(cv);
        }
        
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        try {
            db.beginTransactionNonExclusive();
            db.delete(DBLocal.TABLE_ORDERS, "order_id = ?", new String[]{order.orderUid});
            for (ContentValues cv : orderData) {
                db.insert(DBLocal.TABLE_ORDERS, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        DBHelper.closeDatabase(db);

        repeatOrder(order.orderUid);
    }
    
    public void deleteOrder(Order order) {
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        try {
            db.beginTransactionNonExclusive();
            db.delete(DBLocal.TABLE_ORDERS, "order_id = ?", new String[]{order.orderUid});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        DBHelper.closeDatabase(db);
    }
    
    public void setUnsetOrdersAsSent(List<Order> ordersList) {
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        for (Order order : ordersList) {
            ContentValues cv = new ContentValues();
            cv.put("status", 1);
            cv.put("sent", 1);
            cv.put("processed", 0);
            try {
                db.beginTransactionNonExclusive();
                db.update(DBLocal.TABLE_ORDERS, cv, "order_id = ?", new String[]{order.orderUid});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        DBHelper.closeDatabase(db);
    }

    public void savePreOrdersAsOrders(List<Order> ordersList) {
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        for (Order order : ordersList) {
            ContentValues cv = new ContentValues();
            cv.put("order_type", 1);
            cv.put("status", 0);
            cv.put("sent", 0);
            cv.put("processed", 0);
            try {
                db.beginTransactionNonExclusive();
                db.update(DBLocal.TABLE_ORDERS, cv, "order_id = ?", new String[]{order.orderUid});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        DBHelper.closeDatabase(db);
    }

    public void changeDateOrders(List<Order> ordersList, Date newDate) {
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        for (Order order : ordersList) {
            ContentValues cv = new ContentValues();
            cv.put("order_date", newDate.getTime());
            try {
                db.beginTransactionNonExclusive();
                db.update(DBLocal.TABLE_ORDERS, cv, "order_id = ?", new String[]{order.orderUid});
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }
        }
        DBHelper.closeDatabase(db);
    }

    private void setOrderAsAnswered(String orderUid) {
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        ContentValues cv = new ContentValues();
        cv.put("status", 2);
        cv.put("sent", 1);
        cv.put("processed", 1);
        try {
            db.beginTransactionNonExclusive();
            db.update(DBLocal.TABLE_ORDERS, cv, "order_id = ?", new String[]{orderUid});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        DBHelper.closeDatabase(db);
    }
    
    public void updateAnswer(Answer answer) {
        boolean result = false;
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        ContentValues cv = new ContentValues();
        cv.put("order_id", answer.getOrderId());
        cv.put("description", answer.getDescription());
        cv.put("result", answer.getResult());
        cv.put("date_unload", answer.getUnloadTime().getTime());
        try {
            db.beginTransactionNonExclusive();
            db.delete(DBLocal.TABLE_ANSWERS, "order_id = ?", new String[]{answer.getOrderId()});
            db.insert(DBLocal.TABLE_ANSWERS, null, cv);
            db.setTransactionSuccessful();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        DBHelper.closeDatabase(db);

        if (result) {
            setOrderAsAnswered(answer.getOrderId());
        }
    }
    
    public Answer getAnswer(String orderUid) {
        Answer answer = null;
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return null;
        Cursor cursor = db.query(DBLocal.TABLE_ANSWERS, null, "order_id = ?", new String[]{orderUid}, null, null, null, "1");
        if (cursor.moveToFirst()) {
            answer = new Answer(orderUid);
            answer.setDescription(cursor.getString(cursor.getColumnIndex("description")));
            answer.setResult(cursor.getInt(cursor.getColumnIndex("result")));
            answer.setUnloadTime(new Date(cursor.getLong(cursor.getColumnIndex("date_unload"))));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return answer;
    }

    public void deleteRemoteAnswerResult(String orderUid) {
        new Thread(() -> {
            try {
                Connection conn = new ConnectionFactory().getConnection(ctx);
                if (conn == null) throw new SQLException("Error: SQL connection is NULL");
                PreparedStatement statement = conn.prepareStatement("DELETE FROM results WHERE order_id = ?");
                statement.setString(1, orderUid);
                statement.execute();
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void deleteRemoteOrder(String orderUid) {
        new Thread(() -> {
            try {
                Connection conn = new ConnectionFactory().getConnection(ctx);
                if (conn == null) throw new SQLException("Error: SQL connection is NULL");
                PreparedStatement statement = conn.prepareStatement("DELETE FROM orders WHERE order_id = ?");
                statement.setString(1, orderUid);
                statement.execute();
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public boolean repeatOrder(String orderUid) {
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return false;
        try {
            db.beginTransactionNonExclusive();
            db.delete(DBLocal.TABLE_ANSWERS, "order_id = ?", new String[]{orderUid});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }

        ContentValues cv = new ContentValues();
        cv.put("processed", 0);
        cv.put("status", 0);
        cv.put("sent", 0);
        try {
            db.beginTransactionNonExclusive();
            db.update(DBLocal.TABLE_ORDERS,cv, "order_id = ?", new String[]{orderUid});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }

        DBHelper.closeDatabase(db);

        deleteRemoteOrder(orderUid);
        deleteRemoteAnswerResult(orderUid);
        return true;
    }

    public void saveLocation(double lat, double lon, long time) {
        SQLiteDatabase db = DBHelper.getDatabaseWritable(ctx);
        if (db == null) return;
        ContentValues cv = new ContentValues();
        cv.put("latitude", lat);
        cv.put("longitude", lon);
        cv.put("time", time);
        cv.put("time_update", Calendar.getInstance().getTimeInMillis());
        try {
            db.beginTransactionNonExclusive();
            db.insert(DBLocal.TABLE_LOCATION, null, cv);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db.inTransaction())
                db.endTransaction();
        }
        DBHelper.closeDatabase(db);
    }
    
    public LatLng getStartRoutePosition(long time) {
        double latitude = 0d;
        double longitude = 0d;
        
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return new LatLng(latitude,longitude);
        Cursor cursor = db.query(DBLocal.TABLE_LOCATION, null, "time <= ?", new String[]{String.valueOf(time)}, null, null, "time DESC", "1");
        if (cursor.moveToFirst()) {
            latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
            longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        return new LatLng(latitude,longitude);
    }
    
    public Iterable<LatLng> getRoutePositions(long time) {
        ArrayList<LatLng> positions = new ArrayList<>();
        
        Calendar startTime = Calendar.getInstance();
        startTime.setTimeInMillis(time);
        FormatsUtils.roundDayToStart(startTime);
        
        Calendar endTime = Calendar.getInstance();
        endTime.setTimeInMillis(time);
        FormatsUtils.roundDayToEnd(endTime);
    
        SQLiteDatabase db = DBHelper.getDatabaseReadable(ctx);
        if (db == null) return positions;
        Cursor cursor = db.query(DBLocal.TABLE_LOCATION, null, "time between ? and ?", new String[]{String.valueOf(startTime.getTimeInMillis()),String.valueOf(endTime.getTimeInMillis())}, null, null, "time ASC", null);
        while (cursor.moveToNext()) {
            LatLng latLng = new LatLng(cursor.getDouble(cursor.getColumnIndex("latitude")),cursor.getDouble(cursor.getColumnIndex("longitude")));
            positions.add(latLng);
        }
        cursor.close();
        DBHelper.closeDatabase(db);

        if (positions.size() == 0) {
            positions.add(getStartRoutePosition(time));
        }
        
        return positions;
    }
}
