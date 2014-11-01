package com.ryan.snapshot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.ryan.snapshot.camera.IntentIntegrator;
import com.ryan.snapshot.camera.IntentResult;

public class LiveGame extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game);

        final Button scanCode = (Button) findViewById(R.id.buttonScanQRCode);
        scanCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                IntentIntegrator integrator = new IntentIntegrator(LiveGame.this);
                integrator.initiateScan();
            }
        });

        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.qr_code_test);

        try {
            final int width = bitmap.getWidth(), height = bitmap.getHeight();
            final int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.recycle();

            final RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            final BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            final MultiFormatReader reader = new MultiFormatReader();

            try {
                final Result result = reader.decode(bBitmap);
                log(result.getText());
            }
            catch (Exception e) {
                log(e.toString());
            }
        }
        catch (Exception e) {
            log(e.toString());
        }
    }

    private void makeToast(final String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    private void log(final String text) {
        Log.e("com.ryan.snapshot", text);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            final String contantsString = scanResult.getContents()==null?"0":scanResult.getContents();
            if (contantsString.equalsIgnoreCase("0")) {
                makeToast("No text gotten");
            }
            else {
                makeToast(contantsString);
            }
        }
        else {
            makeToast("Scanning problem");
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
