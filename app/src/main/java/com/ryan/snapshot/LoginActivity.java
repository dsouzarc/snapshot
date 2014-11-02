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
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
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
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;

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

public class LoginActivity extends Activity {
    private MobileServiceClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_login);

        // start Facebook Login
        try {
            mClient = new MobileServiceClient(
                    "https://snapshot.azure-mobile.net/",
                    "gzWFegbXiTLVoLkHtqvDKPzctugOGH61",
                    this
            );

            final Item item = new Item();
            item.Text = "Awesome item";
            mClient.getTable(Item.class).insert(item, new TableOperationCallback<Item>() {
                public void onCompleted(Item entity, Exception exception, ServiceFilterResponse response) {
                    if (exception == null) {
                        makeToast("SUCCESS");
                        // Insert succeeded
                    } else {
                        makeToast("FAILED: " + exception.toString());
                        // Insert failed
                    }
                }
            });

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
                                makeToast(user.getName());
                                log(user.getName());
                            }
                            else {
                                log("ERROR");
                            }
                        }
                    }).executeAsync();
                }
            }
        });
        final Button login = (Button) findViewById(R.id.loginButton);
        login.setOnClickListener(loginListener);
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
        final Request request = new Request(session, "/me/picture", bundle,
                HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                final GraphObject graphObject = response.getGraphObject();
                if(graphObject != null) {
                    try {
                        final JSONObject jsonObject = graphObject.getInnerJSONObject();
                        final JSONObject obj = jsonObject.getJSONObject("picture").getJSONObject("data");
                        final String url = obj.getString("url");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                final Bitmap bitmap = BitmapFactory.decodeStream(HttpRequest(url));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((ImageView) findViewById(R.id.profilePicture)).setImageBitmap(bitmap);
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
        HttpResponse response = null;
        try {
            final DefaultHttpClient httpClient = new DefaultHttpClient();
            final HttpGet request = new HttpGet();
            request.setURI(new URI(strUrl));
            response = httpClient.execute(request);
            final HttpEntity entity = response.getEntity();
            return entity.getContent();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login_screen, menu);
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
