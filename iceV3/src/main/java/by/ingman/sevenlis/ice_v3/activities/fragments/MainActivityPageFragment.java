package by.ingman.sevenlis.ice_v3.activities.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.activities.OrderViewActivity;
import by.ingman.sevenlis.ice_v3.adapters.CustomOrderListAdapter;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.intents.ExchangeDataIntents;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class MainActivityPageFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private Context ctx;
    private Calendar orderDateCal;
    private ListView listViewOrders;
    private View footerNoAdv;
    private View footerIsAdv;
    private View footerSummary;
    private ProgressBar progressBarLoad;
    private LayoutInflater layoutInflater;
    private Handler mHandler;
    private CustomOrderListAdapter customOrderListAdapter;
    private List<Order> ordersList;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    @SuppressLint("RestrictedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderDateCal == null) {
            orderDateCal = Calendar.getInstance();
        }
        if (savedInstanceState != null) {
            long dateMillis = savedInstanceState.getLong("orderDateLong");
            orderDateCal.setTimeInMillis(dateMillis);
        }
        ctx = requireActivity().getApplicationContext();
        mHandler = new Handler();
        ordersList = new ArrayList<>();
        customOrderListAdapter = new CustomOrderListAdapter(ctx, ordersList);
        footerNoAdv = new View(ctx);
        footerIsAdv = new View(ctx);
        footerSummary = new View(ctx);
        layoutInflater = this.getLayoutInflater();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong("orderDateLong", orderDateCal.getTimeInMillis());
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        this.ctx = context;
        super.onAttach(context);
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.activity_main_pager_fragment, container , false);
        
        listViewOrders = resultView.findViewById(R.id.listViewOrders);
        listViewOrders.setAdapter(customOrderListAdapter);
        listViewOrders.setOnItemClickListener((adapterView, view, i, l) -> {
            Order order = ordersList.get(i);
            viewOrder(order);
        });
        listViewOrders.setDividerHeight(0);
        progressBarLoad = resultView.findViewById(R.id.progressBarLoad);
        progressBarLoad.setVisibility(View.GONE);
        listViewOrders.setVisibility(View.VISIBLE);
    
        swipeRefreshLayout = resultView.findViewById(R.id.refresh_list);
        swipeRefreshLayout.setOnRefreshListener(MainActivityPageFragment.this);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
    
        return resultView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshOrdersList(true);
    }
    
    @Override
    public void onRefresh() {
        swipeRefreshLayout.postDelayed(() -> refreshOrdersList(false),0L);
    }
    
    public void refreshOrdersList(final boolean showProgress) {
        new Thread(() -> {
            mHandler.post(() -> {
                listViewOrders.setVisibility(View.GONE);
                if (showProgress) {
                    progressBarLoad.setVisibility(View.VISIBLE);
                }
            });

            Intent intent = ExchangeDataIntents.getExchangeDataServiceIntent(ctx);
            intent.setAction("UPDATE-ORDERS-ONLY");
            ctx.startService(intent);

            final List<Order> orders = new DBLocal(ctx).getOrdersList(orderDateCal);

            mHandler.post(() -> {
                customOrderListAdapter.updateOrdersList(orders);

                listViewOrders.setVisibility(View.VISIBLE);
                if (showProgress) {
                    progressBarLoad.setVisibility(View.GONE);
                }

                refreshOrdersListViewFooters();

                swipeRefreshLayout.setRefreshing(false);
            });
        }).start();
    }
    
    public void refreshOrdersListViewFooters() {
        listViewOrders.removeFooterView(footerNoAdv);
        listViewOrders.removeFooterView(footerIsAdv);
        listViewOrders.removeFooterView(footerSummary);
        if (ordersList.size() != 0) {
            footerNoAdv = createFooterSummary("Итого без рекламы:", 0);
            footerIsAdv = createFooterSummary("Итого реклама:", 1);
            footerSummary = createFooterSummary("Итого:", -1);
            
            listViewOrders.addFooterView(footerNoAdv, null, false);
            listViewOrders.addFooterView(footerIsAdv, null, false);
            listViewOrders.addFooterView(footerSummary, null, false);
        }
    }
    
    public Calendar getOrderDateCal() {
        return this.orderDateCal;
    }
    
    public void setOrderDateCal(Calendar orderDateCal) {
        this.orderDateCal = orderDateCal;
        FormatsUtils.roundDayToStart(this.orderDateCal);
    }
    
    private void viewOrder(Order order) {
        Intent orderViewIntent = new Intent(ctx, OrderViewActivity.class);
        orderViewIntent.setAction(order.orderUid);
        startActivity(orderViewIntent);
    }
    
    @SuppressLint("InflateParams")
    private View createFooterSummary(String caption, int isAdv) {
        if (ordersList == null) ordersList = new ArrayList<>();
        View v = layoutInflater.inflate(R.layout.order_item_list_footer_summary, null, false);
        
        ((TextView) v.findViewById(R.id.textViewCaption)).setText(caption);
        
        double sumPacks = 0d;
        double sumAmount = 0d;
        double sumSumma = 0d;
        double sumWeight = 0d;
        for (Order order : ordersList) {
            if (isAdv != -1 && order.getIsAdvInteger() != isAdv) {
                continue;
            }
            sumPacks += order.packs * order.orderType;
            sumAmount += order.quantity * order.orderType;
            sumSumma += order.summa * order.orderType;
            sumWeight += order.weight * order.orderType;
        }
        String mText;
        
        mText = "Упак.: " + FormatsUtils.getNumberFormatted(sumPacks, 1);
        ((TextView) v.findViewById(R.id.tvPacks)).setText(mText);

        mText = "Кол-во:" + FormatsUtils.getNumberFormatted(sumAmount, 0);
        ((TextView) v.findViewById(R.id.tvAmount)).setText(mText);

        mText = "Масса:" + FormatsUtils.getNumberFormatted(sumWeight, 3);
        ((TextView) v.findViewById(R.id.tvMass)).setText(mText);
        
        mText = "Сумма:" + FormatsUtils.getNumberFormatted(sumSumma, 2);
        ((TextView) v.findViewById(R.id.tvSum)).setText(mText);
        
        return v;
    }
}
