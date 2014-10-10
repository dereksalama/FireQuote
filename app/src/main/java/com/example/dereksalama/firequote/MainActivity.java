package com.example.dereksalama.firequote;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LoginFragment.OnLoginClickedListener,
        QuotesListFragment.OnFragmentInteractionListener {

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_GOOGLE_LOGIN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* Track whether the sign-in button has been clicked so that we know to resolve
 * all issues preventing sign-in without waiting.
 */
    private boolean mGoogleLoginClicked;

    /* Store the connection result from onConnectionFailed callbacks so that we can
     * resolve them when the user clicks sign-in.
     */
    private ConnectionResult mGoogleConnectionResult;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mGoogleIntentInProgress;

    private static final String TAG = MainActivity.class.getSimpleName();

    Firebase ref;

    static final String FIRE_URL = "https://firequote.firebaseio.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        ref = new Firebase(FIRE_URL);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();


        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            if (ref.getAuth() == null)
                getFragmentManager().beginTransaction().add(R.id.container,
                        LoginFragment.newInstance()).commit();
            else
                showQuotes();
        }
    }

    @Override
    protected  void onResume() {
        super.onResume();
        if (ref.getAuth() != null) {
            showQuotes();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        if (id == R.id.action_logout) {
            AuthData authData = ref.getAuth();
            if (authData != null) {
                ref.unauth();
                if (authData.getProvider().equals("google")) {
                /* Logout from Google+ */
                    if (mGoogleApiClient.isConnected()) {
                        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                        mGoogleApiClient.disconnect();
                    }
                }
            }
            getFragmentManager().beginTransaction().replace(R.id.container,
                    LoginFragment.newInstance()).commit();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mGoogleLoginClicked = false;
        getGoogleOAuthTokenAndLogin();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        if (mGoogleConnectionResult.hasResolution()) {
            try {
                mGoogleIntentInProgress = true;
                startIntentSenderForResult(mGoogleConnectionResult.getResolution().getIntentSender(),
                        RC_GOOGLE_LOGIN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (!mGoogleIntentInProgress) {
            // Store the ConnectionResult so that we can use it later when the user clicks
            // 'sign-in'.
            mGoogleConnectionResult = result;

            if (mGoogleLoginClicked) {
                // The user has already clicked 'sign-in' so we attempt to resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }
    }

    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_GOOGLE_LOGIN) {
            if (responseCode != RESULT_OK) {
                mGoogleLoginClicked = false;
            }
            mGoogleIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onLoginClicked(LoginFragment.LoginProvider provider, String email, String password) {
        switch (provider) {
            case GOOGLE:
                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleLoginClicked = true;
                    if (mGoogleConnectionResult != null) {
                        resolveSignInError();
                    } else if (mGoogleApiClient.isConnected()) {
                        getGoogleOAuthTokenAndLogin();
                    } else {
                        mGoogleApiClient.connect();
                    }
                }
            case PASSWORD:
                ref.authWithPassword(email, password, new AuthResultHandler("password"));
        }
    }

    @Override
    public void onFragmentInteraction(String id) {

    }

    private void getGoogleOAuthTokenAndLogin() {

        /* Get OAuth token in Background */
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            String errorMessage = null;
            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    String scope = String.format("oauth2:%s", Scopes.PLUS_LOGIN);
                    token = GoogleAuthUtil.getToken(MainActivity.this, Plus.AccountApi.getAccountName(mGoogleApiClient), scope);
                } catch (IOException transientEx) {
                    /* Network or server error */
                    Log.e(TAG, "Error authenticating with Google: " + transientEx);
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                    Log.w(TAG, "Recoverable Google OAuth error: " + e.toString());
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
                    if (!mGoogleIntentInProgress) {
                        mGoogleIntentInProgress = true;
                        Intent recover = e.getIntent();
                        startActivityForResult(recover, RC_GOOGLE_LOGIN);
                    }
                } catch (GoogleAuthException authEx) {
                    /* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. */
                    Log.e(TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }
                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                mGoogleLoginClicked = false;
                if (token != null) {
                    /* Successfully got OAuth token, now login with Google */
                    ref.authWithOAuthToken("google", token, new AuthResultHandler("google"));
                    showQuotes();
                } else if (errorMessage != null) {
                    Log.e(TAG, errorMessage);
                } else {
                    showQuotes();

                }
            }
        };
        task.execute();
    }

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {

        private final String provider;

        public AuthResultHandler(String provider) {
            this.provider = provider;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            Log.i(TAG, provider + " auth successful");
            setAuthenticatedUser(authData);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.e(TAG, firebaseError.getMessage());
            switch (firebaseError.getCode()) {
                case FirebaseError.USER_DOES_NOT_EXIST:
                    //ref.createUser();
                    break;
                case FirebaseError.INVALID_PASSWORD:
                    Toast.makeText(getApplicationContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private class AuthPasswordResultHandler extends AuthResultHandler implements Firebase.AuthResultHandler {
        private final String email;
        private final String password;

        public AuthPasswordResultHandler(String email, String password) {
            super("password");
            this.email = email;
            this.password = password;
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.e(TAG, firebaseError.getMessage());
            switch (firebaseError.getCode()) {
                case FirebaseError.USER_DOES_NOT_EXIST:
                    //ref.createUser(email, password, new AuthPasswordResultHandler(email, password));
                    break;
                case FirebaseError.INVALID_PASSWORD:
                    Toast.makeText(getApplicationContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void setAuthenticatedUser(AuthData authData) {
        Log.v(TAG, "setting user " + authData.toString());

        Map<String, String> map = new HashMap<String, String>();
        map.put("provider", authData.getProvider());
        if(authData.getProviderData().containsKey("id")) {
            map.put("provider_id", authData.getProviderData().get("id").toString());
        }
        if(authData.getProviderData().containsKey("displayName")) {
            map.put("displayName", authData.getProviderData().get("displayName").toString());
        }
        if(authData.getProviderData().containsKey("email") &&
                authData.getProviderData().get("email") != null) {
            map.put("email", authData.getProviderData().get("email").toString());
        }
        ref.child("users").child(authData.getUid()).setValue(map);


        showQuotes();
    }

    private void showQuotes() {
        QuotesListFragment fragment = QuotesListFragment.newInstance("one", "two");
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        Log.v(TAG, "showing quotes");
    }
}
