package com.ryan.snapshot;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import java.util.Random;
import android.view.MenuItem;
import android.view.Gravity;
import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout.LayoutParams;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Arrays;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import java.util.LinkedList;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import java.net.URISyntaxException;
import java.util.List;
import android.view.View;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.table.TableQueryCallback;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

public class GameDetails extends Activity {

    private MobileServiceClient mClient;
    private String gameID;
    private String myID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_details);

        this.gameID = getIntent().getExtras().getString("id");
        this.myID = getApplicationContext().getSharedPreferences("com.ryan.snapshot",
                Context.MODE_PRIVATE).getString(Constants.TAG_ID, "");
        try {
            this.mClient = new MobileServiceClient("https://snapshot.azure-mobile.net/",
                    "gzWFegbXiTLVoLkHtqvDKPzctugOGH61", GameDetails.this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        final MobileServiceTable<Game> table = mClient.getTable(Game.class);
        table.orderBy("text", QueryOrder.Ascending).execute(new TableQueryCallback<Game>() {
            public void onCompleted(final List<Game> games, int count,
                                    Exception exception, ServiceFilterResponse response) {
                        for(Game game : games) {
                            if (game.id.equals(gameID)) {
                                //Get some info about the game. Good stuff
                            }
                        }
                    }
                }
        );

        final Random gen = new Random();

        final int numOfPlayers = this.gameID.equals(this.myID) ? 1 : gen.nextInt(9) + 1;
        final int highScore = gen.nextInt(100) * 10;

        ((TextView) findViewById(R.id.numberOfPlayers)).setText("Game Size: " + numOfPlayers + "/10");
        ((TextView) findViewById(R.id.highScore)).setText("Current High Score: " + highScore);
        ((TextView) findViewById(R.id.gameDuration)).setText(gen.nextInt(15) + " minutes elapsed");

        final String[] names = {"Logan", "Karen", "O(N)", "Yuan", "K-Starrr", "Tom", "Attilla", "Victoria", "RAC_Kill",
                                "The Dark Lord", "Ana_Killer"};
        final LinkedList<String> legit = new LinkedList<String>(Arrays.asList(names));

        final LinearLayout layout = (LinearLayout) findViewById(R.id.playersLL);
        final LinkedList<String> chosen = new LinkedList<String>();
        for(int i = 0; i < numOfPlayers; i++) {
            final LinearLayout scoreInfo = new LinearLayout(getApplicationContext());
            scoreInfo.setOrientation(LinearLayout.HORIZONTAL);
            scoreInfo.setWeightSum(1);
            final TextView v = new TextView(getApplicationContext());
            v.setGravity(Gravity.LEFT);
            v.setTextColor(Color.BLACK);
            v.setTextSize(24);
            v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,0.5f));
            final String name = legit.get(i);
            v.setText(name);
            scoreInfo.addView(v);
            chosen.add(name);

            final TextView s = new TextView(getApplicationContext());
            s.setTextColor(Color.RED);
            s.setGravity(Gravity.RIGHT);
            s.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,0.5f));
            s.setTextSize(24);
            s.setText(String.valueOf(gen.nextInt(highScore)));
            scoreInfo.addView(s);

            scoreInfo.setPadding(0, 0, 0, 56);

            layout.addView(scoreInfo);
        }

        final Button findAssasin = (Button) findViewById(R.id.findAssasin);
        findAssasin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent toGame = new Intent(GameDetails.this, LiveGame.class);
                toGame.putExtra("names", chosen.toArray(new String[chosen.size()]));
                toGame.putExtra("id", gameID);
                startActivity(toGame);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
