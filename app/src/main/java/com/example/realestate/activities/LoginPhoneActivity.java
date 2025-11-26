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
import com.example.realestate.databinding.ActivityLoginPhoneBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
public class LoginPhoneActivity extends AppCompatActivity {

    private ActivityLoginPhoneBinding binding;
    private static final String TAG = "LOGIN_PHONE_TAG";

    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private String mVerificationId;
    private String phoneCode = "", phoneNumber = "", phoneNumberWithCode = "";

    private static final long OTP_TIMEOUT_SECONDS = 60L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.phoneInputRl.setVisibility(View.VISIBLE);
        binding.otoInputRl.setVisibility(View.GONE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();

        // استرجاع الحالة عند تدوير الشاشة
        if (savedInstanceState != null) {
            mVerificationId = savedInstanceState.getString("verificationId");
            phoneNumberWithCode = savedInstanceState.getString("phoneNumberWithCode");
        }

        // امنع تكرار تعريف الكولباك
        if (mCallbacks == null) {
            initPhoneLoginCallbacks();
        }

        binding.toolbarBackBtn.setOnClickListener(v -> finish());

        binding.sendOtpBtn.setOnClickListener(v -> {
            if (validateData()) {
                startPhoneNumberVerification();
            }
        });

        binding.resendOtpTv.setOnClickListener(v -> {
            if (forceResendingToken == null) {
                MyUtils.toast(this, "Request OTP first");
                return;
            }
            resendVerificationCode(forceResendingToken);
        });

        binding.verifyDtpBtn.setOnClickListener(v -> {
            String otp = binding.optEt.getText().toString().trim();
            if (otp.isEmpty()) {
                binding.optEt.setError("Enter OTP");
                binding.optEt.requestFocus();
            } else if (otp.length() != 6) {
                binding.optEt.setError("OTP must be 6 digits");
                binding.optEt.requestFocus();
            } else {
                verifyPhoneNumberWithCode(otp);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("verificationId", mVerificationId);
        outState.putString("phoneNumberWithCode", phoneNumberWithCode);
        super.onSaveInstanceState(outState);
    }

    /** فحص بيانات اليمن بشكل بسيط: 9 أرقام بعد +967 وتبدأ بـ 7 */
    private boolean validateData() {
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();
        phoneNumberWithCode = phoneCode + phoneNumber;

        Log.d(TAG, "validateData: phone Code: " + phoneCode);
        Log.d(TAG, "validateData: phone Number: " + phoneNumber);
        Log.d(TAG, "validateData: phone Number With Code: " + phoneNumberWithCode);

        if (phoneNumber.isEmpty()) {
            binding.phoneNumberEt.setError("Enter phone number");
            binding.phoneNumberEt.requestFocus();
            return false;
        }

        // فحص سريع لليمن (اختياري)
        if ("+967".equals(phoneCode)) {
            if (phoneNumber.length() != 9 || !phoneNumber.startsWith("7")) {
                binding.phoneNumberEt.setError("Yemen numbers are 9 digits and start with 7");
                binding.phoneNumberEt.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void setSendingUi(boolean sending) {
        binding.sendOtpBtn.setEnabled(!sending);
        binding.resendOtpTv.setEnabled(!sending);
        binding.verifyDtpBtn.setEnabled(!sending);
    }

    private void startPhoneNumberVerification() {
        progressDialog.setMessage("Sending OTP to " + phoneNumberWithCode);
        progressDialog.show();
        setSendingUi(true);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumberWithCode)
                .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneNumberWithCode(String otp) {
        if (mVerificationId == null) {
            MyUtils.toast(this, "Request OTP again");
            return;
        }
        Log.d(TAG, "verifyPhoneNumberWithCode: OTP: " + otp);
        progressDialog.setMessage("Verifying OTP...");
        progressDialog.show();
        setSendingUi(true);

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
        signInWithPhoneAuthCredential(credential);
    }

    private void initPhoneLoginCallbacks() {
        Log.d(TAG, "initPhoneLoginCallbacks");
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent");
                mVerificationId = verificationId;
                forceResendingToken = token;

                progressDialog.dismiss();
                binding.phoneInputRl.setVisibility(View.GONE);
                binding.otoInputRl.setVisibility(View.VISIBLE);

                MyUtils.toast(LoginPhoneActivity.this, "OTP sent to " + phoneNumberWithCode);
                binding.loginPhonelabel.setText("Please type verification code sent to " + phoneNumberWithCode);

                // امنع إعادة الإرسال لمدة 60 ثانية
                startResendCooldown();
                setSendingUi(false);
                Log.d(TAG, "onCodeSent, verificationId=" + verificationId);

            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted (auto/retrieval)");
                // على أجهزة بدون Play Services غالبًا لن يصل هنا، لكن نخليه
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "onVerificationFailed", e);
                progressDialog.dismiss();
                setSendingUi(false);
                MyUtils.toast(LoginPhoneActivity.this, "Failed: " + e.getMessage());
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        Log.d(TAG, "signInWithPhoneAuthCredential");
        progressDialog.setMessage("Logging in...");
        progressDialog.show();
        setSendingUi(true);

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "onSuccess");
                    if (authResult.getAdditionalUserInfo() != null
                            && authResult.getAdditionalUserInfo().isNewUser()) {
                        Log.d(TAG, "New User → save profile");
                        updateUserInfo();
                    } else {
                        Log.d(TAG, "Existing User → go Home");
                        progressDialog.dismiss();
                        setSendingUi(false);
                        startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "signInWithCredential onFailure", e);
                    progressDialog.dismiss();
                    setSendingUi(false);
                    MyUtils.toast(LoginPhoneActivity.this, "Failed to verify: " + e.getMessage());
                });
    }

    private void resendVerificationCode(PhoneAuthProvider.ForceResendingToken token) {
        if (phoneNumberWithCode == null || phoneNumberWithCode.isEmpty()) {
            MyUtils.toast(this, "Enter phone number first");
            return;
        }

        progressDialog.setMessage("Resending OTP to " + phoneNumberWithCode);
        progressDialog.show();
        setSendingUi(true);

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumberWithCode)
                .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks);

        if (token != null) builder.setForceResendingToken(token);

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private void startResendCooldown() {
        // تعطيل زر إعادة الإرسال لمدة 60 ثانية (UI فقط)
        binding.resendOtpTv.setEnabled(false);
        binding.resendOtpTv.postDelayed(() -> binding.resendOtpTv.setEnabled(true), OTP_TIMEOUT_SECONDS * 1000);
    }

    private void updateUserInfo() {
        Log.d(TAG, "updateUserInfo");
        progressDialog.setMessage("Saving user info...");
        progressDialog.show();

        long timestamp = MyUtils.timestamp();
        String uid = firebaseAuth.getUid();

        HashMap<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", "");
        map.put("name", "");
        map.put("timestamp", timestamp);
        map.put("phoneCode", "" + phoneCode);
        map.put("phoneNumber", "" + phoneNumber);
        map.put("profileImageUrl", "");
        map.put("dob", "");
        map.put("userType", "" + MyUtils.USER_TYPE_PHONE);
        map.put("token", "");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(uid)
                .setValue(map)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "User info saved");
                    progressDialog.dismiss();
                    startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateUserInfo onFailure", e);
                    progressDialog.dismiss();
                    MyUtils.toast(LoginPhoneActivity.this, "Failed to save: " + e.getMessage());
                });
    }
}
