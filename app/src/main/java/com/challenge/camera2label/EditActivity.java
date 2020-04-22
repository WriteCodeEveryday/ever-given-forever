package com.challenge.camera2label;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.challenge.license2label.R;

public class EditActivity extends AppCompatActivity {
    AddressLabel item;
    private void updatePreview() {
        final String line = "" + ((EditText)findViewById(R.id.line)).getText();
        final String line2 = "" + ((EditText)findViewById(R.id.line2)).getText();
        final String line3 = "" + ((EditText)findViewById(R.id.line3)).getText();

        new Thread() {
            public void run() {
                item = new AddressLabel(line, line2, line3);
                PrintableGenerator pr = new PrintableGenerator();
                Bitmap output = pr.buildOutput(item, getApplicationContext());
                item.setPrintable(output);
                final ImageView preview = new ImageView(getApplicationContext());
                Bitmap image = item.getPrintable();

                Matrix matrix = new Matrix();
                if (image.getHeight() < image.getWidth()) {
                    matrix.postRotate(-90);
                    image = Bitmap.createBitmap(image, 0, 0,
                            image.getWidth(), image.getHeight(),
                            matrix, true);
                }
                matrix = new Matrix();
                matrix.postRotate(180);
                image = Bitmap.createBitmap(image, 0, 0,
                        image.getWidth(), image.getHeight(),
                        matrix, true);

                DisplayMetrics met = getApplicationContext()
                        .getResources()
                        .getDisplayMetrics();
                float ratio = (float) image.getWidth() / (float) image.getHeight();
                int height =  (int) (met.heightPixels * 0.4);
                int width = (int) (height * ratio);
                image  = Bitmap.createScaledBitmap(image, width, height, true);


                int size = 3;
                Bitmap borderImage = Bitmap.createBitmap(
                        image.getWidth() + size * 2,
                        image.getHeight() + size * 2,
                        image.getConfig());
                Canvas canvas = new Canvas(borderImage);
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(image, size, size, null);
                image = borderImage;

                preview.setImageBitmap(image);
                preview.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                       updatePreview();
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout temp = findViewById(R.id.preview_qr);
                        temp.removeAllViews();
                        temp.setGravity(Gravity.CENTER);
                        temp.addView(preview, 0);
                    }
                });
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_activity);

        View mContentView = findViewById(R.id.edit_activity_layout);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        if (AddressLabel.LAST != null) {
            String[] printables = AddressLabel.LAST.getPrintables();
            ((EditText) findViewById(R.id.line)).setText(printables[0]);
            ((EditText) findViewById(R.id.line2)).setText(printables[1]);
            ((EditText) findViewById(R.id.line3)).setText(printables[2]);
        }

        updatePreview();

        Button mControlsView = findViewById(R.id.preview_save_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddressLabel.LAST = item;
                Intent completed = new Intent();
                setResult(Activity.RESULT_OK, completed);
                finish();
            }
        });

        mControlsView = findViewById(R.id.preview_cancel_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent completed = new Intent();
                setResult(Activity.RESULT_OK, completed);
                finish();
            }
        });
    }
}
