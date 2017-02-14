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

import by.ingman.sevenlis.ice_v3.classes.Contragent;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class SelectContragentActivity extends AppCompatActivity {
    public static final String CONTRAGENT_CODE_VALUE_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_CODE_VALUE_KEY";
    public static final String CONTRAGENT_NAME_VALUE_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_NAME_VALUE_KEY";
    private ArrayList<Contragent> contragentsList;
    private DBLocal dbLocal = new DBLocal(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contragent);

        EditText editTextFilter = (EditText) findViewById(R.id.etFilter);
        editTextFilter.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    refreshContragentsList();
                    return true;
                }
                return false;
            }
        });

        refreshContragentsList();
    }

    private void refreshContragentsList() {
        String strFilter    = ((EditText) findViewById(R.id.etFilter)).getText().toString();
        String condition    = "";
        String[] conditionArgs  = new String[]{};
        if (!strFilter.isEmpty()) {
            condition       = "search_uppercase like ?";
            conditionArgs   = new String[]{dbLocal.addWildcards(strFilter)};
        }
        contragentsList = dbLocal.getContragents(condition,conditionArgs);

        CustomListAdapter customListAdapter = new CustomListAdapter(this,contragentsList);
        ListView lvContragents = (ListView) findViewById(R.id.listContragents);
        lvContragents.setAdapter(customListAdapter);
        lvContragents.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contragent contragent = contragentsList.get(i);
                Intent answerIntent = new Intent();
                answerIntent.putExtra(CONTRAGENT_CODE_VALUE_KEY, contragent.getCode());
                answerIntent.putExtra(CONTRAGENT_NAME_VALUE_KEY, contragent.getName());
                setResult(RESULT_OK,answerIntent);
                finish();
            }
        });
    }

    public void acceptFilterContragents(View view) {
        refreshContragentsList();
        if (contragentsList.size() == 0) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public class CustomListAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        ArrayList<Contragent> objects;

        CustomListAdapter(Context context, ArrayList<Contragent> objects) {
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
                view = layoutInflater.inflate(R.layout.select_contragent_list_item, viewGroup, false);
            }

            Contragent contragent = getContragent(i);
            double debt  = dbLocal.getContragentDebt(contragent);
            double over  = dbLocal.getContragentOverdue(contragent);
            String fDebt = debt !=0 ? "Задолж.: " + FormatsUtils.getNumberFormatted(debt, 2) : "-";
            String fOver = over !=0 ? "Просроч.: " + FormatsUtils.getNumberFormatted(over, 2) : "";

            ((TextView) view.findViewById(R.id.tvContrName)).setText(contragent.getName());
            ((TextView) view.findViewById(R.id.tvRating)).setText(dbLocal.getContragentRating(contragent));
            ((TextView) view.findViewById(R.id.tvDebt)).setText(fDebt);
            ((TextView) view.findViewById(R.id.tvOverdue)).setText(fOver);

            return view;
        }

        Contragent getContragent(int i) {
            return (Contragent) getItem(i);
        }
    }
}
