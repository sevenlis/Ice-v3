package by.ingman.sevenlis.ice_v3.remote;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

public class FTPClientConnector {
    private static FTPClient ftpClient;
    private static final String FTP_SERVER  = "ftp.ingman.by";
    private static final int FTP_PORT       = 21;
    private static final String FTP_USER    = "ftpingmanby@ftp.ingman.by";
    private static final String FTP_PASS    = "S4IJs3BtNVfw";

    private static FTPClient getClient() {
        if (ftpClient == null)
            ftpClient = new FTPClient();
        return ftpClient;
    }

    public static FTPClient getFtpClient() {
        if (!getClient().isConnected())
            connectClient();
        return getClient();
    }

    private static void connectClient() {
        try {
            if (!getClient().isConnected()) {
                getClient().connect(FTP_SERVER,FTP_PORT);
                getClient().login(FTP_USER,FTP_PASS);
                getClient().enterLocalPassiveMode();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void disconnectClient() {
        if (getClient().isConnected()) {
            try {
                getClient().disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
