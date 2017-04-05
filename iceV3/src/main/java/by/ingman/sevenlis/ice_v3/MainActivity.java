package by.ingman.sevenlis.ice_v3;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.classes.ExchangeDataIntents;
import by.ingman.sevenlis.ice_v3.classes.OnSwipeLeftRightTouchListener;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.remote.sql.CheckApkUpdate;
import by.ingman.sevenlis.ice_v3.remote.sql.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.NotificationsUtil;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class MainActivity extends AppCompatActivity {
    private Context ctx;
    private final int REQUEST_CODE_NEW_ORDER = 0;
    private final int LIST_ITEM_CONTEXT_MENU_DEL = 0;
    private final int LIST_ITEM_CONTEXT_MENU_VIEW = 1;

    private static Calendar orderDateCalendar = null;
    private static DatePickerDialog.OnDateSetListener onDateSetListener;
    private TextView textViewOrderDate;
    private ArrayList<Order> ordersList;

    public static String[] advTypesStrings;
    public static String[] orderStatuses;

    private DBLocal dbLocal;

    private static ExchangeDataIntents exchangeDataIntents;
    private static CheckApkUpdate chkApkUpdate;

    ListView listViewOrders;
    View footerNoAdv;
    View footerIsAdv;
    View footerSummary;
    NotificationsUtil notifUtils;
    ProgressBar progressBarLoad;
    private Handler mHandler = new Handler();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ExchangeDataService.CHANNEL)) {
                refreshOrdersList(false);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        menu.setGroupVisible(R.id.exchangeServiceMenuGroup,false);
        /*MenuItem menuItem = menu.findItem(R.id.add_order);
        menuItem.setIcon(android.R.drawable.ic_input_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);*/
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.add_order: {
                Calendar newOrderDateCal = orderDateCalendar;
                Calendar nowStart = Calendar.getInstance();
                FormatsUtils.roundDayToStart(nowStart);
                if (nowStart.getTimeInMillis() > orderDateCalendar.getTimeInMillis()) {
                    Calendar tomorrow = Calendar.getInstance();
                    tomorrow.add(Calendar.DATE,1);
                    newOrderDateCal = (Calendar) tomorrow.clone();
                }
                Intent intent = new Intent(ctx, OrderActivity.class);
                intent.putExtra("calYear", newOrderDateCal.get(Calendar.YEAR));
                intent.putExtra("calMonth", newOrderDateCal.get(Calendar.MONTH));
                intent.putExtra("calDate", newOrderDateCal.get(Calendar.DAY_OF_MONTH));
                startActivityForResult(intent, REQUEST_CODE_NEW_ORDER);
            } break;
            case R.id.settings: {
                Intent intent = new Intent(ctx,SettingsActivity.class);
                startActivity(intent);
            } break;
            case R.id.update_data: {
                Intent intent = new Intent(ctx,UpdateDataActivity.class);
                startActivity(intent);
            } break;
            case R.id.stopExchangeData: {
                stopExchangeDataService();
            } break;
            case R.id.startExchangeData: {
                startExchangeDataService();
            } break;
            case R.id.refresh_list: {
                refreshOrdersList(true);
            } break;
            case R.id.about_app: {
                Intent intent = new Intent(ctx,AboutActivity.class);
                startActivity(intent);
            } break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_NEW_ORDER) {
                if (data.getExtras() != null) {
                    Long orderDateMillis = data.getExtras().getLong("orderDateMillis");
                    Calendar orderDateCal = Calendar.getInstance();
                    orderDateCal.setTimeInMillis(orderDateMillis);
                    orderDateCalendar = (Calendar) orderDateCal.clone();
                }
                refreshOrdersList(false);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(broadcastReceiver,new IntentFilter(ExchangeDataService.CHANNEL));

        ctx                 = this;
        dbLocal             = new DBLocal(ctx);
        notifUtils          = new NotificationsUtil(ctx);
        exchangeDataIntents = new ExchangeDataIntents();
        chkApkUpdate        = new CheckApkUpdate();
        progressBarLoad     = (ProgressBar) findViewById(R.id.progressBarLoad);

        advTypesStrings     = getResources().getStringArray(R.array.adv_types_strings);
        orderStatuses       = getResources().getStringArray(R.array.order_statuses);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setSubtitle("Журнал заявок");
        }

        orderDateCalendar = Calendar.getInstance();
        textViewOrderDate = (TextView) findViewById(R.id.textViewDate);
        textViewOrderDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                DatePickerDialog dateDialog = new DatePickerDialog(ctx, onDateSetListener,
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH));

                DatePicker datePicker = dateDialog.getDatePicker();
                datePicker.setCalendarViewShown(false);
                Calendar minDate = FormatsUtils.roundDayToStart(Calendar.getInstance());
                Calendar maxDate = FormatsUtils.roundDayToEnd(Calendar.getInstance());
                maxDate.add(Calendar.DATE, SettingsUtils.Settings.getOrderDaysAhead(ctx));
                datePicker.setMinDate(minDate.getTimeInMillis());
                datePicker.setMaxDate(maxDate.getTimeInMillis());

                dateDialog.show();
            }
        });
        textViewDateUpdateText();

        listViewOrders = (ListView) findViewById(R.id.listViewOrders);
        listViewOrders.setOnTouchListener(new OnSwipeLeftRightTouchListener(ctx) {
            public void onSwipeRight() {
                orderDateCalendarAddDay(-1);
            }
            public void onSwipeLeft() {
                orderDateCalendarAddDay(1);
            }
        });
        footerNoAdv     = createFooterSummary("Итого без рекламы:",0);
        footerIsAdv     = createFooterSummary("Итого реклама:",1);
        footerSummary   = createFooterSummary("Итого:",-1);

        progressBarLoad.setVisibility(View.GONE);
        listViewOrders.setVisibility(View.VISIBLE);

        refreshOrdersList(true);

        startExchangeDataService();

        onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                if (orderDateCalendar == null) {
                    orderDateCalendar = Calendar.getInstance();
                }
                orderDateCalendar.set(Calendar.YEAR, year);
                orderDateCalendar.set(Calendar.MONTH, month);
                orderDateCalendar.set(Calendar.DAY_OF_MONTH, day);

                textViewDateUpdateText();
                refreshOrdersList(true);
            }
        };

        startService(new Intent(ctx, CheckApkUpdate.class));
    }

    private void orderDateCalendarAddDay(int days) {
        Calendar now = Calendar.getInstance();

        Calendar minDateCal = (Calendar) now.clone();
        FormatsUtils.roundDayToStart(minDateCal);

        Calendar maxDateCal = (Calendar) now.clone();
        FormatsUtils.roundDayToEnd(maxDateCal);
        maxDateCal.add(Calendar.DATE, SettingsUtils.Settings.getOrderDaysAhead(ctx));

        Calendar newDateCal = (Calendar) orderDateCalendar.clone();
        newDateCal.add(Calendar.DATE, days);

        if (newDateCal.getTimeInMillis() > maxDateCal.getTimeInMillis()) return;

        orderDateCalendar.add(Calendar.DATE,days);
        textViewDateUpdateText();
        refreshOrdersList(true);
    }

    private void startExchangeDataService() {
        startService(ExchangeDataIntents.getExchangeDataServiceIntent(ctx));
        exchangeDataIntents.startExchangeDataServiceAlarm(ctx);
    }

    private void stopExchangeDataService() {
        exchangeDataIntents.stopExchangeDataServiceAlarm(ctx);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        if (SettingsUtils.Settings.getExchangeShutdownOnExit(ctx)) {
            stopExchangeDataService();
            notifUtils.dismissAllUpdateNotifications();
        }
        chkApkUpdate.cancelUpdateAvailableNotification();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    private void viewOrder(Order order) {
        Intent orderViewIntent = new Intent(ctx, OrderViewActivity.class);
        orderViewIntent.setAction(order.orderUid);
        startActivity(orderViewIntent);
    }

    private void refreshOrdersList(final Boolean showProgress) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (showProgress) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBarLoad.setVisibility(View.VISIBLE);
                            listViewOrders.setVisibility(View.GONE);
                        }
                    });
                }

                DBLocal dbLocalThread = new DBLocal(ctx);
                ordersList = dbLocalThread.getOrdersList(orderDateCalendar);

                if (showProgress) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBarLoad.setVisibility(View.GONE);
                            listViewOrders.setVisibility(View.VISIBLE);
                        }
                    });
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshOrdersListView();
                    }
                });
            }
        }).start();
    }

    private void refreshOrdersListView() {
        textViewDateUpdateText();

        CustomOrderListAdapter customOrderListAdapter = new CustomOrderListAdapter(ctx, ordersList);
        listViewOrders.removeFooterView(footerNoAdv);
        listViewOrders.removeFooterView(footerIsAdv);
        listViewOrders.removeFooterView(footerSummary);
        footerNoAdv     = createFooterSummary("Итого без рекламы:",0);
        footerIsAdv     = createFooterSummary("Итого реклама:",1);
        footerSummary   = createFooterSummary("Итого:",-1);
        if (ordersList.size() != 0) {
            listViewOrders.addFooterView(footerNoAdv,null,false);
            listViewOrders.addFooterView(footerIsAdv,null,false);
            listViewOrders.addFooterView(footerSummary,null,false);
        }
        listViewOrders.setAdapter(customOrderListAdapter);
        listViewOrders.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Order order = ordersList.get(i);
                viewOrder(order);
            }
        });
        listViewOrders.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                showPopupMenu(view);
                return true;
            }
        });
        listViewOrders.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {}
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void showPopupMenu(View view) {
        final int position = (int) view.getTag();
        final Order order = ordersList.get(position);
        final PopupMenu popupMenu = new PopupMenu(ctx,view);
        popupMenu.getMenu().add(0,LIST_ITEM_CONTEXT_MENU_DEL,0,"Удалить заявку ID = " + order.orderUid + ".");
        popupMenu.getMenu().add(0,LIST_ITEM_CONTEXT_MENU_VIEW,0,"Просмотр заявки ID = " + order.orderUid + ".");
        popupMenu.setGravity(Gravity.END);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case LIST_ITEM_CONTEXT_MENU_DEL:
                        if (order.status != 0) {
                            Toast.makeText(ctx, "Заявка с ID = " + order.orderUid + " уже в работе. Удаление запрещено.", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        dbLocal.deleteOrder(order);
                        refreshOrdersList(false);
                        break;
                    case LIST_ITEM_CONTEXT_MENU_VIEW: {
                        viewOrder(order);
                    } break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private class CustomOrderListAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        ArrayList<Order> objects;

        CustomOrderListAdapter(Context context, ArrayList<Order> orders) {
            this.ctx = context;
            this.objects = orders;
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
                view = layoutInflater.inflate(R.layout.orders_list_item, viewGroup, false);
            }

            Order order = getOrder(i);

            String mText;

            ((TextView) view.findViewById(R.id.textView_date)).setText(order.getOrderDateString());
            ((TextView) view.findViewById(R.id.textView_orderId)).setText(order.orderUid);
            ((TextView) view.findViewById(R.id.textView_сontragent)).setText(order.contragentName);
            ((TextView) view.findViewById(R.id.textView_point)).setText(order.pointName);
            mText = "Кол.упак: " + order.getPacksString();
            ((TextView) view.findViewById(R.id.textView_qtyPacks)).setText(mText);
            mText = "Кол.ед.:  " + order.getQuantityString();
            ((TextView) view.findViewById(R.id.textView_quantity)).setText(mText);
            mText = "Масса: "    + order.getWeightString();
            ((TextView) view.findViewById(R.id.textView_weight)).setText(mText);
            mText = "Сумма: "    + order.getSummaString();
            ((TextView) view.findViewById(R.id.textView_summa)).setText(mText);
            mText = "Реклама: "  + order.getIsAdvString();
            ((TextView) view.findViewById(R.id.textView_adv)).setText(mText);
            ((TextView) view.findViewById(R.id.textView_advType)).setText(getAdvTypeString(order));
            ((TextView) view.findViewById(R.id.textView_comment)).setText(order.comment);
            ((TextView) view.findViewById(R.id.textView_dateUnload)).setText(getDateTimeUnloadString(order));

            TextView textViewStatus = (TextView) view.findViewById(R.id.textView_status);
            textViewStatus.setText(getOrderStatusString(order.status));
            textViewStatus.setTextColor(order.getStatusResultColor(ctx));

            view.setTag(i);

            return view;
        }

        Order getOrder(int i) {
            return (Order) getItem(i);
        }
    }

    private String getDateTimeUnloadString(Order order) {
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTimeInMillis(order.dateUnload);
        String sD = String.format(Locale.ROOT,"%02d",dateCal.get(Calendar.DAY_OF_MONTH));
        String sM = String.format(Locale.ROOT,"%02d",dateCal.get(Calendar.MONTH) + 1);
        String sY = String.format(Locale.ROOT,"%04d",dateCal.get(Calendar.YEAR));
        String sHour = String.format(Locale.ROOT,"%02d",dateCal.get(Calendar.HOUR_OF_DAY));
        String sMinute = String.format(Locale.ROOT,"%02d",dateCal.get(Calendar.MINUTE));
        String sSecond = String.format(Locale.ROOT,"%02d",dateCal.get(Calendar.SECOND));
        return sD + "." + sM + "." + sY + "  " + sHour + ":" + sMinute + ":" + sSecond;
    }

    public void textViewDateUpdateText() {
        textViewOrderDate.setText(FormatsUtils.getDateFormatted(orderDateCalendar.getTime()));
    }

    public String getAdvTypeString(Order order) {
        if (!order.isAdvertising) return "";
        return advTypesStrings[order.advType];
    }

    public String getOrderStatusString(int idx) {
        return orderStatuses[idx];
    }

    View createFooterSummary(String caption, int isAdv) {
        if (ordersList == null) ordersList = new ArrayList<>();
        View v = getLayoutInflater().inflate(R.layout.order_item_list_footer_summary, null);

        ((TextView) v.findViewById(R.id.textViewCaption)).setText(caption);

        double sumPacks     = 0;
        double sumAmount    = 0;
        double sumSumma     = 0;
        double sumWeight    = 0;
        for (Order order : ordersList) {
            if (isAdv != -1 && order.getIsAdvInteger() != isAdv) {
                continue;
            }
            sumPacks    += order.packs;
            sumAmount   += order.quantity;
            sumSumma    += order.summa;
            sumWeight   += order.weight;
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
}