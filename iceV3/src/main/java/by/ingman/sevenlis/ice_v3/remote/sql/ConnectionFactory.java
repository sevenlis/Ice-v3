package by.ingman.sevenlis.ice_v3.remote.sql;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.text.MessageFormat;

import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class ConnectionFactory {
    private Context ctx;

    public ConnectionFactory(Context context) {
        this.ctx = context;

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    public Connection getConnection() {
        if (!isConnected()) return null;

        String user     = SettingsUtils.RemoteDB.getUserName(ctx);
        String password = SettingsUtils.RemoteDB.getPassword(ctx);

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(getConnectionURL(), user, password);
            connection.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    @NonNull
    private String getConnectionURL() {
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
