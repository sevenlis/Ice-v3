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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.local.sql.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class MainActivityPageFragment extends Fragment {
    Context ctx;
    Calendar fragmentOrderDateCal;
    ListView listViewOrders;
    View footerNoAdv;
    View footerIsAdv;
    View footerSummary;
    ProgressBar progressBarLoad;
    LayoutInflater layoutInflater;
    CustomOrderListAdapter customOrderListAdapter;
    Handler mHandler;
    ArrayList<Order> ordersList;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (fragmentOrderDateCal == null) {
            fragmentOrderDateCal = Calendar.getInstance();
        }
        if (savedInstanceState != null) {
            long dateMillis = savedInstanceState.getLong("orderDateLong");
            fragmentOrderDateCal.setTimeInMillis(dateMillis);
        }
        ctx                     = getActivity().getApplicationContext();
        mHandler                = new Handler();
        ordersList              = new ArrayList<>();
        customOrderListAdapter  = new CustomOrderListAdapter(ctx, ordersList);
        footerNoAdv             = new View(ctx);
        footerIsAdv             = new View(ctx);
        footerSummary           = new View(ctx);
        layoutInflater          = getLayoutInflater(new Bundle());
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("orderDateLong", fragmentOrderDateCal.getTimeInMillis());
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.ctx = context;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.activity_main_page, container, false);
        
        listViewOrders = (ListView) resultView.findViewById(R.id.listViewOrders);
        listViewOrders.setAdapter(customOrderListAdapter);
        listViewOrders.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Order order = ordersList.get(i);
                viewOrder(order);
            }
        });
        progressBarLoad = (ProgressBar) resultView.findViewById(R.id.progressBarLoad);
        progressBarLoad.setVisibility(View.GONE);
        listViewOrders.setVisibility(View.VISIBLE);
        
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
                final ArrayList<Order> newOrdersList = dbLocalThread.getOrdersList(fragmentOrderDateCal);
                
                if (showProgress) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            customOrderListAdapter.updateOrdersList(newOrdersList);
                            
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
        fragmentOrderDateCal = dateCalendar;
        FormatsUtils.roundDayToStart(fragmentOrderDateCal);
        refreshOrdersList(showProgress);
    }
    
    public void refreshOrdersListView() {
        listViewOrders.removeFooterView(footerNoAdv);
        listViewOrders.removeFooterView(footerIsAdv);
        listViewOrders.removeFooterView(footerSummary);
        if (ordersList.size() != 0) {
            footerNoAdv     = createFooterSummary("Итого без рекламы:",0);
            footerIsAdv     = createFooterSummary("Итого реклама:",1);
            footerSummary   = createFooterSummary("Итого:",-1);
    
            listViewOrders.addFooterView(footerNoAdv,null,false);
            listViewOrders.addFooterView(footerIsAdv,null,false);
            listViewOrders.addFooterView(footerSummary,null,false);
        }
    }
    
    private void viewOrder(Order order) {
        Intent orderViewIntent = new Intent(ctx, OrderViewActivity.class);
        orderViewIntent.setAction(order.orderUid);
        startActivity(orderViewIntent);
    }
    
    View createFooterSummary(String caption, int isAdv) {
        if (ordersList == null) ordersList = new ArrayList<>();
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
