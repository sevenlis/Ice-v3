package by.ingman.sevenlis.ice_v3.classes;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

import static java.util.UUID.randomUUID;

public class Order implements Parcelable {
    public static final Creator<Order> CREATOR = new Creator<Order>() {
        @Override
        public Order createFromParcel(Parcel in) {
            return new Order(in);
        }
        
        @Override
        public Order[] newArray(int size) {
            return new Order[size];
        }
    };
    private Context ctx;
    public String orderUid;
    public Date orderDate;
    public Contragent contragent;
    public String contragentCode;
    public String contragentName;
    public Point point;
    public String pointCode;
    public String pointName;
    public Storehouse storehouse;
    public String storehouseCode;
    public String storehouseName;
    public Agreement agreement;
    public String agreementId;
    public String agreementName;
    public Answer answer;
    public boolean isAdvertising;
    public int orderType;
    public int advType;
    public String comment;
    public List<OrderItem> orderItems;
    public double quantity;
    public double packs;
    public double summa;
    public double weight;
    public int status;
    public long dateUnload;
    private DBLocal dbLocal;
    
    public Order(Context ctx) {
        this.ctx = ctx;
        this.dbLocal = new DBLocal(ctx);
        this.orderUid = randomUUID().toString();
        this.isAdvertising = false;
        this.orderType = 1;
        this.advType = 0;
        this.status = 0;
        setStorehouse(dbLocal.getDefaultStorehouse());
        Calendar now = Calendar.getInstance();
        this.dateUnload = now.getTimeInMillis();
        setContragent(new Contragent("", ""));
        setPoint(new Point("", ""));
        this.agreement = new Agreement("","");
        this.answer = dbLocal.getAnswer(this.orderUid);
    }
    
    public Order(Parcel in) {
        this.orderUid = in.readString();
        this.contragentCode = in.readString();
        this.contragentName = in.readString();
        this.pointCode = in.readString();
        this.pointName = in.readString();
        this.storehouseCode = in.readString();
        this.storehouseName = in.readString();
        this.isAdvertising = in.readByte() != 0;
        this.advType = in.readInt();
        this.comment = in.readString();
        this.quantity = in.readDouble();
        this.packs = in.readDouble();
        this.summa = in.readDouble();
        this.weight = in.readDouble();
        this.status = in.readInt();
        this.dateUnload = in.readLong();
        this.orderDate = new Date(in.readLong());
        this.agreementId = in.readString();
        this.agreementName = in.readString();
        this.orderType = in.readInt();

        this.contragent = new Contragent(this.contragentCode, this.contragentName);
        this.point = new Point(this.pointCode, this.pointName);
        this.storehouse = new Storehouse(this.storehouseCode, this.storehouseName);
        this.agreement = new Agreement(this.agreementId, this.agreementName);

        this.answer = dbLocal.getAnswer(this.orderUid);
    }

    public String getOrderTypeString() {
        String[] orderTypes = ctx.getResources().getStringArray(R.array.order_types);
        return orderTypes[getOrderTypeArrayPosition()];
    }
    private int getOrderTypeArrayPosition() {
        switch (orderType) {
            case 1:
                return 0;
            case -1:
                return 1;
            case -20:
                return 2;
        }
        return 0;
    }

    public String getOrderStatusString() {
        String[] orderStatuses = ctx.getResources().getStringArray(R.array.order_statuses);
        return orderStatuses[status];
    }

    public String getOrderAdvTypeString() {
        String[] ordedAdvTypes = ctx.getResources().getStringArray(R.array.adv_types_strings);
        return !isAdvertising ? "" : ordedAdvTypes[advType];
    }
    
    public void setContragent(Contragent contragent) {
        if (contragent == null) return;
        this.contragent = contragent;
        this.contragentCode = contragent.getCode();
        this.contragentName = contragent.getName();
    }
    
    public void setPoint(Point point) {
        if (point == null) return;
        this.point = point;
        this.pointCode = point.code;
        this.pointName = point.name;
    }
    
