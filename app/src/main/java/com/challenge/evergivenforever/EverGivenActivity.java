package com.challenge.evergivenforever;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.brother.ptouch.sdk.BLEPrinter;
import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.challenge.license2label.R;

public class EverGivenActivity extends AppCompatActivity {
    private static String desiredPrinter = "QL-820NWB";
    private static PrinterInfo.Model model;
    private static PrinterInfo info;
    private static Printer printer;

    // Connects to the printer.
    protected void connect(){
        if (printer != null) {
            return;
        }

        printer = new Printer();
        info = printer.getPrinterInfo();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                printer = null; // Not enabled.
                return;
            }
        }

        boolean done = false;

        // Try BLE first.
        if (!done) {
            List<BLEPrinter> bleList = printer.getBLEPrinters(BluetoothAdapter.getDefaultAdapter(), 30);
            for (BLEPrinter printer: bleList) {
                if (printer.localName.contains(desiredPrinter)) {
                    model = PrinterInfo.Model.valueOf(dashToLower(desiredPrinter));
                    info.port = PrinterInfo.Port.BLE;
                    info.setLocalName(printer.localName); // Probably wrong.
                    done = true;
                    return;
                }
            }
        }

        // Try non-BLE after.
        if (!done) {
            List<BluetoothDevice> pairedDevices = getPairedBluetoothDevice(bluetoothAdapter);
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains(desiredPrinter)) {
                    model = PrinterInfo.Model.valueOf(dashToLower(desiredPrinter));
                    printer.setBluetooth(BluetoothAdapter.getDefaultAdapter());
                    info.printerModel = model;
                    info.port = PrinterInfo.Port.BLUETOOTH;
                    info.macAddress = device.getAddress();
                    done = true;
                }
            }
        }

        // Set your desired options or eject if the printer wasn't found.
        if (done) {
            info.labelNameIndex = LabelInfo.QL700.W29H90.ordinal();
            info.printMode = PrinterInfo.PrintMode.FIT_TO_PAGE;
            info.isAutoCut = true;
            printer.setPrinterInfo(info);
        } else {
            printer = null;
            info = null;
        }
    }

    // Tiny little helper.
    private static String dashToLower(String val) {
        return val.replace("-","_");
    }

    // Just grab the file and print it.
    private static void print(Context ctx) {
        System.out.println("PRINTING TO: " + info.macAddress);

        Resources resources = ctx.getResources();
        int resource = ctx.getResources().getIdentifier("ql_820nwb_label", "drawable", ctx.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resource);

        android.graphics.Bitmap.Config bitmapConfig = bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        Bitmap mutableBitmap = bitmap.copy(bitmapConfig, true);

        // CRITICAL - If your forget this, you will get an "INVALID_PARAMETER" error
        info.workPath = ctx.getFilesDir().getAbsolutePath() + "/";

        // Don't forget to open and close your communication
        printer.startCommunication();
        PrinterStatus result = printer.printImage(mutableBitmap);
        if (result.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
            System.out.println("Error: " + result.errorCode);
        }
        printer.endCommunication();
    }

    // Magic for bluetooth connections - Boilerplate
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static List<BluetoothDevice> getPairedBluetoothDevice(BluetoothAdapter bluetoothAdapter) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.size() == 0) {
            return new ArrayList<>();
        }
        ArrayList<BluetoothDevice> devices = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                devices.add(device);
            }
            else {
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                    devices.add(device);
                }
            }
        }

        return devices;
    }

    @Override
    protected void onResume() {
        super.onResume();
        connect();
        View mContentView = findViewById(R.id.setup_activity_layout);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connect();
        setContentView(R.layout.evergiven_forever_activity);

        View mContentView = findViewById(R.id.setup_activity_layout);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        Button mControlsView = findViewById(R.id.print_ever_given_button);
        mControlsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print(getApplicationContext());
            }
        });

        ImageView icon = findViewById(R.id.icon);
        icon.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                print(getApplicationContext());
            }
        });
    }
}
