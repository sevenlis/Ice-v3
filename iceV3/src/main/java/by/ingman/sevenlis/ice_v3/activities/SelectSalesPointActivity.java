package by.ingman.sevenlis.ice_v3.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Objects;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Point;
import by.ingman.sevenlis.ice_v3.local.DBLocal;

public class SelectSalesPointActivity extends AppCompatActivity {
    public static final String CONTRAGENT_CODE_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_CODE_KEY";
    public static final String CONTRAGENT_NAME_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_NAME_KEY";
    public static final String SALESPOINT_CODE_VALUE_KEY = "by.ingman.sevenlis.ice_v3.SALESPOINT_CODE_VALUE_KEY";
    public static final String SALESPOINT_NAME_VALUE_KEY = "by.ingman.sevenlis.ice_v3.SALESPOINT_NAME_VALUE_KEY";
    private List<Point> pointsList;
    private DBLocal dbLocal;
    private String mContragentCode;
    private String mContragentName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_salespoint);

        dbLocal = new DBLocal(this);
        
        EditText editTextFilter = findViewById(R.id.etFilter);
        
        editTextFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshPointsList(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            
            }
        });
        
        editTextFilter.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                refreshPointsList();
                return true;
            }
            return false;
        });
        
        if (getIntent().getExtras() != null) {
            mContragentCode = getIntent().getExtras().getString(CONTRAGENT_CODE_KEY);
            mContragentName = getIntent().getExtras().getString(CONTRAGENT_NAME_KEY);
            
            ((TextView) findViewById(R.id.textContragent)).setText(mContragentName);
        }
        
        refreshPointsList();
    }
    
    private void refreshPointsList(String filter) {
        String condition = "code_k = ? AND name_k = ?";
        String[] conditionArgs = new String[]{this.mContragentCode, this.mContragentName};
        if (!filter.isEmpty()) {
            condition += " AND point_uppercase like ?";
            conditionArgs = new String[]{this.mContragentCode, this.mContragentName, dbLocal.addWildcards(filter)};
        }
        pointsList = dbLocal.getPoints(condition, conditionArgs);
        CustomListAdapter customListAdapter = new CustomListAdapter(this, pointsList);
        ListView lvPoints = findViewById(R.id.listPoints);
        lvPoints.setAdapter(customListAdapter);
        lvPoints.setOnItemClickListener((adapterView, view, i, l) -> {
            Point point = pointsList.get(i);
            Intent answerIntent = new Intent();
            answerIntent.putExtra(SALESPOINT_CODE_VALUE_KEY, point.code);
            answerIntent.putExtra(SALESPOINT_NAME_VALUE_KEY, point.name);
            setResult(RESULT_OK, answerIntent);
            finish();
        });
    }
    
    private void refreshPointsList() {
        String filter = ((EditText) findViewById(R.id.etFilter)).getText().toString();
        refreshPointsList(filter);
    }
    
    public void acceptFilterPoints(View view) {
        refreshPointsList();
        if (pointsList.size() == 0) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
    
    private static class CustomListAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        List<Point> objects;
        
        CustomListAdapter(Context context, List<Point> objects) {
            this.ctx = context;
            this.objects = objects;
            this.layoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        
        @Override
        public int getCount() {
            return objects.size();
        }
        
        @Override
        public Object getItem(int i) {
            return objects.get(i);
        }
        
        @Override
        public long getItemId(int i) {
            return i;
        }
        
        @Override
        public View getView(int i, View customView, ViewGroup viewGroup) {
            View view = customView;
            if (view == null) {
                view = layoutInflater.inflate(R.layout.select_salespoint_list_item, viewGroup, false);
            }
            Point point = getPoint(i);
            ((TextView) view.findViewById(R.id.tvPointName)).setText(point.name);
            return view;
        }
        
        private Point getPoint(int i) {
            return (Point) getItem(i);
        }
    }
}
