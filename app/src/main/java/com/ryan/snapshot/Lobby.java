package com.ryan.snapshot;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import android.os.AsyncTask;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
public class Lobby extends ListActivity {

    ArrayList<String> listItems=new ArrayList<String>();
    ArrayAdapter<String> adapter;
    int clickCounter=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        setListAdapter(adapter);

        final Button buttonToLiveGame = (Button) findViewById(R.id.buttonLiveGame);
        buttonToLiveGame.setOnClickListener(liveGameListener);
    }

    private class GetOnlineGames extends AsyncTask<Void, Void, ArrayList<Game>> {
        @Override
        public ArrayList<Game> doInBackground(Void... params) {
            final ArrayList<Game> games = new ArrayList<Game>();

            final MobileServiceTable<Game> table = SnapShot_API.getClient().getTable(Game.class);
            table.orderBy("text", QueryOrder.Ascending).execute(
                    new TableQueryCallback<Game>() {
                        public void onCompleted(final List<Game> games,
                                                int count, Exception exception,
                                                ServiceFilterResponse response) {

                        }
                    });
            return games;
        }

    }

    public void addItems(View v) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.set_people_limit, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        final EditText input = (EditText) promptView.findViewById(R.id.userInput);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            if (!input.getText().toString().trim().isEmpty()) {
                                int num = Integer.parseInt(input.getText().toString());
                                listItems.add(input.getText().toString());
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getApplicationContext(), "Make sure you type a value!", Toast.LENGTH_LONG).show();
                            }
                        } catch (NumberFormatException e) {
                            // input is not a number
                            Toast.makeText(getApplicationContext(), "Make sure the value is an integer!", Toast.LENGTH_LONG).show();
                        }
                        //editTextMainScreen.setText(input.getText());
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();

        // listItems.add("Clicked : "+clickCounter++);
        // adapter.notifyDataSetChanged();
    }

    private final View.OnClickListener liveGameListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent toLiveGame = new Intent(Lobby.this, LiveGame.class);
            startActivity(toLiveGame);
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game_set_up, menu);
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
