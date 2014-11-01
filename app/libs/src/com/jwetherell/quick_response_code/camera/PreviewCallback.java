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

package com.jwetherell.quick_response_code.camera;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

final class PreviewCallback implements android.hardware.Camera.PreviewCallback {

    private static final String TAG = com.jwetherell.quick_response_code.camera.PreviewCallback.class.getSimpleName();

    private final CameraConfigurationManager configManager;
    private android.os.Handler previewHandler;
    private int previewMessage;

    PreviewCallback(CameraConfigurationManager configManager) {
        this.configManager = configManager;
    }

    void setHandler(android.os.Handler previewHandler, int previewMessage) {
        this.previewHandler = previewHandler;
        this.previewMessage = previewMessage;
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
        android.graphics.Point cameraResolution = configManager.getCameraResolution();
        android.os.Handler thePreviewHandler = previewHandler;
        if (thePreviewHandler != null) {
            android.os.Message message = thePreviewHandler.obtainMessage(previewMessage, cameraResolution.x, cameraResolution.y, data);
            message.sendToTarget();
            previewHandler = null;
        } else {
            android.util.Log.d(TAG, "Got preview callback, but no handler for it");
        }
    }

}
