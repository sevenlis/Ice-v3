package by.ingman.sevenlis.ice_v3;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class SelectProductActivity extends AppCompatActivity {
    public static final String PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY = "by.ingman.sevenlis.ice_v3.PARCELABLE_ITEM_SINGLE_ARRAY_KEY";
    CheckBox checkBoxInputTypeNumeric;
    EditText editTextFilter;
    ArrayList<Product> arrayListProducts;
    DBLocal dbLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_product);

        dbLocal = new DBLocal(this);

        editTextFilter = (EditText) findViewById(R.id.editTextFilter);
        editTextFilter.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    refreshListViewItemsList();
                    return true;
                }
                return false;
            }
        });

        checkBoxInputTypeNumeric = (CheckBox) findViewById(R.id.checkBoxInputType);
        checkBoxInputTypeNumeric.setChecked(SettingsUtils.Settings.getItemSearchInputTypeNumeric(this));
        changeEditTextFilterInputType();

        refreshListViewItemsList();
    }

    private void changeEditTextFilterInputType() {
        editTextFilter.setText("");
        boolean isNumericType = checkBoxInputTypeNumeric.isChecked();
        if (isNumericType) {
            editTextFilter.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else {
            editTextFilter.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    public void applyFilterItems(View view) {
        refreshListViewItemsList();
        if (arrayListProducts.size() == 0) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void refreshListViewItemsList() {
        String strFilter = editTextFilter.getText().toString();
        String condition = "code_s = ?";
        String[] conditionArgs = new String[]{SettingsUtils.Settings.getDefaultStoreHouseCode(this)};
        if (!strFilter.isEmpty()) {
            if (checkBoxInputTypeNumeric.isChecked()) {
                condition += " AND code_p like ?";
            } else {
                condition += " AND search_uppercase like ?";
            }
            conditionArgs = new String[]{SettingsUtils.Settings.getDefaultStoreHouseCode(this), dbLocal.addWildcards(strFilter)};
        }
        arrayListProducts = dbLocal.getProducts(condition, conditionArgs);
        CustomListViewAdapter customListViewAdapter = new CustomListViewAdapter(this, arrayListProducts);
        ListView listViewItemsList = (ListView) findViewById(R.id.listViewProductsList);
        listViewItemsList.setAdapter(customListViewAdapter);
        listViewItemsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayList<Product> singleProductArray = new ArrayList<>(1);
                singleProductArray.add(0,arrayListProducts.get(i));

                Intent answerIntent = new Intent();
                answerIntent.putParcelableArrayListExtra(PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY,singleProductArray);
                setResult(RESULT_OK,answerIntent);
                finish();
            }
        });
        if (arrayListProducts.size() == 1) {
            ArrayList<Product> singleProductArray = new ArrayList<>(1);
            singleProductArray.add(0,arrayListProducts.get(0));

            Intent answerIntent = new Intent();
            answerIntent.putParcelableArrayListExtra(PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY,singleProductArray);
            setResult(RESULT_OK,answerIntent);
            finish();
        } else {
            editTextFilter.setFocusable(true);
            editTextFilter.requestFocus();
        }
    }

    public void checkBoxInputTypeNumericOnClick(View view) {
        changeEditTextFilterInputType();
    }

    private class CustomListViewAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        ArrayList<Product> objects;

        CustomListViewAdapter(Context context, ArrayList<Product> objects) {
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
            String mText;
            Product product = getObjectItem(i);

            mText = product.code;
            ((TextView) view.findViewById(R.id.tvItemCode)).setText(mText);

            mText = product.name;
            ((TextView) view.findViewById(R.id.tvItemName)).setText(mText);

            mText = "Упак.: " + FormatsUtils.getNumberFormatted(dbLocal.getProductRestPacks(product) - dbLocal.getProductBlockPacks(product), 1);
            ((TextView) view.findViewById(R.id.tvRestPacks)).setText(mText);

            mText = "Кол.: " + FormatsUtils.getNumberFormatted(dbLocal.getProductRestAmount(product) - dbLocal.getProductBlockAmount(product), 0);
            ((TextView) view.findViewById(R.id.tvRestAmount)).setText(mText);

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
