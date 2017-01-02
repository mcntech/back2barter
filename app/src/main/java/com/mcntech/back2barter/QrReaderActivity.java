package com.mcntech.back2barter;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.samples.vision.barcodereader.ui.camera.CameraSource;

import java.io.IOException;

public class QrReaderActivity extends AppCompatActivity implements View.OnClickListener {

    SurfaceView cameraView;
    TextView barcodeInfo;
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;

    Button btnAccept;
    Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_reader);

        btnAccept = (Button) findViewById(R.id.btn_ok);
        btnCancel = (Button) findViewById(R.id.btn_cancel);

        btnAccept.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        cameraView = (SurfaceView)findViewById(R.id.camera_view);
        barcodeInfo = (TextView)findViewById(R.id.code_info);

        barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    barcodeInfo.post(new Runnable() {    // Use the post method of the TextView
                        public void run() {
                            barcodeInfo.setText(    // Update the TextView
                                    barcodes.valueAt(0).displayValue
                            );
                        }
                    });
                }
            }
        });
    }
    @Override
    public void onClick(View v) {

        if (v == btnAccept) {
            onBtnAccept();
        } else if (btnCancel == v) {
            onBtnCancel();
        }
    }

    void onBtnAccept() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", barcodeInfo.getText().toString());
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
    void onBtnCancel() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", "");
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }

}
