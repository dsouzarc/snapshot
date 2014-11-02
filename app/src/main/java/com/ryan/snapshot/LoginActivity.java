package com.ryan.snapshot;

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

public class LoginActivity extends Activity {

    private MobileServiceClient mClient;
    private Context theC;
    private SharedPreferences thePrefs;
    private SharedPreferences.Editor theEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_login);

        theC = getApplicationContext();
        thePrefs = theC.getSharedPreferences("com.ryan.snapshot", Context.MODE_PRIVATE);
        theEditor = thePrefs.edit();

        try {
            SnapShot_API.theActivity = LoginActivity.this;
            final SnapShot_API theAPI = SnapShot_API.getApi();
            this.mClient = SnapShot_API.getClient();

            final ListenableFuture<MobileServiceUser> mLogin =
                    mClient.login(MobileServiceAuthenticationProvider.Facebook);
            Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                @Override
                public void onFailure(Throwable exc) {
                    makeToast("ERROR: " + exc.getMessage());
                    log("Error logging in: " + exc.getMessage());
                }
                @Override
                public void onSuccess(MobileServiceUser user) {
                    makeToast("You are now logged in" + user.getUserId());
                }
            });
        }
        catch(Exception e) {
            makeToast(e.toString());
        }

        Session.openActiveSession(this, true, new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (session.isOpened()) {
                    executeMeRequest(session);
                    Request.newMeRequest(session, new Request.GraphUserCallback() {
                        @Override
                        public void onCompleted(GraphUser user, Response response) {
                            if (user != null) {
                                ((TextView) findViewById(R.id.loggedInTV)).setText("Logged in as " +
                                        user.getName());
                                final String fbID = user.getId();
                                final User me = new User();
                                me.facebookid = fbID;
                                updateSettings(Constants.TAG_FACEBOOKID, fbID);
                                makeToast("FB ID: " + fbID);

                                final MobileServiceTable<User> table = mClient.getTable(User.class);
                                table.where().field("facebookid").eq(fbID).
                                        execute(new TableQueryCallback<User>() {
                                            public void onCompleted(final List<User> result, int count,
                                                                    Exception exception, ServiceFilterResponse response) {
                                                if (exception == null) {
                                                    if (result.size() == 0) {
                                                        makeToast("Not in DB");
                                                        mClient.getTable(User.class).insert(me, new TableOperationCallback<User>() {
                                                            public void onCompleted(User entity, Exception exception, ServiceFilterResponse response) {
                                                                if (exception == null) {
                                                                    makeToast("Successfully inserted");
                                                                } else {
                                                                    makeToast("Unsuccessful insertion");
                                                                }
                                                                updateSettings(Constants.TAG_ID, entity.id);
                                                            }
                                                        });
                                                        makeToast("In DB Now");
                                                    }
                                                    else {
                                                        makeToast("Already in DB");

                                                        for(User user : result) {
                                                            if(user.facebookid.endsWith(me.facebookid)) {
                                                                updateSettings(Constants.TAG_ID, user.id);
                                                            }
                                                        }
                                                    }
                                                }
                                                else {
                                                    makeToast("Went wrong: " + exception.getMessage());
                                                }
                                            }
                                        });
                            }
                        }
                    }).executeAsync();
                }
            }
        });
        final Button login = (Button) findViewById(R.id.loginButton);
        login.setOnClickListener(loginListener);
    }

    private void updateSettings(final String tag, final String value) {
        this.theEditor.putString(tag, value);
        this.theEditor.commit();
    }

    private final View.OnClickListener loginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent toLobby = new Intent(LoginActivity.this, Lobby.class);
            startActivity(toLobby);
        }
    };

    public void executeMeRequest(Session session) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean("redirect", false);
        bundle.putString("height", "600");
        bundle.putString("type", "normal");
        bundle.putString("width", "600");
        bundle.putString("fields", "picture");

        final Request request = new Request(session, "/me/picture", bundle, HttpMethod.GET,
                new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                final GraphObject graphObject = response.getGraphObject();

                if(graphObject != null) {
                    try {
                        final JSONObject jsonObject = graphObject.getInnerJSONObject();
                        final JSONObject obj =
                                jsonObject.getJSONObject("picture").getJSONObject("data");
                        final String url = obj.getString("url");

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final Bitmap bitmap = BitmapFactory.decodeStream(HttpRequest(url));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((ImageView) findViewById(R.id.profilePicture))
                                                .setImageBitmap(bitmap);
                                    }
                                });
                            }
                        }).start();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Request.executeBatchAsync(request);
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

    private void log(final String log) {
        Log.e("com.ryan.snapshot", log);
    }

    private void makeToast(final String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login_screen, menu);
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
