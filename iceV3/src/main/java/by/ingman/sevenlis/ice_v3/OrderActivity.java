package by.ingman.sevenlis.ice_v3;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import by.ingman.sevenlis.ice_v3.classes.Contragent;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.classes.Point;
import by.ingman.sevenlis.ice_v3.classes.Product;
import by.ingman.sevenlis.ice_v3.classes.Storehouse;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class OrderActivity extends AppCompatActivity {
    public static final int SELECT_CONTRAGENTS_REQUEST_CODE = 0;
    public static final int SELECT_POINT_REQUEST_CODE = 1;
    public static final int SELECT_ORDER_ITEM_REQUEST_CODE = 2;
    public static final int CHANGE_ORDER_ITEM_REQUEST_CODE = 4;
    public static final int LIST_ITEM_CONTEXT_MENU_DEL = 3;
    public static final int LIST_ITEM_CONTEXT_MENU_CHANGE = 5;
    private static final int OPTIONS_MENU_ADD_ORDER_ITEM_ID = 0;
    private static final int OPTIONS_MENU_SET_ORDER_DATE_ITEM_ID = 1;
    private Context ctx;
    Order mOrder;
    Spinner advTypesSpinner;
    CheckBox isAdvCheckBox;
    Calendar orderDateCalendar;
    DatePickerDialog.OnDateSetListener onDateSetListener;
    TextView textViewOrderDate;
    Contragent mContragent;
    Point mPoint;
    Storehouse mStorehouse;
    ArrayList<OrderItem> mOrderItems;
    DBLocal dbLocal;

    View footerSummary;
    View footerSubmit;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem;
        
        menu.add(0,OPTIONS_MENU_SET_ORDER_DATE_ITEM_ID,Menu.NONE,"DATE");
        menuItem = menu.findItem(OPTIONS_MENU_SET_ORDER_DATE_ITEM_ID);
        menuItem.setIcon(R.drawable.ic_date_range_white)
                .setTitle("ДАТА")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Calendar cal = Calendar.getInstance();
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
            }
        });
        
        menu.add(0,OPTIONS_MENU_ADD_ORDER_ITEM_ID,Menu.NONE,"ADD");
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

        mStorehouse = mOrder.storehouse;
        advTypesSpinner = (Spinner) findViewById(R.id.spinner_advType);
        isAdvCheckBox = (CheckBox) findViewById(R.id.checkBox_isAdv);
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

        textViewOrderDate = (TextView) findViewById(R.id.textViewDate);

        textViewOrderDateRefresh();

        footerSummary = createFooterSummary();
        footerSubmit = createFooterSubmit();

        onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                if (orderDateCalendar == null) {
                    orderDateCalendar = Calendar.getInstance();
                }
                orderDateCalendar.set(year, monthOfYear, dayOfMonth);
                textViewOrderDateRefresh();
            }
        };

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            this.mOrder = savedInstanceState.getParcelable("ORDER");

            this.mOrderItems = savedInstanceState.getParcelableArrayList("ORDER_ITEMS");
            this.mOrder.orderItems = mOrderItems;
            refreshOrderItemsList();

            this.mContragent = mOrder.contragent;
            ((TextView) findViewById(R.id.textView_contragent)).setText(this.mContragent.getName());

            this.mPoint      = mOrder.point;
            ((TextView) findViewById(R.id.textView_point)).setText(this.mPoint.name);

            this.mStorehouse = mOrder.storehouse;

            this.orderDateCalendar.setTime(mOrder.orderDate);
            textViewOrderDateRefresh();

            isAdvCheckBox.setChecked(mOrder.isAdvertising);
            setAdvTypesSpinnerState();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ORDER", mOrder);
        outState.putParcelableArrayList("ORDER_ITEMS", mOrderItems);
    }

    public void isAdvOnClick(View view) {
        boolean isAdv = isAdvCheckBox.isChecked();
        mOrder.isAdvertising = isAdv;
        if (!isAdv) {
            mOrder.advType = 0;
        }
        setAdvTypesSpinnerState();
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
        Intent intent = new Intent(this, SelectContragentActivity.class);
        startActivityForResult(intent,SELECT_CONTRAGENTS_REQUEST_CODE);
    }

    public void startSalespointSelection() {
        Intent intent = new Intent(this, SelectSalespointActivity.class);
        intent.putExtra(SelectSalespointActivity.CONTRAGENT_CODE_KEY,this.mContragent.getCode());
        intent.putExtra(SelectSalespointActivity.CONTRAGENT_NAME_KEY,this.mContragent.getName());
        startActivityForResult(intent,SELECT_POINT_REQUEST_CODE);
    }

    public void startOrderItemSelection() {
        Intent intent = new Intent(this, SelectOrderItemActivity.class);
        startActivityForResult(intent,SELECT_ORDER_ITEM_REQUEST_CODE);
    }

    public void startOrderItemChange(OrderItem orderItem) {
        int position = mOrderItems.indexOf(orderItem);
        Intent intent = new Intent(this, SelectOrderItemActivity.class);
        intent.putExtra(SelectOrderItemActivity.ORDER_ITEM_PARCELABLE_VALUE_KEY, orderItem);
        intent.putExtra(SelectOrderItemActivity.ORDER_ITEM_POSITION_VALUE_KEY, position);
        startActivityForResult(intent,CHANGE_ORDER_ITEM_REQUEST_CODE);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.textView_contragent): {
                startContragentSelection();
            } break;
            case (R.id.textView_contr_label): {
                startContragentSelection();
            } break;

            case (R.id.textView_point): {
                if (mContragent == null) {
                    Toast.makeText(this, "Не выбран Контрагент!", Toast.LENGTH_SHORT).show();
                    break;
                }
                startSalespointSelection();
            } break;
            case (R.id.textView_point_label): {
                if (mContragent == null) {
                    Toast.makeText(this, "Не выбран Контрагент!", Toast.LENGTH_SHORT).show();
                    break;
                }
                startSalespointSelection();
            } break;
            case (R.id.buttonSubmitOrder): {
                EditText editTextComment = (EditText) findViewById(R.id.editTextComment);
                if (editTextComment != null) mOrder.comment = editTextComment.getText().toString();
                if (!checkDataFilling()) return;
                dbLocal.saveOrder(mOrder);
                Intent answerIntent = new Intent();
                answerIntent.putExtra("orderDateMillis",orderDateCalendar.getTimeInMillis());
                setResult(RESULT_OK,answerIntent);
                finish();
            }
        }
    }

    private boolean checkDataFilling() {
        boolean result = true;
        if (this.mOrder.contragent == null || this.mOrder.contragent.getCode().isEmpty()) {
            Toast.makeText(ctx, "Не выбран Контрагент!", Toast.LENGTH_SHORT).show();
            result = false;
        }
        if (this.mOrder.point == null || this.mOrder.point.code.isEmpty()) {
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
                String code = data.getExtras().getString(SelectContragentActivity.CONTRAGENT_CODE_VALUE_KEY);
                String name = data.getExtras().getString(SelectContragentActivity.CONTRAGENT_NAME_VALUE_KEY);
                this.mContragent = new Contragent(code, name);
                this.mOrder.setContragent(this.mContragent);
                ((TextView) findViewById(R.id.textView_contragent)).setText(this.mContragent.getName());
                this.mPoint = null;
                this.mOrder.point = null;
                ((TextView) findViewById(R.id.textView_point)).setText("Выберите пункт разгрузки");
                startSalespointSelection();
                return;
            }
            if (requestCode == SELECT_POINT_REQUEST_CODE) {
                String code = data.getExtras().getString(SelectSalespointActivity.SALESPOINT_CODE_VALUE_KEY);
                String name = data.getExtras().getString(SelectSalespointActivity.SALESPOINT_NAME_VALUE_KEY);
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
                    OrderItem orderItem = new OrderItem(mProduct,quantity);
                    addOrUpdateOrderItems(orderItem, quantity);
                    refreshOrderItemsList();
                    startOrderItemSelection();
                }
                return;
            }
            if (requestCode == CHANGE_ORDER_ITEM_REQUEST_CODE) {
                OrderItem oi = data.getExtras().getParcelable(SelectOrderItemActivity.ORDER_ITEM_PARCELABLE_VALUE_KEY);
                int position = data.getExtras().getInt(SelectOrderItemActivity.ORDER_ITEM_POSITION_VALUE_KEY);
                this.mOrderItems.set(position, oi);
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
                this.mOrderItems.set(i,oi);
                this.mOrder.setOrderItems(this.mOrderItems);
                return;
            }
        }
        this.mOrderItems.add(orderItem);
        this.mOrder.setOrderItems(this.mOrderItems);
    }

    private void refreshOrderItemsList() {
        EditText editTextComment = (EditText) findViewById(R.id.editTextComment);
        if (editTextComment != null) mOrder.comment = editTextComment.getText().toString();

        ListView listViewOrderItems = (ListView) findViewById(R.id.lvOrderItems);
        CustomListViewAdapter customListViewAdapter = new CustomListViewAdapter(this,mOrderItems);
        listViewOrderItems.removeFooterView(footerSummary);
        listViewOrderItems.removeFooterView(footerSubmit);
        footerSummary = createFooterSummary();
        footerSubmit = createFooterSubmit();
        listViewOrderItems.addFooterView(footerSummary,null,false);
        listViewOrderItems.addFooterView(footerSubmit,null,false);
        listViewOrderItems.setAdapter(customListViewAdapter);
        listViewOrderItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int position = (int) view.getTag();
                final OrderItem orderItem = mOrderItems.get(position);
                startOrderItemChange(orderItem);
            }

        });
        listViewOrderItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                showPopupMenu(view);
                return true;
            }
        });
    }

    private void showPopupMenu(View view) {
        final int position = (int) view.getTag();
        final OrderItem orderItem = mOrderItems.get(position);
        final PopupMenu popupMenu = new PopupMenu(ctx,view);
        popupMenu.getMenu().add(0,LIST_ITEM_CONTEXT_MENU_DEL,0,"Удалить " + orderItem.product.code + " " + orderItem.product.name + ".");
        popupMenu.getMenu().add(0,LIST_ITEM_CONTEXT_MENU_CHANGE,0,"Редактировать");
        popupMenu.setGravity(Gravity.END);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case LIST_ITEM_CONTEXT_MENU_DEL:
                        mOrderItems.remove(position);
                        refreshOrderItemsList();
                        break;
                    case LIST_ITEM_CONTEXT_MENU_CHANGE:
                        startOrderItemChange(orderItem);
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    View createFooterSummary() {
        View v = getLayoutInflater().inflate(R.layout.order_item_list_footer_summary, null);

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

        mText = "Упак.: " + FormatsUtils.getNumberFormatted(sumPacks, 1);
        ((TextView) v.findViewById(R.id.tvPacks)).setText(mText);

        mText = "Масса: " + FormatsUtils.getNumberFormatted(sumWeight, 3);
        ((TextView) v.findViewById(R.id.tvMass)).setText(mText);

        mText = "Кол-во: " + FormatsUtils.getNumberFormatted(sumAmount, 0);
        ((TextView) v.findViewById(R.id.tvAmount)).setText(mText);

        mText = "Сумма: " + FormatsUtils.getNumberFormatted(sumSumma, 2);
        ((TextView) v.findViewById(R.id.tvSum)).setText(mText);

        return v;
    }

    View createFooterSubmit() {
        View v = getLayoutInflater().inflate(R.layout.order_item_list_footer_submit, null);

        EditText editTextComment = (EditText) v.findViewById(R.id.editTextComment);
        editTextComment.setText(mOrder.comment);

        TextView textViewComment = (TextView) v.findViewById(R.id.textViewComment);
        textViewComment.setVisibility(View.GONE);

        TextView textViewAnswer = (TextView) v.findViewById(R.id.textViewAnswer);
        textViewAnswer.setVisibility(View.GONE);

        return v;
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
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    OrderActivity.super.onBackPressed();
                }
            }).create().show();
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

            mText = "Кол.: " + FormatsUtils.getNumberFormatted(orderItem.quantity, 0);
            ((TextView) view.findViewById(R.id.tvAmount)).setText(mText);

            mText = "Масса: " + FormatsUtils.getNumberFormatted(orderItem.weight, 3);
            ((TextView) view.findViewById(R.id.tvMass)).setText(mText);

            mText = "Сумма: " + FormatsUtils.getNumberFormatted(orderItem.summa, 2);
            ((TextView) view.findViewById(R.id.tvSum)).setText(mText);

            view.setTag(i);

            return view;
        }

        OrderItem getOrderItem(int i) {
            return (OrderItem) getItem(i);
        }
    }
}
