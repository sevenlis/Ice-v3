package by.ingman.sevenlis.ice_v3;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        int curVersionCode      = getVersionCodeLocal();
        double curVersionName   = Double.valueOf(getVersionNameLocal());
        ((TextView) findViewById(R.id.textView5)).setText("ver." + FormatsUtils.getNumberFormatted(curVersionCode,0).trim() + " (" + FormatsUtils.getNumberFormatted(curVersionName,2).trim() + ")");
        ((TextView) findViewById(R.id.textView2)).setText(getPackageName());
    }

    private int getVersionCodeLocal() {
        int versionCode = 1;
        try { versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) { e.printStackTrace(); }
        return versionCode;
    }

    private String getVersionNameLocal() {
        String versionName = "0.00";
        try { versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) { e.printStackTrace(); }
        return versionName;
    }

}
