package by.ingman.sevenlis.ice_v3.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Answer;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class OrderViewActivity extends AppCompatActivity {
    private DBLocal dbLocal;
    private View footerSummary;
    private View footerSubmit;
    private Order mOrder;
    private ViewGroup orderViewGroup = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_view);
        
        dbLocal = new DBLocal(this);
        mOrder = dbLocal.getOrder(getIntent().getAction());
        if (mOrder == null) {
            finish();
            return;
        }
        
        String[] advTypesStrings = getResources().getStringArray(R.array.adv_types_strings);
        String[] orderStatuses = getResources().getStringArray(R.array.order_statuses);
        
        TextView textViewOrderDate = (TextView) findViewById(R.id.textViewOrderDate);
        textViewOrderDate.setText(FormatsUtils.getDateFormattedWithSeconds(mOrder.orderDate));
        
        TextView textViewClientName = (TextView) findViewById(R.id.textViewClientName);
        textViewClientName.setText(mOrder.contragent.getName());
        
        TextView textViewPoint = (TextView) findViewById(R.id.textViewPointName);
        textViewPoint.setText(mOrder.pointName);
        
        TextView textViewIsAdv = (TextView) findViewById(R.id.textViewIsAdv);
        String isAdvString = "Реклама: " + (mOrder.isAdvertising ? "ДА" : "НЕТ");
        textViewIsAdv.setText(isAdvString);
        
        TextView textViewAdvType = (TextView) findViewById(R.id.textViewAdvType);
        textViewAdvType.setText(advTypesStrings[mOrder.advType]);
        textViewAdvType.setVisibility(mOrder.isAdvertising ? View.VISIBLE : View.INVISIBLE);
        
        TextView textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewStatus.setText(orderStatuses[mOrder.status]);
        textViewStatus.setTextColor(mOrder.getStatusResultColor(this));
        
        refreshOrderItemsList();
    }
    
    private void refreshOrderItemsList() {
        ListView listViewOrderItems = (ListView) findViewById(R.id.listOrderItems);
        OrderViewActivity.CustomListViewAdapter customListViewAdapter = new OrderViewActivity.CustomListViewAdapter(this, mOrder.orderItems);
        listViewOrderItems.removeFooterView(footerSummary);
        listViewOrderItems.removeFooterView(footerSubmit);
        footerSummary = createFooterSummary();
        footerSubmit = createFooterSubmit();
        listViewOrderItems.addFooterView(footerSummary, null, false);
        listViewOrderItems.addFooterView(footerSubmit, null, false);
        listViewOrderItems.setAdapter(customListViewAdapter);
        listViewOrderItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            
            }
        });
        listViewOrderItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return false;
            }
        });
    }
    
    private View createFooterSummary() {
        View v = getLayoutInflater().inflate(R.layout.order_item_list_footer_summary, orderViewGroup);
        
        double sumPacks = 0;
        double sumAmount = 0;
        double sumSumma = 0;
        double sumWeight = 0;
        for (OrderItem orderItem : mOrder.orderItems) {
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
    
    private View createFooterSubmit() {
        View v = getLayoutInflater().inflate(R.layout.order_item_list_footer_submit, orderViewGroup);
        
        EditText editTextComment = (EditText) v.findViewById(R.id.editTextComment);
        editTextComment.setVisibility(View.GONE);
        
        Button buttonSubmit = (Button) v.findViewById(R.id.buttonSubmitOrder);
        buttonSubmit.setVisibility(View.GONE);
        
        TextView textViewComment = (TextView) v.findViewById(R.id.textViewComment);
        textViewComment.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
        textViewComment.setText(mOrder.comment);
        
        TextView textViewAnswer = (TextView) v.findViewById(R.id.textViewAnswer);
        textViewAnswer.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
        Answer answer = dbLocal.getAnswer(mOrder.orderUid);
        if (answer != null) {
            String answerInfo = "Ответ на заявку:\n" + answer.getDescription() + "\n" + "Сформирован: " + FormatsUtils.getDateFormattedWithSeconds(answer.getUnloadTime());
            textViewAnswer.setText(answerInfo);
        }
        return v;
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
            
            mText = "Кол.: " + FormatsUtils.getNumberFormatted(orderItem.quantity, 0);
            ((TextView) view.findViewById(R.id.tvAmount)).setText(mText);
            
            mText = "Масса: " + FormatsUtils.getNumberFormatted(orderItem.weight, 3);
            ((TextView) view.findViewById(R.id.tvMass)).setText(mText);
            
            mText = "Сумма: " + FormatsUtils.getNumberFormatted(orderItem.summa, 2);
            ((TextView) view.findViewById(R.id.tvSum)).setText(mText);
            
            return view;
        }
        
        OrderItem getOrderItem(int i) {
            return (OrderItem) getItem(i);
        }
    }
}
