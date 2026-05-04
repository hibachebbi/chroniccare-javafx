package com.chroniccare.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

public class QRCodeService {
    private static final String QR_CODE_DIR = "qrcodes";
    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    static {
        File dir = new File(QR_CODE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static String generateQRCode(String registrationData) throws Exception {
        String qrToken = UUID.randomUUID().toString();
        String filename = qrToken + ".png";
        String filePath = Paths.get(QR_CODE_DIR, filename).toString();

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(registrationData, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", Paths.get(filePath));

        return qrToken;
    }

    public static String getQRCodePath(String qrToken) {
        return Paths.get(QR_CODE_DIR, qrToken + ".png").toString();
    }

    public static boolean deleteQRCode(String qrToken) {
        File file = new File(getQRCodePath(qrToken));
        return file.exists() && file.delete();
    }
}
