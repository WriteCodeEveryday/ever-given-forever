package com.challenge.camera2label;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

import com.challenge.license2label.BuildConfig;

public class PrintableGenerator {
    private String file = null;
    public PrintableGenerator() {
        build();
    }

    private void build() {
        String printerModel = PrinterManager.getModel();
        String printerMode = PrinterManager.getMode();
        if (printerMode != null && printerModel != null) {
            file = PrinterManager.dashToLower(PrinterManager.getModel().toLowerCase()) + "_" + PrinterManager.getMode();
        } else if (printerModel != null) {
            file = PrinterManager.dashToLower(PrinterManager.getModel().toLowerCase()) + "_label";
        } else {
            file = PrinterManager.dashToLower(PrinterManager.getSupportedModels()[0].toLowerCase()) + "_label";
        }
    }

    public Bitmap buildOutput(AddressLabel item, Context ctx) {
        if (file == null) {
            build();
        }

        Resources resources = ctx.getResources();
        int scale = (int) resources.getDisplayMetrics().density;
        int resource = ctx.getResources().getIdentifier(file, "drawable", ctx.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resource);
        if (bitmap == null) {
            return null;
        }

        android.graphics.Bitmap.Config bitmapConfig = bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        Bitmap rotatedBitmap = bitmap.copy(bitmapConfig, true);

        boolean rotated = bitmap.getHeight() > bitmap.getWidth();
        int qrCodeDimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int qrCodeSize = (int) (qrCodeDimension * 0.5);
        int qrCodeTop = qrCodeSize/2;

        int textCodeDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());

        if (rotated) {
            // Matrixes for rotations.
            Matrix rotate = new Matrix();
            rotate.postRotate(90);

            rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotate, true);
        }


        //Paints for text and background
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.BLACK);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setStyle(Paint.Style.FILL);
        bg.setColor(Color.WHITE);

        Canvas canvas = new Canvas(rotatedBitmap);

        String[] outputs = item.getPrintables();
        Rect largest = new Rect();
        int length = 0;
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i].length() > length) {
                length = outputs[i].length();
                text.setTextSize((int) textCodeDimension / 20);
                text.getTextBounds(outputs[i], 0, outputs[i].length(), largest);
            }
        }

        // draw text to the Canvas center
        Rect bounds = new Rect();
        text.setTextSize((int) textCodeDimension / 20);
        text.getTextBounds(outputs[1], 0, outputs[1].length(), bounds);

        int maxX = textCodeDimension - bounds.width() - (int)(qrCodeSize * 0.1);
        int minX = (int)(qrCodeSize * 1.3);
        int diff = (maxX - minX)/2;
        int x = maxX - diff;
        int y = (rotatedBitmap.getHeight() + bounds.height())/6;
        int aboveY = y - (int) (bounds.height() * 0.75);
        int belowY = y + (int) (bounds.height() * 0.75);
        canvas.drawText(outputs[1], x, y * scale, text);

        item.generateQrCode(qrCodeSize);
        Bitmap qrCode = item.getQrCode();
        canvas.drawBitmap(qrCode, (int) (qrCodeSize * 0.1), qrCodeTop, null);

        // draw text slight above Canvas center
        String appName = "Camera2Label v" + BuildConfig.VERSION_CODE;
        text.setTextSize((int) qrCodeDimension / 15);
        text.getTextBounds(appName, 0, appName.length(), bounds);

        canvas.drawText(appName, (int) (qrCodeSize * 0.1), qrCodeSize + qrCodeTop + bounds.height(), text);

        // draw text slight above Canvas center
        text.setTextSize((int) textCodeDimension/ 20);
        text.getTextBounds(outputs[0], 0, outputs[0].length(), bounds);
        canvas.drawText(outputs[0], x, aboveY * scale, text);

        // draw text slight above Canvas center
        text.setTextSize((int) textCodeDimension / 20);
        text.getTextBounds(outputs[2], 0, outputs[2].length(), bounds);
        canvas.drawText(outputs[2], x, belowY * scale, text);

        if (rotated) {
            Matrix counter = new Matrix();
            counter.postRotate(270);
            rotatedBitmap = Bitmap.createBitmap(rotatedBitmap, 0, 0, rotatedBitmap.getWidth(), rotatedBitmap.getHeight(), counter, true);
        }
        return  rotatedBitmap;
    }

    private Rect generateBackground(Rect bounds, int x, int y, int scale) {
        Rect background = new Rect(bounds.left, bounds.top, bounds.right, bounds.bottom);
        background.left += x * scale;
        background.right += x * scale;
        background.top += y * scale;
        background.bottom += y * scale;

        int xSize = background.left - background.right;
        int ySize = background.top - background.bottom;
        background.left += xSize * 0.1;
        background.right -= xSize * 0.1;
        background.top += ySize * 0.3;
        background.bottom -= ySize * 0.3;
        return background;
    }
}
