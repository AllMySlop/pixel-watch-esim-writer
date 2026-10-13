package io.github.watchesim.helper;

import android.os.Bundle;
import android.view.WindowManager;
import com.journeyapps.barcodescanner.CaptureActivity;

/** Keep the QR credential out of screenshots while the camera is open. */
public final class ScanActivity extends CaptureActivity {
    @Override protected void onCreate(Bundle state) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(state);
    }
}
