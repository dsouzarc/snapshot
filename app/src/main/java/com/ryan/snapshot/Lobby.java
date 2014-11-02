package com.ryan.snapshot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.table.TableQueryCallback;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

import java.util.LinkedList;
import java.util.List;

public class Lobby extends Activity {

    private LinearLayout theLayout;
    private Context theC;
    private boolean isInGame;
    private String myID;
    private final LinkedList<Game> allGames = new LinkedList<Game>();
    private MobileServiceClient mClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        this.theC = getApplicationContext();
        this.theLayout = (LinearLayout) findViewById(R.id.theLayout);
        this.myID = getApplicationContext().getSharedPreferences("com.ryan.snapshot",
                Context.MODE_PRIVATE).getString(Constants.TAG_ID, "");


        try {
            this.mClient = new MobileServiceClient("https://snapshot.azure-mobile.net/",
                    "gzWFegbXiTLVoLkHtqvDKPzctugOGH61", Lobby.this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        new GetOnlineGames().execute();
    }

    private class GetOnlineGames extends AsyncTask<Void, Void, Void> {
        @Override
        public Void doInBackground(Void... params) {
            try {
                final MobileServiceTable<Game> table = mClient.getTable(Game.class);
                table.orderBy("text", QueryOrder.Ascending).execute(
                        new TableQueryCallback<Game>() {
                            public void onCompleted(final List<Game> games,
                                                    int count, Exception exception,
                                                    ServiceFilterResponse response) {
                                allGames.addAll(games);
                            }
                        });
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onPostExecute(Void param) {
            if(allGames.size() == 0) {
                final Game game = new Game();
                game.id = "696969696";
                allGames.add(game);
            }

            for(Game game : allGames) {
                if(myID.length() > 2 && game.id.equals(myID)) {
                    isInGame = true;
                }
                theLayout.addView(getView(game));
            }
        }
    }

    private View getView(final Game game) {
        final TextView theView = new TextView(theC);
        theView.setTextColor(Color.BLACK);
        theView.setTextSize(20);
        theView.setText("Join game: " + game.id);

        theView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent viewGame = new Intent(Lobby.this, GameDetails.class);
                viewGame.putExtra("id", game.id);
                startActivity(viewGame);
            }
        });
        return theView;
    }

    private void log(final String text) {
        Log.e("com.ryan.snapshot", text);
    }

    private void makeToast(final String text) {
        Toast.makeText(theC, text, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game_set_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_game:
                if(isInGame) {
                    makeToast("Sorry, you're already in a game");
                    return true;
                }
                if(!isInGame) {
                    makeToast("Making new game....");
                    final Game newGame = new Game();
                    newGame.id = myID;
                    mClient.getTable(Game.class).insert(newGame, new TableOperationCallback<Game>() {
                        public void onCompleted(Game entity, Exception exception, ServiceFilterResponse response) {
                            if (exception == null) {
                                makeToast("Successfully created new game");
                                final Intent viewGame = new Intent(Lobby.this, LiveGame.class);
                                viewGame.putExtra("id", newGame.id);
                                startActivity(viewGame);
                            } else {
                                makeToast("Game was not created " + exception.getMessage());
                            }
                        }
                    });
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
