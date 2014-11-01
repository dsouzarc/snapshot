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

import java.util.Collection;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.jwetherell.quick_response_code.data.Preferences;

/**
 * A class which deals with reading, parsing, and setting the camera parameters
 * which are used to configure the camera hardware.
 */
public final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";
    private static final int MIN_PREVIEW_PIXELS = 320 * 240; // small screen
    private static final int MAX_PREVIEW_PIXELS = 800 * 480; // large/HD screen

    private final android.content.Context context;
    private android.graphics.Point screenResolution;
    private android.graphics.Point cameraResolution;

    public CameraConfigurationManager(android.content.Context context) {
        this.context = context;
    }

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    void initFromCameraParameters(android.hardware.Camera camera) {
        android.hardware.Camera.Parameters parameters = camera.getParameters();
        android.view.WindowManager manager = (android.view.WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
        android.view.Display display = manager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        // We're landscape-only, and have apparently seen issues with display
        // thinking it's portrait
        // when waking from sleep. If it's not landscape, assume it's mistaken
        // and reverse them:
        if (width < height) {
            android.util.Log.i(TAG, "Display reports portrait orientation; assuming this is incorrect");
            int temp = width;
            width = height;
            height = temp;
        }
        screenResolution = new android.graphics.Point(width, height);
        android.util.Log.i(TAG, "Screen resolution: " + screenResolution);
        cameraResolution = findBestPreviewSizeValue(parameters, screenResolution, false);
        android.util.Log.i(TAG, "Camera resolution: " + cameraResolution);
    }

    void setDesiredCameraParameters(android.hardware.Camera camera) {
        android.hardware.Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            android.util.Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        initializeTorch(parameters);
        String focusMode = findSettableValue(parameters.getSupportedFocusModes(), android.hardware.Camera.Parameters.FOCUS_MODE_AUTO, android.hardware.Camera.Parameters.FOCUS_MODE_MACRO);
        if (focusMode != null) {
            parameters.setFocusMode(focusMode);
        }

        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        camera.setParameters(parameters);
    }

    public android.graphics.Point getCameraResolution() {
        return cameraResolution;
    }

    public android.graphics.Point getScreenResolution() {
        return screenResolution;
    }

    void setTorch(android.hardware.Camera camera, boolean newSetting) {
        android.hardware.Camera.Parameters parameters = camera.getParameters();
        doSetTorch(parameters, newSetting);
        camera.setParameters(parameters);
    }

    private static void initializeTorch(android.hardware.Camera.Parameters parameters) {
        doSetTorch(parameters, Preferences.KEY_FRONT_LIGHT);
    }

    private static void doSetTorch(android.hardware.Camera.Parameters parameters, boolean newSetting) {
        String flashMode;
        if (newSetting) {
            flashMode = findSettableValue(parameters.getSupportedFlashModes(), android.hardware.Camera.Parameters.FLASH_MODE_TORCH, android.hardware.Camera.Parameters.FLASH_MODE_ON);
        } else {
            flashMode = findSettableValue(parameters.getSupportedFlashModes(), android.hardware.Camera.Parameters.FLASH_MODE_OFF);
        }
        if (flashMode != null) {
            parameters.setFlashMode(flashMode);
        }
    }

    private static android.graphics.Point findBestPreviewSizeValue(android.hardware.Camera.Parameters parameters, android.graphics.Point screenResolution, boolean portrait) {
        android.graphics.Point bestSize = null;
        int diff = Integer.MAX_VALUE;
        for (android.hardware.Camera.Size supportedPreviewSize : parameters.getSupportedPreviewSizes()) {
            int pixels = supportedPreviewSize.height * supportedPreviewSize.width;
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
                continue;
            }
            int supportedWidth = portrait ? supportedPreviewSize.height : supportedPreviewSize.width;
            int supportedHeight = portrait ? supportedPreviewSize.width : supportedPreviewSize.height;
            int newDiff = Math.abs(screenResolution.x * supportedHeight - supportedWidth * screenResolution.y);
            if (newDiff == 0) {
                bestSize = new android.graphics.Point(supportedWidth, supportedHeight);
                break;
            }
            if (newDiff < diff) {
                bestSize = new android.graphics.Point(supportedWidth, supportedHeight);
                diff = newDiff;
            }
        }
        if (bestSize == null) {
            android.hardware.Camera.Size defaultSize = parameters.getPreviewSize();
            bestSize = new android.graphics.Point(defaultSize.width, defaultSize.height);
        }
        return bestSize;
    }

    private static String findSettableValue(java.util.Collection<String> supportedValues, String... desiredValues) {
        android.util.Log.i(TAG, "Supported values: " + supportedValues);
        String result = null;
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }
        android.util.Log.i(TAG, "Settable value: " + result);
        return result;
    }

}
