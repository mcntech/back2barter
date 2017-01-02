package com.mcntech.back2barter;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramp on 12/31/16.
 */

public class TransactionEx implements Parcelable {
    String name;
    String txid;

    TransactionEx(String tmpName, String tmpTxid) {
        name = tmpName;
        txid = tmpTxid;
    }
    String getTxid() { return txid;}
    String getName() { return name;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(txid);
    }

    public static final Parcelable.Creator<TransactionEx> CREATOR =
            new Parcelable.Creator<TransactionEx>() {
                public TransactionEx createFromParcel(Parcel in) {
                    return new TransactionEx(in);
                }

                @Override
                public TransactionEx[] newArray(int size) {
                    return new TransactionEx[size];
                }
            };

    // "De-parcel object
    public TransactionEx(Parcel in) {
        name = in.readString();
        txid = in.readString();
    }
}

