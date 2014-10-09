package com.example.dereksalama.firequote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;


public class Quotes extends PlusBaseActivity {

    Firebase firebase;

    private static final String TAG = "Quotes";
    public static final String FIRE_URL = "https://firequote.firebaseio.com";

    @Override
    protected void onPlusClientSignOut() {
        super.onPlusClientSignOut();
        firebase.unauth();
        signOut();
        startLoginActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quotes);
        Firebase.setAndroidContext(getApplicationContext());

        firebase = new Firebase(FIRE_URL);
        firebase.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData == null) {
                    startLoginActivity();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthData authData = firebase.getAuth();
        if (authData == null) {
            Log.v(TAG, "No user, starting login");
            startLoginActivity();
        } else {
            TextView welcomeTextView = (TextView) findViewById(R.id.welcome_text);
            if (authData.getProviderData().containsKey("displayName")) {
                welcomeTextView.setText((String) authData.getProviderData().get("displayName"));
            } else if (authData.getProviderData().containsKey("email")) {
                welcomeTextView.setText((String) authData.getProviderData().get("email"));
            }
        }
    }

    private void startLoginActivity() {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.quotes, menu);
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
        } else if (id == R.id.action_logout) {
            AuthData authData = firebase.getAuth();
            if (authData.getProvider().equals("google")) {
                // inherited google sign out method
                signOut();
            }
            firebase.unauth();
        }
        return super.onOptionsItemSelected(item);
    }
}