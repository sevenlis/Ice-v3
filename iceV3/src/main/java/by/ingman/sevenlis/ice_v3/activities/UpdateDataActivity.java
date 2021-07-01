package by.ingman.sevenlis.ice_v3.activities;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import by.ingman.sevenlis.ice_v3.BuildConfig;
import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.local.DBHelper;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.remote.ConnectionFactory;
import by.ingman.sevenlis.ice_v3.remote.FTPClientConnector;
import by.ingman.sevenlis.ice_v3.services.UpdateDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.GenericFileProvider;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class UpdateDataActivity extends AppCompatActivity {
    static ProgressDialog progressDialog;
    private Context context;
    private Handler mHandler;
    private Button pressedButton;
    private int[] pressedButtons = new int[]{-1,-1,-1};
    private boolean[] pressedButtonsStates = new boolean[]{true,true,true};

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(UpdateDataService.CHANNEL)) {
                if (intent.getExtras() != null) {
                    if (intent.getExtras().getInt(UpdateDataService.EXTRA_ACTION_KEY) == UpdateDataService.EXTRA_ACTION_SEND_MESSAGE) {
                        String messageOnBroadcast = intent.getExtras().getString(UpdateDataService.MESSAGE_ON_BROADCAST_KEY);
                        Toast.makeText(context, messageOnBroadcast, Toast.LENGTH_SHORT).show();
                        //((TextView) findViewById(R.id.textViewInfo)).setText(messageOnBroadcast);
                        
                        pressedButton = findViewById(intent.getExtras().getInt(UpdateDataService.EXTRA_UPDATE_ALL_CLIENTS_BUTTON_ID_KEY));
                        if (pressedButton != null) {
                            pressedButton.setEnabled(true);
                            pressedButtons[0] = -1;
                            pressedButtonsStates[0] = true;
                        }

                        pressedButton = findViewById(intent.getExtras().getInt(UpdateDataService.EXTRA_UPDATE_ALL_DEBTS_BUTTON_ID_KEY));
                        if (pressedButton != null) {
                            pressedButton.setEnabled(true);
                            pressedButtons[1] = -1;
                            pressedButtonsStates[1] = true;
                        }

                        pressedButton = findViewById(intent.getExtras().getInt(UpdateDataService.EXTRA_UPDATE_ALL_RESTS_BUTTON_ID_KEY));
                        if (pressedButton != null) {
                            pressedButton.setEnabled(true);
                            pressedButtons[2] = -1;
                            pressedButtonsStates[2] = true;
                        }
                    }
                }
            }
        }
    };

    public Context getContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_data);
        
        context = this;
        registerReceiver(broadcastReceiver, new IntentFilter(UpdateDataService.CHANNEL));
        
        mHandler = new Handler();

        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);

        if (savedInstanceState != null) {
            pressedButton = findViewById(savedInstanceState.getInt("pressedButtonId"));
            if (pressedButton != null)
                pressedButton.setEnabled(savedInstanceState.getBoolean("pressedButtonState"));

            pressedButtons = savedInstanceState.getIntArray("pressedButtons");
            pressedButtonsStates = savedInstanceState.getBooleanArray("pressedButtonsStates");
            if (pressedButtons != null && pressedButtonsStates != null) {
                for (int i = 0; i <= 2; i++) {
                    int pressedButtonId = pressedButtons[i];
                    boolean pressedButtonState = pressedButtonsStates[i];
                    pressedButton = findViewById(pressedButtonId);
                    if (pressedButton != null) {
                        pressedButton.setEnabled(pressedButtonState);
                    }
                }
            }
        }

        if (Objects.equals(getIntent().getAction(), "START_APK_UPDATE")) {
            updateAPK(findViewById(R.id.buttonUpdateAPK));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pressedButton != null) {
            outState.putInt("pressedButtonId",pressedButton.getId());
            outState.putBoolean("pressedButtonState",pressedButton.isEnabled());
        }
        outState.putIntArray("pressedButtons",pressedButtons);
        outState.putBooleanArray("pressedButtonsStates",pressedButtonsStates);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    static private void setProgress(int progress, int maximum, String title) {
        progressDialog.setMessage(title);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
        if (progress == -1) {
            progressDialog.setIndeterminate(true);
        } else {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(maximum);
            progressDialog.setProgress(progress);
        }
    }

    private boolean isNotConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        return ni == null || !ni.isConnected();
    }
    
    public void buttonRestsOnClick(View view) {
        if (isNotConnected()) {
            Toast.makeText(context, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        pressedButton = findViewById(view.getId());
        pressedButton.setEnabled(false);
        pressedButtons[2] = view.getId();
        pressedButtonsStates[2] = false;
        Intent intent = new Intent(context, UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_RESTS_VALUE);
        intent.putExtra(UpdateDataService.EXTRA_UPDATE_ALL_RESTS_BUTTON_ID_KEY, pressedButton.getId());
        context.startService(intent);
    }
    
    public void buttonClientsOnClick(View view) {
        if (isNotConnected()) {
            Toast.makeText(context, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        pressedButton = findViewById(view.getId());
        pressedButton.setEnabled(false);
        pressedButtons[0] = view.getId();
        pressedButtonsStates[0] = false;
        Intent intent = new Intent(context, UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_CLIENTS_VALUE);
        intent.putExtra(UpdateDataService.EXTRA_UPDATE_ALL_CLIENTS_BUTTON_ID_KEY, pressedButton.getId());
        startService(intent);
    }
    
    public void buttonDebtsOnClick(View view) {
        if (isNotConnected()) {
            Toast.makeText(context, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        pressedButton = findViewById(view.getId());
        pressedButton.setEnabled(false);
        pressedButtons[1] = view.getId();
        pressedButtonsStates[1] = false;
        Intent intent = new Intent(context, UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_DEBTS_VALUE);
        intent.putExtra(UpdateDataService.EXTRA_UPDATE_ALL_DEBTS_BUTTON_ID_KEY, pressedButton.getId());
        startService(intent);
    }
    
    public void updateAPK(View view) {
        if (isNotConnected()) {
            Toast.makeText(context, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        //downloadAndInstallUpdateApk();
        new UpdateAPKTask(this).execute();
    }

    private void startUpdateApkIntent(File apkFile) {
        if (apkFile != null && apkFile.exists()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_INSTALL_PACKAGE);

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = GenericFileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".file.provider", apkFile);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                apkUri = Uri.fromFile(apkFile);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            intent.setDataAndType(apkUri, MimeTypeMap.getSingleton().getMimeTypeFromExtension("apk"));
            startActivity(intent);
        } else {
            Toast.makeText(context, "Файл APK не найден!", Toast.LENGTH_LONG).show();
        }
    }

    public void downloadAndInstallUpdateApk() {
        final File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "iceV3.apk");

        //Delete update file if exists
        if (apkFile.exists())
            if (!apkFile.delete())
                Toast.makeText(context, "Error deleting file.", Toast.LENGTH_SHORT).show();

        //get url of app on server
        String url = "http://ftp.ingman.by/iceV3/iceV3-debug.apk";

        //set download manager
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Загрузка обновления...");
        request.setTitle(getResources().getString(R.string.app_name));

        //set destination
        request.setDestinationUri(Uri.fromFile(apkFile));

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Objects.requireNonNull(manager).enqueue(request);

        //set BroadcastReceiver to install app when .apk is downloaded
        final BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                startUpdateApkIntent(apkFile);
                unregisterReceiver(this);
                finish();
            }
        };
        //register receiver for when .apk download is compete
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
    
    public void getOrdersFromRemote(View view) {
        if (isNotConnected()) {
            Toast.makeText(context, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        if (SettingsUtils.Settings.getManagerCode(context).equals("---") || SettingsUtils.Settings.getManagerCode(context).equals("")) {
            Toast.makeText(context, "Не указан код менеджера для загрузки заявок!", Toast.LENGTH_LONG).show();
            return;
        }
        new GetOrdersFromRemoteTask(this).execute();
    }
    
    static class GetOrdersFromRemoteTask extends AsyncTask<Void, Integer, Void> {
        WeakReference<UpdateDataActivity> weakReference;
        String managerName, managerCode;
        String progressDialogTitle;
        Handler mHandler;

        public GetOrdersFromRemoteTask(UpdateDataActivity updateDataActivity) {
            super();
            this.weakReference = new WeakReference<>(updateDataActivity);
            this.managerName = SettingsUtils.Settings.getUser1cName(getWeakContext()).toUpperCase();
            this.managerCode = SettingsUtils.Settings.getManagerCode(getWeakContext()).toUpperCase();
            this.mHandler = new Handler(Looper.getMainLooper());
        }

        private Context getWeakContext() {
            return weakReference.get().getContext();
        }

        @Override
        protected Void doInBackground(Void... params) {
            SettingsUtils.Runtime.setUpdateInProgress(getWeakContext(), true);
            try {
                getOrdersFromRemote();
                getAnswersFromRemote();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            SettingsUtils.Runtime.setUpdateInProgress(getWeakContext(), false);
            return null;
        }
        
        private void getOrdersFromRemote() throws SQLException {
            progressDialogTitle = "Получение заявок...";
            mHandler.post(() -> setProgress(-1, 0, progressDialogTitle));

            //int daysAhead = SettingsUtils.Settings.getOrderLogDepth(getWeakContext());
            Calendar cal = Calendar.getInstance();
            //cal.add(Calendar.DATE, -daysAhead);
            FormatsUtils.roundDayToStart(cal);
            String fDate = FormatsUtils.getDateFormatted(cal.getTime(), "yyyy-MM-dd HH:mm:ss.000");

            List<String> uidList = new DBLocal(getWeakContext()).getOrdersUids();

            int rsSize = 0, rsCount = 0;
            Connection connection = new ConnectionFactory().getConnection(getWeakContext());
            if (connection != null) {
                List<ContentValues> contentValuesList = new ArrayList<>();
                try {
                    //PreparedStatement stat_count = connection.prepareStatement("SELECT COUNT(*) as count_rs FROM orders WHERE (UPPER(LTRIM(RTRIM(name_m))) = '" + managerName + "' OR UPPER(LTRIM(RTRIM(code_m))) = '" + managerCode + "') AND order_date > CAST('" + fDate + "' as datetime)");
                    PreparedStatement stat_count = connection.prepareStatement("SELECT COUNT(*) as count_rs FROM orders WHERE (UPPER(LTRIM(RTRIM(code_m))) = '" + managerCode + "' AND order_date > CAST('" + fDate + "' as datetime))");
                    ResultSet rs_count = stat_count.executeQuery();
                    
                    if (rs_count.next()) {
                        rsSize = rs_count.getInt("count_rs");
                        publishProgress(rsCount, rsSize);
                    }

                    //PreparedStatement stat = connection.prepareStatement("SELECT * FROM orders WHERE (UPPER(LTRIM(RTRIM(name_m))) = '" + managerName + "' OR UPPER(LTRIM(RTRIM(code_m))) = '" + managerCode + "') AND order_date > CAST('" + fDate + "' as datetime) ORDER BY in_datetime");
                    PreparedStatement stat = connection.prepareStatement("SELECT * FROM orders WHERE (UPPER(LTRIM(RTRIM(code_m))) = '" + managerCode + "' AND order_date > CAST('" + fDate + "' as datetime)) ORDER BY in_datetime");
                    ResultSet rs = stat.executeQuery();
                    while (rs != null && rs.next()) {
                        ContentValues cv = new ContentValues();
                        cv.put("order_id",          rs.getString("order_id"));
                        cv.put("name_m",            rs.getString("name_m"));
                        cv.put("order_date",        rs.getTimestamp("order_date").getTime());
                        cv.put("is_advertising",    rs.getInt("is_advertising"));
                        cv.put("adv_type",          rs.getInt("adv_type"));
                        cv.put("order_type",        rs.getInt("order_type"));
                        cv.put("code_k",            rs.getString("code_k"));
                        cv.put("name_k",            rs.getString("name_k"));
                        cv.put("code_r",            rs.getString("code_r"));
                        cv.put("name_r",            rs.getString("name_r"));
                        cv.put("code_s",            rs.getString("code_s"));
                        cv.put("name_s",            rs.getString("name_s"));
                        cv.put("code_p",            rs.getString("code_p"));
                        cv.put("name_p",            rs.getString("name_p"));
                        cv.put("agreementId",       rs.getString("agreementId"));
                        cv.put("weight_p",          rs.getDouble("weight_p"));
                        cv.put("price_p",           rs.getDouble("price_p"));
                        cv.put("num_in_pack_p",     rs.getDouble("num_in_pack_p"));
                        cv.put("amount",            rs.getDouble("amount"));
                        cv.put("amt_packs",         rs.getDouble("amt_packs"));
                        cv.put("weight",            rs.getDouble("weight"));
                        cv.put("price",             rs.getDouble("price"));
                        cv.put("summa",             rs.getDouble("summa"));
                        cv.put("comments",          rs.getString("comments"));
                        cv.put("status",            2);
                        cv.put("processed",         1);
                        cv.put("sent",              1);
                        cv.put("date_unload",       rs.getTimestamp("in_datetime").getTime());

                        if (!uidList.contains(String.valueOf(cv.get("order_id"))))
                            contentValuesList.add(cv);

                        publishProgress(rsCount++, rsSize);
                    }
                } finally {
                    try {
                        if (!connection.isClosed()) connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                progressDialogTitle = "Запись полученных заявок...";
                mHandler.post(() -> setProgress(-1, 0, progressDialogTitle));
                rsSize = contentValuesList.size(); rsCount = 0;
                for (ContentValues cv: contentValuesList) {
                    putOrderToLocalDB(cv);
                    publishProgress(rsCount++, rsSize);
                }
            }
        }
        
        private void getAnswersFromRemote() throws SQLException {
            progressDialogTitle = "Получение ответов на загруженные заявки...";
            mHandler.post(() -> setProgress(-1, 0, progressDialogTitle));
            int daysAhead = SettingsUtils.Settings.getOrderLogDepth(getWeakContext());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -daysAhead);
            FormatsUtils.roundDayToStart(cal);
            String fDate = FormatsUtils.getDateFormatted(cal.getTime(), "yyyy-MM-dd HH:mm:ss.000");

            List<String> uidList = new DBLocal(getWeakContext()).getAnswersUids();
            
            int rsSize = 0, rsCount = 0;
            Connection connection = new ConnectionFactory().getConnection(getWeakContext());
            if (connection != null) {
                List<ContentValues> contentValuesList = new ArrayList<>();
                try {
                    //PreparedStatement stat_count = connection.prepareStatement("SELECT COUNT(*) AS count_rs FROM results AS R WHERE R.order_id in (SELECT DISTINCT O.order_id FROM orders AS O WHERE (UPPER(LTRIM(RTRIM(O.name_m))) = '" + managerName + "' OR UPPER(LTRIM(RTRIM(O.code_m))) = '" + managerCode + "') AND O.order_date > CAST('" + fDate + "' as datetime))");
                    PreparedStatement stat_count = connection.prepareStatement("SELECT COUNT(*) AS count_rs FROM results AS R WHERE R.order_id in (SELECT DISTINCT O.order_id FROM orders AS O WHERE (UPPER(LTRIM(RTRIM(O.code_m))) = '" + managerCode + "' AND O.order_date > CAST('" + fDate + "' as datetime)))");
                    ResultSet rs_count = stat_count.executeQuery();
                    
                    if (rs_count.next()) {
                        rsSize = rs_count.getInt("count_rs");
                        publishProgress(rsCount, rsSize);
                    }

                    //PreparedStatement stat = connection.prepareStatement("SELECT DISTINCT * FROM results AS R WHERE R.order_id in (SELECT DISTINCT O.order_id FROM orders AS O WHERE (UPPER(LTRIM(RTRIM(O.name_m))) = '" + managerName + "' OR UPPER(LTRIM(RTRIM(O.code_m))) = '" + managerCode + "') AND O.order_date > CAST('" + fDate + "' as datetime)) ORDER BY R.datetime_unload");
                    PreparedStatement stat = connection.prepareStatement("SELECT DISTINCT * FROM results AS R WHERE R.order_id in (SELECT DISTINCT O.order_id FROM orders AS O WHERE (UPPER(LTRIM(RTRIM(O.code_m))) = '" + managerCode + "' AND O.order_date > CAST('" + fDate + "' as datetime))) ORDER BY R.datetime_unload");
                    ResultSet rs = stat.executeQuery();
                    while (rs != null && rs.next()) {
                        ContentValues cv = new ContentValues();
                        cv.put("order_id",      rs.getString("order_id"));
                        cv.put("description",   rs.getString("description"));
                        cv.put("date_unload",   rs.getTimestamp("datetime_unload").getTime());
                        cv.put("result",        rs.getInt("result"));

                        if (!uidList.contains(String.valueOf(cv.get("order_id"))))
                            contentValuesList.add(cv);

                        publishProgress(rsCount++, rsSize);
                    }
                } finally {
                    try {
                        if (!connection.isClosed()) connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                progressDialogTitle = "Запись ответов на загруженные заявки...";
                mHandler.post(() -> setProgress(-1, 0, progressDialogTitle));
                rsSize = contentValuesList.size(); rsCount = 0;
                for (ContentValues cv : contentValuesList) {
                    putAnswerToLocalDB(cv);
                    publishProgress(rsCount++, rsSize);
                }
            }

        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0];
            int maximum = values[1];
            setProgress(progress, maximum, progressDialogTitle);
        }
        
        @Override
        protected void onPostExecute(Void aVoid) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            super.onPostExecute(aVoid);
        }
        
        private void putOrderToLocalDB(ContentValues cv) {
            if (!checkOrderEntry(cv)) {
                SQLiteDatabase db = DBHelper.getDatabaseWritable(weakReference.get().getApplicationContext());
                if (db == null) return;
                try {
                    db.beginTransactionNonExclusive();
                    db.insert("orders", null, cv);
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                }
                DBHelper.closeDatabase(db);
            }
        }
        
        private Boolean checkOrderEntry(ContentValues cv) {
            SQLiteDatabase db = DBHelper.getDatabaseReadable(weakReference.get().getApplicationContext());
            if (db == null) return false;
            String order_id = cv.getAsString("order_id");
            String code_p = cv.getAsString("code_p");
            Cursor cursor = db.query(true, "orders", null, "order_id = ? AND code_p = ?", new String[]{order_id, code_p}, null, null, null, "1");
            Boolean entryExist = cursor.moveToFirst();
            cursor.close();
            DBHelper.closeDatabase(db);
            return entryExist;
        }
        
        private void putAnswerToLocalDB(ContentValues cv) {
            if (!checkAnswerEntry(cv)) {
                SQLiteDatabase db = DBHelper.getDatabaseReadable(weakReference.get().getApplicationContext());
                if (db == null) return;
                try {
                    db.beginTransactionNonExclusive();
                    db.insert("answers", null, cv);
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                }
                DBHelper.closeDatabase(db);
            }
        }
        
        private Boolean checkAnswerEntry(ContentValues cv) {
            SQLiteDatabase db = DBHelper.getDatabaseReadable(weakReference.get().getApplicationContext());
            if (db == null) return false;
            String order_id = cv.getAsString("order_id");
            Cursor cursor = db.query(true, "answers", null, "order_id = ?", new String[]{order_id}, null, null, null, "1");
            Boolean entryExist = cursor.moveToFirst();
            cursor.close();
            DBHelper.closeDatabase(db);
            return entryExist;
        }
        
        private Product getProduct(String pCode, String pName) {
            Product product = new Product(pCode, pName, 0, 0, 0);
            SQLiteDatabase db = DBHelper.getDatabaseReadable(weakReference.get().getApplicationContext());
            if (db == null) return product;
            Cursor cursor = db.query(true, "rests", null, "code_p = ?", new String[]{pCode}, null, null, null, "1");
            if (cursor.moveToFirst()) {
                product.weight = cursor.getDouble(cursor.getColumnIndex("gross_weight"));
                product.price = cursor.getDouble(cursor.getColumnIndex("price"));
                product.num_in_pack = cursor.getDouble(cursor.getColumnIndex("amt_in_pack"));
            }
            cursor.close();
            DBHelper.closeDatabase(db);
            return product;
        }
    }

    static class UpdateAPKTask extends AsyncTask<Void, Integer, Void> {
        private final WeakReference<UpdateDataActivity> weakReference;
        private static final int batchSize = 2048;
        private static final int progressSizeDivider = 1024;
        private final File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "iceV3.apk");
        private String errorMessage = null;
        private String progressDialogTitle;
        private final Handler mHandler;

        public UpdateAPKTask(UpdateDataActivity updateDataActivity) {
            super();
            this.weakReference = new WeakReference<>(updateDataActivity);
            this.mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        protected Void doInBackground(Void... params) {
            SettingsUtils.Runtime.setUpdateInProgress(weakReference.get().context, true);

            progressDialogTitle = "Получение файла обновления...";
            mHandler.post(() -> setProgress(-1, 0, progressDialogTitle));

            FTPClient ftpClient = FTPClientConnector.getFtpClient();

            OutputStream output = null;
            InputStream input = null;
            try {
                // download the file
                boolean deleted = true;
                boolean created = true;
                if (apkFile.exists()) {
                    deleted = apkFile.delete();
                }
                if (deleted) {
                    created = apkFile.createNewFile();
                }
                if (!created) {
                    weakReference.get().mHandler.post(() -> Toast.makeText(weakReference.get().context, "Error creating file " + apkFile.getPath(), Toast.LENGTH_SHORT).show());
                    return null;
                }
                output = new FileOutputStream(apkFile);

                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

                String ftpFilePath = "/iceV3/iceV3-debug.apk";

                int fileLength = 0;
                FTPFile[] ftpFiles = ftpClient.listFiles(ftpFilePath);
                if (ftpFiles.length == 1 && ftpFiles[0].isFile()) {
                    fileLength = Long.valueOf(ftpFiles[0].getSize()).intValue();
                }

                publishProgress(0, fileLength);

                input = ftpClient.retrieveFileStream(ftpFilePath);

                byte[] data = new byte[batchSize];
                int count;
                int countTotal = 0;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    // publishing the progress....
                    if (fileLength > 0) { // only if total length is known
                        countTotal += count;
                        publishProgress(countTotal, fileLength);
                    }

                    if (count != 0)
                        output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = "Ошибка обновления " + e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                    //do nothing
                }
                FTPClientConnector.disconnectClient();
            }
            SettingsUtils.Runtime.setUpdateInProgress(weakReference.get().context, false);
            return null;
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0] / progressSizeDivider;
            int maximum = values[1] / progressSizeDivider;
            setProgress(progress, maximum, progressDialogTitle);
        }
        
        @Override
        protected void onPostExecute(Void aVoid) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            if (errorMessage != null) {
                Toast.makeText(weakReference.get().context, errorMessage, Toast.LENGTH_SHORT).show();
                errorMessage = null;
            } else {
                if (apkFile.exists()) {
                    //Toast.makeText(weakReference.get().ctx, "Обновление загружено в папку Загрузок", Toast.LENGTH_LONG).show();
                    weakReference.get().startUpdateApkIntent(apkFile);
                }
            }
        }
    }
    
}