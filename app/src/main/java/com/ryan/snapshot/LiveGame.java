package com.ryan.snapshot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Color;
import android.view.Menu;
import android.graphics.Paint;
import android.view.MenuItem;
import android.graphics.Canvas;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Matrix;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import android.widget.ImageView;

public class LiveGame extends Activity {
    private static final int PROFILE_PHOTO_SIZE = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game);

        final ImageView target = (ImageView) findViewById(R.id.target_photoIV);
        target.setImageBitmap(generateCrossHair(BitmapFactory.decodeResource(getResources(), R.drawable.sample_profile_photo)));

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

    private Bitmap generateCrossHair(final Bitmap profile) {
        final Bitmap profilePhoto = Bitmap.createScaledBitmap(profile, PROFILE_PHOTO_SIZE,
                PROFILE_PHOTO_SIZE, false);
        final Bitmap bmOverlay = Bitmap.createBitmap(profilePhoto.getWidth(),
                profilePhoto.getHeight(), profilePhoto.getConfig());
        final Paint paint = new Paint();
        paint.setStrokeWidth(6f);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        final Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(profilePhoto, new Matrix(), null);
        canvas.drawLine(0, profilePhoto.getHeight()/2, profilePhoto.getWidth(), profilePhoto.getHeight()/2, paint);
        canvas.drawLine(profilePhoto.getWidth()/2, 0, profilePhoto.getWidth()/2, profilePhoto.getHeight(), paint);
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
