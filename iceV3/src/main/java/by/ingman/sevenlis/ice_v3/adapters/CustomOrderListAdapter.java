package by.ingman.sevenlis.ice_v3.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class CustomOrderListAdapter extends BaseAdapter {
    private final Context ctx;
    private final LayoutInflater layoutInflater;
    private final List<Order> objects;
    private static String[] advTypesStrings;
    private static String[] orderStatuses;
    private static String[] orderTypes;
    
    public CustomOrderListAdapter(Context context, List<Order> orders) {
        this.ctx = context;
        this.objects = orders;
        this.layoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        advTypesStrings = ctx.getResources().getStringArray(R.array.adv_types_strings);
        orderStatuses = ctx.getResources().getStringArray(R.array.order_statuses);
        orderTypes = ctx.getResources().getStringArray(R.array.order_types);
    }
    
    public void updateOrdersList(List<Order> orders) {
        this.objects.clear();
        this.objects.addAll(orders);
        this.notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return this.objects.size();
    }
    
    @Override
    public Object getItem(int i) {
        return this.objects.get(i);
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
        ((TextView) view.findViewById(R.id.textView_buyer)).setText(order.contragentName);
        ((TextView) view.findViewById(R.id.textView_buyer)).setTextColor(ctx.getResources().getColor(order.contragent.isInStop() ? R.color.color_red : R.color.dark));
        ((TextView) view.findViewById(R.id.textView_point)).setText(order.pointName);
        mText = "Упак:  " + FormatsUtils.getNumberFormatted(order.packs,1);
        ((TextView) view.findViewById(R.id.textView_qtyPacks)).setText(mText);
        mText = "Кол-во:" + FormatsUtils.getNumberFormatted(order.quantity,0);
        ((TextView) view.findViewById(R.id.textView_quantity)).setText(mText);
        mText = "Масса:" + FormatsUtils.getNumberFormatted(order.weight,3);
        ((TextView) view.findViewById(R.id.textView_weight)).setText(mText);
        mText = "Сумма:" + FormatsUtils.getNumberFormatted(order.summa,2);
        ((TextView) view.findViewById(R.id.textView_summa)).setText(mText);

        if (order.orderType == -1) {
            mText = getOrderTypeString(order);
            ((TextView) view.findViewById(R.id.textView_adv)).setText(mText);
            ((TextView) view.findViewById(R.id.textView_adv)).setTextColor(ctx.getResources().getColor(R.color.color_red));
        } else {
            mText = "Реклама: " + order.getIsAdvString();
            ((TextView) view.findViewById(R.id.textView_adv)).setText(mText);
            ((TextView) view.findViewById(R.id.textView_adv)).setTextColor(ctx.getResources().getColor(R.color.dark_grey));
        }
        ((TextView) view.findViewById(R.id.textView_advType)).setText(getAdvTypeString(order));

        ((TextView) view.findViewById(R.id.textView_comment)).setText(order.comment);
        ((TextView) view.findViewById(R.id.textView_dateUnload)).setText(getDateTimeUnloadString(order));
        
        TextView textViewStatus = view.findViewById(R.id.textView_status);
        textViewStatus.setText(getOrderStatusString(order.status));
        textViewStatus.setTextColor(order.getAnswerResultColor(ctx));
        
        view.setTag(i);
        
        return view;
    }
    
    private Order getOrder(int i) {
        return (Order) getItem(i);
    }
    
    private String getDateTimeUnloadString(Order order) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss",Locale.getDefault()).format(new Date(order.dateUnload));
    }
    
    private String getAdvTypeString(Order order) {
        return !order.isAdvertising ? "" : advTypesStrings[order.advType];
    }
    
    private String getOrderStatusString(int idx) {
        return orderStatuses[idx];
    }

    private String getOrderTypeString(Order order) {
        return orderTypes[getOrderTypesArrayPosition(order.orderType)];
    }

    private int getOrderTypesArrayPosition(int orderType) {
        if (orderType == -1) {
            return 1;
        }
        return 0;
    }
}
