package by.ingman.sevenlis.ice_v3.classes;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Agreement implements Parcelable {
    public static final Creator<Agreement> CREATOR = new Creator<Agreement>() {
        @Override
        public Agreement createFromParcel(Parcel in) {
            return new Agreement(in);
        }

        @Override
        public Agreement[] newArray(int size) {
            return new Agreement[size];
        }
    };

    private String id;
    private String name;

    public Agreement(String id, String name) {
        this.id = id;
        this.name = name;
    }

    protected Agreement(Parcel in) {
        id = in.readString();
        name = in.readString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
