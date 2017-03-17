package by.ingman.sevenlis.ice_v3;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
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

    private DBLocal dbLocal = new DBLocal(this);

    Product mProduct = null;
    EditText editTextNumPacks;
    double mNumPacks = 1.0;
    EditText editTextNumPcs;
    double mNumPcs = 1.0;
    Button buttonSubmit;
    OrderItem orderItem = null;
    int orderItemPosition = 0;

    CheckBox checkBoxInputTypeNumeric;
    EditText editTextFilter;

    LinearLayout layoutNumPcs;
    LinearLayout layoutNumPacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_order_item);

        buttonSubmit = (Button) findViewById(R.id.btnSubmit);
        buttonSubmit.setEnabled(false);

        layoutNumPcs    = (LinearLayout) findViewById(R.id.quantityPcsSelection);
        layoutNumPacks  = (LinearLayout) findViewById(R.id.quantityPacksSelection);

        editTextNumPacks = (EditText) findViewById(R.id.editTextNumPacks);
        editTextNumPacks.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocused) {
                if (editTextNumPacks.getText().toString().isEmpty() || editTextNumPacks.getText().toString().equals(".")) {
                    editTextNumPacks.setText("0.0");
                }
                if (isFocused) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    //imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.SHOW_FORCED);

                } else {
                    mNumPacks = Double.valueOf(editTextNumPacks.getText().toString());
                    recalculatePcs();
                }
            }
        });
        editTextNumPacks.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (editTextNumPacks.getText().toString().isEmpty() || editTextNumPacks.getText().toString().equals(".")) {
                    editTextNumPacks.setText("0.0");
                }
                mNumPacks = Double.valueOf(editTextNumPacks.getText().toString());
                recalculatePcs();
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    btnSubmitOnClick(editTextNumPacks);
                    return true;
                }
                return false;
            }
        });

        editTextNumPcs = (EditText) findViewById(R.id.editTextNumPcs);
        editTextNumPcs.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocused) {
                if (editTextNumPcs.getText().toString().isEmpty() || editTextNumPcs.getText().toString().equals(".")) {
                    editTextNumPcs.setText("0.0");
                }
                if (isFocused) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                } else {
                    mNumPcs = Math.round(Double.valueOf(editTextNumPcs.getText().toString()));
                    recalculatePacks();
                }
            }
        });
        editTextNumPcs.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (editTextNumPcs.getText().toString().isEmpty() || editTextNumPcs.getText().toString().equals(".")) {
                    editTextNumPcs.setText("0.0");
                }
                mNumPcs = Math.round(Double.valueOf(editTextNumPcs.getText().toString()));
                recalculatePacks();
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    btnSubmitOnClick(editTextNumPcs);
                    return true;
                }
                return false;
            }
        });

        editTextFilter = (EditText) findViewById(R.id.editTextFilter);
        editTextFilter.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    ArrayList<Product> productArrayList = getProductList();
                    if (productArrayList.size() == 1) {
                        mProduct = productArrayList.get(0);
                        refreshProductInfo();
                        tvNumPacksText();
                        recalculatePcs();
                        editTextNumPacks.requestFocus();
                    } else {
                        searchProduct(SelectOrderItemActivity.this.findViewById(R.id.textViewProduct));
                    }
                    return true;
                }
                return false;
            }
        });

        checkBoxInputTypeNumeric = (CheckBox) findViewById(R.id.checkBoxInputType);
        checkBoxInputTypeNumeric.setChecked(SettingsUtils.Settings.getItemSearchInputTypeNumeric(this));
        changeEditTextFilterInputType();

        if (getIntent().getExtras() != null) {
            orderItem = getIntent().getParcelableExtra(ORDER_ITEM_PARCELABLE_VALUE_KEY);
            orderItemPosition = getIntent().getExtras().getInt(ORDER_ITEM_POSITION_VALUE_KEY);
            if (orderItem != null) {
                this.mProduct = orderItem.product;
                this.mNumPcs = orderItem.quantity;
                this.mNumPacks = orderItem.packs;
            }
        }

        refreshProductInfo();
        if (mProduct != null) {
            tvNumPacksText();
            recalculatePcs();
            editTextNumPacks.setFocusable(true);
            editTextNumPacks.requestFocus();
        } else {
            editTextFilter.setFocusable(true);
            editTextFilter.requestFocus();
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private ArrayList<Product> getProductList() {
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
        return dbLocal.getProducts(condition, conditionArgs);
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

    public void checkBoxInputTypeOnClick(View view) {
        changeEditTextFilterInputType();
    }
    void tvNumPacksText() { editTextNumPacks.setText(String.format(Locale.ROOT,"%.1f",this.mNumPacks)); }

    void tvNumPcsText() { editTextNumPcs.setText(String.format(Locale.ROOT,"%.0f",this.mNumPcs)); }

    public void searchProduct(View view) {
        Intent intent = new Intent(this, SearchProductActivity.class);
        intent.putExtra(CHECKBOX_INPUT_TYPE_STATUS_KEY,checkBoxInputTypeNumeric.isChecked());
        intent.putExtra(STRING_FILTER_KEY,editTextFilter.getText().toString());
        startActivityForResult(intent, SEARCH_PRODUCT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data.getExtras() != null) {
            if (requestCode == SEARCH_PRODUCT_REQUEST_CODE) {
                ArrayList<Product> arraySingleProduct = data.getExtras().getParcelableArrayList(SearchProductActivity.PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY);
                if (arraySingleProduct != null) mProduct = arraySingleProduct.get(0);
                checkBoxInputTypeNumeric.setChecked(data.getExtras().getBoolean(CHECKBOX_INPUT_TYPE_STATUS_KEY));
                editTextFilter.setText(data.getExtras().getString(STRING_FILTER_KEY));
                refreshProductInfo();
                recalculatePcs();
                tvNumPacksText();
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

        mText = "Упак.: " + FormatsUtils.getNumberFormatted(dbLocal.getProductRestPacks(mProduct) - dbLocal.getProductBlockPacks(mProduct),1);
        ((TextView) findViewById(R.id.textViewRestPacks)).setText(mText);

        mText = "Кол.: " + FormatsUtils.getNumberFormatted(dbLocal.getProductRestAmount(mProduct) - dbLocal.getProductBlockAmount(mProduct),0);
        ((TextView) findViewById(R.id.textViewRestAmount)).setText(mText);

        mText = "Цена: " + FormatsUtils.getNumberFormatted(mProduct.price,2);
        ((TextView) findViewById(R.id.textViewPrice)).setText(mText);

        mText = "В упак.: " + FormatsUtils.getNumberFormatted(mProduct.num_in_pack, 1);
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
        singleProductArray.add(0,mProduct);
        answerIntent.putParcelableArrayListExtra(PRODUCT_ARRAY_PARCELABLE_VALUE_KEY,singleProductArray);
        answerIntent.putExtra(ORDER_ITEM_QUANTITY_VALUE_KEY, this.mNumPcs);

        orderItem = new OrderItem(this.mProduct,this.mNumPcs);
        answerIntent.putExtra(ORDER_ITEM_PARCELABLE_VALUE_KEY, orderItem);
        answerIntent.putExtra(ORDER_ITEM_POSITION_VALUE_KEY, orderItemPosition);

        setResult(RESULT_OK,answerIntent);
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
}
