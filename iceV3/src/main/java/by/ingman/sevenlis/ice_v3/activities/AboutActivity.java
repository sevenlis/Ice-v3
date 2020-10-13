package by.ingman.sevenlis.ice_v3.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class AboutActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        int curVersionCode = getVersionCodeLocal();
        double curVersionName = Double.parseDouble(getVersionNameLocal());
        String sVersion = "ver." + FormatsUtils.getNumberFormatted(curVersionCode, 0).trim() + " (" + FormatsUtils.getNumberFormatted(curVersionName, 3).trim() + ")";
        ((TextView) findViewById(R.id.textView5)).setText(sVersion.replace(",", "."));
        ((TextView) findViewById(R.id.textView2)).setText(getPackageName());
    }
    
    private int getVersionCodeLocal() {
        int versionCode = 1;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
    
    private String getVersionNameLocal() {
        String versionName = "0.000";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }
    
}
