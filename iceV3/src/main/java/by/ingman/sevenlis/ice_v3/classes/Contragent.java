package by.ingman.sevenlis.ice_v3.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Contragent implements Parcelable {
    public static final Creator<Contragent> CREATOR = new Creator<Contragent>() {
        @Override
        public Contragent createFromParcel(Parcel in) {
            return new Contragent(in);
        }
        
        @Override
        public Contragent[] newArray(int size) {
            return new Contragent[size];
        }
    };
    private String code;
    private String name;
    private String rating;
    private double debt;
    private double overdue;
    private boolean inStop;
    
    public Contragent(String code, String name) {
        setCode(code);
        setName(name);
    }
    
    private Contragent(Parcel in) {
        this.code = in.readString();
        this.name = in.readString();
        this.rating = in.readString();
        this.debt = in.readDouble();
        this.overdue = in.readDouble();
        this.inStop = in.readInt() == 1;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public boolean isInStop() {
        return inStop;
    }

    public void setInStop(boolean inStop) {
        this.inStop = inStop;
    }

    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.code);
        parcel.writeString(this.name);
        parcel.writeString(this.rating);
        parcel.writeDouble(this.debt);
        parcel.writeDouble(this.overdue);
        parcel.writeInt(this.inStop ? 1 : 0);
    }
}
