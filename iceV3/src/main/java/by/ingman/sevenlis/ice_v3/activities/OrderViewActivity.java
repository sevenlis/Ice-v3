package by.ingman.sevenlis.ice_v3.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Answer;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.OrderItem;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.services.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class OrderViewActivity extends AppCompatActivity {
    private View footerSummary;
    private View footerSubmit;
    private Order mOrder;
    private ViewGroup orderViewGroup = null;
    private Context context;
    private Menu mOptionsMenu;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ExchangeDataService.CHANNEL_ORDERS_UPDATES)) {
                Answer answer = new DBLocal(context).getAnswer(mOrder.orderUid);
                mOptionsMenu.setGroupVisible(R.id.edit_order_group, answer == null || answer.getResult() < 0);
                mOptionsMenu.findItem(R.id.item_edit).setVisible(false);
                mOrder.setAnswer(answer);
                if (footerSubmit != null) {
                    TextView textViewAnswer = footerSubmit.findViewById(R.id.textViewAnswer);
                    textViewAnswer.setText(getAnswerDescribe(mOrder));
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_view);

        context = getApplicationContext();

        DBLocal dbLocal = new DBLocal(context);
        mOrder = dbLocal.getOrder(getIntent().getAction());
        if (mOrder == null) {
            finish();
            return;
        }
        
        String[] advTypesStrings = getResources().getStringArray(R.array.adv_types_strings);
        String[] orderStatuses = getResources().getStringArray(R.array.order_statuses);
        
        TextView textViewOrderDate = findViewById(R.id.textViewOrderDate);
        textViewOrderDate.setText(FormatsUtils.getDateFormattedWithSeconds(mOrder.orderDate));

        TextView textViewStorehouse = findViewById(R.id.textView_storehouse);
        textViewStorehouse.setText(mOrder.storehouse.name);

        TextView textViewAgreement = findViewById(R.id.textView_agreement);
        textViewAgreement.setText(mOrder.agreement.getName());
        
        TextView textViewClientName = findViewById(R.id.textViewClientName);
        textViewClientName.setText(mOrder.contragent.getName());
        if (mOrder.contragent.isInStop())
            textViewClientName.setTextColor(getResources().getColor(R.color.color_red));
        
        TextView textViewPoint = findViewById(R.id.textViewPointName);
        textViewPoint.setText(mOrder.pointName);
        
        TextView textViewIsAdv = findViewById(R.id.textViewIsAdv);
        String isAdvString = "Реклама: " + (mOrder.isAdvertising ? "ДА" : "НЕТ");
        textViewIsAdv.setText(isAdvString);
        if (mOrder.orderType == -1) {
            textViewIsAdv.setText(getOrderTypeString(mOrder));
            textViewIsAdv.setTextColor(getResources().getColor(R.color.color_red));
        }

        TextView textViewAdvType = findViewById(R.id.textViewAdvType);
        textViewAdvType.setText(advTypesStrings[mOrder.advType]);
        textViewAdvType.setVisibility(mOrder.isAdvertising ? View.VISIBLE : View.INVISIBLE);

        TextView textViewStatus = findViewById(R.id.textViewStatus);
        textViewStatus.setText(orderStatuses[mOrder.status]);
        textViewStatus.setTextColor(mOrder.getAnswerResultColor(this));
        
        refreshOrderItemsList();

        registerReceiver(broadcastReceiver, new IntentFilter(ExchangeDataService.CHANNEL_ORDERS_UPDATES));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.order_menu, menu);
        menu.findItem(R.id.item_edit).setVisible(false);
        menu.findItem(R.id.item_send).setVisible((mOrder.getAnswerResult() == -1) && (mOrder.getSent() == 0));
        mOptionsMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_send) {
            repeatSendOrder(mOrder.orderUid);
        } else if (item.getItemId() == R.id.item_edit) {
            Intent intent = new Intent(context, OrderActivity.class);
            intent.setAction(OrderActivity.ACTION_ORDER_EDIT);
            intent.putExtra("order_id",mOrder.orderUid);
            intent.putExtra("longDate",mOrder.orderDate.getTime());
            startActivity(intent);
            finish();
        } else if (item.getItemId() == R.id.item_copy) {
            Intent intent = new Intent(context, OrderActivity.class);
            intent.setAction(OrderActivity.ACTION_ORDER_COPY);
            intent.putExtra("order_id",mOrder.orderUid);
            intent.putExtra("longDate", Calendar.getInstance().getTimeInMillis());
            startActivityForResult(intent,MainActivity.REQUEST_CODE_NEW_ORDER);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void repeatSendOrder(String orderUid) {
        new Thread(() -> {
            final boolean result = new DBLocal(context).repeatOrder(orderUid);
            if (result) {
                Intent intent = new Intent(ExchangeDataService.CHANNEL_ORDERS_UPDATES);
                sendBroadcast(intent);
                finish();
            }
        }).start();
    }

    private String getOrderTypeString(Order order) {
        return getResources().getStringArray(R.array.order_types)[getOrderTypesArrayPosition(order.orderType)];
    }

    private int getOrderTypesArrayPosition(int orderType) {
        if (orderType == -1) {
            return 1;
        }
        return 0;
    }

    private void refreshOrderItemsList() {
        ListView listViewOrderItems = findViewById(R.id.listOrderItems);
        OrderViewActivity.CustomListViewAdapter customListViewAdapter = new OrderViewActivity.CustomListViewAdapter(this, mOrder.orderItems);
        listViewOrderItems.removeFooterView(footerSummary);
        listViewOrderItems.removeFooterView(footerSubmit);
        footerSummary = createFooterSummary();
        footerSubmit = createFooterSubmit();
        listViewOrderItems.addFooterView(footerSummary, null, false);
        listViewOrderItems.addFooterView(footerSubmit, null, false);
        listViewOrderItems.setAdapter(customListViewAdapter);
        listViewOrderItems.setOnItemClickListener((adapterView, view, i, l) -> {

        });
        listViewOrderItems.setOnItemLongClickListener((adapterView, view, i, l) -> false);
    }
    
    private View createFooterSummary() {
        View v = getLayoutInflater().inflate(R.layout.order_item_list_footer_summary, orderViewGroup);
        
        double sumPacks = 0;
        double sumAmount = 0;
        double sumSumma = 0;
        double sumWeight = 0;
        for (OrderItem orderItem : mOrder.orderItems) {
            sumPacks += orderItem.packs * mOrder.orderType;
            sumAmount += orderItem.quantity * mOrder.orderType;
            sumSumma += orderItem.summa * mOrder.orderType;
            sumWeight += orderItem.weight * mOrder.orderType;
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

    private String getAnswerDescribe(Order order) {
        String answerInfo = "";
        Answer answer = order.answer;
        if (answer != null) {
            answerInfo = "Ответ на заявку:\n" + answer.getDescription() + "\n" + "Сформирован: " + FormatsUtils.getDateFormattedWithSeconds(answer.getUnloadTime());
        }
        return answerInfo;
    }
    
    private View createFooterSubmit() {
        View v = getLayoutInflater().inflate(R.layout.order_item_list_footer_submit, orderViewGroup);
        
        EditText editTextComment = v.findViewById(R.id.editTextComment);
        editTextComment.setVisibility(View.GONE);
        
        Button buttonSubmit = v.findViewById(R.id.buttonSubmitOrder);
        buttonSubmit.setVisibility(View.GONE);
        
        TextView textViewComment = v.findViewById(R.id.textViewComment);
        textViewComment.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
        textViewComment.setText(mOrder.comment);
        
        TextView textViewAnswer = v.findViewById(R.id.textViewAnswer);
        textViewAnswer.setBackgroundColor(ContextCompat.getColor(this, R.color.light_grey));
        textViewAnswer.setText(getAnswerDescribe(mOrder));
        return v;
    }
    
    private class CustomListViewAdapter extends BaseAdapter {
        Context ctx;
        LayoutInflater layoutInflater;
        List<OrderItem> objects;
        
        CustomListViewAdapter(Context context, List<OrderItem> objects) {
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
        public View getView(int i, View view, ViewGroup viewGroup) {
            orderViewGroup = viewGroup;
            
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
            buttonIncreasePacks.setVisibility(View.INVISIBLE);
            ImageButton buttonDecreasePacks = view.findViewById(R.id.ibDecrease);
            buttonDecreasePacks.setVisibility(View.INVISIBLE);
            
            return view;
        }
        
        OrderItem getOrderItem(int i) {
            return (OrderItem) getItem(i);
        }
    }
}
