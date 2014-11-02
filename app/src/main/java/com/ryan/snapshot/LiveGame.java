package com.ryan.snapshot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.table.TableQueryCallback;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
import java.util.Random;


public class LiveGame extends Activity {
    private static final int PROFILE_PHOTO_SIZE = 500;
    private static final Random generator = new Random();

    private final String[] ids = {"10152344936108836", "10152120255581423", "549744230",
            "1326593998", "1428857830", "10204140890910254", "100005948885022", "100000083906146",
            "594899401", "629564354", "1013566223", "1063650160"};

    private CircularImageView target;
    private String[] names;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game);

        this.names = getIntent().getExtras().getStringArray("names");
        final int random = generator.nextInt(names.length);
        this.name = names[random];
        this.target = (CircularImageView) findViewById(R.id.target_photoIV);
        this.target.setImageBitmap(generateCrossHair(getResizedBitmap(getBitmapFromAsset("P" + (random + 1) + ".png"), 500, 500)));

        final Bundle bundle = new Bundle();
        bundle.putBoolean("redirect", false);
        bundle.putString("height", "600");
        bundle.putString("type", "normal");
        bundle.putString("width", "600");
        //.putString("fields", "picture");
        bundle.putString("fields", "id, name, picture");
        final Request request = new Request(Session.getActiveSession(), "/" + ids[generator.nextInt(ids.length)] +
                "/picture", bundle, HttpMethod.GET,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        log("WE DID IT");
                        final GraphObject graphObject = response.getGraphObject();

                        if(graphObject != null) {
                            log("NOPE");
                            try {
                                final JSONObject jsonObject = graphObject.getInnerJSONObject();
                                final JSONObject obj =
                                        jsonObject.getJSONObject("picture").getJSONObject("data");
                                final String url = obj.getString("url");
                                log("HERE");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Bitmap bitmap = BitmapFactory.decodeStream(HttpRequest(url));
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                log("YOLO");
                                                target.setImageBitmap(generateCrossHair(bitmap));
                                            }
                                        });
                                    }
                                }).start();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                log(e.toString());
                            }
                        }
                    }
                });
        target.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(LiveGame.this);
                integrator.initiateScan();
            }
        });

        ((TextView) findViewById(R.id.targetName)).setText(name);
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

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
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


    public static InputStream HttpRequest(final String strUrl) {
        try {
            final DefaultHttpClient httpClient = new DefaultHttpClient();
            final HttpGet request = new HttpGet();
            request.setURI(new URI(strUrl));
            final HttpResponse response = httpClient.execute(request);
            final HttpEntity entity = response.getEntity();
            return entity.getContent();
        }
        catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {

            final String contantsString = scanResult.getContents() == null ? "0" : scanResult.getContents();
            log(String.valueOf(name.hashCode()) + name);
            log("other: " + contantsString);
            if (contantsString.equalsIgnoreCase("0")) {
                Toast.makeText(this, "No Assasin Code Detected", Toast.LENGTH_LONG).show();
            }
            else {
                if(Integer.parseInt(contantsString.replace(" ", "")) == name.hashCode()) {
                    Toast.makeText(this, "Target Assasinated. +200 points. Finding new target...", Toast.LENGTH_LONG).show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(2000);
                                final Intent restart = new Intent(LiveGame.this, LiveGame.class);
                                restart.putExtra("names", names);
                                startActivity(restart);
                            }
                            catch (Exception e) {
                                e.getMessage();
                            }
                        }
                    }).start();
                }
                else {
                    Toast.makeText(this, "Sorry, wrong target", Toast.LENGTH_LONG).show();
                }
            }
        }
        else {
            Toast.makeText(this, "No Assasin Code Detected", Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap getBitmapFromAsset(String strName) {
        final AssetManager assetManager = getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(strName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        return bitmap;
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
