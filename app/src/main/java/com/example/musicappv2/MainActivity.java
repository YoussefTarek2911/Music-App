package com.example.musicappv2;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    private CardView googleBtn;
    FirebaseAuth mAuth;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent =new Intent(getApplicationContext(), DisplaySongs.class);
            startActivity(intent);
            //finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText userName = findViewById(R.id.userNameEditText);
        EditText password = findViewById(R.id.passwordEditText);
        Button button = findViewById(R.id.signInButton);

        Button forget = findViewById(R.id.forgotPasswordButton);
        forget.setOnClickListener(v ->
        {
            Intent intent2 = new Intent(getApplicationContext(),ForgotPasswordActivity.class);
            startActivity(intent2);

        });

        googleBtn = findViewById(R.id.googleSignIn);
        mAuth=FirebaseAuth.getInstance();
        ProgressBar progressBar =findViewById(R.id.progress_bar);
        TextView textView =findViewById(R.id.regNow);
        googlesetup();
        textView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(),Reg.class);
            startActivity(intent);
            //finish();
        });
        button.setOnClickListener(v -> {
            String user = userName.getText().toString();
            String pass =password.getText().toString();
            if(user.isEmpty())
            {
                Toast.makeText(MainActivity.this,"Enter User",Toast.LENGTH_SHORT).show();
                return;
            }
            if(pass.isEmpty())
            {
                Toast.makeText(MainActivity.this,"Enter password",Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.signInWithEmailAndPassword(user, pass)
                    .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "Authentication sucssful.",
                                        Toast.LENGTH_SHORT).show();
                                Intent intent =new Intent(getApplicationContext(), DisplaySongs.class);
                                startActivity(intent);
                                //finish();
                            } else {

                                Toast.makeText(MainActivity.this, "Invalid Password",
                                        Toast.LENGTH_SHORT).show();

                            }
                        }
                    });


        });}

    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                task.getResult(ApiException.class);
                navigateToSecondActivity();
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }

    }
    private void googlesetup()
    {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if(acct!=null){
            navigateToSecondActivity();
        }


        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }
    void signIn(){
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent,1000);
    }
    void navigateToSecondActivity(){
        finish();
        Intent intent = new Intent(MainActivity.this, DisplaySongs.class);
        startActivity(intent);
    }
}