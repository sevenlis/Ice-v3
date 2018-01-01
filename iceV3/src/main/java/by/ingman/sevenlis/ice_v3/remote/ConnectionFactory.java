package by.ingman.sevenlis.ice_v3.remote;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;

import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class ConnectionFactory {
    public ConnectionFactory() {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private boolean isConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        return ni != null && ni.isConnected();
    }
    
    public Connection getConnection(Context ctx) throws SQLException {
        if (!isConnected(ctx)) return null;
    
        String user = SettingsUtils.RemoteDB.getUserName(ctx);
        String password = SettingsUtils.RemoteDB.getPassword(ctx);
        
        Connection connection = DriverManager.getConnection(getConnectionURL(ctx), user, password);
        connection.setAutoCommit(false);
        
        return connection;
    }
    
    @NonNull
    private String getConnectionURL(Context ctx) {
        String urlFormat;
        urlFormat = "jdbc:jtds:sqlserver://{0}:{1}/{2}";
        String instance = SettingsUtils.RemoteDB.getInstance(ctx);
        if (!instance.isEmpty()) {
            urlFormat = "jdbc:jtds:sqlserver://{0}:{1}/{2};instance={3}";
        }
        String host = SettingsUtils.RemoteDB.getHost(ctx);
        String port = SettingsUtils.RemoteDB.getPort(ctx);
        String baseName = SettingsUtils.RemoteDB.getDBName(ctx);
        
        return MessageFormat.format(urlFormat, host, port, baseName, instance);
    }
}
