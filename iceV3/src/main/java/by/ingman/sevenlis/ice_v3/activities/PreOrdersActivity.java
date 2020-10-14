package by.ingman.sevenlis.ice_v3.activities;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.adapters.PreOrdersExpListAdapter;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.PreOrdersListGroup;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.services.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class PreOrdersActivity extends AppCompatActivity {
    private Context context;
    private PreOrdersExpListAdapter preOrdersExpListAdapter;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preorders);

        this.context = PreOrdersActivity.this;
        this.preOrdersExpListAdapter = new PreOrdersExpListAdapter(context,getPreOrdersListGroups());

        ExpandableListView expandableListView = findViewById(R.id.groups_list);
        expandableListView.setAdapter(preOrdersExpListAdapter);
        expandableListView.setDividerHeight(0);

        registerForContextMenu(expandableListView);

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            Order order = (Order) preOrdersExpListAdapter.getChild(groupPosition, childPosition);
            Intent intent = new Intent(context, OrderActivity.class).setAction(OrderActivity.ACTION_ORDER_EDIT);
            intent.putExtra("order_id", order.orderUid);
            intent.putExtra("longDate", order.orderDate.getTime());
            startActivity(intent);
            return true;
        });

        expandableListView.setOnItemLongClickListener((parent, view, position, id) -> {
            int itemType = ExpandableListView.getPackedPositionType(id);
            return itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD;
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals(ExchangeDataService.CHANNEL_ORDERS_UPDATES)) {
                    preOrdersExpListAdapter.setGroups(getPreOrdersListGroups());
                    preOrdersExpListAdapter.notifyDataSetChanged();
                }
            }
        };
        registerReceiver(broadcastReceiver,new IntentFilter(ExchangeDataService.CHANNEL_ORDERS_UPDATES));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preorders_group_menu,menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int itemId = item.getItemId();
        if (itemId == R.id.save_orders) {
            saveOrders(groupPosition);
            return true;
        } else if (itemId == R.id.delete_orders) {
            deleteOrders(groupPosition);
            return true;
        } else if (itemId == R.id.change_date_orders) {
            changeDateOrders(groupPosition);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void saveOrders(int groupPosition) {
        DBLocal dbLocal = new DBLocal(context);
        PreOrdersListGroup preOrdersListGroup = (PreOrdersListGroup) preOrdersExpListAdapter.getGroup(groupPosition);
        List<Order> orderList = preOrdersListGroup.getOrderList();

        new AlertDialog.Builder(this)
                .setTitle("Подтверждение действия")
                .setMessage("Перевести все предварительные заказы на " + FormatsUtils.getDateFormatted(preOrdersListGroup.getDate()) + " в статус заявок на продажу?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    dbLocal.savePreOrdersAsOrders(orderList);
                    preOrdersExpListAdapter.setGroups(getPreOrdersListGroups());
                    preOrdersExpListAdapter.notifyDataSetChanged();
                }).create().show();
    }

    private void deleteOrders(int groupPosition) {
        DBLocal dbLocal = new DBLocal(context);
        PreOrdersListGroup preOrdersListGroup = (PreOrdersListGroup) preOrdersExpListAdapter.getGroup(groupPosition);
        List<Order> orderList = preOrdersListGroup.getOrderList();

        new AlertDialog.Builder(this)
                .setTitle("Подтверждение действия")
                .setMessage("Удалить все предварительные заказы на " + FormatsUtils.getDateFormatted(preOrdersListGroup.getDate()) + "?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    for (Order order : orderList) {
                        dbLocal.deleteOrder(order);
                    }
                    preOrdersExpListAdapter.setGroups(getPreOrdersListGroups());
                    preOrdersExpListAdapter.notifyDataSetChanged();
                }).create().show();
    }

    private void changeDateOrders(int groupPosition) {
        DBLocal dbLocal = new DBLocal(context);
        PreOrdersListGroup preOrdersListGroup = (PreOrdersListGroup) preOrdersExpListAdapter.getGroup(groupPosition);
        List<Order> orderList = preOrdersListGroup.getOrderList();

        DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            dbLocal.changeDateOrders(orderList, calendar.getTime());

            preOrdersExpListAdapter.setGroups(getPreOrdersListGroups());
            preOrdersExpListAdapter.notifyDataSetChanged();
        };

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(preOrdersListGroup.getDate());

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,onDateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        DatePicker datePicker = datePickerDialog.getDatePicker();
        datePicker.setCalendarViewShown(false);
        Calendar nowDate = FormatsUtils.roundDayToStart(Calendar.getInstance());
        Calendar maxDate = FormatsUtils.roundDayToEnd(Calendar.getInstance());
            maxDate.add(Calendar.DATE, SettingsUtils.Settings.getOrderDaysAhead(context));
        datePicker.setMinDate(nowDate.getTimeInMillis());
        datePicker.setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private List<PreOrdersListGroup> getPreOrdersListGroups() {
        List<PreOrdersListGroup> listGroups = new ArrayList<>();

        DBLocal dbLocal = new DBLocal(context);
        List<Date> dates = dbLocal.getPreOrdersDatesDesc();
        for (Date date : dates) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            List<Order> orders = dbLocal.getPreOrdersList(calendar);

            listGroups.add(new PreOrdersListGroup(date,orders));
        }

        return listGroups;
    }
}