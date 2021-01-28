package by.ingman.sevenlis.ice_v3.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

import static by.ingman.sevenlis.ice_v3.activities.SelectOrderItemActivity.CHECKBOX_INPUT_TYPE_STATUS_KEY;
import static by.ingman.sevenlis.ice_v3.activities.SelectOrderItemActivity.STRING_FILTER_KEY;

public class SearchProductActivity extends AppCompatActivity {
    public static final String PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY = SearchProductActivity.class.getSimpleName() + ".PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY";
    public static final String STOREHOUSE_CODE_KEY = SearchProductActivity.class.getSimpleName() + ".storehouse_code_key";
    public static final String ORDER_TYPE_KEY = SearchProductActivity.class.getSimpleName() + ".order_type_key";
    CheckBox checkBoxInputTypeNumeric;
    EditText editTextFilter;
    List<Product> arrayListProducts = new ArrayList<>();
    DBLocal dbLocal;
    String mStorehouseCode;
    int mOrderType;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_product);
        
        dbLocal = new DBLocal(this);
        mStorehouseCode = SettingsUtils.Settings.getDefaultStoreHouseCode(this);
        if (getIntent().getExtras() != null) {
            mStorehouseCode = getIntent().getExtras().getString(STOREHOUSE_CODE_KEY);
            mOrderType = getIntent().getIntExtra(ORDER_TYPE_KEY, 1);
        }
        dbLocal.setStorehouseCode(mStorehouseCode);
        
        editTextFilter = findViewById(R.id.autoCompleteTextView);
        
        editTextFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshListViewItemsList();
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        editTextFilter.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                refreshListViewItemsList();
                //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return true;
            }
            return false;
        });
        
        checkBoxInputTypeNumeric = findViewById(R.id.checkBoxInputType);
        checkBoxInputTypeNumeric.setOnCheckedChangeListener((buttonView, isChecked) -> changeEditTextFilterInputType());
        if (getIntent().getExtras() != null) {
            checkBoxInputTypeNumeric.setChecked(getIntent().getExtras().getBoolean(CHECKBOX_INPUT_TYPE_STATUS_KEY));
            editTextFilter.setText(getIntent().getExtras().getString(STRING_FILTER_KEY));
        } else {
            checkBoxInputTypeNumeric.setChecked(SettingsUtils.Settings.getItemSearchInputTypeNumeric(this));
            refreshListViewItemsList();
        }
    }
    
    private void changeEditTextFilterInputType() {
        boolean isNumericType = checkBoxInputTypeNumeric.isChecked();
        if (isNumericType) {
            editTextFilter.setInputType(InputType.TYPE_CLASS_NUMBER);
            editTextFilter.setHint(getResources().getString(R.string.code_search));
        } else {
            editTextFilter.setInputType(InputType.TYPE_CLASS_TEXT);
            editTextFilter.setHint(getResources().getString(R.string.name_search));
        }
    }
    
