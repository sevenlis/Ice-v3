package by.ingman.sevenlis.ice_v3.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class SelectOrderItemActivity extends AppCompatActivity {
    public static final int SEARCH_PRODUCT_REQUEST_CODE = 0;
    
    public static final String PRODUCT_ARRAY_PARCELABLE_VALUE_KEY = "by.ingman.sevenlis.ice_v3.ProductArray_parcelable";
    public static final String ORDER_ITEM_QUANTITY_VALUE_KEY = "by.ingman.sevenlis.ice_v3.OrderItemQuantity";
    public static final String ORDER_ITEM_PARCELABLE_VALUE_KEY = "by.ingman.sevenlis.ice_v3.OrderItem_parcelable";
    public static final String ORDER_ITEM_POSITION_VALUE_KEY = "by.ingman.sevenlis.ice_v3.OrderItem_position_in_list";
    
    public static final String CHECKBOX_INPUT_TYPE_STATUS_KEY = "by.ingman.sevenlis.ice_v3.CheckBox_input_status_key";
    public static final String STRING_FILTER_KEY = "by.ingman.sevenlis.ice_v3.String_filter_key";
    public static final String STOREHOUSE_CODE_KEY = SelectOrderItemActivity.class.getSimpleName() + ".storehouse_code_key";
    Product mProduct = null;
    EditText editTextNumPacks;
    double mNumPacks = 1.0d;
    EditText editTextNumPcs;
    double mNumPcs = 1.0d;
    Button buttonSubmit;
    OrderItem orderItem = null;
    int orderItemPosition = -1;
    CheckBox checkBoxInputTypeNumeric;
    AutoCompleteTextView autoCompleteTextView;
    AutoCompleteTextViewAdapter autoCompleteTextViewAdapter;
    List<Product> autoCompleteTextProductList;
    LinearLayout layoutNumPcs;
    LinearLayout layoutNumPacks;
    private DBLocal dbLocal;
    String mStorehouseCode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_order_item);

        dbLocal = new DBLocal(this);
        mStorehouseCode = SettingsUtils.Settings.getDefaultStoreHouseCode(this);
        if (getIntent().getExtras() != null) {
            mStorehouseCode = getIntent().getExtras().getString(STOREHOUSE_CODE_KEY);
        }
        dbLocal.setStorehouseCode(mStorehouseCode);
        
        buttonSubmit = findViewById(R.id.btnSubmit);
        buttonSubmit.setEnabled(false);
        
        layoutNumPcs = findViewById(R.id.quantityPcsSelection);
        layoutNumPacks = findViewById(R.id.quantityPacksSelection);
        
        editTextNumPacks = findViewById(R.id.editTextNumPacks);
        editTextNumPacks.setOnFocusChangeListener((view, isFocused) -> {
            if (editTextNumPacks.getText().toString().isEmpty() || editTextNumPacks.getText().toString().equals(".")) {
                editTextNumPacks.setText("0.0");
            }
            if (isFocused) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.SHOW_FORCED);

            } else {
                mNumPacks = Double.parseDouble(editTextNumPacks.getText().toString());
                recalculatePcs();
            }
        });
        editTextNumPacks.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (editTextNumPacks.getText().toString().isEmpty() || editTextNumPacks.getText().toString().equals(".")) {
                editTextNumPacks.setText("0.0");
            }
            mNumPacks = Double.parseDouble(editTextNumPacks.getText().toString());
            recalculatePcs();
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                btnSubmitOnClick(editTextNumPacks);
                return true;
            }
            return false;
        });
        
        editTextNumPcs = findViewById(R.id.editTextNumPcs);
        editTextNumPcs.setOnFocusChangeListener((view, isFocused) -> {
            if (editTextNumPcs.getText().toString().isEmpty() || editTextNumPcs.getText().toString().equals(".")) {
                editTextNumPcs.setText("0.0");
            }
            if (isFocused) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            } else {
                mNumPcs = Math.round(Double.parseDouble(editTextNumPcs.getText().toString()));
                recalculatePacks();
            }
        });
        editTextNumPcs.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (editTextNumPcs.getText().toString().isEmpty() || editTextNumPcs.getText().toString().equals(".")) {
                editTextNumPcs.setText("0.0");
            }
            mNumPcs = Math.round(Double.parseDouble(editTextNumPcs.getText().toString()));
            recalculatePacks();
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                btnSubmitOnClick(editTextNumPcs);
                return true;
            }
            return false;
        });

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        autoCompleteTextView.setThreshold(2);

        autoCompleteTextProductList = getProductList();
        autoCompleteTextViewAdapter = new AutoCompleteTextViewAdapter(this, autoCompleteTextProductList);
        autoCompleteTextView.setAdapter(autoCompleteTextViewAdapter);
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            setProduct((Product) autoCompleteTextViewAdapter.getItem(position));
            editTextNumPacks.requestFocus();
            autoCompleteTextView.setText(null);
        });
        autoCompleteTextView.setOnKeyListener((v, keyCode, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                List<Product> productArrayList = getProductList();
                if (productArrayList.size() == 1) {
                    setProduct(productArrayList.get(0));
                    editTextNumPacks.requestFocus();
                    autoCompleteTextView.setText(null);
                } else {
                    searchProduct(findViewById(R.id.textViewProduct));
                }
                return true;
            }
            return false;
        });
        
        checkBoxInputTypeNumeric = findViewById(R.id.checkBoxInputType);
        checkBoxInputTypeNumeric.setOnCheckedChangeListener((buttonView, isChecked) -> changeEditTextFilterInputType());
        checkBoxInputTypeNumeric.setChecked(SettingsUtils.Settings.getItemSearchInputTypeNumeric(this));

        if (getIntent().getExtras() != null) {
            orderItem = getIntent().getParcelableExtra(ORDER_ITEM_PARCELABLE_VALUE_KEY);
            orderItemPosition = getIntent().getExtras().getInt(ORDER_ITEM_POSITION_VALUE_KEY);
        }

        if (savedInstanceState != null) {
            orderItem = savedInstanceState.getParcelable("orderItem");
            orderItemPosition = savedInstanceState.getInt("orderItemPosition");
        }

        if (orderItem != null) {
            this.mProduct = orderItem.product;
            this.mNumPcs = orderItem.quantity;
            this.mNumPacks = orderItem.packs;
        }

        refreshProductInfo();
        if (mProduct != null) {
            tvNumPacksText();
            recalculatePcs();
            editTextNumPacks.setFocusable(true);
            editTextNumPacks.requestFocus();
        } else {
            autoCompleteTextView.setFocusable(true);
            autoCompleteTextView.requestFocus();
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        if (orderItem == null)
            searchProduct(findViewById(R.id.textViewProduct));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (orderItem != null) {
            orderItem.product = mProduct;
            orderItem.quantity = mNumPcs;
            orderItem.packs = mNumPacks;
            orderItem.reCalcAll();
        }
        outState.putParcelable("orderItem",orderItem);
        outState.putInt("orderItemPosition",orderItemPosition);
        super.onSaveInstanceState(outState);
    }

    private List<Product> getProductList() {
        String strFilter = autoCompleteTextView.getText().toString();
        String condition = "code_s = ?";
        String[] conditionArgs = new String[]{mStorehouseCode};
        if (!strFilter.isEmpty()) {
            if (checkBoxInputTypeNumeric.isChecked()) {
                condition += " AND code_p like ?";
            } else {
                condition += " AND search_uppercase like ?";
            }
            conditionArgs = new String[]{mStorehouseCode, dbLocal.addWildcards(strFilter)};
        }
        return dbLocal.getProducts(condition, conditionArgs);
    }
    
    private void changeEditTextFilterInputType() {
        autoCompleteTextView.setText(null);
        boolean isNumericType = checkBoxInputTypeNumeric.isChecked();
        if (isNumericType) {
            autoCompleteTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
            autoCompleteTextView.setHint(getResources().getString(R.string.code_search));
        } else {
            autoCompleteTextView.setInputType(InputType.TYPE_CLASS_TEXT);
            autoCompleteTextView.setHint(getResources().getString(R.string.name_search));
        }
    }
    
    public void checkBoxInputTypeOnClick(View view) {
        changeEditTextFilterInputType();
    }
    
    void tvNumPacksText() {
        editTextNumPacks.setText(String.format(Locale.ROOT, "%.1f", this.mNumPacks));
    }
    
    void tvNumPcsText() {
        editTextNumPcs.setText(String.format(Locale.ROOT, "%.0f", this.mNumPcs));
    }
    
    public void searchProduct(View view) {
        Intent intent = new Intent(this, SearchProductActivity.class);
        intent.putExtra(CHECKBOX_INPUT_TYPE_STATUS_KEY, checkBoxInputTypeNumeric.isChecked());
        intent.putExtra(STRING_FILTER_KEY, autoCompleteTextView.getText().toString());
        intent.putExtra(SearchProductActivity.STOREHOUSE_CODE_KEY, mStorehouseCode);
        startActivityForResult(intent, SEARCH_PRODUCT_REQUEST_CODE);
    }

    public void setProduct(Product product) {
        mProduct = product;
        refreshProductInfo();
        tvNumPacksText();
        recalculatePcs();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data.getExtras() != null) {
            if (requestCode == SEARCH_PRODUCT_REQUEST_CODE) {
                ArrayList<Product> arraySingleProduct = data.getExtras().getParcelableArrayList(SearchProductActivity.PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY);
                if (arraySingleProduct != null)
                    setProduct(arraySingleProduct.get(0));
                checkBoxInputTypeNumeric.setChecked(data.getExtras().getBoolean(CHECKBOX_INPUT_TYPE_STATUS_KEY));
                autoCompleteTextView.setText(null);
                editTextNumPacks.requestFocus();
            }
        } else {
            finish();
        }
    }
    
    private void refreshProductInfo() {
        if (mProduct == null) {
            layoutNumPcs.setVisibility(View.GONE);
            layoutNumPacks.setVisibility(View.GONE);
            buttonSubmit.setVisibility(View.GONE);
            return;
        } else {
            layoutNumPcs.setVisibility(View.VISIBLE);
            layoutNumPacks.setVisibility(View.VISIBLE);
            buttonSubmit.setVisibility(View.VISIBLE);
        }
        
        buttonSubmit.setEnabled(true);
        
        String mText;
        
        mText = mProduct.code + " " + mProduct.name;
        ((TextView) findViewById(R.id.textViewProduct)).setText(mText);
        
        mText = "Упак.: " + FormatsUtils.getNumberFormatted(dbLocal.getProductRestPacks(mProduct) - dbLocal.getProductBlockPacks(mProduct), 1);
        ((TextView) findViewById(R.id.textViewRestPacks)).setText(mText);
        
        mText = "Кол.: " + FormatsUtils.getNumberFormatted(dbLocal.getProductRestAmount(mProduct) - dbLocal.getProductBlockAmount(mProduct), 0);
        ((TextView) findViewById(R.id.textViewRestAmount)).setText(mText);
        
        mText = "Цена: " + FormatsUtils.getNumberFormatted(mProduct.price, 2);
        ((TextView) findViewById(R.id.textViewPrice)).setText(mText);
        
        mText = "В упак.: " + FormatsUtils.getNumberFormatted(mProduct.num_in_pack, 0);
        ((TextView) findViewById(R.id.textViewNumInPack)).setText(mText);
        
        this.mNumPcs = this.mNumPacks * mProduct.num_in_pack;
        
    }
    
    public void btnSubmitOnClick(View view) {
        if (this.mNumPacks <= 0 || this.mNumPcs <= 0) {
            Toast.makeText(this, "Количество должно быть больше нуля!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent answerIntent = new Intent();
        ArrayList<Product> singleProductArray = new ArrayList<>(1);
        singleProductArray.add(0, mProduct);
        answerIntent.putParcelableArrayListExtra(PRODUCT_ARRAY_PARCELABLE_VALUE_KEY, singleProductArray);
        answerIntent.putExtra(ORDER_ITEM_QUANTITY_VALUE_KEY, this.mNumPcs);
        if (orderItem == null) {
            orderItem = new OrderItem(this.mProduct, this.mNumPcs);
        } else {
            orderItem.product = mProduct;
            orderItem.quantity = mNumPcs;
            orderItem.reCalcAll();
        }
        answerIntent.putExtra(ORDER_ITEM_PARCELABLE_VALUE_KEY, orderItem);
        answerIntent.putExtra(ORDER_ITEM_POSITION_VALUE_KEY, orderItemPosition);

        setResult(RESULT_OK, answerIntent);
        finish();
    }
    
    public void numPackIncrease(View view) {
        this.mNumPacks = Math.round(this.mNumPacks);
        this.mNumPacks++;
        recalculatePcs();
        tvNumPacksText();
    }
    
    public void numPackDecrease(View view) {
        this.mNumPacks = Math.round(this.mNumPacks);
        if (this.mNumPacks <= 1) return;
        this.mNumPacks--;
        recalculatePcs();
        tvNumPacksText();
    }
    
    public void numPcsDecrease(View view) {
        this.mNumPcs = Math.round(mNumPcs);
        if (this.mNumPcs <= 1) return;
        this.mNumPcs--;
        recalculatePacks();
        tvNumPcsText();
    }
    
    public void numPcsIncrease(View view) {
        this.mNumPcs = Math.round(this.mNumPcs);
        this.mNumPcs++;
        recalculatePacks();
        tvNumPcsText();
    }
    
    void recalculatePacks() {
        if (this.mProduct.num_in_pack != 0) {
            this.mNumPacks = this.mNumPcs / this.mProduct.num_in_pack;
        } else {
            this.mNumPacks = 0.0;
        }
        tvNumPacksText();
    }
    
    void recalculatePcs() {
        this.mNumPcs = Math.round(this.mNumPacks * this.mProduct.num_in_pack);
        tvNumPcsText();
    }

    class AutoCompleteTextViewAdapter extends BaseAdapter implements Filterable {
        private Context context;
        private List<?> objects;

        public AutoCompleteTextViewAdapter(Context context, List<Product> objects) {
            this.context = context;
            this.objects = objects;
        }

        @Override
        public int getCount() {
            return objects.size();
        }

        @Override
        public Object getItem(int position) {
            return objects.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.select_product_list_item, parent, false);
            }
            String mText;
            Product product = (Product) getItem(position);

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

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        List<Product> productList = getProductList();
                        // Assign the data to the FilterResults
                        filterResults.values = productList;
                        filterResults.count = productList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        objects = (List<?>) results.values;
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }
    }
}
