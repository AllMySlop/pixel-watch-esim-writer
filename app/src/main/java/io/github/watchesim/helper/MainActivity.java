package io.github.watchesim.helper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public final class MainActivity extends Activity {
    private EditText input;
    private TextView status;
    private View reviewCard;
    private CheckBox confirmation;
    private Button continueButton;
    private ActivationCode reviewedCode;
    private boolean scanning;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        root.requestApplyInsets();
        input = findViewById(R.id.code);
        status = findViewById(R.id.status);
        reviewCard = findViewById(R.id.review_card);
        confirmation = findViewById(R.id.confirm);
        continueButton = findViewById(R.id.continue_button);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                invalidateReview();
                status.setText("");
            }
            @Override public void afterTextChanged(Editable text) {}
        });
        confirmation.setOnCheckedChangeListener((button, checked) ->
                continueButton.setEnabled(checked && reviewedCode != null));
        findViewById(R.id.review).setOnClickListener(view -> review());
        findViewById(R.id.scan).setOnClickListener(view -> scan());
        findViewById(R.id.clear).setOnClickListener(view -> clear());
        continueButton.setOnClickListener(view -> handoff());
    }

    private void invalidateReview() {
        reviewedCode = null;
        confirmation.setChecked(false);
        continueButton.setEnabled(false);
        reviewCard.setVisibility(View.GONE);
        ((TextView) findViewById(R.id.server)).setText("");
    }

    private void review() {
        invalidateReview();
        try {
            ActivationCode code = ActivationCode.parse(input.getText().toString());
            if (code.needsConfirmationCode) {
                status.setText(R.string.confirmation_required);
                return;
            }
            reviewedCode = code;
            ((TextView) findViewById(R.id.server)).setText(code.server);
            status.setText("");
            reviewCard.setVisibility(View.VISIBLE);
            input.clearFocus();
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(input.getWindowToken(), 0);
            reviewCard.post(() -> {
                if (reviewedCode != null) {
                    ((android.widget.ScrollView) findViewById(R.id.root))
                            .smoothScrollTo(0, reviewCard.getTop());
                }
            });
        } catch (IllegalArgumentException exception) {
            status.setText(input.getText().toString().trim().isEmpty()
                    ? R.string.empty_code : R.string.invalid_code);
        }
    }

    private void scan() {
        invalidateReview();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            status.setText(R.string.camera_unavailable);
            return;
        }
        try {
            scanning = true;
            new IntentIntegrator(this)
                    .setCaptureActivity(ScanActivity.class)
                    .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                    .setBeepEnabled(false)
                    .setBarcodeImageEnabled(false)
                    .setOrientationLocked(false)
                    .setPrompt(getString(R.string.scan_prompt))
                    .initiateScan();
        } catch (ActivityNotFoundException | SecurityException exception) {
            scanning = false;
            status.setText(R.string.scan_failed);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            scanning = false;
            if (result.getContents() != null) {
                input.setText(result.getContents());
                review();
            }
        }
    }

    private void handoff() {
        if (reviewedCode == null || !confirmation.isChecked()) return;
        // Re-check at the moment of handoff. Never open an implicit eSIM intent.
        if (!WatchHandoff.isAvailable(getPackageManager())) {
            status.setText(R.string.missing_companion);
            return;
        }
        try {
            startActivity(WatchHandoff.createIntent(reviewedCode));
            clear();
            status.setText(R.string.handoff_sent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            status.setText(R.string.blocked_companion);
        }
    }

    private void clear() {
        input.setText("");
        invalidateReview();
        status.setText("");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!scanning) clear();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        // Deliberately do not persist the activation credential or reviewed state.
        super.onSaveInstanceState(state);
    }
}
