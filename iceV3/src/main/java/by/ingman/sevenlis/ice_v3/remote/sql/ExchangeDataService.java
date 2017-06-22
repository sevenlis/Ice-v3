package by.ingman.sevenlis.ice_v3.remote.sql;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

import by.ingman.sevenlis.ice_v3.classes.Answer;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.local.sql.DBHelper;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.NotificationsUtil;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class ExchangeDataService extends IntentService {
    public static final String CHANNEL = "by.ingman.sevenlis.ice_v3." + ExchangeDataService.class.getSimpleName() + ".broadcastChannel";
    public static final String CHANNEL_ORDERS_UPDATES = "by.ingman.sevenlis.ice_v3." + ExchangeDataService.class.getSimpleName() + ".broadcastOrdersUpdatesChannel";
    public static final String MESSAGE_ON_BROADCAST_KEY = ".ExchangeDataService.message_on_broadcast_key";

    private DBHelper dbHelper;
    private DBLocal dbLocal;
    private NotificationsUtil notifUtils;

    private static boolean isConnected;

    public ExchangeDataService() { super(ExchangeDataService.class.getSimpleName()); }

    @Override
    public void onCreate() {
        super.onCreate();
        this.dbHelper = new DBHelper(this);
        this.dbLocal = new DBLocal(this);
        isConnected = isConnect();
        notifUtils = new NotificationsUtil(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SettingsUtils.Runtime.setUpdateInProgress(this,false);
    }

    private boolean isConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (isConnected) doExchangeData();
    }

    private void doExchangeData() {
        if (SettingsUtils.Runtime.getUpdateInProgress(this)) return;

        SettingsUtils.Runtime.setUpdateInProgress(this,true);
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
        SettingsUtils.Runtime.setUpdateInProgress(this,false);
    }

    private void sendBroadcastMessage(String channel, String messageBroadcast) {
        Intent intent = new Intent(channel);
        intent.putExtra(MESSAGE_ON_BROADCAST_KEY,messageBroadcast);

        sendBroadcast(intent);
    }

    private void updateDebts() throws SQLException {
        Connection conn;
        String messageOnBroadcast = "";

        ArrayList<ContentValues> cvList = new ArrayList<>();

        long internalDate = 0;
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        Cursor cursor = dbr.query(true,DBLocal.TABLE_DEBTS, new String[]{"date_unload"}, null, null, null, null, null, "1");
        if (cursor.moveToFirst()) {
            internalDate = cursor.getLong(cursor.getColumnIndex("date_unload"));
        }
        cursor.close();
        dbr.close();

        long externalDate = 0;
        conn = new ConnectionFactory(this).getConnection();
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT TOP 1 datetime_unload FROM debts");
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

        conn = new ConnectionFactory(this).getConnection();
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM debts ORDER BY name_k, code_k");
                ResultSet rs = stat.executeQuery();
                while (rs != null && rs.next()) {
                    ContentValues cv = new ContentValues();
                    cv.put("code_k",rs.getString("code_k"));
                    cv.put("name_k",rs.getString("name_k"));
                    cv.put("code_mk",rs.getString("code_mk"));
                    cv.put("rating",rs.getString("rating"));
                    cv.put("debt",rs.getDouble("debt"));
                    cv.put("overdue",rs.getDouble("overdue"));
                    cv.put("date_unload",rs.getTimestamp("datetime_unload").getTime());
                    cv.put("search_uppercase",rs.getString("name_k").toUpperCase());
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

        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        try {
            dbw.beginTransaction();
            dbw.delete(DBLocal.TABLE_DEBTS, null, null);

            for (ContentValues cv : cvList) {
                dbw.insert(DBLocal.TABLE_DEBTS,null,cv);
            }
            dbw.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_DEBTS + " in local DB.";
        } finally {
            dbw.endTransaction();
        }
        dbw.close();

        sendBroadcastMessage(CHANNEL, messageOnBroadcast);

        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
    }

    private void updateRests() throws SQLException {
        Connection conn;
        String messageOnBroadcast = "";

        ArrayList<ContentValues> cvList = new ArrayList<>();

        long internalDate = 0;
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        Cursor cursor = dbr.query(true,DBLocal.TABLE_RESTS, new String[]{"date_unload"}, null, null, null, null, null, "1");
        if (cursor.moveToFirst()) {
            internalDate = cursor.getLong(cursor.getColumnIndex("date_unload"));
        }
        cursor.close();
        dbr.close();

        long externalDate = 0;
        conn = new ConnectionFactory(this).getConnection();
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT TOP 1 datetime_unload FROM rests");
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

        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);

        conn = new ConnectionFactory(this).getConnection();
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM rests ORDER BY code_p, name_p");
                ResultSet rs = stat.executeQuery();
                while (rs != null && rs.next()) {
                    ContentValues cv = new ContentValues();
                    cv.put("code_s",rs.getString("code_s"));
                    cv.put("name_s",rs.getString("name_s"));
                    cv.put("code_p",rs.getString("code_p"));
                    cv.put("name_p",rs.getString("name_p"));
                    cv.put("packs",rs.getDouble("packs"));
                    cv.put("amount",rs.getDouble("amount"));
                    cv.put("price",rs.getDouble("price"));
                    cv.put("gross_weight",rs.getDouble("gross_weight"));
                    cv.put("amt_in_pack",rs.getDouble("amt_in_pack"));
                    cv.put("date_unload",rs.getTimestamp("datetime_unload").getTime());
                    cv.put("search_uppercase",rs.getString("name_p").toUpperCase());
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

        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        try {
            dbw.beginTransaction();
            dbw.delete(DBLocal.TABLE_RESTS, null, null);

            for (ContentValues cv : cvList) {
                dbw.insert(DBLocal.TABLE_RESTS,null,cv);
            }
            dbw.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_RESTS + " in local DB.";
        } finally {
            dbw.endTransaction();
        }
        dbw.close();

        sendBroadcastMessage(CHANNEL, messageOnBroadcast);

        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
    }

    private void updateClients() throws SQLException {
        Connection conn;
        String messageOnBroadcast = "";

        ArrayList<ContentValues> cvList = new ArrayList<>();

        long internalDate = 0;
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        Cursor cursor = dbr.query(true,DBLocal.TABLE_CONTRAGENTS, new String[]{"date_unload"}, null, null, null, null, null, "1");
        if (cursor.moveToFirst()) {
            internalDate = cursor.getLong(cursor.getColumnIndex("date_unload"));
        }
        cursor.close();
        dbr.close();

        long externalDate = 0;
        conn = new ConnectionFactory(this).getConnection();
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT TOP 1 datetime_unload FROM clients");
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

        conn = new ConnectionFactory(this).getConnection();
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM clients ORDER BY name_k, code_k");
                ResultSet rs = stat.executeQuery();
                while (rs != null && rs.next()) {
                    ContentValues cv = new ContentValues();
                    cv.put("code_k",rs.getString("code_k"));
                    cv.put("name_k",rs.getString("name_k"));
                    cv.put("code_mk",rs.getString("code_mk"));
                    cv.put("name_mk",rs.getString("name_mk"));
                    cv.put("code_r",rs.getString("code_r"));
                    cv.put("name_r",rs.getString("name_r"));
                    cv.put("code_mr",rs.getString("code_mr"));
                    cv.put("name_mr",rs.getString("name_mr"));
                    cv.put("date_unload",rs.getTimestamp("datetime_unload").getTime());
                    cv.put("client_uppercase",rs.getString("name_k").toUpperCase());
                    cv.put("point_uppercase",rs.getString("name_r").toUpperCase());
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

        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        try {
            dbw.beginTransaction();
            dbw.delete(DBLocal.TABLE_CONTRAGENTS, null, null);

            for (ContentValues cv : cvList) {
                dbw.insert(DBLocal.TABLE_CONTRAGENTS,null,cv);
            }
            dbw.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_CONTRAGENTS + " in local DB.";
        } finally {
            dbw.endTransaction();
        }
        dbw.close();

        sendBroadcastMessage(CHANNEL, messageOnBroadcast);

        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
    }

    private void sentUnsentOrders() throws SQLException {
        Connection conn;
        boolean success = false;
        int[] batchResults = new int[]{};
        String messageOnBroadcast = "";
        String managerName = SettingsUtils.Settings.getManagerName(this);

        ArrayList<Order> orderUnsentList = dbLocal.getUnsentOrdersList();
        if (orderUnsentList.size() == 0) return;

        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ORDERS_ID);

        conn = new ConnectionFactory(this).getConnection();
        if (conn != null) {
            try {
                String statementString = "INSERT INTO orders (order_id, name_m, order_date, is_advertising, code_k, name_k, code_r, name_r, code_s, name_s, code_p, name_p, amt_packs, amount, comments, in_datetime, adv_type) " +
                                                     "VALUES (?,        ?,      ?,          ?,              ?,      ?,      ?,      ?,      ?,      ?,      ?,      ?,      ?,         ?,      ?,        ?,           ?)";
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

                        stat.addBatch();
                    }
                }
                batchResults = stat.executeBatch();
                success = true;

            } catch (Exception e) {
                e.printStackTrace();
                success = false;
                try {
                    if (conn.isClosed()) {
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
            }
        }

        if (success) {
            messageOnBroadcast += "Все не отправленные заявки отправлены";
            dbLocal.setUnsetOrdersAsSent(orderUnsentList);
        }

        sendBroadcastMessage(CHANNEL_ORDERS_UPDATES, messageOnBroadcast);

        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_ORDERS_ID);
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
        Connection conn = new ConnectionFactory(this).getConnection();
        Answer answer = null;
        if (conn != null) {
            try {
                PreparedStatement stat = conn.prepareStatement("SELECT * FROM results WHERE order_id = ?");
                stat.setString(1, orderUid);
                ResultSet rs = stat.executeQuery();
                if (rs.next()) {
                    answer = new Answer();
                    answer.setOrderId(rs.getString("order_id"));
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
        ArrayList<String> orderUnansweredUids = dbLocal.getUnansweredOrdersUids();
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
