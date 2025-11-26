package com.example.realestate.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityRegisterEmailBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterEmailActivity extends AppCompatActivity {

    private ActivityRegisterEmailBinding binding;


    private static final String TAG="REGISTER_EMAIL_TAG";

    FirebaseAuth firebaseAuth;

    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        binding=ActivityRegisterEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


          firebaseAuth=FirebaseAuth.getInstance();

          progressDialog=new ProgressDialog(this);
          progressDialog.setTitle("Please wait");
          progressDialog.setCanceledOnTouchOutside(false);

          binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  finish();
              }
          });

          binding.haveAcountTv.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  finish();
              }
          });

          binding.registerBtn.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  validateData();
              }
          });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private String email,password,cpassword;
    private  void validateData(){
        email=binding.emailEt.getText().toString().trim();
        password=binding.passwordEt.getText().toString();
        cpassword=binding.cPasswordEt.getText().toString();

        Log.d(TAG,"validateData: Email:"+email);
        Log.d(TAG,"validateData: password:"+password);
        Log.d(TAG,"validateData: Confimr Password:"+cpassword);

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
              binding.emailEt.setError("Invalid Email Pattern");
              binding.emailEt.requestFocus();
        }
        else if(password.isEmpty()){
            binding.passwordEt.setError("Invalid password Pattern");
            binding.passwordEt.requestFocus();
        } else if (!password.equals(cpassword)) {
            binding.cPasswordEt.setError("Password doesn't match");
            binding.cPasswordEt.requestFocus();

        }
        else{
            registerUser();
        }

    }

    private void registerUser(){
        progressDialog.setMessage("Creating Acount");
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.e(TAG,"oonSuccess: Rigister success ");

                        updateUserInfo();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"onFailure: ",e);
                        MyUtils.toast(RegisterEmailActivity.this,"Failed due to "+e.getMessage());
                    }
                });


    }

    private void updateUserInfo(){

        progressDialog.setTitle("Saving User info...");

        long timestemp=MyUtils.timestamp();
        String registeredUserEmail=firebaseAuth.getCurrentUser().getEmail();
        String registeredUserUId=firebaseAuth.getUid();
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("uid",registeredUserUId);
        hashMap.put("email",registeredUserEmail);
        hashMap.put("name","");
        hashMap.put("timestemp",timestemp);
        hashMap.put("phoneCode","");
        hashMap.put("phoneNumber","");
        hashMap.put("profileImageUrl","");

        hashMap.put("dob","");
        hashMap.put("userType",MyUtils.USER_TYPE_EMAIL);
        hashMap.put("token","");

        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registeredUserUId)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.e(TAG,"onSuccess: Info save... ");
                        startActivity(new Intent(RegisterEmailActivity.this, MainActivity.class));
                        finishAffinity();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"onFailure: ",e);
                        MyUtils.toast(RegisterEmailActivity.this,"Failed to save due to "+e.getMessage());

                    }
                });

    }
}