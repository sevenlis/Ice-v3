package by.ingman.sevenlis.ice_v3;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class SelectOrderItemActivity extends AppCompatActivity {
    public static final int SELECT_PRODUCT_REQUEST_CODE = 0;

    public static final String PRODUCT_ARRAY_PARCELABLE_VALUE_KEY = "by.ingman.sevenlis.ice_v3.ProductArray_parcelable";
    public static final String ORDER_ITEM_QUANTITY_VALUE_KEY = "by.ingman.sevenlis.ice_v3.orderItemQuantity";

    private DBLocal dbLocal = new DBLocal(this);

    Product mProduct = null;
    EditText editTextNumPacks;
    double mNumPacks = 1.0;
    EditText editTextNumPcs;
    double mNumPcs = 1.0;
    Button buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_order_item);

        buttonSubmit = (Button) findViewById(R.id.btnSubmit);
        buttonSubmit.setEnabled(false);

        editTextNumPacks = (EditText) findViewById(R.id.editTextNumPacks);
        editTextNumPacks.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocused) {
                if (isFocused) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                } else {
                    mNumPacks = Double.valueOf(editTextNumPacks.getText().toString().equals("") ? "0.0" : editTextNumPacks.getText().toString());
                    recalculatePcs();
                }
            }
        });
        editTextNumPacks.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                mNumPacks = Double.valueOf(editTextNumPacks.getText().toString().equals("") ? "0.0" : editTextNumPacks.getText().toString());
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
                if (isFocused) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                } else {
                    mNumPcs = Math.round(Double.valueOf(editTextNumPcs.getText().toString().equals("") ? "0.0" : editTextNumPcs.getText().toString()));
                    recalculatePacks();
                }
            }
        });
        editTextNumPcs.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                mNumPcs = Math.round(Double.valueOf(editTextNumPcs.getText().toString().equals("") ? "0.0" : editTextNumPcs.getText().toString()));
                recalculatePacks();
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    btnSubmitOnClick(editTextNumPcs);
                    return true;
                }
                return false;
            }
        });

        if (mProduct == null & getIntent().getExtras() == null) {
            selectItem(this.findViewById(R.id.textViewProduct));
        } else if (mProduct != null) {
            tvNumPacksText();
            recalculatePcs();
        }
    }

    void tvNumPacksText() { editTextNumPacks.setText(String.format(Locale.ROOT,"%.1f",this.mNumPacks)); }

    void tvNumPcsText() { editTextNumPcs.setText(String.format(Locale.ROOT,"%.0f",this.mNumPcs)); }

    public void selectItem(View view) {
        Intent intent = new Intent(this, SelectProductActivity.class);
        startActivityForResult(intent, SELECT_PRODUCT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PRODUCT_REQUEST_CODE) {
                ArrayList<Product> arraySingleProduct = data.getExtras().getParcelableArrayList(SelectProductActivity.PARCELABLE_PRODUCT_SINGLE_ARRAY_KEY);
                if (arraySingleProduct != null) mProduct = arraySingleProduct.get(0);
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
        if (mProduct == null) return;

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
