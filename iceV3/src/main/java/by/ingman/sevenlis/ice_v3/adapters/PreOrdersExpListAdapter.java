package by.ingman.sevenlis.ice_v3.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.classes.Order;
import by.ingman.sevenlis.ice_v3.classes.PreOrdersListGroup;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class PreOrdersExpListAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private List<PreOrdersListGroup> mGroups;
    private final LayoutInflater layoutInflater;

    public PreOrdersExpListAdapter(Context context, List<PreOrdersListGroup> mGroups) {
        this.context = context;
        this.mGroups = mGroups;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setGroups(List<PreOrdersListGroup> mGroups) {
        this.mGroups = mGroups;
    }

    public List<PreOrdersListGroup> getGroups() {
        return mGroups;
    }

    @Override
    public int getGroupCount() {
        return mGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mGroups.get(groupPosition).getOrdersCount();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return ((PreOrdersListGroup) getGroup(groupPosition)).geOrder(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = layoutInflater.inflate(R.layout.preorders_group,parent,false);

        PreOrdersListGroup listGroup = (PreOrdersListGroup) getGroup(groupPosition);

        String text;
        TextView tvDate = convertView.findViewById(R.id.group_date_text);
        tvDate.setText(FormatsUtils.getDateFormatted(listGroup.getDate()));

        text = "Заявок:" + FormatsUtils.getNumberFormatted(listGroup.getOrdersCount(), 4,0);
        TextView tvOrdersCount = convertView.findViewById(R.id.group_orders_count_text);
        tvOrdersCount.setText(text);

        text = "Адресов: " + FormatsUtils.getNumberFormatted(listGroup.getPointsCount(), 4,0);
        TextView tvPointsCount = convertView.findViewById(R.id.group_points_count_text);
        tvPointsCount.setText(text);

        text = "Упаковок:" + FormatsUtils.getNumberFormatted(listGroup.getPacks(), 4,0);
        TextView tvPacksCount = convertView.findViewById(R.id.group_packs_count_text);
        tvPacksCount.setText(text);

        convertView.setTag(groupPosition);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = layoutInflater.inflate(R.layout.orders_list_item,parent,false);


        Order order = (Order) getChild(groupPosition,childPosition);

        String mText;

        ((TextView) convertView.findViewById(R.id.textView_date)).setText(order.getOrderDateString());
        ((TextView) convertView.findViewById(R.id.textView_orderId)).setText(order.orderUid);
        ((TextView) convertView.findViewById(R.id.textView_buyer)).setText(order.contragentName);
        ((TextView) convertView.findViewById(R.id.textView_point)).setText(order.pointName);
        mText = "Упак:  " + FormatsUtils.getNumberFormatted(order.packs,1);
        ((TextView) convertView.findViewById(R.id.textView_qtyPacks)).setText(mText);
        mText = "Кол-во:" + FormatsUtils.getNumberFormatted(order.quantity,0);
        ((TextView) convertView.findViewById(R.id.textView_quantity)).setText(mText);
        mText = "Масса:" + FormatsUtils.getNumberFormatted(order.weight,3);
        ((TextView) convertView.findViewById(R.id.textView_weight)).setText(mText);
        mText = "Сумма:" + FormatsUtils.getNumberFormatted(order.summa,2);
        ((TextView) convertView.findViewById(R.id.textView_summa)).setText(mText);

        mText = "Реклама: " + order.getIsAdvString();
        ((TextView) convertView.findViewById(R.id.textView_adv)).setText(mText);
        ((TextView) convertView.findViewById(R.id.textView_advType)).setText(order.getOrderAdvTypeString());

        ((TextView) convertView.findViewById(R.id.textView_comment)).setText(order.comment);
        ((TextView) convertView.findViewById(R.id.textView_dateUnload)).setText(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date(order.dateUnload)));

        TextView textViewStatus = convertView.findViewById(R.id.textView_status);
        textViewStatus.setText(order.getOrderStatusString());
        textViewStatus.setTextColor(order.getAnswerResultColor(context));

        convertView.setTag(childPosition);

        return convertView;
    }
}
