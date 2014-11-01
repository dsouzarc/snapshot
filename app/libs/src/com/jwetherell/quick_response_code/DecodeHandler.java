/*
 * Copyright (C) 2010 ZXing authors
 * 
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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.Map;

import com.jwetherell.quick_response_code.R;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

final class DecodeHandler extends android.os.Handler {

    private static final String TAG = com.jwetherell.quick_response_code.DecodeHandler.class.getSimpleName();

    private final IDecoderActivity activity;
    private final com.google.zxing.MultiFormatReader multiFormatReader;
    private boolean running = true;

    DecodeHandler(IDecoderActivity activity, java.util.Map<com.google.zxing.DecodeHintType, Object> hints) {
        multiFormatReader = new com.google.zxing.MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.activity = activity;
    }

    @Override
    public void handleMessage(android.os.Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
        case R.id.decode:
            decode((byte[]) message.obj, message.arg1, message.arg2);
            break;
        case R.id.quit:
            running = false;
            android.os.Looper.myLooper().quit();
            break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     * 
     * @param data
     *            The YUV preview frame.
     * @param width
     *            The width of the preview frame.
     * @param height
     *            The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        com.google.zxing.Result rawResult = null;
        PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
        if (source != null) {
            com.google.zxing.BinaryBitmap bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (com.google.zxing.ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }

        android.os.Handler handler = activity.getHandler();
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            long end = System.currentTimeMillis();
            android.util.Log.d(TAG, "Found barcode in " + (end - start) + " ms");
            if (handler != null) {
                android.os.Message message = android.os.Message.obtain(handler, R.id.decode_succeeded, rawResult);
                android.os.Bundle bundle = new android.os.Bundle();
                bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                android.os.Message message = android.os.Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
    }

}
