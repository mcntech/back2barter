package com.mcntech.back2barter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.EditText;
import android.util.Log;

import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;
import org.bitcoinj.wallet.listeners.ScriptsChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnPay;
    Button btnGetSndAddrQrCode;
    Button btnReceivePayment;
    Button btnShowTransactions;

    TextView txtStatus;
    TextView txtRcvAddress;
    EditText txtSndAddress;
    TextView txtBalance;

    ImageView imgRcvAddress;

    String mStatus;
    String mRcvAddress;
    String mSndAddress;
    String mBalance;
    NumberPicker np[];
    NumberPicker np_currency;

    WalletAppKit mKit = null;

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
        setContentView(R.layout.activity_main);

        btnPay = (Button) findViewById(R.id.btn_pay);
        btnGetSndAddrQrCode = (Button) findViewById(R.id.btn_getsnd_qrcode);
        btnReceivePayment = (Button) findViewById(R.id.btn_show_qrcode);
        btnShowTransactions = (Button) findViewById(R.id.btn_transactions);

        txtStatus = (TextView) findViewById(R.id.txt_status);
        txtRcvAddress = (TextView) findViewById(R.id.txt_rcv_address);
        txtSndAddress = (EditText) findViewById(R.id.txt_snd_address);

        txtBalance = (TextView) findViewById(R.id.txt_balance);
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

        class MyListener implements NumberPicker.OnValueChangeListener {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mCurrencyId = newVal;
                float amount = getCurrencyPicker(oldVal);
                setCurrencyPicker(mCurrencyId, amount);
            }
        }
        mCurrencyId = CURRENCY_BITCOIN;
        np_currency.setOnValueChangedListener(new MyListener());
        np_currency.setValue(mCurrencyId);

        btnPay.setOnClickListener(MainActivity.this);
        btnGetSndAddrQrCode.setOnClickListener(MainActivity.this);
        btnReceivePayment.setOnClickListener(MainActivity.this);
        btnShowTransactions.setOnClickListener(MainActivity.this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                initWallet();
            }
        }).start();
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

    void updateRcvAddress(String address) {
        mRcvAddress = address;
        runOnUiThread(mRunnableRcvAddress);
    }

    private Runnable mRunnableRcvAddress = new Runnable() {
        @Override
        public void run() {
            txtRcvAddress.setText(mRcvAddress);
            Bitmap bitmap = null;

            try {
                bitmap = encodeAsBitmap(mRcvAddress);
            } catch (WriterException e) {
             Log.d(TAG, " Error generating QR Code for receive address");
            }
            if(bitmap != null) {
                imgRcvAddress.setImageBitmap(bitmap);
            }
        }
    };

    void updatePaymentInfo(BitcoinURI bitcionURI) {
        mSndAddress = bitcionURI.getAddress().toString();
        Coin amount = bitcionURI.getAmount();
        if(amount != null) {
            mPaymentBtc = (float) amount.getValue() / 100000000;
        } else {
            mPaymentBtc = 0f;
        }
        runOnUiThread(mRunnableSndAddress);
    }

    private Runnable mRunnableSndAddress = new Runnable() {
        @Override
        public void run() {
            txtSndAddress.setText(mSndAddress);
            setCurrencyPicker(mCurrencyId, mPaymentBtc);
        }
    };


    void updateBalance(String balance) {
        mBalance = balance;
        runOnUiThread(mRunnableBalance);
    }

    private Runnable mRunnableBalance = new Runnable() {
        @Override
        public void run() {
            txtBalance.setText(mBalance);
        }
    };

    @Override
    public void onClick(View v) {

        if (v == btnPay) {

            mPaymentBtc = getCurrencyPicker(mCurrencyId);
            btnPay.setEnabled(false);
            //sendRequest(btc);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendRequest(mPaymentBtc);
                    }  catch (Exception e) {

                    }
                }
            }).start();
        } else if (btnGetSndAddrQrCode == v) {
            Intent intent = new Intent(this, QrReaderActivity.class);
            startActivityForResult(intent, SND_ADDR_QRCODE);
        } else if(btnReceivePayment == v) {
            Intent intent = new Intent(this, ReceivePaymentActivity.class);
            intent.putExtra("address", mRcvAddress);
            startActivity(intent);
        } else if( btnShowTransactions == v) {
            showTransactions();
  /*          Intent intent = new Intent(this, TransactionListActivity.class);
            intent.putExtra("address", mRcvAddress);
            startActivity(intent);*/
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SND_ADDR_QRCODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                try {
                    String  coinUriString = data.getStringExtra("result");
                    BitcoinURI coinUri = new BitcoinURI(mNetworkparams, coinUriString);
                    updatePaymentInfo(coinUri);
                } catch (BitcoinURIParseException e){

                }
            }
        }
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

    public void initWallet() {


        String[] args = new String[2];
        try {
            Log.d(TAG, "initWallet: NetworkParameters");
            mNetworkparams = TestNet3Params.get();

            Log.d(TAG, "initWallet: WalletAppKit");
            WalletAppKit kit = new WalletAppKit(mNetworkparams, new File(getFilesDir().getPath()), "walletappkit-example");

            Log.d(TAG, "initWallet: startAsync");
            kit.startAsync();

            Log.d(TAG, "initWallet: awaitRunning");
            kit.awaitRunning();

            BitcoinURI coinUri = new BitcoinURI("bitcoin:mupBAFeT63hXfeeT4rnAUcpKHDkz1n4fdw?amount=0.015");
            updatePaymentInfo(coinUri);

            Log.d(TAG, "initWallet: Install callbacks");
            kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
                @Override
                public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                    Log.d(TAG, "-----> coins resceived: " + tx.getHashAsString());
                    Log.d(TAG, "received: " + tx.getValue(wallet));
                    updateStatus("received: " + tx.getValue(wallet));
                    //updateBalance(newBalance.toString());
                    showBalance();
                }
            });

            kit.wallet().addCoinsSentEventListener(new WalletCoinsSentEventListener() {
                @Override
                public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                    Log.d(TAG, "coins sent");
                    updateStatus("coins sent");
                    //updateBalance(newBalance.toString());
                    showBalance();
                }
            });

            kit.wallet().addKeyChainEventListener(new KeyChainEventListener() {
                @Override
                public void onKeysAdded(List<ECKey> keys) {
                    Log.d(TAG, "new key added");
                    updateStatus("new key added");
                }
            });

            kit.wallet().addScriptsChangeEventListener(new ScriptsChangeEventListener() {
                @Override
                public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
                    Log.d(TAG, "new script added");
                    updateStatus("new script added");
                }
            });

            kit.wallet().addTransactionConfidenceEventListener(new TransactionConfidenceEventListener() {
                @Override
                public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
                    Log.d(TAG, "-----> confidence changed: " + tx.getHashAsString());
                    TransactionConfidence confidence = tx.getConfidence();
                    Log.d(TAG, "new block depth: " + confidence.getDepthInBlocks());
                    updateStatus("new block depth: " + confidence.getDepthInBlocks());
                }
            });

            Log.d(TAG, "send money to: " + kit.wallet().freshReceiveAddress().toString());
            updateRcvAddress(kit.wallet().freshReceiveAddress().toString());

            mKit = kit;
            showBalance();
        } catch (Exception e) {

        }
    }

    public void  sendRequest(float btc) throws Exception {
        // Address exception cannot happen as we validated it beforehand.

        if(mKit == null) {
            Log.d(TAG,"Wallet Not Initialized yet");
            updateStatus("Wallet Not Initialized yet");
            return;
        }


            Coin value = Coin.valueOf((long)(btc * 1000000000l));
            Address to = Address.fromBase58(mNetworkparams, mSndAddress);

            try {
                updateStatus("Paying btc=" + btc);
                Wallet.SendResult result = mKit.wallet().sendCoins(mKit.peerGroup(), to, value);
                System.out.println("coins sent. transaction hash: " + result.tx.getHashAsString());


                Futures.addCallback(result.broadcastComplete, new FutureCallback<Transaction>() {
                    @Override
                    public void onSuccess(Transaction result) {
                        //checkGuiThread();
                        //overlayUI.done();
                        updateStatus("Payment Success");
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                            btnPay.setEnabled(true);
                        }});
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // We died trying to empty the wallet.
                        //crashAlert(t);
                        updateStatus("Payment Failed");
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                btnPay.setEnabled(true);
                            }});
                    }
                });

                //result.tx.getConfidence().addEventListener((tx, reason) -> {
                //    if (reason == TransactionConfidence.Listener.ChangeReason.SEEN_PEERS)
                //        //updateTitleForBroadcast();
                //});
                //sendBtn.setDisable(true);
                //address.setDisable(true);
                //((HBox)amountEdit.getParent()).getChildren().remove(amountEdit);
                //((HBox)btcLabel.getParent()).getChildren().remove(btcLabel);
                //updateTitleForBroadcast();

            } catch (InsufficientMoneyException e) {

                Log.d(TAG,"Not enough coins in your wallet. Missing " + e.missing.getValue() + " satoshis are missing (including fees)");
                updateStatus("Not enough coins in your wallet. Missing " + e.missing.getValue() + " satoshis are missing (including fees)");
                Log.d(TAG,"Send money to: " + mKit.wallet().currentReceiveAddress().toString());
                updateStatus("Send money to: " + mKit.wallet().currentReceiveAddress().toString());
                //overlayUI.done();
            } catch (ECKey.KeyIsEncryptedException e) {
                //askForPasswordAndRetry();
            }

    }

    void showBalance() {
        if(mKit != null) {
            String balance = mKit.wallet().getBalance().toFriendlyString();
            updateBalance(balance);
        }
    }


    void showTransactions()
    {
        if(mKit == null)
            return;

        List<Transaction> transactions = mKit.wallet().getTransactionsByTime();
        ArrayList<TransactionEx> tList = new ArrayList<TransactionEx>();
        for(Transaction transaction : transactions) {
            TransactionEx item = new TransactionEx(transaction.getMemo(), transaction.getHashAsString());
            tList.add(item);
        }
        Intent i = new Intent(this, TransactionListActivity.class);
        i.putParcelableArrayListExtra("transactions", tList); // i.putExtra("obj", new ItemDetailsWrapper(list));
        startActivity(i);
    }
}
