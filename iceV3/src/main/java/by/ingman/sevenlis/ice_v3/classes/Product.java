package by.ingman.sevenlis.ice_v3.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable {
    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }
        
        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };
    public String code;
    public String name;
    public double weight;
    public double price;
    public double num_in_pack;
    
    public Product(String code, String name, double weight, double price, double num_in_pack) {
        this.code = code;
        this.name = name;
        this.weight = weight;
        this.price = price;
        this.num_in_pack = num_in_pack;
        if (this.num_in_pack <= 0) this.num_in_pack = 1;
    }
    
    private Product(Parcel in) {
        this.code = in.readString();
        this.name = in.readString();
        this.weight = in.readDouble();
        this.price = in.readDouble();
        this.num_in_pack = in.readDouble();
        if (this.num_in_pack <= 0) this.num_in_pack = 1;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.code);
        parcel.writeString(this.name);
        parcel.writeDouble(this.weight);
        parcel.writeDouble(this.price);
        parcel.writeDouble(this.num_in_pack);
    }
}
