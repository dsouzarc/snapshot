package com.ryan.snapshot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.pkmmte.view.CircularImageView;
import com.ryan.snapshot.QRCode_Reader.IntentIntegrator;
import com.ryan.snapshot.QRCode_Reader.IntentResult;

public class LiveGame extends Activity {
    private static final int PROFILE_PHOTO_SIZE = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game);

        final CircularImageView target = (CircularImageView) findViewById(R.id.target_photoIV);
        target.setImageBitmap(generateCrossHair(getResizedBitmap(250, 250, R.drawable.sample_profile_photo)));

        target.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(LiveGame.this);
                integrator.initiateScan();
            }
        });
    }

    public String getString(final Bitmap qrCode) {
        try {
            final int width = qrCode.getWidth(), height = qrCode.getHeight();
            final int[] pixels = new int[width * height];
            qrCode.getPixels(pixels, 0, width, 0, 0, width, height);
            qrCode.recycle();
            final RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            final BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            final MultiFormatReader reader = new MultiFormatReader();

            try {
                final Result result = reader.decode(bBitmap);
                return result.getText();
            }
            catch (Exception e) {
                log(e.toString());
                return e.toString();
            }
        }
        catch (Exception e) {
            log(e.toString());
            return e.toString();
        }
    }

    public Bitmap getResizedBitmap(int targetW, int targetH,  final int resID) {
        final BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), resID, bmOptions);

        final int photoW = bmOptions.outWidth;
        final int photoH = bmOptions.outHeight;
        final int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        return BitmapFactory.decodeResource(getResources(), resID, bmOptions);
    }

    private Bitmap generateCrossHair(final Bitmap profilePhoto) {
        final Bitmap bmOverlay = Bitmap.createBitmap(profilePhoto.getWidth(),
                profilePhoto.getHeight(), profilePhoto.getConfig());
        final Paint paint = new Paint();
        paint.setStrokeWidth(8f);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        final Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(profilePhoto, new Matrix(), null);
        canvas.drawLine(0, profilePhoto.getHeight()/2, profilePhoto.getWidth(),
                profilePhoto.getHeight()/2, paint);
        canvas.drawLine(profilePhoto.getWidth()/2, 0, profilePhoto.getWidth()/2,
                profilePhoto.getHeight(), paint);
        return bmOverlay;
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
