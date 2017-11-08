package by.ingman.sevenlis.ice_v3.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class OrderItem implements Parcelable {
    public static final Creator<OrderItem> CREATOR = new Creator<OrderItem>() {
        @Override
        public OrderItem createFromParcel(Parcel in) {
            return new OrderItem(in);
        }
        
        @Override
        public OrderItem[] newArray(int size) {
            return new OrderItem[size];
        }
    };
    public Product product;
    public double quantity;
    public double packs;
    public double summa;
    public double weight;
    
    public OrderItem(Product product, double quantity) {
        this.product = product;
        this.quantity = quantity;
        this.reCalcAll();
    }
    
    private OrderItem(Parcel in) {
        this.product = in.readParcelable(Product.class.getClassLoader());
        this.quantity = in.readDouble();
        this.packs = in.readDouble();
        this.summa = in.readDouble();
        this.weight = in.readDouble();
    }
    
    private void calcSumma() {
        this.summa = this.quantity * this.product.price;
    }
    
    private void calcPacks() {
        if (this.product.num_in_pack == 0) this.product.num_in_pack = 1;
        this.packs = this.quantity / this.product.num_in_pack;
    }
    
    private void calcWeight() {
        this.weight = this.quantity * this.product.weight;
    }
    
    public void reCalcAll() {
        this.calcPacks();
        this.calcSumma();
        this.calcWeight();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.product, i);
        parcel.writeDouble(this.quantity);
        parcel.writeDouble(this.packs);
        parcel.writeDouble(this.summa);
        parcel.writeDouble(this.weight);
    }
}
