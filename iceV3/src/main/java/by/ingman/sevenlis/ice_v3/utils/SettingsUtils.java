package by.ingman.sevenlis.ice_v3.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import by.ingman.sevenlis.ice_v3.R;

public class SettingsUtils {
    public static final String APP_FOLDER = "iceV3";

    private static final String PREF_LAST_UPDATE_DATE = "PREF_LAST_UPDATE_DATE";
    private static final String PREF_UPDATE_IN_PROGRESS = "PREF_UPDATE_IN_PROGRESS";
    private static final String PREF_PRODUCTS_IN_SELECT = "PREF_PRODUCTS_IN_SELECT";

    private static final String PREF_MANAGER_NAME = "managerName";
    private static final String PREF_DEFAULT_STOREHOUSE_CODE = "storehouseDefaultCode";
    private static final String PREF_NOTIFICATIONS_ENABLED = "updateNotificationsEnabled";
    private static final String PREF_DATA_UPDATE_INTERVAL = "exchangeFrequency";
    private static final String PREF_APK_UPDATE_URL = "apkUpdateUrl";
    private static final String PREF_ORDER_DAYS_AHEAD = "orderDaysAhead";
    private static final String PREF_ORDER_LOG_DEPTH = "orderLogDepth";
    private static final String PREF_ITEM_SEARCH_INPUT_NUMERIC = "itemSearchInputNumeric";
    private static final String PREF_EXCHANGE_SHUTDOWN_ON_EXIT = "exchangeShutdownOnExit";

    private static final String PREF_REMOTE_DB_HOST = "host";
    private static final String PREF_REMOTE_DB_PORT = "port";
    private static final String PREF_REMOTE_DB_NAME = "baseName";
    private static final String PREF_REMOTE_DB_USER = "usernameDB";
    private static final String PREF_REMOTE_DB_PASS = "passwordDB";
    private static final String PREF_REMOTE_DB_INST = "instance";

    public static class Runtime {
        public static long getLastUpdateDate(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getLong(PREF_LAST_UPDATE_DATE, 0);
        }

        public static void setLastUpdateDate(Context ctx, long date) {
            PreferenceManager.getDefaultSharedPreferences(ctx).edit().putLong(PREF_LAST_UPDATE_DATE, date).apply();
        }

        public static boolean getUpdateInProgress(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_UPDATE_IN_PROGRESS, false);
        }

        public static void setUpdateInProgress(Context ctx, boolean inProgress) {
            PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(PREF_UPDATE_IN_PROGRESS, inProgress).apply();
        }

        public static void setProductsInSelect(Context ctx, boolean inSelect) {
            PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(PREF_PRODUCTS_IN_SELECT, inSelect).apply();
        }

        public static boolean getProductsInSelect(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_PRODUCTS_IN_SELECT, false);
        }
    }

    public static class Settings {
        public static String getManagerName(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_MANAGER_NAME, ctx.getResources().getString(R.string.pref_manager_name_default));
        }

        public static String getDefaultStoreHouseCode(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_DEFAULT_STOREHOUSE_CODE, ctx.getResources().getString(R.string.pref_storehouse_code_default));
        }

        public static boolean getNotificationsEnabled(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_NOTIFICATIONS_ENABLED, true);
        }

        public static int getDataUpdateInterval(Context ctx) {
            return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_DATA_UPDATE_INTERVAL, "30"));
        }

        public static String getApkUpdateUrl(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_APK_UPDATE_URL, "http://ingman.by/agent/iceV3.apk");
        }

        public static int getOrderDaysAhead(Context ctx) {
            return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_ORDER_DAYS_AHEAD, "10"));
        }
    
        public static int getOrderLogDepth(Context ctx) {
            return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_ORDER_LOG_DEPTH, "30"));
        }
    
        public static boolean getItemSearchInputTypeNumeric(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_ITEM_SEARCH_INPUT_NUMERIC, true);
        }

        public static boolean getExchangeShutdownOnExit(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_EXCHANGE_SHUTDOWN_ON_EXIT, false);
        }
    }

    public static class RemoteDB {
        public static String getHost(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REMOTE_DB_HOST, "212.98.187.177");
        }

        public static String getInstance(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REMOTE_DB_INST, "");
        }

        public static String getPort(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REMOTE_DB_PORT, "41433");
        }

        public static String getDBName(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REMOTE_DB_NAME, "ingmanXchange");
        }

        public static String getUserName(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REMOTE_DB_USER, "androidXchange");
        }

        public static String getPassword(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REMOTE_DB_PASS, "androidXchangeIngman759153!");
        }
    }

}
