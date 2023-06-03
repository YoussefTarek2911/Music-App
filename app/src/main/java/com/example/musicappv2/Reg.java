package com.example.musicappv2;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class Reg extends AppCompatActivity {
    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);
        EditText userName = findViewById(R.id.userReg);
        EditText password = findViewById(R.id.passreg);
        Button button = findViewById(R.id.Regbutton);
        mAuth=FirebaseAuth.getInstance();
        ProgressBar progressBar=findViewById(R.id.progress_bar);

        button.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String userreg = userName.getText().toString();
            String passreg =password.getText().toString();


            if(userreg.isEmpty())
            {
                Toast.makeText(Reg.this,"Enter User",Toast.LENGTH_SHORT).show();
                return;
            }
            if(passreg.isEmpty())
            {
                Toast.makeText(Reg.this,"Enter password",Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(userreg, passreg)
                    .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {

                                Toast.makeText(Reg.this, "Authentication created.", Toast.LENGTH_SHORT).show();
                                createNewFolder();
                                goToNextActivity();

                            } else {

                                Toast.makeText(Reg.this, "You have an account go to login", Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
        });
    }
    private void createNewFolder() {
        String userEmail = mAuth.getCurrentUser().getEmail();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference userFolderRef = storageRef.child("Songs/" + userEmail + "/");
        StorageReference folderMarkerRef = userFolderRef.child(".folderMarker");

        // Create an empty file as a marker for the folder
        byte[] dummyData = new byte[]{};
        folderMarkerRef.putBytes(dummyData);
    }
    private void goToNextActivity()
    {
        startActivity(new Intent(this, DisplaySongs.class));
        finish();
    }
}