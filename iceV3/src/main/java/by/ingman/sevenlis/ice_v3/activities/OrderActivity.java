package by.ingman.sevenlis.ice_v3.activities;

import static java.util.UUID.randomUUID;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Agreement;
import by.ingman.sevenlis.ice_v3.classes.Contragent;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.classes.Point;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.classes.Storehouse;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.services.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class OrderActivity extends AppCompatActivity {
    public static final String ACTION_ORDER_COPY = OrderActivity.class.getSimpleName() + "_ACTION_ORDER_COPY";
    public static final String ACTION_ORDER_EDIT = OrderActivity.class.getSimpleName() + "_ACTION_ORDER_EDIT";
    public static final int SELECT_CONTRAGENTS_REQUEST_CODE = 0;
    public static final int SELECT_POINT_REQUEST_CODE = 1;
    public static final int SELECT_ORDER_ITEM_REQUEST_CODE = 2;
    public static final int CHANGE_ORDER_ITEM_REQUEST_CODE = 4;
    public static final int LIST_ITEM_CONTEXT_MENU_DEL = 3;
    public static final int LIST_ITEM_CONTEXT_MENU_CHANGE = 5;
    private static final int OPTIONS_MENU_ADD_ORDER_ITEM_ID = 0;
    private static final int OPTIONS_MENU_SET_ORDER_DATE_ITEM_ID = 1;
    private Order mOrder;
    private Spinner advTypesSpinner;
    private CheckBox isAdvCheckBox;
    private Calendar orderDateCalendar;
    private DatePickerDialog.OnDateSetListener onDateSetListener;
    private TextView textViewOrderDate;
    private Storehouse mStorehouse;
    private Contragent mContragent;
    private Agreement mAgreement;
    private Point mPoint;
    private ArrayList<OrderItem> mOrderItems;
    private DBLocal dbLocal;
    private View footerSummary;
    private View footerSubmit;
    private Context ctx;
    private ViewGroup orderViewGroup = null;
    private CustomListViewAdapter customListViewAdapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem;
        
        menu.add(0, OPTIONS_MENU_SET_ORDER_DATE_ITEM_ID, Menu.NONE, "DATE");
        menuItem = menu.findItem(OPTIONS_MENU_SET_ORDER_DATE_ITEM_ID);
        menuItem.setIcon(R.drawable.ic_date_range_white)
                .setTitle("ДАТА")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menuItem.setOnMenuItemClickListener(item -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(mOrder.orderDate);
            DatePickerDialog dateDialog = new DatePickerDialog(ctx, onDateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            DatePicker datePicker = dateDialog.getDatePicker();
            datePicker.setCalendarViewShown(false);
            Calendar nowDate = FormatsUtils.roundDayToStart(Calendar.getInstance());
            Calendar maxDate = FormatsUtils.roundDayToEnd(Calendar.getInstance());
            maxDate.add(Calendar.DATE, SettingsUtils.Settings.getOrderDaysAhead(ctx));
            datePicker.setMinDate(nowDate.getTimeInMillis());
            datePicker.setMaxDate(maxDate.getTimeInMillis());

            dateDialog.show();

            return false;
        });
        
        menu.add(0, OPTIONS_MENU_ADD_ORDER_ITEM_ID, Menu.NONE, "ADD");
        menuItem = menu.findItem(OPTIONS_MENU_ADD_ORDER_ITEM_ID);
        menuItem.setIcon(R.drawable.ic_add_circle_outline_white)
                .setTitle("ДОБАВИТЬ")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == OPTIONS_MENU_ADD_ORDER_ITEM_ID) {
            startOrderItemSelection();
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        
        ctx = this;
        dbLocal = new DBLocal(ctx);
        
        mOrder = new Order(ctx);
        mOrderItems = new ArrayList<>();

        if (savedInstanceState != null) {
            this.mOrder = savedInstanceState.getParcelable("ORDER");

            this.mOrderItems = savedInstanceState.getParcelableArrayList("ORDER_ITEMS");
            this.mOrder.orderItems = mOrderItems;
        } else if (getIntent().getAction() != null && getIntent().getExtras() != null) {
            if (getIntent().getAction().equals(ACTION_ORDER_EDIT) || getIntent().getAction().equals(ACTION_ORDER_COPY)) {
                this.mOrder = dbLocal.getOrder(getIntent().getExtras().getString("order_id"));
                this.mOrderItems = (ArrayList<OrderItem>) this.mOrder.orderItems;
            }
            if (getIntent().getAction().equals(ACTION_ORDER_COPY)) {
                this.mOrder.orderUid = randomUUID().toString();
            }
        }

        mStorehouse = mOrder.storehouse;
        ((TextView) findViewById(R.id.textView_storehouse)).setText(mStorehouse.name);

        mAgreement = mOrder.agreement;
    
        advTypesSpinner = findViewById(R.id.spinner_advType);
        isAdvCheckBox = findViewById(R.id.checkBox_isAdv);

        Spinner orderTypesSpinner = findViewById(R.id.spinner_orderType);
        orderTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mOrder.orderType = getOrderType(i);
                if (mOrder.orderType == -1)
                    mOrder.isAdvertising = false;
                setAdvVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        orderTypesSpinner.setSelection(getOrderTypesSpinnerPosition(mOrder));

        advTypesSpinner.setSelection(mOrder.advType);
        advTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mOrder.advType = i;
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        
        setAdvTypesSpinnerState();
        
        orderDateCalendar = Calendar.getInstance();
        if (getIntent().getExtras() != null) {
            long longDate = getIntent().getExtras().getLong("longDate");
            orderDateCalendar.setTimeInMillis(longDate);
        }
        
        textViewOrderDate = findViewById(R.id.textViewDate);
        
        textViewOrderDateRefresh();
        
        footerSummary = getLayoutInflater().inflate(R.layout.order_item_list_footer_summary, orderViewGroup);

        footerSubmit = getLayoutInflater().inflate(R.layout.order_item_list_footer_submit, orderViewGroup);
        
        onDateSetListener = (datePicker, year, monthOfYear, dayOfMonth) -> {
            if (orderDateCalendar == null) {
                orderDateCalendar = Calendar.getInstance();
            }
            orderDateCalendar.set(year, monthOfYear, dayOfMonth);
            textViewOrderDateRefresh();
        };
        
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        if (savedInstanceState != null || Objects.equals(getIntent().getAction(), ACTION_ORDER_EDIT) || Objects.equals(getIntent().getAction(), ACTION_ORDER_COPY)) {
            this.mContragent = mOrder.contragent;
            ((TextView) findViewById(R.id.textView_contragent)).setText(this.mContragent.getName());
            if (this.mContragent.isInStop())
                ((TextView) findViewById(R.id.textView_contragent)).setTextColor(getResources().getColor(R.color.color_red));
            
            this.mPoint = mOrder.point;
            ((TextView) findViewById(R.id.textView_point)).setText(this.mPoint.name);
            
            this.orderDateCalendar.setTime(mOrder.orderDate);
            textViewOrderDateRefresh();
            
            isAdvCheckBox.setChecked(mOrder.isAdvertising);
            setAdvTypesSpinnerState();

            this.mAgreement = mOrder.agreement;
            ((TextView) findViewById(R.id.textView_agreement)).setText(this.mAgreement.getName());

            this.mStorehouse = mOrder.storehouse;
            ((TextView) findViewById(R.id.textView_storehouse)).setText(this.mStorehouse.name);
        }

        ListView listViewOrderItems = findViewById(R.id.lvOrderItems);
        customListViewAdapter = new CustomListViewAdapter(this, mOrderItems);
        listViewOrderItems.addFooterView(footerSummary, null, false);
        listViewOrderItems.addFooterView(footerSubmit, null, false);
        listViewOrderItems.setAdapter(customListViewAdapter);
        listViewOrderItems.setOnItemClickListener((adapterView, view, i, l) -> {
            final int position = (int) view.getTag();
            final OrderItem orderItem = mOrderItems.get(position);
            startOrderItemChange(orderItem,position);
        });
        listViewOrderItems.setOnItemLongClickListener((adapterView, view, i, l) -> {
            showPopupMenu(view);
            return true;
        });

        EditText editTextComment = footerSubmit.findViewById(R.id.editTextComment);
        editTextComment.setText(mOrder.comment);

        refreshOrderItemsList();
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        EditText editTextComment = footerSubmit.findViewById(R.id.editTextComment);
        if (editTextComment != null) mOrder.comment = editTextComment.getText().toString();

        outState.putParcelable("ORDER", mOrder);
        outState.putParcelableArrayList("ORDER_ITEMS", mOrderItems);
    }

    private int getOrderTypesSpinnerPosition(Order order) {
        int orderType = order.orderType;
        switch (orderType) {
            case 1:
                return 0;
            case -1:
                return 1;
            case -20:
                return 2;
        }
        return 0;
    }

    private int getOrderType(int orderTypeSpinnerPosition) {
        switch (orderTypeSpinnerPosition) {
            case 0:
                return 1;
            case 1:
                return -1;
            case 2:
                return -20;
        }
        return 1;
    }
    
    public void isAdvOnClick(View view) {
        boolean isAdv = isAdvCheckBox.isChecked();
        mOrder.isAdvertising = isAdv;
        if (!isAdv) {
            mOrder.advType = 0;
        }
        setAdvTypesSpinnerState();
    }

    public void setAdvVisibility() {
        if (mOrder.orderType == 1 || mOrder.orderType == -20) {
            isAdvCheckBox.setVisibility(View.VISIBLE);
            advTypesSpinner.setVisibility(View.VISIBLE);
        } else {
            isAdvCheckBox.setVisibility(View.GONE);
            advTypesSpinner.setVisibility(View.GONE);
        }
    }
    
    public void setAdvTypesSpinnerState() {
        advTypesSpinner.setEnabled(isAdvCheckBox.isChecked());
        advTypesSpinner.setSelection(mOrder.advType);
    }
    
    public void textViewOrderDateRefresh() {
        this.mOrder.orderDate = this.orderDateCalendar.getTime();
        textViewOrderDate.setText(FormatsUtils.getDateFormatted(this.orderDateCalendar.getTime()));
    }
    
    public void startContragentSelection() {
        Intent intent = new Intent(this, SelectCounterPartyActivity.class);
        startActivityForResult(intent, SELECT_CONTRAGENTS_REQUEST_CODE);
    }
    
    public void startSalespointSelection() {
        Intent intent = new Intent(this, SelectSalesPointActivity.class);
        intent.putExtra(SelectSalesPointActivity.CONTRAGENT_CODE_KEY, this.mContragent.getCode());
        intent.putExtra(SelectSalesPointActivity.CONTRAGENT_NAME_KEY, this.mContragent.getName());
        startActivityForResult(intent, SELECT_POINT_REQUEST_CODE);
    }
    
    public void startOrderItemSelection() {
        Intent intent = new Intent(this, SelectOrderItemActivity.class);
        intent.putExtra(SelectOrderItemActivity.STOREHOUSE_CODE_KEY, mStorehouse.code);
        intent.putExtra(SelectOrderItemActivity.ORDER_TYPE_KEY, mOrder.getOrderType());
        startActivityForResult(intent, SELECT_ORDER_ITEM_REQUEST_CODE);
    }
    
    public void startOrderItemChange(OrderItem orderItem, int position) {
        Intent intent = new Intent(this, SelectOrderItemActivity.class);
        intent.putExtra(SelectOrderItemActivity.ORDER_ITEM_PARCELABLE_VALUE_KEY, orderItem);
        intent.putExtra(SelectOrderItemActivity.ORDER_ITEM_POSITION_VALUE_KEY, position);
        intent.putExtra(SelectOrderItemActivity.STOREHOUSE_CODE_KEY, mStorehouse.code);
        startActivityForResult(intent, CHANGE_ORDER_ITEM_REQUEST_CODE);
    }

    public void startAgreementSelection() {
        final List<Agreement> agreements = dbLocal.getAgreements(mContragent);

        DialogInterface.OnClickListener onClickListener = (dialogInterface, which) -> {
            ListView listView = ((AlertDialog) dialogInterface).getListView();
            if (which != Dialog.BUTTON_NEGATIVE && listView.getCheckedItemPosition() != -1) {
                mAgreement = agreements.get(listView.getCheckedItemPosition());
                ((TextView) findViewById(R.id.textView_agreement)).setText(mAgreement.getName());
                mOrder.setAgreement(mAgreement);
                dialogInterface.dismiss();
            }
        };

        int checkedItem = -1;
        for (int i = 0; i < agreements.size(); i++) {
            if (agreements.get(i).getId().equals(mAgreement.getId()))
                checkedItem = i;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.agreement_label);
        builder.setPositiveButton("ВЫБРАТЬ", onClickListener);
        builder.setNegativeButton("ОТМЕНА", onClickListener);
        ArrayAdapter<Agreement> arrayAdapter = new ArrayAdapter<>(ctx, android.R.layout.select_dialog_singlechoice, agreements);
        builder.setSingleChoiceItems(arrayAdapter, checkedItem, onClickListener).create().show();

    }

    public void startStorehouseSelection() {
        final List<Storehouse> storehouses = dbLocal.getStorehouses();

        DialogInterface.OnClickListener onClickListener = (dialogInterface, which) -> {
            ListView listView = ((AlertDialog) dialogInterface).getListView();
            if (which != Dialog.BUTTON_NEGATIVE && listView.getCheckedItemPosition() != -1) {
                mStorehouse = storehouses.get(listView.getCheckedItemPosition());
                ((TextView) findViewById(R.id.textView_storehouse)).setText(mStorehouse.name);
                mOrder.setStorehouse(mStorehouse);
                dialogInterface.dismiss();
            }
        };

        int checkedItem = -1;
        for (int i = 0; i < storehouses.size(); i++) {
            if (storehouses.get(i).code.equals(mStorehouse.code))
                checkedItem = i;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.storehouse_label);
        builder.setPositiveButton("ВЫБРАТЬ", onClickListener);
        builder.setNegativeButton("ОТМЕНА", onClickListener);
        ArrayAdapter<Storehouse> arrayAdapter = new ArrayAdapter<>(ctx, android.R.layout.select_dialog_singlechoice, storehouses);
        builder.setSingleChoiceItems(arrayAdapter, checkedItem, onClickListener).create().show();
    }
    
    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.textView_storehouse):
            case (R.id.textView_storehouse_label): {
                startStorehouseSelection();
            }
            break;

            case (R.id.textView_contragent):
            case (R.id.textView_contr_label): {
                startContragentSelection();
            }
            break;

            case (R.id.textView_agreement):
            case (R.id.textView_agreement_label): {
                if (mContragent == null) {
                    Toast.makeText(this, "Не выбран Контрагент!", Toast.LENGTH_SHORT).show();
                    break;
                }
                startAgreementSelection();
            }
            break;

            case (R.id.textView_point):
            case (R.id.textView_point_label): {
                if (mContragent == null) {
                    Toast.makeText(this, "Не выбран Контрагент!", Toast.LENGTH_SHORT).show();
                    break;
                }
                startSalespointSelection();
            }
            break;
            
            case (R.id.buttonSubmitOrder): {
                EditText editTextComment = findViewById(R.id.editTextComment);
                if (editTextComment != null) mOrder.comment = editTextComment.getText().toString();
                if (!checkDataFilling()) return;

                new Thread(() -> {
                    dbLocal.saveOrder(mOrder);
                    Intent answerIntent = new Intent();
                    answerIntent.putExtra("orderDateMillis", orderDateCalendar.getTimeInMillis());
                    setResult(RESULT_OK, answerIntent);
                    if (Objects.equals(getIntent().getAction(), ACTION_ORDER_COPY) || Objects.equals(getIntent().getAction(), ACTION_ORDER_EDIT)) {
                        dbLocal.deleteRemoteAnswerResult(mOrder.orderUid);
                        Intent intent = new Intent(ExchangeDataService.CHANNEL_ORDERS_UPDATES);
                        sendBroadcast(intent);
                    }
                    finish();
                }).start();

            }
            break;
        }
    }
    
    private boolean checkDataFilling() {
        boolean result = true;
        if (this.mOrder.contragent == null || this.mOrder.contragent.getName().isEmpty()) {
            Toast.makeText(ctx, "Не выбран Контрагент!", Toast.LENGTH_SHORT).show();
            result = false;
        }
        if (this.mOrder.point == null || this.mOrder.point.getName().isEmpty()) {
            Toast.makeText(ctx, "Не выбран Пункт разгрузки!", Toast.LENGTH_SHORT).show();
            result = false;
        }
        if (this.mOrder.orderDate == null) {
            Toast.makeText(ctx, "Нет Даты заявки!", Toast.LENGTH_SHORT).show();
            result = false;
        }
        if (this.mOrder.orderItems.size() == 0) {
            Toast.makeText(ctx, "Пустая заявка!", Toast.LENGTH_SHORT).show();
            result = false;
        }
        return result;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data.getExtras() != null) {
            if (requestCode == SELECT_CONTRAGENTS_REQUEST_CODE) {
                String code = data.getExtras().getString(SelectCounterPartyActivity.CONTRAGENT_CODE_VALUE_KEY);
                String name = data.getExtras().getString(SelectCounterPartyActivity.CONTRAGENT_NAME_VALUE_KEY);
                this.mContragent = new Contragent(code, name);
                this.mContragent.setInStop(data.getExtras().getBoolean(SelectCounterPartyActivity.CONTRAGENT_STOP_VALUE_KEY));
                this.mOrder.setContragent(this.mContragent);
                ((TextView) findViewById(R.id.textView_contragent)).setText(this.mContragent.getName());
                if (this.mContragent.isInStop())
                    ((TextView) findViewById(R.id.textView_contragent)).setTextColor(getResources().getColor(R.color.color_red));
                this.mPoint = null;
                this.mOrder.point = null;
                ((TextView) findViewById(R.id.textView_point)).setText(getResources().getString(R.string.select_salepoint));
                this.mAgreement = new Agreement("","");
                this.mOrder.setAgreement(this.mAgreement);
                ((TextView) findViewById(R.id.textView_agreement)).setText(getResources().getString(R.string.select_agreement));
                startSalespointSelection();
                return;
            }
            if (requestCode == SELECT_POINT_REQUEST_CODE) {
                String code = data.getExtras().getString(SelectSalesPointActivity.SALESPOINT_CODE_VALUE_KEY);
                String name = data.getExtras().getString(SelectSalesPointActivity.SALESPOINT_NAME_VALUE_KEY);
                this.mPoint = new Point(code, name);
                this.mOrder.setPoint(this.mPoint);
                ((TextView) findViewById(R.id.textView_point)).setText(this.mPoint.name);
                return;
            }
            if (requestCode == SELECT_ORDER_ITEM_REQUEST_CODE) {
                Product mProduct = null;
                ArrayList<Product> arraySingleProduct = data.getExtras().getParcelableArrayList(SelectOrderItemActivity.PRODUCT_ARRAY_PARCELABLE_VALUE_KEY);
                if (arraySingleProduct != null) mProduct = arraySingleProduct.get(0);
                double quantity = data.getExtras().getDouble(SelectOrderItemActivity.ORDER_ITEM_QUANTITY_VALUE_KEY);
                if (mProduct != null) {
                    OrderItem orderItem = new OrderItem(mProduct, quantity);
                    addOrUpdateOrderItems(orderItem, quantity);
                    refreshOrderItemsList();
                    startOrderItemSelection();
                }
                return;
            }
            if (requestCode == CHANGE_ORDER_ITEM_REQUEST_CODE) {
                OrderItem oi = data.getExtras().getParcelable(SelectOrderItemActivity.ORDER_ITEM_PARCELABLE_VALUE_KEY);
                int position = data.getExtras().getInt(SelectOrderItemActivity.ORDER_ITEM_POSITION_VALUE_KEY);

                int sameItemPosition = position;
                for (OrderItem mOrderItem : mOrderItems) {
                    assert oi != null;
                    if (mOrderItem.getProductCode().equals(oi.getProductCode())) {
                        sameItemPosition = mOrderItems.indexOf(mOrderItem);
                    }
                }
                if (sameItemPosition == position) {
                    this.mOrderItems.set(position, oi);
                } else {
                    addOrUpdateOrderItems(oi,oi.quantity);
                    mOrderItems.remove(position);
                }

                this.mOrder.setOrderItems(this.mOrderItems);
                refreshOrderItemsList();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void addOrUpdateOrderItems(OrderItem orderItem, double quantity) {
        for (int i = 0; i <= this.mOrderItems.size() - 1; i++) {
            OrderItem oi = this.mOrderItems.get(i);
            if (oi.product.code.equals(orderItem.product.code)) {
                oi.quantity += quantity;
                oi.reCalcAll();
                this.mOrderItems.set(i, oi);
                this.mOrder.setOrderItems(this.mOrderItems);
                return;
            }
        }
        this.mOrderItems.add(orderItem);
        this.mOrder.setOrderItems(this.mOrderItems);
    }
    
    private void refreshOrderItemsList() {
        customListViewAdapter.notifyDataSetChanged();
        updateFooterSummary();
        updateFooterSubmit();
        setFootersVisibility();
    }
    
    private void showPopupMenu(View view) {
        final int position = (int) view.getTag();
        final OrderItem orderItem = mOrderItems.get(position);
        final PopupMenu popupMenu = new PopupMenu(ctx, view);
        popupMenu.getMenu().add(0, LIST_ITEM_CONTEXT_MENU_DEL, 0, "Удалить " + orderItem.product.code + " " + orderItem.product.name + ".");
        popupMenu.getMenu().add(0, LIST_ITEM_CONTEXT_MENU_CHANGE, 0, "Редактировать");
        popupMenu.setGravity(Gravity.END);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case LIST_ITEM_CONTEXT_MENU_DEL:
                    mOrderItems.remove(position);
                    refreshOrderItemsList();
                    break;
                case LIST_ITEM_CONTEXT_MENU_CHANGE:
                    startOrderItemChange(orderItem, position);
                    break;
            }
            return false;
        });
        popupMenu.show();
    }

    private void setFootersVisibility() {
        if (mOrderItems.size() == 0) {
            footerSummary.setVisibility(View.GONE);
            footerSubmit.setVisibility(View.GONE);
        } else {
            footerSummary.setVisibility(View.VISIBLE);
            footerSubmit.setVisibility(View.VISIBLE);
        }
    }

    private void updateFooterSummary() {
        double sumPacks = 0;
        double sumAmount = 0;
        double sumSumma = 0;
        double sumWeight = 0;
        for (OrderItem orderItem : mOrderItems) {
            sumPacks += orderItem.packs;
            sumAmount += orderItem.quantity;
            sumSumma += orderItem.summa;
            sumWeight += orderItem.weight;
        }
        String mText;

        mText = "Упак.:  " + FormatsUtils.getNumberFormatted(sumPacks, 1);
        ((TextView) footerSummary.findViewById(R.id.tvPacks)).setText(mText);

        mText = "Кол-во: " + FormatsUtils.getNumberFormatted(sumAmount, 0);
        ((TextView) footerSummary.findViewById(R.id.tvAmount)).setText(mText);

        mText = "Масса: " + FormatsUtils.getNumberFormatted(sumWeight, 3);
        ((TextView) footerSummary.findViewById(R.id.tvMass)).setText(mText);

        mText = "Сумма: " + FormatsUtils.getNumberFormatted(sumSumma, 2);
        ((TextView) footerSummary.findViewById(R.id.tvSum)).setText(mText);
    }
    
    private void updateFooterSubmit() {
        EditText editTextComment = footerSubmit.findViewById(R.id.editTextComment);
        editTextComment.setText(mOrder.comment);
        
        TextView textViewComment = footerSubmit.findViewById(R.id.textViewComment);
        textViewComment.setVisibility(View.GONE);

        TextView textViewAnswer = footerSubmit.findViewById(R.id.textViewAnswer);
        textViewAnswer.setVisibility(View.GONE);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Заявка не сохранена.")
                .setMessage("Выйти без сохранения заявки?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (arg0, arg1) ->
                        OrderActivity.super.onBackPressed()).create().show();
    }
    
    
    private class CustomListViewAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        ArrayList<OrderItem> objects;

        CustomListViewAdapter(Context context, ArrayList<OrderItem> objects) {
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
            orderViewGroup = viewGroup;
            
            View view = customView;
            if (view == null) {
                view = layoutInflater.inflate(R.layout.order_item_list_item, viewGroup, false);
            }
            String mText;
            OrderItem orderItem = getOrderItem(i);
            
            mText = orderItem.product.code + " " + orderItem.product.name;
            ((TextView) view.findViewById(R.id.tvProduct)).setText(mText);
            
            mText = "Упак.: " + FormatsUtils.getNumberFormatted(orderItem.packs, 1);
            ((TextView) view.findViewById(R.id.tvAmountPacks)).setText(mText);
            
            mText = "Кол-во:" + FormatsUtils.getNumberFormatted(orderItem.quantity, 0);
            ((TextView) view.findViewById(R.id.tvAmount)).setText(mText);
            
            mText = "Масса:" + FormatsUtils.getNumberFormatted(orderItem.weight, 3);
            ((TextView) view.findViewById(R.id.tvMass)).setText(mText);
            
            mText = "Сумма:" + FormatsUtils.getNumberFormatted(orderItem.summa, 2);
            ((TextView) view.findViewById(R.id.tvSum)).setText(mText);
            
            view.setTag(i);

            ImageButton buttonIncreasePacks = view.findViewById(R.id.ibIncrease);
            buttonIncreasePacks.setOnClickListener(view1 -> {
                getOrderItem(i).increasePacks();
                updateFooterSummary();
                notifyDataSetChanged();
            });

            ImageButton buttonDecreasePacks = view.findViewById(R.id.ibDecrease);
            buttonDecreasePacks.setOnClickListener(view12 -> {
                getOrderItem(i).decreasePacks();
                updateFooterSummary();
                notifyDataSetChanged();
            });

            return view;
        }
        
        OrderItem getOrderItem(int i) {
            return (OrderItem) getItem(i);
        }
    }
}
