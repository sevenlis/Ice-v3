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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Objects;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Contragent;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class SelectCounterPartyActivity extends AppCompatActivity {
    public static final String CONTRAGENT_CODE_VALUE_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_CODE_VALUE_KEY";
    public static final String CONTRAGENT_NAME_VALUE_KEY = "by.ingman.sevenlis.ice_v3.CONTRAGENT_NAME_VALUE_KEY";
    public static final String CONTRAGENT_STOP_VALUE_KEY = SelectCounterPartyActivity.class.getSimpleName() + ".CONTRAGENT_STOP_VALUE_KEY";
    Boolean useRecent = false;
    ImageButton buttonRecent;
    EditText editTextFilter;
    private List<Contragent> contragentsList;
    private DBLocal dbLocal;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contragent);

        dbLocal = new DBLocal(this);
        
        buttonRecent = findViewById(R.id.btnRecent);
        
        editTextFilter = findViewById(R.id.etFilter);
        
        editTextFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshContragentsList(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            
            }
        });
        
        editTextFilter.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                refreshContragentsList();
                return true;
            }
            return false;
        });
        
        refreshContragentsList();
    }
    
    public void buttonRecentOnClick(View view) {
        useRecent = !useRecent;
        
        if (useRecent) {
            buttonRecent.setImageResource(android.R.drawable.btn_star_big_on);
            editTextFilter.setText("");
        } else {
            buttonRecent.setImageResource(android.R.drawable.btn_star_big_off);
        }
        editTextFilter.setEnabled(!useRecent);
        refreshContragentsList();
    }
    
    private void refreshContragentsList(String strFilter) {
        String condition = "";
        String[] conditionArgs = new String[]{};
        if (!strFilter.isEmpty()) {
            condition = "client_uppercase like ?";
            conditionArgs = new String[]{dbLocal.addWildcards(strFilter)};
        }
        if (useRecent) {
            contragentsList = dbLocal.getRecentContragents();
        } else {
            contragentsList = dbLocal.getContragents(condition, conditionArgs);
        }
        
        CustomListAdapter customListAdapter = new CustomListAdapter(this, contragentsList);
        ListView lvContragents = findViewById(R.id.listContragents);
        lvContragents.setAdapter(customListAdapter);
        lvContragents.setOnItemClickListener((adapterView, view, i, l) -> {
            Contragent contragent = contragentsList.get(i);
            Intent answerIntent = new Intent();
            answerIntent.putExtra(CONTRAGENT_CODE_VALUE_KEY, contragent.getCode());
            answerIntent.putExtra(CONTRAGENT_NAME_VALUE_KEY, contragent.getName());
            answerIntent.putExtra(CONTRAGENT_STOP_VALUE_KEY, contragent.isInStop());
            setResult(RESULT_OK, answerIntent);
            finish();
        });
    }
    
    private void refreshContragentsList() {
        String strFilter = ((EditText) findViewById(R.id.etFilter)).getText().toString();
        refreshContragentsList(strFilter);
    }
    
    public void acceptFilterContragents(View view) {
        refreshContragentsList();
        if (contragentsList.size() == 0) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
    
    private class CustomListAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        List<Contragent> objects;
        
        CustomListAdapter(Context context, List<Contragent> objects) {
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
            double debt = dbLocal.getContragentDebt(contragent);
            double over = dbLocal.getContragentOverdue(contragent);
            String fDebt = debt != 0 ? "Задолж.: " + FormatsUtils.getNumberFormatted(debt, 2) : "-";
            String fOver = over != 0 ? "Просроч.: " + FormatsUtils.getNumberFormatted(over, 2) : "";
            
            ((TextView) view.findViewById(R.id.tvContrName)).setText(contragent.getName());
            ((TextView) view.findViewById(R.id.tvRating)).setText(dbLocal.getContragentRating(contragent));
            ((TextView) view.findViewById(R.id.tvDebt)).setText(fDebt);
            ((TextView) view.findViewById(R.id.tvOverdue)).setText(fOver);

            ((TextView) view.findViewById(R.id.tvContrName)).setTextColor(getResources().getColor(contragent.isInStop() ? R.color.color_red : R.color.dark));
            
            return view;
        }
        
        Contragent getContragent(int i) {
            return (Contragent) getItem(i);
        }
    }
}
