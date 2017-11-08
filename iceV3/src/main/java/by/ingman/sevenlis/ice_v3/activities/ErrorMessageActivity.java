package by.ingman.sevenlis.ice_v3.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import by.ingman.sevenlis.ice_v3.R;

public class ErrorMessageActivity extends AppCompatActivity {
    public static final String EXTRA_ERROR_MESSAGE = "EXTRA_ERROR_MESSAGE";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_message);
        
        if (getActionBar() != null) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        TextView textView = (TextView) findViewById(R.id.textErrorMessage);
        
        String errorMessage = getIntent().getExtras().getString(EXTRA_ERROR_MESSAGE, null);
        
        if (errorMessage != null) {
            textView.setText(errorMessage);
        }
    }
    
}
