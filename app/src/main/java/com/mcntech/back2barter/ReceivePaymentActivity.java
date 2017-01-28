package com.mcntech.back2barter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;
import org.bitcoinj.wallet.listeners.ScriptsChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.uri.BitcoinURI;

import java.io.File;
import java.util.List;

public class ReceivePaymentActivity extends AppCompatActivity implements View.OnClickListener {

    TextView txtStatus;
    TextView txtRcvAddress;

    ImageView imgRcvAddress;

    String mStatus;
    String mRcvAddress = "0123456789012345678901234567890123456789";
    float  mRcvAmount = 0.0f;

    NumberPicker np[];
    NumberPicker np_currency;

    String TAG = "b2b";
    final int SND_ADDR_QRCODE = 1;
    public final static int WIDTH = 128;

    int CURRENCY_BITCOIN = 0;
    String currencyList[] = {"Bitcoin", "Dollar", "Euro", "RMB", "INR"};
    int mCurrencyId = 0;
    int mDecimalPos = 1;
    float btc_to_dollar = 1000.0f;
    float mPaymentBtc = 0.0f;
    NetworkParameters mNetworkparams = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_payment);

        Bundle extras = getIntent().getExtras();
        mRcvAddress = extras.getString("address");

        txtStatus = (TextView) findViewById(R.id.txt_status);
        txtRcvAddress = (TextView) findViewById(R.id.txt_rcv_address);
        imgRcvAddress = (ImageView) findViewById(R.id.img_rcv_address);
        np = new NumberPicker[5];
        np_currency = (NumberPicker) findViewById(R.id.np_currency);
        np[0] = (NumberPicker) findViewById(R.id.np0);
        np[1] = (NumberPicker) findViewById(R.id.np1);
        np[2] = (NumberPicker) findViewById(R.id.np2);
        np[3] = (NumberPicker) findViewById(R.id.np3);
        np[4] = (NumberPicker) findViewById(R.id.np4);

        setCurrencyPicker(CURRENCY_BITCOIN, 0.001f);

        np_currency.setDisplayedValues(currencyList);
        np_currency.setMinValue(0);
        np_currency.setMaxValue(currencyList.length - 1);

        class CurrencyChangeListener implements NumberPicker.OnValueChangeListener {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mCurrencyId = newVal;
                mRcvAmount = getCurrencyPicker(oldVal);
                setCurrencyPicker(mCurrencyId, mRcvAmount);
                updateRcvUri();
            }
        }

        class AmountChangeListner implements NumberPicker.OnValueChangeListener {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mRcvAmount = getCurrencyPicker(mCurrencyId);
                updateRcvUri();
            }
        }

        mCurrencyId = CURRENCY_BITCOIN;
        np_currency.setOnValueChangedListener(new CurrencyChangeListener());
        np_currency.setValue(mCurrencyId);

        np[0].setOnValueChangedListener(new AmountChangeListner());
        np[1].setOnValueChangedListener(new AmountChangeListner());
        np[2].setOnValueChangedListener(new AmountChangeListner());
        np[3].setOnValueChangedListener(new AmountChangeListner());
        np[4].setOnValueChangedListener(new AmountChangeListner());
        updateRcvUri();
    }

    float getCurrencyPicker(int currencyId)
    {
        float amount;
        if(currencyId == CURRENCY_BITCOIN) {
            amount = (float)np[4].getValue() + (float)np[3].getValue() * 0.1f + (float)np[2].getValue() * 0.01f + (float)np[1].getValue() * 0.001f + (float)(np[0].getValue() * 0.0001);
        } else {
            amount = np[4].getValue() * 100f + np[3].getValue() * 10f + np[2].getValue() + np[1].getValue() * 0.10f + np[0].getValue() * 0.01f;
            amount = amount / btc_to_dollar;
        }
        return amount;
    }

    void setCurrencyPicker(int currencyId, float amount) {
        if(currencyId == CURRENCY_BITCOIN) {
            mDecimalPos = 3;
            for (int i = 0; i < 5; i++) {
                np[i].setMinValue(0);
                np[i].setMaxValue(9);

                if(i == mDecimalPos) {
                    np[i].setFormatter(new NumberPicker.Formatter() {
                        @Override
                        public String format(int i) {
                            return String.format(".%1d", i);
                        }
                    });

                } else {
                    np[i].setFormatter(new NumberPicker.Formatter() {
                        @Override
                        public String format(int i) {
                            return String.format("%1d", i);
                        }
                    });
                }
            }

            int amountUbtc100s = (int)(amount * 10000f);
            int ubtc100s = amountUbtc100s  % 10;
            int mbtcs1s =(int) ((amountUbtc100s / 10) % 10);
            int mbtc10s =(int) ((amountUbtc100s / 100) % 10);
            int mbtc100s =(int)((amountUbtc100s / 1000) % 10);
            int btc =(int) (amountUbtc100s / 10000);
            np[0].setValue(ubtc100s);
            np[1].setValue(mbtcs1s);
            np[2].setValue(mbtc10s);
            np[3].setValue(mbtc100s);
            np[4].setValue(btc);

        } else {
            mDecimalPos = 1;
            for (int i = 0; i < 5; i++) {
                np[i].setMinValue(0);
                np[i].setMaxValue(9);
            }

            for (int i = 0; i < 5; i++) {
                if(i == mDecimalPos) {
                    np[i].setFormatter(new NumberPicker.Formatter() {
                        @Override
                        public String format(int i) {
                            return String.format(".%1d", i);
                        }
                    });

                } else {
                    np[i].setFormatter(new NumberPicker.Formatter() {
                        @Override
                        public String format(int i) {
                            return String.format("%1d", i);
                        }
                    });
                }
            }

            long amountCents = (int)((amount * btc_to_dollar) * 100f);
            int cents = (int) (amountCents % 10);
            int dimes = (int) ((amountCents / 10) % 10);
            int dollars = (int) (amountCents / 100);
            int dollar1s = dollars % 10;
            int dollar10s = (dollars / 10) % 10;
            int dollar100s = (dollars / 100) % 100;
            np[0].setValue(cents);
            np[1].setValue(dimes);
            np[2].setValue(dollar1s);
            np[3].setValue(dollar10s);
            np[4].setValue(dollar100s);
        }
    }

    void updateStatus(String status) {
        mStatus = status;
        runOnUiThread(mRunnable);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            txtStatus.setText(mStatus);
        }
    };

    void updateRcvUri() {
        runOnUiThread(mRunnableRcvAddress);
    }

    private Runnable mRunnableRcvAddress = new Runnable() {
        @Override
        public void run() {

            Bitmap bitmap = null;

            try {
                String btcUri = "bitcoin:" + mRcvAddress + "?amount="  + mRcvAmount;
                txtRcvAddress.setText(btcUri);
                bitmap = encodeAsBitmap(btcUri);
            } catch (WriterException e) {
             Log.d(TAG, " Error generating QR Code for receive address");
            }
            if(bitmap != null) {
                imgRcvAddress.setImageBitmap(bitmap);
            }

        }
    };

    @Override
    public void onClick(View v) {

    }


    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? getResources().getColor(R.color.black):getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
        return bitmap;
    } /// end of this method
}
