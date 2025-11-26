package com.example.realestate.activities;

import android.app.ProgressDialog;
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
import com.example.realestate.databinding.ActivityChangePasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private static final String TAG="CHANGE_PASWORD_TAG";

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        binding=ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth=FirebaseAuth.getInstance();
        firebaseUser=firebaseAuth.getCurrentUser();
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });


    }

    private String currentPassword="";
    private String newPassword="";
    private String confirmNewPassword="";
    private void validateData(){
        Log.d(TAG,"validateData: ");
        currentPassword=binding.currentpasswordEt.getText().toString().trim();
        confirmNewPassword=binding.confirmNewpasswordEt.getText().toString().trim();
        newPassword=binding.newpasswordEt.getText().toString().trim();

        if(currentPassword.isEmpty()){
            binding.currentpasswordEt.setError("Enter curent password");
            binding.currentpasswordEt.requestFocus();
        } else if (newPassword.isEmpty()) {
            binding.newpasswordEt.setError("Enter new password");
            binding.newpasswordEt.requestFocus();

        } else if (confirmNewPassword.isEmpty()) {
            binding.confirmNewpasswordEt.setError("Enter confirm password");
            binding.confirmNewpasswordEt.requestFocus();
        } else if (!newPassword.equals(confirmNewPassword)) {
            binding.confirmNewpasswordEt.setError("Passwor doesn't match");
            binding.confirmNewpasswordEt.requestFocus();

        }
        else {
            authenticateUserForUpdatePassword();
        }


    }

    private void authenticateUserForUpdatePassword(){
        Log.d(TAG,"authenticateUserForUpdatePassword: ");

        progressDialog.setMessage("Authenticating User");
        progressDialog.show();

        AuthCredential authCredential= EmailAuthProvider.getCredential(firebaseUser.getEmail(),currentPassword);
        firebaseUser.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"onSuccess: Authenticating sucess ");
                            updatePassword();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"onFailure: ",e);
                        progressDialog.dismiss();
                        MyUtils.toast(ChangePasswordActivity.this,"Faild to authenticate due to "+ e.getMessage());


                    }
                });

    }

    private  void updatePassword(){
        Log.d(TAG,"updatePassword ");
        progressDialog.setMessage("Updating password");
        progressDialog.show();
        firebaseUser.updatePassword(newPassword)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"onSuccess: Password updated");
                        progressDialog.dismiss();
                        MyUtils.toast(ChangePasswordActivity.this,"Password updated...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"onFailure: ",e);
                        progressDialog.dismiss();
                        MyUtils.toast(ChangePasswordActivity.this,"Failed to update due to "+e.getMessage());

                    }
                });

    }
}