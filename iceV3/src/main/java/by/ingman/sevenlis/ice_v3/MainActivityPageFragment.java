package by.ingman.sevenlis.ice_v3;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class MainActivityPageFragment extends Fragment {
    private Context ctx;
    
    public Calendar orderDateCalendar;
    
    public static String[] advTypesStrings;
    public static String[] orderStatuses;
    
    ListView listViewOrders;
    View footerNoAdv;
    View footerIsAdv;
    View footerSummary;
    ProgressBar progressBarLoad;
    LayoutInflater layoutInflater;
    
    private Handler mHandler = new Handler();
    private ArrayList<Order> ordersList = new ArrayList<>();
    
    public static MainActivityPageFragment newInstance(Context context, Date date) {
        MainActivityPageFragment pageFragment = new MainActivityPageFragment();
        pageFragment.ctx = context;
        pageFragment.orderDateCalendar = Calendar.getInstance();
        pageFragment.orderDateCalendar.setTime(date);
        FormatsUtils.roundDayToStart(pageFragment.orderDateCalendar);
        return pageFragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderDateCalendar == null) {
            orderDateCalendar = Calendar.getInstance();
            if (savedInstanceState != null) {
                long dateMillis = savedInstanceState.getLong("orderDateLong");
                orderDateCalendar.setTimeInMillis(dateMillis);
            }
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("orderDateLong", orderDateCalendar.getTimeInMillis());
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.ctx = context;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.activity_main_page, container, false);
        
        layoutInflater = inflater;
    
        View footerView = new View(ctx);
    
        listViewOrders = (ListView) resultView.findViewById(R.id.listViewOrders);
        footerNoAdv     = footerView;
        footerIsAdv     = footerView;
        footerSummary   = footerView;
    
        progressBarLoad = (ProgressBar) resultView.findViewById(R.id.progressBarLoad);
        progressBarLoad.setVisibility(View.GONE);
        listViewOrders.setVisibility(View.VISIBLE);
        
        orderStatuses = getResources().getStringArray(R.array.order_statuses);
        advTypesStrings = getResources().getStringArray(R.array.adv_types_strings);
        
        refreshOrdersList(true);
    
        return resultView;
    }
    
    public void refreshOrdersList(final Boolean showProgress) {
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
    
    public void refreshOrdersList(final Boolean showProgress, Calendar dateCalendar) {
        orderDateCalendar = dateCalendar;
        FormatsUtils.roundDayToStart(orderDateCalendar);
        refreshOrdersList(showProgress);
    }
    
    public void refreshOrdersListView() {
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
    }
    
    private void viewOrder(Order order) {
        Intent orderViewIntent = new Intent(ctx, OrderViewActivity.class);
        orderViewIntent.setAction(order.orderUid);
        startActivity(orderViewIntent);
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
    
    public String getAdvTypeString(Order order) {
        if (!order.isAdvertising) return "";
        return advTypesStrings[order.advType];
    }
    
    public String getOrderStatusString(int idx) {
        return orderStatuses[idx];
    }
    
    View createFooterSummary(String caption, int isAdv) {
        if (ordersList == null) ordersList = new ArrayList<>();
        //LayoutInflater layoutInflater = getLayoutInflater(new Bundle());
        View v = layoutInflater.inflate(R.layout.order_item_list_footer_summary, null, false);
        
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
