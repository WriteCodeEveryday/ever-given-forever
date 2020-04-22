package com.challenge.camera2label;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;

import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.challenge.license2label.R;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class ScannerActivity extends AppCompatActivity {
    private boolean isPrinting = false;
    private boolean shouldPrint;
    private Camera mCamera;
    private CameraPreview mPreview;

    private BarcodeDetector detector;
    private TextRecognizer recognizer;

    @Override
    protected void onResume() {
        super.onResume();
        View mContentView = findViewById(R.id.scanner_activity_layout);
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

        setContentView(R.layout.scanner_activity);

        View mContentView = findViewById(R.id.scanner_activity_layout);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        Bundle extras = getIntent().getExtras();
        // Whether to print results right away. only use for safe data (QR / License)
        shouldPrint = extras.getBoolean("PRINT", false);
        boolean isPHOTO = extras.getBoolean("PHOTO",false);
        boolean isOCR = extras.getBoolean("OCR", false);
        boolean isQR = extras.getBoolean("QR", false);
        boolean isLICENSE = extras.getBoolean("LICENSE", false);
        final boolean[] config = new boolean[] {isPHOTO, isOCR, isQR, isLICENSE};

        detector = new BarcodeDetector.Builder(this)
                    .setBarcodeFormats(Barcode.PDF417 | Barcode.DRIVER_LICENSE | Barcode.QR_CODE)
                    .build();
        recognizer = new TextRecognizer.Builder(this).build();


        if (!config[0]) {
            mCamera = getCameraInstance();
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Camera.Parameters parameters = camera.getParameters();
                    int width = parameters.getPreviewSize().width;
                    int height = parameters.getPreviewSize().height;

                    YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

                    byte[] bytes = out.toByteArray();
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    onImage(bitmap, config);
                }
            });
            int text = -1;
            if (isLICENSE) {
                text = R.string.license_overlay_text;
            } else if (isQR) {
                text = R.string.qr_label_overlay_text;
            } else if (isOCR) {
                text = R.string.label_overlay_text;
            }

            LinearLayout preview = (LinearLayout) findViewById(R.id.scanner_activity_preview);
            mPreview = new CameraPreview(this, mCamera);
            preview.addView(mPreview);
        } else {
            String uri = extras.getString("PHOTO_DATA");
            try {
                Bitmap picture = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(uri));
                onImage(picture, config);
            }  catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent completed = new Intent();
            setResult(Activity.RESULT_CANCELED, completed);
            finish();
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(){
        Camera c = null;
        if (!checkCameraHardware(getApplicationContext())) {
            System.out.println("No Camera");
            return null;
        }

        try {
            c = Camera.open(); // attempt to get a Camera instance
            Camera.Parameters params = c.getParameters();
            if (params.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } // Set Auto Focus.
            /*if (params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } // Set Flash On. */
            c.setParameters(params);

            // Set Contrast to Maximum.
            String parmListStr = params.flatten();
            String[] parms = parmListStr.split(";");
            int maxContrast = 0, curContrast = 0, newContrast = 0;
            for(String str:parms){
                if(str.contains("max-contrast=")){
                    String[] values = str.split("=");
                    maxContrast = Integer.getInteger(values[1]);
                } else if (str.contains("contrast=")){
                    String[] values = str.split("=");
                    curContrast = Integer.getInteger(values[1]);
                }
            }

            if (maxContrast > 0 && curContrast >= 0){
                //calculate contrast as per your needs and set it to camera parameters as below
                newContrast = (curContrast + 1) < maxContrast? (curContrast + 1): maxContrast;
                params.set("contrast", newContrast);
                c.setParameters(params);
            }
        }
        catch (Exception e){
            System.out.println("Camera Error: " + e);
        }
        return c; // returns null if camera is unavailable
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    protected void onImage(Bitmap data, boolean[] payload) {
        if (isPrinting) {
            return;
        }

        if (payload[0]) {
            processBarcodes(data);
            processOCR(data);
        } else if (payload[1]) {
            processOCR(data);
        } else  {
            processBarcodes(data);
        }
    }

    private void processOCR(final Bitmap data) {
        Frame frame = new Frame.Builder().setBitmap(data).build();
        final SparseArray<TextBlock> text = recognizer.detect(frame);

        for (int i = 0; i < text.size(); i++) {
            TextBlock current = text.valueAt(i);
            final List<? extends Text> textComponents = current.getComponents();

            String name = null;
            String address = null;
            String address2 = null;
            if (textComponents.size() == 2) {
                String first = textComponents.get(0).getValue();
                String second = textComponents.get(1).getValue();

                String[] results = new String[3];
                if (first.length() < second.length()) {
                    String[] seg = second.split(",", 2);
                    results[0] = first;
                    results[1] = seg[0];
                    if (seg.length > 1) {
                        results[2] = seg[1];
                    }
                } else {
                    String[] seg = first.split(",", 2);
                    results[0] = seg[0];
                    if (seg.length > 1) {
                        results[1] = seg[1];
                        results[2] = second;
                    } else {
                        results[1] = second;
                    }
                }

                name = results[0];
                address = results[1];
                address2 = results[2];
            }
            else if (textComponents.size() == 3) {
                isPrinting = true;
                AddressLabel.LAST = null;

                name = textComponents.get(0).getValue();
                address = textComponents.get(1).getValue();
                address2 = textComponents.get(2).getValue();
            } else if (textComponents.size() == 4) {
                isPrinting = true;
                AddressLabel.LAST = null;

                name = textComponents.get(0).getValue();
                name += " " +textComponents.get(1).getValue();
                address = textComponents.get(2).getValue();;
                address2 = textComponents.get(3).getValue();
            }

            data.recycle();

            if (name != null && address != null) { // Allow address 2 to be null.
                final String finalName = name;
                final String finalAddress = address;
                final String finalAddress1 = address2;
                new Thread() {
                    @Override
                    public void run() {
                        AddressLabel item = new AddressLabel(finalName, finalAddress, finalAddress1);

                        PrintableGenerator pr = new PrintableGenerator();
                        Bitmap output = pr.buildOutput(item, getApplicationContext());

                        item.setPrintable(output);
                        AddressLabel.LAST = item;
                    }
                }.start();

                Intent completed = new Intent();
                setResult(Activity.RESULT_OK, completed);
                finish();
            }
        }
    }

    private void processBarcodes(final Bitmap data) {
        Frame frame = new Frame.Builder().setBitmap(data).build();
        final SparseArray<Barcode> barcodes = detector.detect(frame);
        if (barcodes.size() > 0) {
            isPrinting = true;
            AddressLabel.LAST = null;
            new Thread() {
                public void run() {
                    for (int i = 0; i < barcodes.size(); i++) {
                        String name = null;
                        String address = null;
                        String address2 = null;
                        AddressLabel item = null;

                        Barcode current = barcodes.valueAt(i);
                        switch (current.valueFormat) {
                            case Barcode.QR_CODE:
                                item = new AddressLabel(current.rawValue);
                                break;
                            case Barcode.PDF417:
                            case Barcode.TEXT:
                                if (current.valueFormat == Barcode.TEXT && current.rawValue.split(";").length > 1) {
                                    item = new AddressLabel(current.rawValue);
                                } else {
                                    String[] segments = current.rawValue.split("\\n");
                                    boolean fullName = false;
                                    String city = null;
                                    String state = null;
                                    String postalCode = null;
                                    for (int j = 0; j < segments.length; j++) {
                                        String trimmed = segments[j].trim();
                                        String extracted = null;
                                        if (trimmed.startsWith("DAA") || (trimmed.contains("ANSI") && trimmed.contains("DAA"))) { // Full Name
                                            extracted = trimmed.split("DAA")[1];
                                            name = extracted;
                                            fullName = true;
                                        } else if (trimmed.startsWith("DAC")) { // Last Name
                                            if (!fullName) {
                                                extracted = trimmed.split("DAC")[1];
                                                if (name == null) {
                                                    name = extracted;
                                                } else {
                                                    name = extracted + " " + name;
                                                }
                                            }
                                        } else if (trimmed.startsWith("DAB")) { // First Name
                                            if (!fullName) {
                                                extracted = trimmed.split("DAB")[1];
                                                if (name == null) {
                                                    name = extracted;
                                                } else {
                                                    name = name + " " + extracted;
                                                }
                                            }
                                        } else if (trimmed.startsWith("DAG")) { //address
                                            extracted = trimmed.split("DAG")[1];
                                            address = extracted;
                                        } else if (trimmed.startsWith("DAI")) { // city
                                            extracted = trimmed.split("DAI")[1];
                                            city = extracted;
                                        } else if (trimmed.startsWith("DAJ")) { // state
                                            extracted = trimmed.split("DAJ")[1];
                                            state = extracted;
                                        } else if (trimmed.startsWith("DAK")) { // postalCode
                                            extracted = trimmed.split("DAK")[1];
                                            postalCode = extracted;
                                        }

                                    }

                                    address2 = city; // We hope all licenses have a city...
                                    if (state != null) {
                                        address2 += ", " + state;
                                    }
                                    if (postalCode != null) {
                                        address2 += " " + postalCode.split("-")[0];
                                    }

                                    item = new AddressLabel(name, address, address2);
                                }
                                break;
                            case Barcode.DRIVER_LICENSE:
                                Barcode.DriverLicense lic = current.driverLicense;
                                name = lic.firstName;
                                if (lic.middleName != null) {
                                    name += " " + lic.middleName;
                                }
                                if (lic.lastName != null) {
                                    name += " " + lic.lastName;
                                }
                                address = lic.addressStreet;
                                address2 = lic.addressCity;
                                if (lic.addressState != null) {
                                    address2 += ", " + lic.addressState;
                                }
                                if (lic.addressZip != null) {
                                    address2 += " " + lic.addressZip.split("-")[0];
                                }

                                item = new AddressLabel(name, address, address2);
                                break;
                        }

                        data.recycle();

                        if(item != null) {
                            PrintableGenerator pr = new PrintableGenerator();
                            Bitmap output = pr.buildOutput(item, getApplicationContext());

                            item.setPrintable(output);
                            AddressLabel.LAST = item;

                            if (shouldPrint) {
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
                        }
                    }
                }
            }.start();

            Intent completed = new Intent();
            setResult(Activity.RESULT_OK, completed);
            finish();
        }
    }
}
