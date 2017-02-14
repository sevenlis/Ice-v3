package by.ingman.sevenlis.ice_v3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.remote.sql.UpdateDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class UpdateDataActivity extends AppCompatActivity {
    private static final String PROGRESS_BAR_VISIBILITY_OUT_STATE = "PROGRESS_BAR_VISIBILITY_OUT_STATE";
    private static final String TEXT_INFO_VALUE = "TEXT_INFO_VALUE";
    private Context ctx;
    private ProgressBar progressBar;
    private TextView textViewProgressPercent;
    private TextView textViewProgressValue;
    private TextView textViewInfo;
    private LinearLayout linearLayoutProgress;
    private static boolean updateIsRunning = false;
    private File apkFile;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UpdateDataService.CHANNEL)) {
                if (intent.getExtras() != null) {
                    if (intent.getExtras().getBoolean(UpdateDataService.EXTRA_ACTION_STARTED_KEY, false)) {
                        displayProgressBar(true);
                    }

                    if (intent.getExtras().getBoolean(UpdateDataService.EXTRA_ACTION_COMPLETE_KEY, false)) {
                        displayProgressBar(false);
                    }

                    if (intent.getExtras().getInt(UpdateDataService.EXTRA_ACTION_KEY) == UpdateDataService.EXTRA_ACTION_SEND_MESSAGE) {
                        String messageOnBroadcast = intent.getExtras().getString(UpdateDataService.MESSAGE_ON_BROADCAST_KEY);
                        Toast.makeText(context, messageOnBroadcast, Toast.LENGTH_SHORT).show();
                        ((TextView) findViewById(R.id.textViewInfo)).setText(messageOnBroadcast);
                    }

                    if (intent.getExtras().getInt(UpdateDataService.EXTRA_ACTION_KEY) == UpdateDataService.EXTRA_ACTION_SEND_PROGRESS_INCREMENT_VALUE){
                        progressBarProgressIncrement(
                                (int) intent.getExtras().getDouble(UpdateDataService.EXTRA_PROGRESS_CURRENT_VALUE_KEY),
                                intent.getExtras().getInt(UpdateDataService.EXTRA_PROGRESS_INCREMENT_VALUE_KEY),
                                intent.getExtras().getInt(UpdateDataService.EXTRA_PROGRESS_TOTAL_VALUE_KEY));
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
        registerReceiver(broadcastReceiver,new IntentFilter(UpdateDataService.CHANNEL));
        updateIsRunning = SettingsUtils.Runtime.getUpdateInProgress(this);

        textViewInfo = (TextView) findViewById(R.id.textViewInfo);

        linearLayoutProgress = (LinearLayout) findViewById(R.id.linProgress);
        linearLayoutProgress.setVisibility(View.GONE);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textViewProgressPercent = (TextView) findViewById(R.id.textViewProgressPercent);
        textViewProgressValue = (TextView) findViewById(R.id.textViewProgressValue);

        if (savedInstanceState != null) {
            boolean progressVisible = savedInstanceState.getBoolean(PROGRESS_BAR_VISIBILITY_OUT_STATE);
            if (updateIsRunning & progressVisible) {
                linearLayoutProgress.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
                progressBarUpdateTexts();
            }
            textViewInfo.setText(savedInstanceState.getString(TEXT_INFO_VALUE));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (linearLayoutProgress.getVisibility() == View.VISIBLE) {
            outState.putBoolean(PROGRESS_BAR_VISIBILITY_OUT_STATE,true);
        } else {
            outState.putBoolean(PROGRESS_BAR_VISIBILITY_OUT_STATE,false);
        }
        outState.putString(TEXT_INFO_VALUE,textViewInfo.getText().toString());
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
        if (updateIsRunning) return;
        textViewInfo.setText("Запущено обновление таблицы остатков номенклатуры...");

        Intent intent = new Intent(getApplicationContext(),UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_RESTS_VALUE);
        startService(intent);
    }

    public void buttonClientsOnClick(View view) throws Exception {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        if (updateIsRunning) return;
        textViewInfo.setText("Запущено обновление таблицы контрагентов и пунктов разгрузок...");

        Intent intent = new Intent(getApplicationContext(),UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_CLIENTS_VALUE);
        startService(intent);

    }

    public void buttonDebtsOnClick(View view) {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        if (updateIsRunning) return;
        textViewInfo.setText("Запущено обновление таблицы задолженностей контрагентов...");

        Intent intent = new Intent(getApplicationContext(),UpdateDataService.class);
        intent.putExtra(UpdateDataService.EXTRA_ACTION_KEY, UpdateDataService.EXTRA_UPDATE_ALL_DEBTS_VALUE);
        startService(intent);
    }

    public void displayProgressBar(boolean display) {
        linearLayoutProgress.setVisibility(display ? View.VISIBLE : View.GONE);
        if (display) progressBar.setIndeterminate(true);
        progressBar.setProgress(0);
        progressBar.setMax(0);
        progressBarUpdateTexts();
    }

    public void progressBarProgressIncrement(int curValue, int increment, int maxValue) {
        if (increment == 0) {
            progressBarProgressIncrement(curValue,maxValue);
            return;
        }
        progressBar.setIndeterminate(false);
        progressBar.setProgress(curValue - increment);
        progressBar.setMax(maxValue);
        progressBar.incrementProgressBy(increment);
        progressBarUpdateTexts();
    }

    public void progressBarProgressIncrement(int curValue, int maxValue) {
        progressBar.setIndeterminate(false);
        int increment = curValue - progressBar.getProgress();
        if (increment <= 0) return;
        progressBarProgressIncrement(curValue,increment,maxValue);
    }

    public void progressBarUpdateTexts() {
        String mText;
        int curValue = progressBar.getProgress();
        int maxValue = progressBar.getMax();
        double curValueD = (double) curValue;
        double maxValueD = (double) maxValue;
        double percent = maxValue == 0 ? 0 : curValueD / maxValueD * 100;

        mText = String.format(Locale.ROOT,"%3.0f",percent) + " %";
        textViewProgressPercent.setText(mText);
        textViewProgressPercent.setVisibility(progressBar.isIndeterminate() ? View.GONE : View.VISIBLE);

        mText = String.valueOf(curValue) + " / " + String.valueOf(maxValue);
        textViewProgressValue.setText(mText);
        textViewProgressValue.setVisibility(progressBar.isIndeterminate() ? View.GONE : View.VISIBLE);
    }

    public void updateAPK(View view) {
        if (!isConnected()) {
            Toast.makeText(ctx, "Соединение отсутствует", Toast.LENGTH_SHORT).show();
            return;
        }
        textViewInfo.setText("Запущено скачивание установочного файла...");
        displayProgressBar(true);
        UpdateAPKTask updateAPKTask = new UpdateAPKTask();
        updateAPKTask.execute();
    }

    private void startUpdateIntent() {
        displayProgressBar(false);
        SettingsUtils.Runtime.setUpdateInProgress(ctx,false);
        if (apkFile != null && apkFile.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            startActivity(intent);
            textViewInfo.setText("Установка АРК-файла не выполнена.");
        } else {
            textViewInfo.setText(R.string.apk_file_not_found);
            Toast.makeText(ctx, "Файл APK не найден!", Toast.LENGTH_LONG).show();
            textViewInfo.setText("Обновление данных");
        }
    }

    private class UpdateAPKTask extends AsyncTask<Void, Integer, Void> {
        private static final int batchSize = 4096;
        private static final int progressSizeDivider = 1024;

        private String errorMessage = null;

        @Override
        protected Void doInBackground(Void... params) {
            SettingsUtils.Runtime.setUpdateInProgress(ctx,true);

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

                File appFolder = new File(String.format("%s/%s",Environment.getExternalStorageDirectory(), SettingsUtils.APP_FOLDER));
                if (!appFolder.exists()) {
                    if (!appFolder.mkdir()) {
                        Toast.makeText(ctx, "Error creating folder " + appFolder.getPath(), Toast.LENGTH_SHORT).show();
                        return null;
                    }
                }

                // download the file
                boolean deleted = true;
                boolean created = true;
                input = connection.getInputStream();
                apkFile = new File(String.format("%s/%s/iceV3.apk", Environment.getExternalStorageDirectory(), SettingsUtils.APP_FOLDER));
                if (apkFile.exists()) {
                    deleted = apkFile.delete();
                }
                if (deleted) {
                    created = apkFile.createNewFile();
                }
                if (!created) {
                    Toast.makeText(ctx, "Error creating file " + apkFile.getPath(), Toast.LENGTH_SHORT).show();
                    return null;
                }
                output = new FileOutputStream(apkFile);

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
                errorMessage =  "Ошибка обновления " + e.toString();
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
            int max = values[1] / progressSizeDivider;
            progressBarProgressIncrement(progress,max);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (errorMessage != null) {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                errorMessage = null;
            } else {
                Toast.makeText(getApplicationContext(), "Обновление загружено", Toast.LENGTH_SHORT).show();
            }
            startUpdateIntent();
        }
    }

}
