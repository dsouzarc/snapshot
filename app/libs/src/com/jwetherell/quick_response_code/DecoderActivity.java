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

import java.io.IOException;
import java.util.Collection;

import com.jwetherell.quick_response_code.R;
import com.jwetherell.quick_response_code.camera.CameraManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Example Decoder Activity.
 * 
 * @author Justin Wetherell (phishman3579@gmail.com)
 */
public class DecoderActivity extends android.app.Activity implements IDecoderActivity, android.view.SurfaceHolder.Callback {

    private static final String TAG = com.jwetherell.quick_response_code.DecoderActivity.class.getSimpleName();

    protected DecoderActivityHandler handler = null;
    protected ViewfinderView viewfinderView = null;
    protected CameraManager cameraManager = null;
    protected boolean hasSurface = false;
    protected java.util.Collection<com.google.zxing.BarcodeFormat> decodeFormats = null;
    protected String characterSet = null;

    @Override
    public void onCreate(android.os.Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.decoder);
        android.util.Log.v(TAG, "onCreate()");

        android.view.Window window = getWindow();
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        handler = null;
        hasSurface = false;
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

        // CameraManager must be initialized here, not in onCreate().
        if (cameraManager == null) cameraManager = new CameraManager(getApplication());

        if (viewfinderView == null) {
            viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
            viewfinderView.setCameraManager(cameraManager);
        }

        showScanner();

        android.view.SurfaceView surfaceView = (android.view.SurfaceView) findViewById(R.id.preview_view);
        android.view.SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(android.view.SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.v(TAG, "onPause()");

        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }

        cameraManager.closeDriver();

        if (!hasSurface) {
            android.view.SurfaceView surfaceView = (android.view.SurfaceView) findViewById(R.id.preview_view);
            android.view.SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_FOCUS || keyCode == android.view.KeyEvent.KEYCODE_CAMERA) {
            // Handle these events so they don't launch the Camera app
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void surfaceCreated(android.view.SurfaceHolder holder) {
        if (holder == null)
            android.util.Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(android.view.SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {
        // Ignore
    }

    @Override
    public ViewfinderView getViewfinder() {
        return viewfinderView;
    }

    @Override
    public android.os.Handler getHandler() {
        return handler;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void handleDecode(com.google.zxing.Result rawResult, android.graphics.Bitmap barcode) {
        drawResultPoints(barcode, rawResult);
    }

    protected void drawResultPoints(android.graphics.Bitmap barcode, com.google.zxing.Result rawResult) {
        com.google.zxing.ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            android.graphics.Canvas canvas = new android.graphics.Canvas(barcode);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(getResources().getColor(R.color.result_image_border));
            paint.setStrokeWidth(3.0f);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            android.graphics.Rect border = new android.graphics.Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
            canvas.drawRect(border, paint);

            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1]);
            } else if (points.length == 4 && (rawResult.getBarcodeFormat() == com.google.zxing.BarcodeFormat.UPC_A || rawResult.getBarcodeFormat() == com.google.zxing.BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and
                // metadata
                drawLine(canvas, paint, points[0], points[1]);
                drawLine(canvas, paint, points[2], points[3]);
            } else {
                paint.setStrokeWidth(10.0f);
                for (com.google.zxing.ResultPoint point : points) {
                    canvas.drawPoint(point.getX(), point.getY(), paint);
                }
            }
        }
    }

    protected static void drawLine(android.graphics.Canvas canvas, android.graphics.Paint paint, com.google.zxing.ResultPoint a, com.google.zxing.ResultPoint b) {
        canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
    }

    protected void showScanner() {
        viewfinderView.setVisibility(android.view.View.VISIBLE);
    }

    protected void initCamera(android.view.SurfaceHolder surfaceHolder) {
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) handler = new DecoderActivityHandler(this, decodeFormats, characterSet, cameraManager);
        } catch (java.io.IOException ioe) {
            android.util.Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            android.util.Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }
}
