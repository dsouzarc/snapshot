package com.ryan.snapshot;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.Result;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;

import com.ryan.snapshot.camera.IntentIntegrator;
import android.widget.Button;
import android.widget.Toast;
import com.ryan.snapshot.camera.IntentResult;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
public class LiveGame extends Activity {

    public static final String TAG = "com.ryan.snapshot";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game);

        final Button scanCode = (Button) findViewById(R.id.buttonScanQRCode);
        scanCode.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                IntentIntegrator integrator = new IntentIntegrator(LiveGame.this);
                integrator.initiateScan();
            }
        });
        makeToast("Starting");
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), com.ryan.snapshot.R.drawable.qr_code_test);

        try
        {
            makeToast("Here");
            if (bitmap == null) {
                Log.e(TAG, "uri is not a bitmap,");
            }
            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.recycle();
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();
            makeToast("Down");
            try
            {
                makeToast("Try");
                Result result = reader.decode(bBitmap);
                makeToast("Success");
                Toast.makeText(getApplicationContext(), result.getText(), Toast.LENGTH_LONG).show();
                makeToast("W???");
            }
            catch (Exception e)
            {
                makeToast(e.toString());
                Log.e(TAG, "decode exception", e);
            }
        }
        catch (Exception e)
        {
            makeToast("Down: " + e.toString());
            Log.e(TAG, "can not open file", e);
        }
    }

    private void makeToast(final String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {

            // handle scan result
            final String contantsString = scanResult.getContents()==null?"0":scanResult.getContents();
            if (contantsString.equalsIgnoreCase("0")) {
                Toast.makeText(this, "Problem to get the  contant Number", Toast.LENGTH_LONG).show();

            }else {
                Toast.makeText(this, contantsString, Toast.LENGTH_LONG).show();

            }
        }
        else{
            Toast.makeText(this, "Problem to secan the barcode.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.live_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
