package by.ingman.sevenlis.ice_v3.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import by.ingman.sevenlis.ice_v3.R;

public class SettingsUtils {
    public static final String APP_FOLDER = "iceV3";
    
    public static final String LOCATION_TRACKING_TYPE_ALWAYS = "always";
    public static final String LOCATION_TRACKING_TYPE_PERIOD = "periodically";
    
    public static final String PREF_LOCATION_TRACKING_TYPE = "locationTrackingTypeKey";
    public static final String PREF_LOCATION_TRACKING_INTERVAL = "locationTrackingFrequency";
    
    public static final String PREF_LOCATION_TRACKING_PROVIDERS = "locationTrackingProvidersKey";
    
    public static final String LOCATION_TRACKING_PROVIDER_GPS = "gps";
    public static final String LOCATION_TRACKING_PROVIDER_NETWORK = "network";
    public static final String LOCATION_TRACKING_PROVIDER_PASSIVE = "passive";
    
    private static final String PREF_UPDATE_IN_PROGRESS = "PREF_UPDATE_IN_PROGRESS";

    private static final String PREF_USER_1C_NAME = "user1cName";
    private static final String PREF_MANAGER_CODE = "manager_code_pref_key";
    private static final String PREF_DEFAULT_STOREHOUSE_CODE = "storehouseDefaultCode";
    private static final String PREF_NOTIFICATIONS_ENABLED = "updateNotificationsEnabled";
    private static final String PREF_DATA_UPDATE_INTERVAL = "exchangeFrequency";
    private static final String PREF_ORDER_DAYS_AHEAD = "orderDaysAhead";
    private static final String PREF_ORDER_LOG_DEPTH = "orderLogDepth";
    private static final String PREF_ITEM_SEARCH_INPUT_NUMERIC = "itemSearchInputNumeric";
    private static final String PREF_EXCHANGE_SHUTDOWN_ON_EXIT = "exchangeShutdownOnExit";
    private static final String PREF_LOCATION_TRACKING_SHUTDOWN_ON_EXIT = "locationTrackingShutdownOnExit";
    
    private static final String PREF_REMOTE_DB_HOST = "host";
    private static final String PREF_REMOTE_DB_PORT = "port";
    private static final String PREF_REMOTE_DB_NAME = "baseName";
    private static final String PREF_REMOTE_DB_USER = "usernameDB";
    private static final String PREF_REMOTE_DB_PASS = "passwordDB";
    private static final String PREF_REMOTE_DB_INST = "instance";
    
    public static class Runtime {
        public static boolean getUpdateInProgress(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_UPDATE_IN_PROGRESS, false);
        }
        
        public static void setUpdateInProgress(Context ctx, boolean inProgress) {
            PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(PREF_UPDATE_IN_PROGRESS, inProgress).apply();
        }
    }
    
    public static class Settings {
        public static String getUser1cName(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_USER_1C_NAME, ctx.getResources().getString(R.string.pref_user_1c_name_default));
        }

        public static String getManagerCode(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_MANAGER_CODE, "");
        }

        public static String[] getStorehousesCodes(Context ctx) {
            String defCode = PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_DEFAULT_STOREHOUSE_CODE, ctx.getResources().getString(R.string.pref_storehouse_code_default));
            return Objects.requireNonNull(defCode).split(",");
        }

        public static String getDefaultStoreHouseCode(Context ctx) {
            String[] defCodes = getStorehousesCodes(ctx);
            return defCodes[0];
        }
        
        public static boolean getNotificationsEnabled(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_NOTIFICATIONS_ENABLED, true);
        }
        
        public static int getDataUpdateInterval(Context ctx) {
            return Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_DATA_UPDATE_INTERVAL, "30")));
        }
    
        public static int getLocationTrackingInterval(Context ctx) {
            return Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_LOCATION_TRACKING_INTERVAL, "10")));
        }
    
        public static String getLocationTrackingType(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_LOCATION_TRACKING_TYPE, LOCATION_TRACKING_TYPE_ALWAYS);
        }
        
        public static Set<String> getLocationTrackingProviders(Context ctx) {
            HashSet<String> defaults = new HashSet<>();
            defaults.add(LOCATION_TRACKING_PROVIDER_GPS);
            return PreferenceManager.getDefaultSharedPreferences(ctx).getStringSet(PREF_LOCATION_TRACKING_PROVIDERS, defaults);
        }
    
        public static boolean getLocationTrackingShutdownOnExit(Context ctx) {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_LOCATION_TRACKING_SHUTDOWN_ON_EXIT, false);
        }
        
        public static int getOrderDaysAhead(Context ctx) {
            return Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_ORDER_DAYS_AHEAD, "10")));
        }
        
        public static int getOrderLogDepth(Context ctx) {
            return Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_ORDER_LOG_DEPTH, "10")));
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
