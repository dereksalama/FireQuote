package com.example.dereksalama.firequote;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class LoginFragment extends Fragment {

    public enum LoginProvider { GOOGLE, FACEBOOK, PASSWORD };


    private static final String TAG = LoginFragment.class.getSimpleName();
    private OnLoginClickedListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LoginFragment.
     */
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }
    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_login, container, false);


      layout.findViewById(R.id.google_sign_in_button).setOnClickListener(
              new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onLoginClicked(LoginProvider.GOOGLE, null, null);
            }
        });

        layout.findViewById(R.id.password_sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String email = ((EditText) view.findViewById(R.id.email_input)).getText().toString();
                        String password = ((EditText) view.findViewById(R.id.password_input)).getText().toString();

                        if (email == null || password == null) {
                            Log.e(TAG, "null login fields");

                        }
                        mListener.onLoginClicked(LoginProvider.PASSWORD, email, password);
                    }
                }
        );

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLoginClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        Log.v(TAG, "activity attached");

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnLoginClickedListener {
        public void onLoginClicked(LoginProvider provider, String email, String password);
    }

}
