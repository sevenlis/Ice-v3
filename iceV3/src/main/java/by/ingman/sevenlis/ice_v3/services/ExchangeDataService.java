package by.ingman.sevenlis.ice_v3.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import by.ingman.sevenlis.ice_v3.classes.Answer;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.local.DBHelper;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.remote.ConnectionFactory;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.NotificationsUtil;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class ExchangeDataService extends IntentService {
    public static final String CHANNEL = "by.ingman.sevenlis.ice_v3." + ExchangeDataService.class.getSimpleName() + ".broadcastChannel";
    public static final String CHANNEL_ORDERS_UPDATES = "by.ingman.sevenlis.ice_v3." + ExchangeDataService.class.getSimpleName() + ".broadcastOrdersUpdatesChannel";
    public static final String MESSAGE_ON_BROADCAST_KEY = ".ExchangeDataService.message_on_broadcast_key";
    private static int VERSION = 0;
    private static boolean isConnected;
    private DBLocal dbLocal;
    private NotificationsUtil notifUtils;

    public ExchangeDataService() {
        super(ExchangeDataService.class.getSimpleName());
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        this.dbLocal = new DBLocal(this);
        isConnected = isConnect();
        notifUtils = new NotificationsUtil(this);
        try {
            VERSION = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        SettingsUtils.Runtime.setUpdateInProgress(this, false);
    }
    
    private boolean isConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        return ni != null && ni.isConnected();
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        if (isConnected) doExchangeData(intent);
    }

    private void exchangeOrdersData() {
        SettingsUtils.Runtime.setUpdateInProgress(this, true);
        try {
            sentUnsentOrders();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            receiveAnswers();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SettingsUtils.Runtime.setUpdateInProgress(this, false);
    }

    private void exchangeReferences() {
        SettingsUtils.Runtime.setUpdateInProgress(this, true);
        try {
            updateRests();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            updateDebts();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            updateClients();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        SettingsUtils.Runtime.setUpdateInProgress(this, false);
    }
    
    private void doExchangeData(Intent intent) {

        exchangeOrdersData();

        if (intent.getAction() != null)
            if (intent.getAction().equals("UPDATE-ORDERS-ONLY")) return;

        exchangeReferences();

    }
    
    private void sendBroadcastMessage(String channel, String messageBroadcast) {
        Intent intent = new Intent(channel);
        intent.putExtra(MESSAGE_ON_BROADCAST_KEY, messageBroadcast);
        
        sendBroadcast(intent);
    }
    
    private void updateDebts() throws SQLException {
        Connection conn;
        String messageOnBroadcast = "";
        
        ArrayList<ContentValues> cvList = new ArrayList<>();
        
        long internalDate = 0;
        SQLiteDatabase dbr = DBHelper.getDatabaseReadable(this);
        Cursor cursor = dbr.query(true, DBLocal.TABLE_DEBTS, new String[]{"date_unload"}, null, null, null, null, null, "1");
        if (cursor.moveToFirst()) {
            internalDate = cursor.getLong(cursor.getColumnIndex("date_unload"));
        }
        cursor.close();
        DBHelper.closeDatabase(dbr);
        
        long externalDate = 0;
        conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT TOP (1) datetime_unload FROM debts");
                ResultSet rs = stat.executeQuery();
                
                if (rs != null && rs.next()) {
                    externalDate = rs.getTimestamp("datetime_unload").getTime();
                }
            } finally {
                try {
                    if (!conn.isClosed()) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    messageOnBroadcast += "Error closing connection to remote DB.";
                }
            }
        } else {
            messageOnBroadcast += "Connection to remote DB is null.";
        }
        
        if (internalDate >= externalDate) return;
        
        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
        
        conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM debts ORDER BY name_k, code_k");
                ResultSet rs = stat.executeQuery();
                while (rs != null && rs.next()) {
                    ContentValues cv = new ContentValues();
                    cv.put("code_k", rs.getString("code_k"));
                    cv.put("name_k", rs.getString("name_k"));
                    cv.put("code_mk", rs.getString("code_mk"));
                    cv.put("rating", rs.getString("rating"));
                    cv.put("debt", rs.getDouble("debt"));
                    cv.put("overdue", rs.getDouble("overdue"));
                    cv.put("date_unload", rs.getTimestamp("datetime_unload").getTime());
                    cv.put("search_uppercase", rs.getString("name_k").toUpperCase());
                    cvList.add(cv);
                }
                messageOnBroadcast += "Обновление таблицы задолженностей контрагентов завершено.";
            } finally {
                try {
                    if (!conn.isClosed()) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    messageOnBroadcast += "Error closing connection to remote DB.";
                }
            }
        } else {
            messageOnBroadcast += "Connection to remote DB is null.";
        }
        
        SQLiteDatabase dbw = DBHelper.getDatabaseReadable(this);
        try {
            dbw.beginTransactionNonExclusive();
            dbw.delete(DBLocal.TABLE_DEBTS, null, null);
            
            for (ContentValues cv : cvList) {
                dbw.insert(DBLocal.TABLE_DEBTS, null, cv);
            }
            dbw.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_DEBTS + " in local DB.";
        } finally {
            dbw.endTransaction();
        }
        DBHelper.closeDatabase(dbw);
        
        sendBroadcastMessage(CHANNEL, messageOnBroadcast);
        
        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
    }
    
    private void updateRests() throws SQLException {
        Connection conn;
        String messageOnBroadcast = "";
        
        ArrayList<ContentValues> cvList = new ArrayList<>();
        
        long internalDate = 0;
        SQLiteDatabase dbr = DBHelper.getDatabaseReadable(this);
        Cursor cursor = dbr.query(true, DBLocal.TABLE_RESTS, new String[]{"date_unload"}, null, null, null, null, null, "1");
        if (cursor.moveToFirst()) {
            internalDate = cursor.getLong(cursor.getColumnIndex("date_unload"));
        }
        cursor.close();
        DBHelper.closeDatabase(dbr);
        
        long externalDate = 0L;
        conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT TOP (1) datetime_unload FROM rests");
                ResultSet rs = stat.executeQuery();
                
                if (rs != null && rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("datetime_unload");
                    if (timestamp != null)
                        externalDate = timestamp.getTime();
                }
            } finally {
                try {
                    if (!conn.isClosed()) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    messageOnBroadcast += "Error closing connection to remote DB.";
                }
            }
        } else {
            messageOnBroadcast += "Connection to remote DB is null.";
        }
        
        if (internalDate >= externalDate) return;
        
        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
        
        conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM rests ORDER BY code_p, name_p");
                ResultSet rs = stat.executeQuery();
                while (rs != null && rs.next()) {
                    ContentValues cv = new ContentValues();
                    cv.put("code_s", rs.getString("code_s"));
                    cv.put("name_s", rs.getString("name_s"));
                    cv.put("code_p", rs.getString("code_p"));
                    cv.put("name_p", rs.getString("name_p"));
                    cv.put("packs", rs.getDouble("packs"));
                    cv.put("amount", rs.getDouble("amount"));
                    cv.put("price", rs.getDouble("price"));
                    cv.put("gross_weight", rs.getDouble("gross_weight"));
                    cv.put("amt_in_pack", rs.getDouble("amt_in_pack"));
                    cv.put("date_unload", rs.getTimestamp("datetime_unload").getTime());
                    cv.put("search_uppercase", rs.getString("name_p").toUpperCase());
                    cvList.add(cv);
                }
                messageOnBroadcast += "Обновление таблицы остатков товаров завершено.";
            } finally {
                try {
                    if (!conn.isClosed()) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    messageOnBroadcast += "Error closing connection to remote DB.";
                }
            }
        } else {
            messageOnBroadcast += "Connection to remote DB is null.";
        }
        
        SQLiteDatabase dbw = DBHelper.getDatabaseReadable(this);
        try {
            dbw.beginTransactionNonExclusive();
            dbw.delete(DBLocal.TABLE_RESTS, null, null);
            
            for (ContentValues cv : cvList) {
                dbw.insert(DBLocal.TABLE_RESTS, null, cv);
            }
            dbw.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_RESTS + " in local DB.";
        } finally {
            dbw.endTransaction();
        }
        DBHelper.closeDatabase(dbw);
        
        sendBroadcastMessage(CHANNEL, messageOnBroadcast);
        
        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
    }

    private void updateAgreements() throws SQLException {
        List<ContentValues> cvList = new ArrayList<>();
        Connection conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM agreements ORDER BY name_k, code_k");
                ResultSet rs = stat.executeQuery();
                while (rs != null && rs.next()) {
                    ContentValues cv = new ContentValues();
                    cv.put("code_k", rs.getString("code_k"));
                    cv.put("name_k", rs.getString("name_k"));
                    cv.put("id", rs.getString("id"));
                    cv.put("name", rs.getString("name"));
                    cvList.add(cv);
                }
            } finally {
                try {
                    if (!conn.isClosed()) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        SQLiteDatabase dbw = DBHelper.getDatabaseReadable(this);
        try {
            dbw.beginTransactionNonExclusive();
            dbw.delete(DBLocal.TABLE_AGREEMENTS, null, null);

            for (ContentValues cv : cvList) {
                dbw.insert(DBLocal.TABLE_AGREEMENTS, null, cv);
            }
            dbw.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbw.endTransaction();
        }
        DBHelper.closeDatabase(dbw);
    }
    
    private void updateClients() throws SQLException {
        Connection conn;
        String messageOnBroadcast = "";
        
        ArrayList<ContentValues> cvList = new ArrayList<>();
        
        long internalDate = 0;
        SQLiteDatabase dbr = DBHelper.getDatabaseReadable(this);
        Cursor cursor = dbr.query(true, DBLocal.TABLE_CONTRAGENTS, new String[]{"date_unload"}, null, null, null, null, null, "1");
        if (cursor.moveToFirst()) {
            internalDate = cursor.getLong(cursor.getColumnIndex("date_unload"));
        }
        cursor.close();
        DBHelper.closeDatabase(dbr);
        
        long externalDate = 0;
        conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT TOP (1) datetime_unload FROM clients");
                ResultSet rs = stat.executeQuery();
                
                if (rs != null && rs.next()) {
                    externalDate = rs.getTimestamp("datetime_unload").getTime();
                }
            } finally {
                try {
                    if (!conn.isClosed()) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    messageOnBroadcast += "Error closing connection to remote DB.";
                }
            }
        } else {
            messageOnBroadcast += "Connection to remote DB is null.";
        }
        
        if (internalDate >= externalDate) return;
        
        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
        
        conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM clients ORDER BY name_k, code_k");
                ResultSet rs = stat.executeQuery();
                while (rs != null && rs.next()) {
                    ContentValues cv = new ContentValues();
                    cv.put("code_k", rs.getString("code_k"));
                    cv.put("name_k", rs.getString("name_k"));
                    cv.put("code_mk", rs.getString("code_mk"));
                    cv.put("name_mk", rs.getString("name_mk"));
                    cv.put("code_r", rs.getString("code_r"));
                    cv.put("name_r", rs.getString("name_r"));
                    cv.put("code_mr", rs.getString("code_mr"));
                    cv.put("name_mr", rs.getString("name_mr"));
                    cv.put("date_unload", rs.getTimestamp("datetime_unload").getTime());
                    cv.put("client_uppercase", rs.getString("name_k").toUpperCase());
                    cv.put("point_uppercase", rs.getString("name_r").toUpperCase());
                    cv.put("in_stop", rs.getInt("in_stop"));
                    cvList.add(cv);
                }
                messageOnBroadcast += "Обновление таблицы контрагентов и разгрузок завершено.";
            } finally {
                try {
                    if (!conn.isClosed()) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    messageOnBroadcast += "Error closing connection to remote DB.";
                }
            }
        } else {
            messageOnBroadcast += "Connection to remote DB is null.";
        }
        
        SQLiteDatabase dbw = DBHelper.getDatabaseReadable(this);
        try {
            dbw.beginTransactionNonExclusive();
            dbw.delete(DBLocal.TABLE_CONTRAGENTS, null, null);
            
            for (ContentValues cv : cvList) {
                dbw.insert(DBLocal.TABLE_CONTRAGENTS, null, cv);
            }
            dbw.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_CONTRAGENTS + " in local DB.";
        } finally {
            dbw.endTransaction();
        }
        DBHelper.closeDatabase(dbw);

        updateAgreements();
        
        sendBroadcastMessage(CHANNEL, messageOnBroadcast);
        
        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
    }

    private boolean deleteOrdersRemote(List<Order> orderList) {
        Connection conn;
        try {
            conn = new ConnectionFactory().getConnection(this);
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            return false;
        }
        if (conn == null) return false;

        String queryString = "DELETE FROM orders WHERE order_id IN (%s)";
        String statementString = String.format(queryString, FormatsUtils.getOrdersUidsForInClause(orderList));
        try {
            PreparedStatement statement = conn.prepareStatement(statementString);
            statement.executeUpdate();
            if (!conn.isClosed()) {
                conn.commit();
                conn.close();
                return true;
            }
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            return false;
        }
        return false;
    }
    
    private void sentUnsentOrders() throws SQLException {
        Connection conn;
        boolean success = false;
        int[] batchResults = new int[]{};
        String messageOnBroadcast = "";
        String managerName = SettingsUtils.Settings.getUser1cName(this);
        String managerCode = SettingsUtils.Settings.getManagerCode(this);
        
        List<Order> orderUnsentList = dbLocal.getUnsentOrdersList();
        if (orderUnsentList.size() == 0) return;
        if (!deleteOrdersRemote(orderUnsentList)) return;
        
        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ORDERS_ID);
        
        conn = new ConnectionFactory().getConnection(this);
        if (conn != null) {
            try {
                String statementString =
                        "INSERT INTO orders (" +
                                "order_id, " +
                                "name_m, " +
                                "order_date, " +
                                "is_advertising, " +
                                "code_k, " +
                                "name_k, " +
                                "code_r, " +
                                "name_r, " +
                                "code_s, " +
                                "name_s, " +
                                "code_p, " +
                                "name_p, " +
                                "amt_packs, " +
                                "amount, " +
                                "comments, " +
                                "in_datetime, " +
                                "adv_type, " +
                                "weight_p, " +
                                "price_p, " +
                                "num_in_pack_p, " +
                                "weight, " +
                                "price, " +
                                "summa, " +
                                "version, " +
                                "agreementId, " +
                                "order_type," +
                                "code_m" +
                                ")" +
                        " VALUES (" +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?, " +
                                "?" +
                                ")";
                PreparedStatement stat = conn.prepareStatement(statementString);
                
                for (Order order : orderUnsentList) {
                    for (OrderItem orderItem : order.orderItems) {
                        stat.setString(1, order.orderUid);
                        stat.setString(2, managerName);
                        stat.setTimestamp(3, new Timestamp(order.orderDate.getTime()));
                        stat.setInt(4, order.isAdvertising ? 1 : 0);
                        stat.setString(5, order.contragentCode);
                        stat.setString(6, order.contragentName);
                        stat.setString(7, order.pointCode);
                        stat.setString(8, order.pointName);
                        stat.setString(9, order.storehouseCode);
                        stat.setString(10, order.storehouseName);
                        stat.setString(11, orderItem.product.code);
                        stat.setString(12, orderItem.product.name);
                        stat.setDouble(13, orderItem.packs);
                        stat.setDouble(14, orderItem.quantity);
                        stat.setString(15, order.comment);
                        stat.setTimestamp(16, new Timestamp(new Date().getTime()));
                        stat.setInt(17, order.advType);
                        stat.setDouble(18, orderItem.product.weight);
                        stat.setDouble(19, orderItem.product.price);
                        stat.setDouble(20, orderItem.product.num_in_pack);
                        stat.setDouble(21, orderItem.product.weight * orderItem.quantity);
                        stat.setDouble(22, orderItem.product.price);
                        stat.setDouble(23, orderItem.product.price * orderItem.quantity);
                        stat.setInt(24, VERSION);
                        stat.setString(25, order.agreement.getId());
                        stat.setInt(26, order.orderType);
                        stat.setString(27, managerCode);
                        
                        stat.addBatch();
                    }
                }
                batchResults = stat.executeBatch();
                success = true;
                
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
                try {
                    if (!conn.isClosed()) {
                        conn.rollback();
                        conn.close();
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                throw e;
            } finally {
                if (isAnyBatchFailed(batchResults)) {
                    success = false;
                }
                try {
                    if (!conn.isClosed()) {
                        if (success) {
                            conn.commit();
                        } else {
                            conn.rollback();
                        }
                        conn.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ORDERS_ID);
            }
        }
        
        if (success) {
            messageOnBroadcast += "Все заявки отправлены";
            dbLocal.setUnsetOrdersAsSent(orderUnsentList);
        }
        
        sendBroadcastMessage(CHANNEL_ORDERS_UPDATES, messageOnBroadcast);
        
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ORDERS_ID);
    }
    
    private boolean isAnyBatchFailed(int[] results) {
        boolean isFailed = false;
        for (int r : results) {
            if (r == PreparedStatement.EXECUTE_FAILED) {
                isFailed = true;
                break;
            }
        }
        return isFailed;
    }
    
    public Answer getRemoteAnswer(String orderUid) throws Exception {
        Connection conn = new ConnectionFactory().getConnection(this);
        Answer answer = null;
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM results WHERE order_id = ?");
                stat.setString(1, orderUid);
                ResultSet rs = stat.executeQuery();
                if (rs.next()) {
                    answer = new Answer(orderUid);
                    answer.setDescription(rs.getString("description"));
                    answer.setUnloadTime(rs.getTimestamp("datetime_unload"));
                    answer.setResult(rs.getInt("result"));
                }
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return answer;
    }
    
    private void receiveAnswers() throws Exception {
        boolean answersReceived = false;
        List<String> orderUnansweredUids = dbLocal.getUnansweredOrdersUids();
        if (orderUnansweredUids.size() == 0) return;
        
        //notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ANSWERS_ID);
        
        for (String orderUid : orderUnansweredUids) {
            Answer answer = getRemoteAnswer(orderUid);
            if (answer != null) {
                dbLocal.updateAnswer(answer);
                answersReceived = true;
            }
        }
        
        //notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ANSWERS_ID);
        
        if (answersReceived) {
            sendBroadcastMessage(CHANNEL_ORDERS_UPDATES, "Ответы на заявки получены");
            notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ANSWERS_ID, "Получен ответ", "Получены ответы на отправленные заявки");
        }
        
    }
}
