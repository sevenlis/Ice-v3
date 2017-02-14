package by.ingman.sevenlis.ice_v3;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import by.ingman.sevenlis.ice_v3.classes.Point;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;

public class SelectSalespointActivity extends AppCompatActivity {
    public static final String CONTRAGENT_CODE_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_CODE_KEY";
    public static final String CONTRAGENT_NAME_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_NAME_KEY";
    public static final String SALESPOINT_CODE_VALUE_KEY = "by.ingman.sevenlis.ice_v3.SALESPOINT_CODE_VALUE_KEY";
    public static final String SALESPOINT_NAME_VALUE_KEY = "by.ingman.sevenlis.ice_v3.SALESPOINT_NAME_VALUE_KEY";
    private ArrayList<Point> pointsList;
    private DBLocal dbLocal = new DBLocal(this);
    private String mContragentCode;
    private String mContragentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_salespoint);

        EditText editTextFilter = (EditText) findViewById(R.id.etFilter);
        editTextFilter.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    refreshPointsList();
                    return true;
                }
                return false;
            }
        });

        if (getIntent().getExtras() != null) {
            mContragentCode = getIntent().getExtras().getString(CONTRAGENT_CODE_KEY);
            mContragentName = getIntent().getExtras().getString(CONTRAGENT_NAME_KEY);
        }

        refreshPointsList();
    }

    private void refreshPointsList() {
        String filter           = ((EditText) findViewById(R.id.etFilter)).getText().toString();
        String condition        = "code_k = ? AND name_k = ?";
        String[] conditionArgs  = new String[]{this.mContragentCode, this.mContragentName};
        if (!filter.isEmpty()) {
            condition      += " AND search_uppercase like ?";
            conditionArgs   = new String[]{this.mContragentCode, this.mContragentName, dbLocal.addWildcards(filter)};
        }
        pointsList = dbLocal.getPoints(condition,conditionArgs);
        CustomListAdapter customListAdapter = new CustomListAdapter(this, pointsList);
        ListView lvPoints = (ListView) findViewById(R.id.listPoints);
        lvPoints.setAdapter(customListAdapter);
        lvPoints.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Point point = pointsList.get(i);
                Intent answerIntent = new Intent();
                answerIntent.putExtra(SALESPOINT_CODE_VALUE_KEY, point.code);
                answerIntent.putExtra(SALESPOINT_NAME_VALUE_KEY, point.name);
                setResult(RESULT_OK,answerIntent);
                finish();
            }
        });
    }

    public void acceptFilterPoints(View view) {
        refreshPointsList();
        if (pointsList.size() == 0) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private class CustomListAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        ArrayList<Point> objects;

        CustomListAdapter(Context context, ArrayList<Point> objects) {
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

        Point getPoint(int i) {
            return (Point) getItem(i);
        }
    }
}
