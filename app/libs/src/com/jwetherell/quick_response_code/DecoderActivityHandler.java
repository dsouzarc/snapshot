/*
 * Copyright (C) 2008 ZXing authors
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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Collection;

import com.jwetherell.quick_response_code.R;
import com.jwetherell.quick_response_code.camera.CameraManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DecoderActivityHandler extends android.os.Handler {

    private static final String TAG = com.jwetherell.quick_response_code.DecoderActivityHandler.class.getSimpleName();

    private final IDecoderActivity activity;
    private final DecodeThread decodeThread;
    private final CameraManager cameraManager;
    private com.jwetherell.quick_response_code.DecoderActivityHandler.State state;

    private enum State {
        PREVIEW, SUCCESS, DONE
    }

    DecoderActivityHandler(IDecoderActivity activity, java.util.Collection<com.google.zxing.BarcodeFormat> decodeFormats, String characterSet,
            CameraManager cameraManager) {
        this.activity = activity;
        decodeThread = new DecodeThread(activity, decodeFormats, characterSet, new ViewfinderResultPointCallback(
                activity.getViewfinder()));
        decodeThread.start();
        state = com.jwetherell.quick_response_code.DecoderActivityHandler.State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(android.os.Message message) {
        switch (message.what) {
            case R.id.auto_focus:
                // Log.d(TAG, "Got auto-focus message");
                // When one auto focus pass finishes, start another. This is the
                // closest thing to
                // continuous AF. It does seem to hunt a bit, but I'm not sure
                // what else to do.
                if (state == com.jwetherell.quick_response_code.DecoderActivityHandler.State.PREVIEW) cameraManager.requestAutoFocus(this, R.id.auto_focus);
                break;
            case R.id.restart_preview:
                android.util.Log.d(TAG, "Got restart preview message");
                restartPreviewAndDecode();
                break;
            case R.id.decode_succeeded:
                android.util.Log.d(TAG, "Got decode succeeded message");
                state = com.jwetherell.quick_response_code.DecoderActivityHandler.State.SUCCESS;
                android.os.Bundle bundle = message.getData();
                android.graphics.Bitmap barcode = bundle == null ? null : (android.graphics.Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
                activity.handleDecode((com.google.zxing.Result) message.obj, barcode);
                break;
            case R.id.decode_failed:
                // We're decoding as fast as possible, so when one decode fails,
                // start another.
                state = com.jwetherell.quick_response_code.DecoderActivityHandler.State.PREVIEW;
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
                break;
            case R.id.return_scan_result:
                android.util.Log.d(TAG, "Got return scan result message");
                if (activity instanceof android.app.Activity) {
                    ((android.app.Activity) activity).setResult(android.app.Activity.RESULT_OK, (android.content.Intent) message.obj);
                    ((android.app.Activity) activity).finish();
                } else {
                    android.util.Log.e(TAG, "Scan result message, activity is not Activity. Doing nothing.");
                }
                break;
        }
    }

    public void quitSynchronously() {
        state = com.jwetherell.quick_response_code.DecoderActivityHandler.State.DONE;
        cameraManager.stopPreview();
        android.os.Message quit = android.os.Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause()
            // will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    void restartPreviewAndDecode() {
        if (state == com.jwetherell.quick_response_code.DecoderActivityHandler.State.SUCCESS) {
            state = com.jwetherell.quick_response_code.DecoderActivityHandler.State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
            cameraManager.requestAutoFocus(this, R.id.auto_focus);
            activity.getViewfinder().drawViewfinder();
        }
    }
}
