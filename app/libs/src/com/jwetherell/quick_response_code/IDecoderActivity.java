package com.jwetherell.quick_response_code;

import android.graphics.Bitmap;
import android.os.Handler;

import com.jwetherell.quick_response_code.camera.CameraManager;
import com.google.zxing.Result;

public interface IDecoderActivity {

    public ViewfinderView getViewfinder();

    public android.os.Handler getHandler();

    public CameraManager getCameraManager();

    public void handleDecode(com.google.zxing.Result rawResult, android.graphics.Bitmap barcode);
}
