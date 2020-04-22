package com.challenge.camera2label;

import android.graphics.Bitmap;

import com.challenge.license2label.BuildConfig;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

public class AddressLabel {
    private String name;
    private String line;
    private String line2;
    private String qrSequence;
    private Bitmap qrCode;
    private Bitmap printable;
    public static AddressLabel LAST;

    public AddressLabel(String raw) {
        this(raw.split(";"));
    }

    public AddressLabel(String[] parts) {
        this(parts[0], parts[1], parts[2]);
    }

    public AddressLabel(String addressName, String addressLine1, String addressLine2) {
        name = addressName;
        if (addressLine2 != null) {
            line = addressLine1;
            line2 = addressLine2;
        } else { // Try carving line2 from the original line.
            String[] segs = addressLine1.split(" ");
            if (segs.length > 2) {
                line = segs[0];
                for (int i = 1; i < segs.length - 2; i++) {
                    line += " " + segs[i];
                }
                line2 = segs[segs.length-2] + " " +segs[segs.length-1]; //Carved.
            } else { // Insufficient segments for carving.
                line = addressLine1;
                line2 = "";
            }
        }

        name = name.trim();
        line = line.trim();
        line2 = line2.trim();

        qrSequence = buildQrSequence(true);
    }


    private String buildQrSequence(boolean md5) {
        String built = name + ";" + line + ";" + line2;
        built += ";" + System.currentTimeMillis();
        built += ";" + BuildConfig.VERSION_CODE;
        return built;
    }

    public String[] getPrintables() {
        return new String[]{ name, line, line2};
    }

    public Bitmap getQrCode() {
        return qrCode;
    }

    public void setPrintable(Bitmap incoming) {
        if (printable != null) {
            printable.recycle();
        }
        printable = incoming;
    }

    public Bitmap getPrintable() { return printable; }

    public void generateQrCode(int size) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            Map<EncodeHintType, Object> hintMap = new HashMap<EncodeHintType, Object>();
            hintMap.put(EncodeHintType.MARGIN, new Integer(0)); // No white padding around the QR Code.

            BitMatrix bitMatrix = multiFormatWriter.encode(qrSequence, BarcodeFormat.QR_CODE, size, size, hintMap);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap output = barcodeEncoder.createBitmap(bitMatrix);
            if (qrCode != null) {
                qrCode.recycle();
            }
            qrCode = output;
        } catch (WriterException e) {
            System.out.println("Error in QR: " + e);
        }
    }

    //Return a name for the address label
    public String getName() {
        return name.replaceAll("[^a-zA-Z0-9]", "")
                + line.replaceAll("[^a-zA-Z0-9]", "")
                + line2.replaceAll("[^a-zA-Z0-9]", "");
    }
}
