/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jwetherell.quick_response_code;

import java.text.DateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.jwetherell.quick_response_code.R;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.jwetherell.quick_response_code.result.ResultHandler;
import com.jwetherell.quick_response_code.result.ResultHandlerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Example Capture Activity.
 * 
 * @author Justin Wetherell (phishman3579@gmail.com)
 */
public class CaptureActivity extends DecoderActivity {

    private static final String TAG = com.jwetherell.quick_response_code.CaptureActivity.class.getSimpleName();
    private static final java.util.Set<com.google.zxing.ResultMetadataType> DISPLAYABLE_METADATA_TYPES = java.util.EnumSet.of(com.google.zxing.ResultMetadataType.ISSUE_NUMBER, com.google.zxing.ResultMetadataType.SUGGESTED_PRICE,
            com.google.zxing.ResultMetadataType.ERROR_CORRECTION_LEVEL, com.google.zxing.ResultMetadataType.POSSIBLE_COUNTRY);

    private android.widget.TextView statusView = null;
    private android.view.View resultView = null;
    private boolean inScanMode = false;

    @Override
    public void onCreate(android.os.Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.capture);
        android.util.Log.v(TAG, "onCreate()");

        resultView = findViewById(R.id.result_view);
        statusView = (android.widget.TextView) findViewById(R.id.status_view);

        inScanMode = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.v(TAG, "onDestroy()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.v(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.v(TAG, "onPause()");
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            if (inScanMode)
                finish();
            else
                onResume();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void handleDecode(com.google.zxing.Result rawResult, android.graphics.Bitmap barcode) {
        drawResultPoints(barcode, rawResult);

        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
        handleDecodeInternally(rawResult, resultHandler, barcode);
    }

    protected void showScanner() {
        inScanMode = true;
        resultView.setVisibility(android.view.View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(android.view.View.VISIBLE);
        viewfinderView.setVisibility(android.view.View.VISIBLE);
    }

    protected void showResults() {
        inScanMode = false;
        statusView.setVisibility(android.view.View.GONE);
        viewfinderView.setVisibility(android.view.View.GONE);
        resultView.setVisibility(android.view.View.VISIBLE);
    }

    // Put up our own UI for how to handle the decodBarcodeFormated contents.
    private void handleDecodeInternally(com.google.zxing.Result rawResult, ResultHandler resultHandler, android.graphics.Bitmap barcode) {
        onPause();
        showResults();

        android.widget.ImageView barcodeImageView = (android.widget.ImageView) findViewById(R.id.barcode_image_view);
        if (barcode == null) {
            barcodeImageView.setImageBitmap(android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.icon));
        } else {
            barcodeImageView.setImageBitmap(barcode);
        }

        android.widget.TextView formatTextView = (android.widget.TextView) findViewById(R.id.format_text_view);
        formatTextView.setText(rawResult.getBarcodeFormat().toString());

        android.widget.TextView typeTextView = (android.widget.TextView) findViewById(R.id.type_text_view);
        typeTextView.setText(resultHandler.getType().toString());

        java.text.DateFormat formatter = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT);
        String formattedTime = formatter.format(new java.util.Date(rawResult.getTimestamp()));
        android.widget.TextView timeTextView = (android.widget.TextView) findViewById(R.id.time_text_view);
        timeTextView.setText(formattedTime);

        android.widget.TextView metaTextView = (android.widget.TextView) findViewById(R.id.meta_text_view);
        android.view.View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
        metaTextView.setVisibility(android.view.View.GONE);
        metaTextViewLabel.setVisibility(android.view.View.GONE);
        java.util.Map<com.google.zxing.ResultMetadataType, Object> metadata = rawResult.getResultMetadata();
        if (metadata != null) {
            StringBuilder metadataText = new StringBuilder(20);
            for (java.util.Map.Entry<com.google.zxing.ResultMetadataType, Object> entry : metadata.entrySet()) {
                if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
                    metadataText.append(entry.getValue()).append('\n');
                }
            }
            if (metadataText.length() > 0) {
                metadataText.setLength(metadataText.length() - 1);
                metaTextView.setText(metadataText);
                metaTextView.setVisibility(android.view.View.VISIBLE);
                metaTextViewLabel.setVisibility(android.view.View.VISIBLE);
            }
        }

        android.widget.TextView contentsTextView = (android.widget.TextView) findViewById(R.id.contents_text_view);
        CharSequence displayContents = resultHandler.getDisplayContents();
        contentsTextView.setText(displayContents);
        // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
        int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
        contentsTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, scaledSize);
    }
}
