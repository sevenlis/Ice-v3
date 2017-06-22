package by.ingman.sevenlis.ice_v3;

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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.local.sql.DBHelper;
import by.ingman.sevenlis.ice_v3.remote.sql.ConnectionFactory;
import by.ingman.sevenlis.ice_v3.remote.sql.UpdateDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class UpdateDataActivity extends AppCompatActivity {
    private Context ctx;
    private Handler mHandler;
    private ProgressDialog progressDialog;
    private Button pressedButton;
    
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UpdateDataService.CHANNEL)) {
                if (intent.getExtras() != null) {
                    if (intent.getExtras().getInt(UpdateDataService.EXTRA_ACTION_KEY) == UpdateDataService.EXTRA_ACTION_SEND_MESSAGE) {
                        String messageOnBroadcast = intent.getExtras().getString(UpdateDataService.MESSAGE_ON_BROADCAST_KEY);
                        Toast.makeText(context, messageOnBroadcast, Toast.LENGTH_SHORT).show();
                        ((TextView) findViewById(R.id.textViewInfo)).setText(messageOnBroadcast);
                        if (pressedButton != null) pressedButton.setEnabled(true);
                    }
                }
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_data);
        
        this.ctx = this;
        registerReceiver(broadcastReceiver, new IntentFilter(UpdateDataService.CHANNEL));
        
        mHandler = new Handler();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
    
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }
    
    public void buttonRestsOnClick(View view) {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        pressedButton = (Button) findViewById(view.getId());
        pressedButton.setEnabled(false);
        Intent intent = new Intent(ctx, UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_RESTS_VALUE);
        startService(intent);
    }
    
    public void buttonClientsOnClick(View view) throws Exception {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        pressedButton = (Button) findViewById(view.getId());
        pressedButton.setEnabled(false);
        Intent intent = new Intent(ctx, UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_CLIENTS_VALUE);
        startService(intent);
    }
    
    public void buttonDebtsOnClick(View view) {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        pressedButton = (Button) findViewById(view.getId());
        pressedButton.setEnabled(false);
        Intent intent = new Intent(ctx, UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_DEBTS_VALUE);
        startService(intent);
    }

    public void updateAPK(View view) {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        UpdateAPKTask updateAPKTask = new UpdateAPKTask();
        updateAPKTask.execute();
    }
    
    public void getOrdersFromRemote(View view) {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        GetOrdersFromRemoteTask getOrdersTask = new GetOrdersFromRemoteTask();
        getOrdersTask.execute();
    }
    
    private ProgressDialog showProgressDialog(String title) {
        if (progressDialog != null) progressDialog.dismiss();
        progressDialog = new ProgressDialog(ctx);
        progressDialog.setMessage(title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        return progressDialog;
    }
    
    private void setProgress(ProgressDialog progressDialog, int progress, int maximum) {
        if (progress == 0) {
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(maximum);
        }
        if (maximum != 0) {
            double percent = ((double) progress / (double) maximum) * 100;
            if ((int) percent % 2 == 0 && (int) percent != 0) {
                progressDialog.setProgress(maximum * (int) percent / 100);
            }
        }
    }
    
    class GetOrdersFromRemoteTask extends AsyncTask<Void, Integer, Void> {
        ProgressDialog progressDialog;
        String managerName = SettingsUtils.Settings.getManagerName(ctx).toUpperCase();
        SQLiteDatabase db = new DBHelper(ctx).getReadableDatabase();
        
        @Override
        protected Void doInBackground(Void... params) {
            try {
                getOrdersFromRemote();
                getAnswersFromRemote();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        private void getOrdersFromRemote() throws SQLException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog = showProgressDialog("Получение заявок...");
                }
            });
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -30);
            FormatsUtils.roundDayToStart(cal);
            String fDate = FormatsUtils.getDateFormatted(cal.getTime(), "yyyy-MM-dd HH:mm:ss.000");
            
            int rsSize = 0;
            int rsCount = 0;
            Connection connection = new ConnectionFactory(ctx).getConnection();
            if (connection != null) {
                try {
                    PreparedStatement stat_count = connection.prepareStatement("SELECT COUNT(*) as count_rs FROM orders WHERE UPPER(name_m) = '" + managerName + "' AND order_date > CAST('" + fDate + "' as datetime)");
                    ResultSet rs_count = stat_count.executeQuery();
                    
                    if (rs_count.next()) {
                        rsSize = rs_count.getInt("count_rs");
                        publishProgress(rsCount,rsSize);
                    }
                    
                    PreparedStatement stat = connection.prepareStatement("SELECT * FROM orders WHERE UPPER(name_m) = '" + managerName + "' AND order_date > CAST('" + fDate + "' as datetime) ORDER BY in_datetime");
                    ResultSet rs = stat.executeQuery();
                    while (rs != null && rs.next()) {
                        Product product = getProduct(rs.getString("code_p"), rs.getString("name_p"));
                        
                        ContentValues cv = new ContentValues();
                        cv.put("order_id", rs.getString("order_id"));
                        cv.put("name_m", rs.getString("name_m"));
                        cv.put("order_date", rs.getTimestamp("order_date").getTime());
                        cv.put("is_advertising", rs.getInt("is_advertising"));
                        cv.put("adv_type", rs.getInt("adv_type"));
                        cv.put("code_k", rs.getString("code_k"));
                        cv.put("name_k", rs.getString("name_k"));
                        cv.put("code_r", rs.getString("code_r"));
                        cv.put("name_r", rs.getString("name_r"));
                        cv.put("code_s", rs.getString("code_s"));
                        cv.put("name_s", rs.getString("name_s"));
                        cv.put("code_p", rs.getString("code_p"));
                        cv.put("name_p", rs.getString("name_p"));
                        cv.put("weight_p", product.weight);
                        cv.put("price_p", product.price);
                        cv.put("num_in_pack_p", product.num_in_pack);
                        cv.put("amount", rs.getDouble("amount"));
                        cv.put("amt_packs", rs.getDouble("amt_packs"));
                        cv.put("weight", rs.getDouble("amount") * product.weight);
                        cv.put("price", product.price);
                        cv.put("summa", rs.getDouble("amount") * product.price);
                        cv.put("comments", rs.getString("comments"));
                        cv.put("status", 2);
                        cv.put("processed", 1);
                        cv.put("sent", 1);
                        cv.put("date_unload", rs.getTimestamp("in_datetime").getTime());
                        
                        putOrderToLocalDB(cv);
                        
                        publishProgress(rsCount++, rsSize);
                    }
                } finally {
                    try {
                        if (!connection.isClosed()) connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        private void getAnswersFromRemote() throws SQLException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog = showProgressDialog("Получение ответов на загруженные заявки...");
                }
            });
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -30);
            FormatsUtils.roundDayToStart(cal);
            String fDate = FormatsUtils.getDateFormatted(cal.getTime(), "yyyy-MM-dd HH:mm:ss.000");
            
            int rsSize = 0;
            int rsCount = 0;
            Connection connection = new ConnectionFactory(ctx).getConnection();
            if (connection != null) {
                try {
                    PreparedStatement stat_count = connection.prepareStatement("SELECT COUNT(*) as count_rs FROM results WHERE datetime_unload > CAST('" + fDate + "' as datetime)");
                    ResultSet rs_count = stat_count.executeQuery();
                    
                    if (rs_count.next()) {
                        rsSize = rs_count.getInt("count_rs");
                        publishProgress(rsCount,rsSize);
                    }
                    
                    PreparedStatement stat = connection.prepareStatement("SELECT * FROM results WHERE datetime_unload > CAST('" + fDate + "' as datetime) ORDER BY datetime_unload");
                    ResultSet rs = stat.executeQuery();
                    while (rs != null && rs.next()) {
                        ContentValues cv = new ContentValues();
                        cv.put("order_id", rs.getString("order_id"));
                        cv.put("description", rs.getString("description"));
                        cv.put("date_unload", rs.getTimestamp("datetime_unload").getTime());
                        cv.put("result", rs.getInt("result"));
                        
                        putAnswerToLocalDB(cv);
                        
                        publishProgress(rsCount++, rsSize);
                    }
                } finally {
                    try {
                        if (!connection.isClosed()) connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            progressDialog.dismiss();
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress    = values[0];
            int maximum     = values[1];
            setProgress(progressDialog,progress,maximum);
        }
        
        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            db.close();
            super.onPostExecute(aVoid);
        }
        
        private void putOrderToLocalDB(ContentValues cv) {
            if (!checkOrderEntry(cv)) {
                try {
                    db.beginTransaction();
                    db.insert("orders", null, cv);
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                }
            }
        }
        
        private Boolean checkOrderEntry(ContentValues cv) {
            String order_id = cv.getAsString("order_id");
            String code_p = cv.getAsString("code_p");
            Cursor cursor = db.query(true, "orders", null, "order_id = ? AND code_p = ?", new String[]{order_id, code_p}, null, null, null, "1");
            Boolean entryExist = cursor.moveToFirst();
            cursor.close();
            return entryExist;
        }
        
        private void putAnswerToLocalDB(ContentValues cv) {
            if (!checkAnswerEntry(cv)) {
                try {
                    db.beginTransaction();
                    db.insert("answers", null, cv);
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.endTransaction();
                }
            }
        }
        
        private Boolean checkAnswerEntry(ContentValues cv) {
            String order_id = cv.getAsString("order_id");
            Cursor cursor = db.query(true, "answers", null, "order_id = ?", new String[]{order_id}, null, null, null, "1");
            Boolean entryExist = cursor.moveToFirst();
            cursor.close();
            return entryExist;
        }
        
        private Product getProduct(String pCode, String pName) {
            Product product = new Product(pCode, pName, 0, 0, 0);
            Cursor cursor = db.query(true, "rests", null, "code_p = ?", new String[]{pCode}, null, null, null, "1");
            if (cursor.moveToFirst()) {
                product.weight = cursor.getDouble(cursor.getColumnIndex("gross_weight"));
                product.price = cursor.getDouble(cursor.getColumnIndex("price"));
                product.num_in_pack = cursor.getDouble(cursor.getColumnIndex("amt_in_pack"));
            }
            cursor.close();
            return product;
        }
    }
    
    class UpdateAPKTask extends AsyncTask<Void, Integer, Void> {
        private static final int batchSize = 4096;
        private static final int progressSizeDivider = 1024;
        
        private String errorMessage = null;
        
        private final File appFolder = new File(String.format("%s%s%s", Environment.getExternalStorageDirectory(), File.separator, SettingsUtils.APP_FOLDER));
        private final File apkFile = new File(String.format("%s%s%s", appFolder.getAbsolutePath(), File.separator, "iceV3.apk"));
        
        ProgressDialog progressDialog = showProgressDialog("Получение файла обновления...");
    
        @Override
        protected Void doInBackground(Void... params) {
            SettingsUtils.Runtime.setUpdateInProgress(ctx, true);
            
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SettingsUtils.Settings.getApkUpdateUrl(ctx));
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    errorMessage = String.format("Ошибка обновления. Код %s : %s", connection.getResponseCode(), connection.getResponseMessage());
                    return null;
                }
                
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                
                if (!appFolder.exists()) {
                    if (!appFolder.mkdir()) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ctx, "Error creating folder " + appFolder.getPath(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        return null;
                    }
                }
                // download the file
                boolean deleted = true;
                boolean created = true;
                input = connection.getInputStream();
                if (apkFile.exists()) {
                    deleted = apkFile.delete();
                }
                if (deleted) {
                    created = apkFile.createNewFile();
                }
                if (!created) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ctx, "Error creating file " + apkFile.getPath(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return null;
                }
                output = new FileOutputStream(apkFile);
                
                publishProgress(0, fileLength);
                
                byte data[] = new byte[batchSize];
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
                }
                
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0] / progressSizeDivider;
            int maximum = values[1] / progressSizeDivider;
            setProgress(progressDialog,progress,maximum);
        }
        
        private void startUpdateIntent(File apkFile) {
            SettingsUtils.Runtime.setUpdateInProgress(ctx, false);
            if (apkFile != null && apkFile.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(ctx, "Файл APK не найден!", Toast.LENGTH_LONG).show();
            }
        }
        
        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            if (errorMessage != null) {
                Toast.makeText(ctx, errorMessage, Toast.LENGTH_SHORT).show();
                errorMessage = null;
            } else {
                if (apkFile.exists()) {
                    Toast.makeText(getApplicationContext(), "Обновление загружено", Toast.LENGTH_SHORT).show();
                    startUpdateIntent(apkFile);
                }
            }
        }
    }
    
}