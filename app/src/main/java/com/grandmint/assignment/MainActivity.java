package com.grandmint.assignment;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    public ProgressDialog mProgressDialog;
    private FirebaseAuth firebaseAuth;
    private EditText mEmail, mName, mPassword, mPasswordConf;
    private Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        mName = findViewById(R.id.nameBox);
        mEmail = findViewById(R.id.emailBox);
        mPassword = findViewById(R.id.passBox);
        mPasswordConf = findViewById(R.id.confBox);
        submit = findViewById(R.id.submit);

        final FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String password = preferences.getString("pass", "");
            if (!TextUtils.isEmpty(password)) {
                //Sequentially sign and check if email is verified
                signIn(user.getEmail(), password);
            }
        }

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    createUserAccount(mEmail.getText().toString(), mPasswordConf.getText().toString());
                }
            }
        });
    }

    /*Check if all the details are valid before submission*/
    private boolean validateForm() {
        boolean isValid = true;

        String email = mEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmail.setError("Email is required");
            isValid = false;
        } else {
            mEmail.setError(null);
        }

        String name = mName.getText().toString();
        if (TextUtils.isEmpty(name)) {
            mName.setError("Password is required");
            isValid = false;
        } else {
            mName.setError(null);
        }

        String password = mPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPassword.setError("Password is required");
            isValid = false;
        } else {
            mPassword.setError(null);
        }

        String confPassword = mPasswordConf.getText().toString();
        if (TextUtils.isEmpty(confPassword)) {
            mPasswordConf.setError("Password is required");
            isValid = false;
        } else {
            mPasswordConf.setError(null);
        }

        if (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(password) && !password.equals(confPassword)) {
            mPasswordConf.setError("Passwords don't match");
            isValid = false;
        } else if (confPassword.length() < 8) {
            mPasswordConf.setError("Password should be greater than 7 characters.");
            isValid = false;
        } else {
            //Save password for future reference, since no login option is provided
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            preferences.edit()
                    .putString("pass", confPassword)
                    .apply();
            mPasswordConf.setError(null);
        }

        return isValid;
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /*Create user account if
    * all details are valid
    * and send verification email*/
    private void createUserAccount(String email, String password) {
        Log.d(TAG, "createUserAccount:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressDialog();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            firebaseAuth = FirebaseAuth.getInstance();
                            if(user!=null) {
                                String displayName= mName.getText().toString();
                                UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build();
                                user.updateProfile(profile)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            sendEmailVerification();
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(MainActivity.this, "Account creation failed.",
                                        Toast.LENGTH_SHORT).show();
                                hideProgressDialog();
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Account creation failed.",
                                    Toast.LENGTH_SHORT).show();
                            hideProgressDialog();
                        }
                    }
                });
    }

    /*Sign in if user has already
    * been created*/
    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);

        showProgressDialog();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            TextView helloText = findViewById(R.id.hello);

                            if (user!=null && user.isEmailVerified()) {
                                findViewById(R.id.verifLayout).setVisibility(View.GONE);
                                helloText.setVisibility(View.VISIBLE);
                                helloText.setText(String.format("Hello %s", user.getDisplayName()));
                            } else {
                                findViewById(R.id.verifLayout).setVisibility(View.GONE);
                                helloText.setVisibility(View.VISIBLE);
                                helloText.setText(R.string.verif_req);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        if (!task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        hideProgressDialog();
                    }
                });
    }

    private void signOut() {
        firebaseAuth.signOut();
    }

    private void sendEmailVerification() {
        submit.setEnabled(false);

        final FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this,
                                        "Verification email sent to " + user.getEmail(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e(TAG, "sendEmailVerification", task.getException());
                                Toast.makeText(MainActivity.this,
                                        "Failed to send verification email.",
                                        Toast.LENGTH_SHORT).show();
                                submit.setEnabled(true);
                            }

                            hideProgressDialog();
                        }
                    });
        }
    }
}
