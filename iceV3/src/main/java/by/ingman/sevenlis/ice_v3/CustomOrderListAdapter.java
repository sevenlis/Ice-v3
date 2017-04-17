package by.ingman.sevenlis.ice_v3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.classes.Order;

class CustomOrderListAdapter extends BaseAdapter {
    private Context ctx;
    private LayoutInflater layoutInflater;
    private ArrayList<Order> objects;
    
    CustomOrderListAdapter(Context context, ArrayList<Order> orders) {
        this.ctx = context;
        this.objects = orders;
        this.layoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    void updateOrdersList(ArrayList<Order> orders) {
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
    
    private Order getOrder(int i) {
        return (Order) getItem(i);
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
    
    private String getAdvTypeString(Order order) {
        String[] advTypesStrings = ctx.getResources().getStringArray(R.array.adv_types_strings);
        if (!order.isAdvertising) return "";
        return advTypesStrings[order.advType];
    }
    
    private String getOrderStatusString(int idx) {
        String[] orderStatuses = ctx.getResources().getStringArray(R.array.order_statuses);
        return orderStatuses[idx];
    }
}
