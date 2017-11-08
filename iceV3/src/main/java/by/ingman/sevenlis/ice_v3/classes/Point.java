package by.ingman.sevenlis.ice_v3.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Point implements Parcelable {
    public static final Creator<Point> CREATOR = new Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }
        
        @Override
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };
    public String code;
    public String name;
    
    public Point(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    private Point(Parcel in) {
        code = in.readString();
        name = in.readString();
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
        parcel.writeString(code);
        parcel.writeString(name);
    }
}
