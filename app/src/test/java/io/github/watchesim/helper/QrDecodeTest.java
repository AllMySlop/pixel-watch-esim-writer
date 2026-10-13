package io.github.watchesim.helper;

import static org.junit.Assert.*;
import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.Test;

public class QrDecodeTest {
    @Test public void decodesGeneratedQrIntoTheExactHandoffCredential() throws Exception {
        String code = "LPA:1$smdp.example.com$TEST-123";
        BitMatrix matrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 320, 320);
        int[] pixels = new int[320 * 320];
        for (int y = 0; y < 320; y++) {
            for (int x = 0; x < 320; x++) pixels[y * 320 + x] = matrix.get(x, y) ? 0xff000000 : 0xffffffff;
        }
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(320, 320, pixels)));
        String decoded = new MultiFormatReader().decode(bitmap).getText();
        assertEquals(code, ActivationCode.parse(decoded).payload);
    }
}
