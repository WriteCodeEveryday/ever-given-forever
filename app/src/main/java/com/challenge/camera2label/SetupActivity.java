package com.challenge.camera2label;

import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.challenge.license2label.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SetupActivity extends AppCompatActivity {
    private static final int REQUEST_OPEN_IMAGE = 1337;
    private static final int REQUEST_WRITE_IMAGE = 1338;

    protected void loadPrinterPreferences() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("printer_settings", Context.MODE_PRIVATE);
        String printer = prefs.getString("printer", null);
        String raw_connection = prefs.getString("connection", null);
        PrinterManager.CONNECTION connection = null;
        if (raw_connection != null && !raw_connection.equals("null")) {
            connection = PrinterManager.CONNECTION.valueOf(raw_connection);
        }
        String mode = prefs.getString("mode", null);

        if(printer != null) {
            PrinterManager.setModel(printer);
        }

        if (connection != null) {
            PrinterManager.setConnection(connection);
        }

        if (printer != null && connection != null) {
            PrinterManager.findPrinter(printer, connection);
        }

        if (mode != null) {
            switch(mode) {
                case "label":
                    PrinterManager.loadLabel();
                    break;
                case "roll":
                    PrinterManager.loadRoll();
                    break;
            }
        }
    }

    protected void savePrinterPreferences() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("printer_settings",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String printer = PrinterManager.getModel();
        PrinterManager.CONNECTION connection = PrinterManager.getConnection();
        String mode = PrinterManager.getMode();

        editor.putString("printer", printer);
        editor.putString("connection", String.valueOf(connection));
        editor.putString("mode", mode);


        editor.commit();
    }

    private boolean canPrint() {
        String printer = PrinterManager.getModel();
        PrinterManager.CONNECTION connection = PrinterManager.getConnection();
        String mode = PrinterManager.getMode();

        return printer != null && connection != null && mode != null;
    }

    private void setUpPrinterOptions() {
        String currentModel = PrinterManager.getModel();
        PrinterManager.CONNECTION currentConnection = PrinterManager.getConnection();

        final String[] supportedModels = PrinterManager.getSupportedModels();
        final PrinterManager.CONNECTION[] supportedConnections = PrinterManager.getSupportedConnections();

        RadioGroup connectors = this.findViewById(R.id.connection_selection_group);
        RadioGroup printers = this.findViewById(R.id.printer_selection_group);

        printers.removeAllViews();
        for (int i = 0; i < supportedModels.length; i++) {
            RadioButton button = new RadioButton(this);
            if (currentModel != null) {
                button.setChecked(supportedModels[i].equals(currentModel));
            }
            button.setText(supportedModels[i]);
            button.setId(i);
            final int j = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected()) {
                        PrinterManager.setModel(supportedModels[j]);
                        PrinterManager.setConnection(null);
                        savePrinterPreferences();
                        onPrinterSelectionButton();
                    }
                }
            });
            printers.addView(button);
        }

        connectors.removeAllViews();
        for (int i = 0; i < supportedConnections.length; i++) {
            RadioButton button = new RadioButton(this);
            if (currentConnection != null) {
                button.setChecked(supportedConnections[i].compareTo(currentConnection) == 0);
            }
            button.setText(supportedConnections[i].toString());
            button.setId(i);
            final int j = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected()) {
                        PrinterManager.setConnection(supportedConnections[j]);
                        savePrinterPreferences();
                        onPrinterSelectionButton();
                    }
                }
            });
            connectors.addView(button);
        }

        String currentMode = PrinterManager.getMode();
        RadioButton label = this.findViewById(R.id.radio_option_label);
        RadioButton roll = this.findViewById(R.id.radio_option_roll);
        if (currentMode != null) {
            switch (currentMode) {
                case "roll":
                    roll.setChecked(true);
                    break;
                case "label":
                    label.setChecked(true);
                    break;
            }
        } else {
            roll.setChecked(false);
            label.setChecked(false);
        }
    }

    private void onPrinterSelectionButton() {
        final RadioButton label = this.findViewById(R.id.radio_option_label);
        final RadioButton roll = this.findViewById(R.id.radio_option_roll);

        final PrinterManager.CONNECTION currentConnection = PrinterManager.getConnection();
        final String currentModel = PrinterManager.getModel();

        setUpPrinterOptions();
        if (currentModel == null || currentConnection == null) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    label.setVisibility(View.GONE);
                    roll.setVisibility(View.GONE);
                }
            });
        } else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentConnection == null ||
                            currentModel == null) {
                        return;
                    }
                    if (PrinterManager.getConnection().equals(PrinterManager.CONNECTION.BLUETOOTH)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        }

                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                                .getDefaultAdapter();
                        if (bluetoothAdapter != null) {
                            if (!bluetoothAdapter.isEnabled()) {
                                Intent enableBtIntent = new Intent(
                                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(enableBtIntent);
                            }
                        }
                    }

                    PrinterManager.findPrinter(PrinterManager.getModel(), PrinterManager.getConnection());
                    setUpPrinterOptions();

                    String[] options = PrinterManager.getLabelRoll();
                    if (options.length == 2){
                        label.setText(options[0]);
                        roll.setText(options[1]);

                        label.setVisibility(View.VISIBLE);
                        roll.setVisibility(View.VISIBLE);

                        label.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PrinterManager.setWorkingDirectory(getApplicationContext());
                                PrinterManager.loadLabel();
                                savePrinterPreferences();
                                onPrinterSelectionButton();
                            }
                        });

                        roll.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PrinterManager.setWorkingDirectory(getApplicationContext());
                                PrinterManager.loadRoll();
                                savePrinterPreferences();
                                onPrinterSelectionButton();
                            }
                        });

                        label.setEnabled(true);
                        roll.setEnabled(true);
                    }
                }
            });
        }

        if (AddressLabel.LAST != null) {
            new Thread() {
                public void run() {
                    PrintableGenerator pr = new PrintableGenerator();
                    Bitmap output = pr.buildOutput(AddressLabel.LAST, getApplicationContext());
                    AddressLabel.LAST.setPrintable(output);
                    showLastResult();
                }
            }.start();
        }
    }

    private void showLastResult() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout temp = findViewById(R.id.preview_last_scanned);
                temp.removeAllViews();
                findViewById(R.id.tap_to_print_preview_hint).setVisibility(View.GONE);
                findViewById(R.id.preview_save_button).setVisibility(View.GONE);
                findViewById(R.id.preview_edit_button).setVisibility(View.GONE);

            }
        });

        new Thread() {
            @Override
            public void run() {
                boolean found = false;
                while (!found) {
                    if (AddressLabel.LAST != null) {
                        found = true;
                        if (AddressLabel.LAST.getPrintable() != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    LinearLayout temp = findViewById(R.id.preview_last_scanned);
                                    temp.removeAllViews();
                                    temp.setGravity(Gravity.CENTER);

                                    ImageView preview = new ImageView(getApplicationContext());
                                    Bitmap image = AddressLabel.LAST.getPrintable();

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
                                            PrinterManager.setWorkingDirectory(getApplicationContext());
                                            Printer temp = PrinterManager.getPrinter();
                                            temp.startCommunication();
                                            Bitmap printable = Bitmap.createBitmap(AddressLabel.LAST.getPrintable());
                                            PrinterStatus result = temp.printImage(printable);
                                            if (result.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                                                System.out.println("Error: " + result.errorCode);
                                            }
                                            temp.endCommunication();
                                        }
                                    });
                                    temp.addView(preview, 0);
                                    findViewById(R.id.tap_to_print_preview_hint).setVisibility(View.VISIBLE);
                                    findViewById(R.id.preview_save_button).setVisibility(View.VISIBLE);
                                    findViewById(R.id.preview_edit_button).setVisibility(View.VISIBLE);
                                }
                            });
                        } else {
                            new Thread() {
                                public void run() {
                                    PrintableGenerator pr = new PrintableGenerator();
                                    Bitmap output = pr.buildOutput(AddressLabel.LAST, getApplicationContext());
                                    AddressLabel.LAST.setPrintable(output);
                                    showLastResult();
                                }
                            }.start();
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Could not sleep: " + e);
                    }
                }
            }
        }.start();
    }

    private void showScanOptions() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((Button) findViewById(R.id.printer_scan_button)).setText(R.string.printer_scan_cancel_button);
                findViewById(R.id.printer_scanner_picture_button).setVisibility(View.VISIBLE);
                findViewById(R.id.printer_scanner_ocr_button).setVisibility(View.VISIBLE);
                findViewById(R.id.printer_scanner_qr_button).setVisibility(View.VISIBLE);
                findViewById(R.id.printer_scanner_license_button).setVisibility(View.VISIBLE);

                findViewById(R.id.usb_to_preview_hint).setVisibility(View.INVISIBLE);
                findViewById(R.id.printer_selector_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.connection_selector_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.paper_type).setVisibility(View.INVISIBLE);
            }
        });
    }

    private void hideScanOptions() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((Button) findViewById(R.id.printer_scan_button)).setText(R.string.printer_scan_button);
                findViewById(R.id.printer_scanner_picture_button).setVisibility(View.GONE);
                findViewById(R.id.printer_scanner_ocr_button).setVisibility(View.GONE);
                findViewById(R.id.printer_scanner_qr_button).setVisibility(View.GONE);
                findViewById(R.id.printer_scanner_license_button).setVisibility(View.GONE);

                findViewById(R.id.usb_to_preview_hint).setVisibility(View.VISIBLE);
                findViewById(R.id.printer_selector_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.connection_selector_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.paper_type).setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        View mContentView = findViewById(R.id.setup_activity_layout);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        loadPrinterPreferences();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setup_activity);

        View mContentView = findViewById(R.id.setup_activity_layout);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        loadPrinterPreferences();
        if (canPrint()) {
            Intent scan = new Intent(getApplicationContext(), ScannerActivity.class);
            scan.putExtra("LICENSE", true);
            scan.putExtra("PRINT", true);
            startActivityForResult(scan, 0);
        }

        showLastResult();

        Button mControlsView = findViewById(R.id.preview_save_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    @Override
                    public void run(){
                        String name = AddressLabel.LAST.getName();

                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/png");
                        intent.putExtra(Intent.EXTRA_TITLE, name);
                        startActivityForResult(intent, REQUEST_WRITE_IMAGE);
                    }
                }.start();
            }
        });

        mControlsView = findViewById(R.id.preview_edit_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent edit = new Intent(getApplicationContext(), EditActivity.class);
                startActivityForResult(edit, 0);
            }
        });

        mControlsView = findViewById(R.id.printer_scan_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (findViewById(R.id.printer_scanner_ocr_button).getVisibility() == View.GONE) {
                    showScanOptions();
                } else {
                    hideScanOptions();
                }
            }
        });

        mControlsView = findViewById(R.id.printer_scanner_picture_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        hideScanOptions();
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, REQUEST_OPEN_IMAGE);
                    }
                }.start();
            }
        });
        mControlsView.setVisibility(View.GONE);

        mControlsView = findViewById(R.id.printer_scanner_ocr_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        hideScanOptions();
                        Intent scan = new Intent(getApplicationContext(), ScannerActivity.class);
                        scan.putExtra("OCR", true);
                        startActivityForResult(scan, 0);
                    }
                }.start();
            }
        });
        mControlsView.setVisibility(View.GONE);

        mControlsView = findViewById(R.id.printer_scanner_qr_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        hideScanOptions();
                        Intent scan = new Intent(getApplicationContext(), ScannerActivity.class);
                        scan.putExtra("QR", false);
                        startActivityForResult(scan, 0);
                    }
                }.start();
            }
        });
        mControlsView.setVisibility(View.GONE);

        mControlsView = findViewById(R.id.printer_scanner_license_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() {
                        hideScanOptions();
                        Intent scan = new Intent(getApplicationContext(), ScannerActivity.class);
                        scan.putExtra("LICENSE", true);
                        startActivityForResult(scan, 0);
                    }
                }.start();
            }
        });
        mControlsView.setVisibility(View.GONE);

        onPrinterSelectionButton();

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                Intent scan = new Intent(getApplicationContext(), ScannerActivity.class);
                scan.putExtra("PHOTO", true);
                scan.putExtra("PHOTO_DATA", imageUri.toString());
                startActivityForResult(scan, 0);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_IMAGE) {
            if (resultCode == RESULT_OK) {
                Uri photoUri = data.getData();

                Intent scan = new Intent(getApplicationContext(), ScannerActivity.class);
                scan.putExtra("PHOTO", true);
                scan.putExtra("PHOTO_DATA", photoUri.toString());
                startActivityForResult(scan, 0);
            }
        } else if (requestCode == REQUEST_WRITE_IMAGE) {
            if (resultCode == RESULT_OK) {
                Bitmap image = AddressLabel.LAST.getPrintable();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bytes = stream.toByteArray();
                image.recycle();

                try {
                    OutputStream out = getContentResolver().openOutputStream(data.getData());
                    out.write(bytes);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else if (resultCode == RESULT_OK) {
            showLastResult();
        } else {
            Toast.makeText(SetupActivity.this, R.string.no_results_text,
                    Toast.LENGTH_LONG).show();
        }
    }
}
