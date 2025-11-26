package com.example.realestate.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityDeleteAccountBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DeleteAccountActivity extends AppCompatActivity {


    private ActivityDeleteAccountBinding binding;
    private static final String TAG = "DELETE_ACCOUNT_TAG";
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this);
        binding = ActivityDeleteAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                deleteUserData();
            }
        });


    }

    private void deleteUserData() {
        Log.d(TAG, "deleteUserData: ");
        progressDialog.setMessage("Deleting user data");
        progressDialog.show();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: User data deleted... ");
                        deleteUserProperties();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "deleteUserData:onFailure ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(DeleteAccountActivity.this, "Failed to delete user due to" + e.getMessage());

                    }
                });


    }

    private void deleteUserProperties() {
        Log.d(TAG, "deleteUserProperties: Deleting user properties...");
        progressDialog.setMessage("Deleting user poperties");
        progressDialog.show();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Log.d(TAG, "onDataChange: No properties by this user");
                            deleteAccount();
                            return;

                        }
                        final int total = (int) snapshot.getChildrenCount();
                        final int[] deletedcount = {0};

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            deletedcount[0]++;
                                            Log.d(TAG, "onSuccess: Property deleted: " + deletedcount[0] + "/" + total);

                                            if (deletedcount[0] == total) {
                                                Log.d(TAG, "onSuccess: All user properties deleted");
                                                deleteAccount();

                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "onFailure:Failed to delete property ", e);

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


    }

    private void deleteAccount(){
        Log.d(TAG,"deleteAccount: Deleting user account...");
        progressDialog.setMessage("Deleting user account");
        progressDialog.show();

        firebaseUser.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"onSuccess:Deleted user account");
                        progressDialog.dismiss();
                        startMainActivity();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                     Log.e(TAG,"onFailure: ",e);
                     progressDialog.dismiss();
                     MyUtils.toast(DeleteAccountActivity.this,"Failed to delete account du to "+e.getMessage());
                 startMainActivity();
                    }
                });

    }

    private void startMainActivity(){
        firebaseAuth.signOut();
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startMainActivity();
    }
}