//    public void applyFilterItems(View view) {
//        refreshListViewItemsList();
//        if (arrayListProducts.size() == 0) return;
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
//    }
    
    private void refreshListViewItemsList() {
        String strFilter = editTextFilter.getText().toString();
        String condition = "code_s = ?";
        String[] conditionArgs = new String[]{mStorehouseCode};
        if (!strFilter.isEmpty()) {
            condition += checkBoxInputTypeNumeric.isChecked() ? " AND code_p like ?" : " AND search_uppercase like ?";
            condition += " AND amount <> 0";
            conditionArgs = new String[]{mStorehouseCode, dbLocal.addWildcards(strFilter)};
        }

        if (mOrderType != 1) {
            condition = "";
            conditionArgs = new String[]{};
            if (!strFilter.isEmpty()) {
                condition = checkBoxInputTypeNumeric.isChecked() ? "code_p like ?" : "search_uppercase like ?";
                conditionArgs = new String[]{dbLocal.addWildcards(strFilter)};
            }
        }

        arrayListProducts = dbLocal.getProducts(condition, conditionArgs);
        
        CustomListViewAdapter customListViewAdapter = new CustomListViewAdapter(this, arrayListProducts);
        ListView listViewItemsList = findViewById(R.id.listViewProductsList);
        listViewItemsList.setAdapter(customListViewAdapter);
        listViewItemsList.setOnItemClickListener((adapterView, view, i, l) -> {
            ArrayList<Product> singleProductArray = new ArrayList<>(1);
            singleProductArray.add(0, arrayListProducts.get(i));

            Intent answerIntent = new Intent();
            answerIntent.putParcelableArrayListExtra(PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY, singleProductArray);
            answerIntent.putExtra(CHECKBOX_INPUT_TYPE_STATUS_KEY, checkBoxInputTypeNumeric.isChecked());
            answerIntent.putExtra(STRING_FILTER_KEY, editTextFilter.getText().toString());
            setResult(RESULT_OK, answerIntent);
            finish();
        });
        
        if (arrayListProducts.size() == 1) {
            ArrayList<Product> singleProductArray = new ArrayList<>(1);
            singleProductArray.add(0, arrayListProducts.get(0));
            
            Intent answerIntent = new Intent();
            answerIntent.putParcelableArrayListExtra(PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY, singleProductArray);
            answerIntent.putExtra(CHECKBOX_INPUT_TYPE_STATUS_KEY, checkBoxInputTypeNumeric.isChecked());
            answerIntent.putExtra(STRING_FILTER_KEY, editTextFilter.getText().toString());
            setResult(RESULT_OK, answerIntent);
            finish();
        } else {
            editTextFilter.setFocusable(true);
            editTextFilter.requestFocus();
        }
    }
    
    public void checkBoxInputTypeNumericOnClick(View view) {
        changeEditTextFilterInputType();
        refreshListViewItemsList();
    }
    
    private class CustomListViewAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        List<Product> objects;
        
        CustomListViewAdapter(Context context, List<Product> objects) {
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
                view = layoutInflater.inflate(R.layout.select_product_list_item, viewGroup, false);
            }
            int colorRed = getResources().getColor(R.color.color_red);
            int colorGreen = getResources().getColor(R.color.green_darker);

            Product product = getObjectItem(i);

            double restPacks = dbLocal.getProductRestPacks(product) - dbLocal.getProductBlockPacks(product);
            double restAmount = dbLocal.getProductRestAmount(product) - dbLocal.getProductBlockAmount(product);

            String mText;

            mText = product.code;
            ((TextView) view.findViewById(R.id.tvItemCode)).setText(mText);
            ((TextView) view.findViewById(R.id.tvItemCode)).setTextColor((restPacks <= 0D) || (restAmount <= 0D) ? colorRed : colorGreen);
            
            mText = product.name;
            ((TextView) view.findViewById(R.id.tvItemName)).setText(mText);

            mText = "Упак.: " + FormatsUtils.getNumberFormatted(restPacks, 1);
            ((TextView) view.findViewById(R.id.tvRestPacks)).setText(mText);
            ((TextView) view.findViewById(R.id.tvRestPacks)).setTextColor((restPacks <= 0D) ? colorRed : colorGreen);

            mText = "Кол.: " + FormatsUtils.getNumberFormatted(restAmount, 0);
            ((TextView) view.findViewById(R.id.tvRestAmount)).setText(mText);
            ((TextView) view.findViewById(R.id.tvRestAmount)).setTextColor((restAmount <= 0D) ? colorRed : colorGreen);

            mText = "Цена: " + FormatsUtils.getNumberFormatted(product.price, 2);
            ((TextView) view.findViewById(R.id.tvPrice)).setText(mText);
            
            mText = "В упак.: " + FormatsUtils.getNumberFormatted(product.num_in_pack, 0);
            ((TextView) view.findViewById(R.id.tvNumInPack)).setText(mText);
            
            return view;
        }
        
        Product getObjectItem(int i) {
            return (Product) getItem(i);
        }
    }
}