    public void setStorehouse(Storehouse storehouse) {
        if (storehouse == null) return;
        this.storehouse = storehouse;
        this.storehouseCode = storehouse.code;
        this.storehouseName = storehouse.name;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
        this.agreementId = agreement.getId();
        this.agreementName = agreement.getName();
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null) return;
        this.orderItems = orderItems;
        calcQuantity();
        calcWeight();
        calcPacks();
        calcSumma();
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    private void calcQuantity() {
        this.quantity = 0;
        for (OrderItem orderItem : this.orderItems) {
            this.quantity += orderItem.quantity;
        }
    }
    
    private void calcPacks() {
        this.packs = 0;
        for (OrderItem orderItem : this.orderItems) {
            this.packs += orderItem.packs;
        }
    }
    
    private void calcSumma() {
        this.summa = 0;
        for (OrderItem orderItem : this.orderItems) {
            this.summa += orderItem.summa;
        }
    }
    
    private void calcWeight() {
        this.weight = 0;
        for (OrderItem orderItem : this.orderItems) {
            this.weight += orderItem.product.weight * orderItem.quantity;
        }
    }

    public double getQuantity() {
        return this.quantity;
    }

    public Point getPoint() {
        return point;
    }

    public double getPacks() {
        return packs;
    }

    public double getSumma() {
        return summa;
    }

    public double getWeight() {
        return weight;
    }

    public Contragent getContragent() {
        return contragent;
    }

    public String getQuantityString() {
        return FormatsUtils.getNumberFormatted(this.getQuantity(), 3);
    }
    
    public String getWeightString() {
        return FormatsUtils.getNumberFormatted(this.getWeight(), 3);
    }
    
    public String getPacksString() {
        return FormatsUtils.getNumberFormatted(this.getPacks(), 1);
    }
    
    public String getSummaString() {
        return FormatsUtils.getNumberFormatted(this.getSumma(), 2);
    }
    
    public String getOrderDateString() {
        return FormatsUtils.getDateFormatted(this.orderDate);
    }
    
    public String getIsAdvString() {
        return this.isAdvertising ? "ДА" : "НЕТ";
    }
    
    public int getIsAdvInteger() {
        return this.isAdvertising ? 1 : 0;
    }
    
    public int getStatusResultColor(Context ctx) {
        int mColor = ContextCompat.getColor(ctx, R.color.colorPrimaryDark);
        Answer mAnswer = dbLocal.getAnswer(this.orderUid);
        if (mAnswer == null) return mColor;
        switch (mAnswer.getResult()) {
            case -1:
            case 0: {
                mColor = ContextCompat.getColor(ctx, R.color.color_red);
            }
            break;
            case 1: {
                mColor = ContextCompat.getColor(ctx, R.color.green_darker);
            }
            break;
            default: {
                mColor = ContextCompat.getColor(ctx, R.color.colorPrimaryDark);
            }
            break;
        }
        return mColor;
    }

    public int getAnswerResult() {
        if (answer == null)
            return -1;
        return answer.getResult();
    }

    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.orderUid);
        parcel.writeString(this.contragentCode);
        parcel.writeString(this.contragentName);
        parcel.writeString(this.pointCode);
        parcel.writeString(this.pointName);
        parcel.writeString(this.storehouseCode);
        parcel.writeString(this.storehouseName);
        parcel.writeByte((byte) (this.isAdvertising ? 1 : 0));
        parcel.writeInt(this.advType);
        parcel.writeString(this.comment);
        parcel.writeDouble(this.quantity);
        parcel.writeDouble(this.packs);
        parcel.writeDouble(this.summa);
        parcel.writeDouble(this.weight);
        parcel.writeInt(this.status);
        parcel.writeLong(this.dateUnload);
        parcel.writeLong(this.orderDate.getTime());
        parcel.writeString(this.agreementId);
        parcel.writeString(this.agreementName);
        parcel.writeInt(this.orderType);
    }
}