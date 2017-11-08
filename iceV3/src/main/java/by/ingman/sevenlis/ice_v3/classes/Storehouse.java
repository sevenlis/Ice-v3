package by.ingman.sevenlis.ice_v3.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Storehouse implements Parcelable {
    public static final Creator<Storehouse> CREATOR = new Creator<Storehouse>() {
        @Override
        public Storehouse createFromParcel(Parcel in) {
            return new Storehouse(in);
        }
        
        @Override
        public Storehouse[] newArray(int size) {
            return new Storehouse[size];
        }
    };
    public String code;
    public String name;
    
    public Storehouse(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    private Storehouse(Parcel in) {
        this.code = in.readString();
        this.name = in.readString();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.code);
        parcel.writeString(this.name);
    }
}
