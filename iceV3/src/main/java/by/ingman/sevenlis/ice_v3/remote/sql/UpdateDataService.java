package by.ingman.sevenlis.ice_v3.remote.sql;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import by.ingman.sevenlis.ice_v3.local.sql.DBHelper;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.NotificationsUtil;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class UpdateDataService extends IntentService {
    public UpdateDataService() {
        super(UpdateDataService.class.getSimpleName());
    }

    private DBHelper dbHelper;
    public static final String CHANNEL = "by.ingman.sevenlis.ice_v3." + UpdateDataService.class.getSimpleName()+".broadcastChannel";
    public static String messageOnBroadcast = "";

    public static final String EXTRA_ACTION_KEY = ".UpdateDataService.action_identify_key";
    public static final int EXTRA_UPDATE_ALL_CLIENTS_VALUE = 0;
    public static final int EXTRA_UPDATE_ALL_RESTS_VALUE = 1;
    public static final int EXTRA_UPDATE_ALL_DEBTS_VALUE = 2;

    public static final int EXTRA_ACTION_SEND_MESSAGE = 3;
    public static final String MESSAGE_ON_BROADCAST_KEY = ".UpdateDataService.message_on_broadcast_key";

    public static final String EXTRA_ACTION_COMPLETE_KEY = ".UpdateDataService.action_complete_key";

    private NotificationsUtil notifUtils;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(this);
        notifUtils = new NotificationsUtil(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SettingsUtils.Runtime.setUpdateInProgress(this,true);

        if (intent.getExtras() != null) {
            switch (intent.getExtras().getInt(EXTRA_ACTION_KEY)) {
                case EXTRA_UPDATE_ALL_CLIENTS_VALUE: {
                    try {
                        updateAllContragentsFromRemote();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } break;
                case EXTRA_UPDATE_ALL_RESTS_VALUE: {
                    try {
                        updateAllRestsFromRemote();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } break;
                case EXTRA_UPDATE_ALL_DEBTS_VALUE: {
                    try {
                        updateAllDebtsFromRemote();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } break;
            }
        }
        sendExtraOnResult();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SettingsUtils.Runtime.setUpdateInProgress(this,false);

        Intent extraIntent = new Intent(CHANNEL);
        extraIntent.putExtra(EXTRA_ACTION_COMPLETE_KEY,true);
        sendBroadcast(extraIntent);
    }

    private void sendExtraOnResult() {
        SettingsUtils.Runtime.setUpdateInProgress(this,false);
        Intent extraIntent = new Intent(CHANNEL);
        extraIntent.putExtra(EXTRA_ACTION_KEY,EXTRA_ACTION_SEND_MESSAGE);
        extraIntent.putExtra(MESSAGE_ON_BROADCAST_KEY,messageOnBroadcast);
        extraIntent.putExtra(EXTRA_ACTION_COMPLETE_KEY,true);
        sendBroadcast(extraIntent);
        messageOnBroadcast = "";
    }

    private void updateAllContragentsFromRemote() throws SQLException {
        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
    
        ArrayList<ContentValues> clientsList = new ArrayList<>();
        Connection conn = new ConnectionFactory(this).getConnection();
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
                    clientsList.add(cv);

                }
                messageOnBroadcast += "Обновление таблицы контрагентов завершено.";
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
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(DBLocal.TABLE_CONTRAGENTS, null, null);
            for (ContentValues cv : clientsList) {
                db.insert(DBLocal.TABLE_CONTRAGENTS,null,cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_CONTRAGENTS + " in local DB.";
        } finally {
            db.endTransaction();
        }
        db.close();

        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_CONTRAGENTS_ID);
    }

    private void updateAllDebtsFromRemote() throws SQLException {
        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
        
        ArrayList<ContentValues> debtsList = new ArrayList<>();
        Connection conn = new ConnectionFactory(this).getConnection();
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
                    debtsList.add(cv);
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

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(DBLocal.TABLE_DEBTS, null, null);

            for (ContentValues cv : debtsList) {
                db.insert(DBLocal.TABLE_DEBTS,null,cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_DEBTS + " in local DB.";
        } finally {
            db.endTransaction();
        }
        db.close();

        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_DEBTS_ID);
    }

    private void updateAllRestsFromRemote() throws SQLException {
        notifUtils.showUpdateProgressNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
        
        ArrayList<ContentValues> restsList = new ArrayList<>();
        Connection conn = new ConnectionFactory(this).getConnection();
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
                    restsList.add(cv);
                }
                messageOnBroadcast += "Обновление таблицы остатков номенклатуры завершено.";
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

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(DBLocal.TABLE_RESTS, null, null);

            for (ContentValues cv : restsList) {
                db.insert(DBLocal.TABLE_RESTS,null,cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            messageOnBroadcast += "Error updating " + DBLocal.TABLE_RESTS + " in local DB.";
        } finally {
            db.endTransaction();
        }
        db.close();

        notifUtils.dismissNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
        notifUtils.showUpdateCompleteNotification(NotificationsUtil.NOTIF_UPDATE_PROGRESS_PRODUCTS_ID);
    }
}
