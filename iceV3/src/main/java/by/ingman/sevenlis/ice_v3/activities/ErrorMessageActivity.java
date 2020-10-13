package by.ingman.sevenlis.ice_v3.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

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
        
        TextView textView = findViewById(R.id.textErrorMessage);
        
        String errorMessage = Objects.requireNonNull(getIntent().getExtras()).getString(EXTRA_ERROR_MESSAGE, null);
        
        if (errorMessage != null) {
            textView.setText(errorMessage);
        }
    }
    
}
