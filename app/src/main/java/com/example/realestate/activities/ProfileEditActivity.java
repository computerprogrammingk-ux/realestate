package com.example.realestate.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityProfileEditBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {

    private ActivityProfileEditBinding binding;
    private static final String TAG = "PROFILE_EDIT_TAG";
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private String myUserype = "";

    private Uri imageUri = null;

    private String name = "";
    private String dob = "";
    private String email = "";
    private String phoneCode = "";
    private String phoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadMyinfo();

        binding.toolbarBackBtn.setOnClickListener(v -> finish());

        binding.profileImagePickFab.setOnClickListener(v -> imagePickDialog());

        binding.updateBtn.setOnClickListener(v -> validData());


    }

    /*** اختيار الكاميرا/المعرض ***/
    private void imagePickDialog() {
        PopupMenu popupMenu = new PopupMenu(this, binding.profileImagePickFab);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requetCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});
                } else {
                    requetCameraPermissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                }
            } else if (itemId == 2) {
                pickImageGallery();
            }
            return true;
        });
    }

    private final ActivityResultLauncher<String[]> requetCameraPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    (ActivityResultCallback<Map<String, Boolean>>) result -> {
                        boolean areAllGranted = true;
                        for (boolean isGranted : result.values()) {
                            areAllGranted = areAllGranted && isGranted;
                        }
                        if (areAllGranted) {
                            pickImageCamera();
                        } else {
                            MyUtils.toast(ProfileEditActivity.this, "Camera/Storage permission denied");
                        }
                    });

    private void pickImageCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "temp_image");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "temp_image_description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            try {
                                Glide.with(ProfileEditActivity.this)
                                        .load(imageUri)
                                        .placeholder(R.drawable.person_black)
                                        .into(binding.profileIv);
                            } catch (Exception e) {
                                Log.e(TAG, "camera result error", e);
                            }
                        } else {
                            MyUtils.toast(ProfileEditActivity.this, "Cancelled...");
                        }
                    });

    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            imageUri = result.getData().getData();
                            try {
                                Glide.with(ProfileEditActivity.this)
                                        .load(imageUri)
                                        .placeholder(R.drawable.person_black)
                                        .into(binding.profileIv);
                            } catch (Exception e) {
                                Log.e(TAG, "gallery result error", e);
                            }
                        } else {
                            MyUtils.toast(ProfileEditActivity.this, "Cancelled...!");
                        }
                    });

    /*** تحويل الصورة إلى Base64 (مخفّضة) ***/
    private String toBase64(Uri uri) {
        try {
            // 1) قراءة الأبعاد فقط
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream probe = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(probe, null, o);
            }
            // 2) تصغير حتى أقصى بُعد ~1024px
            int sample = 1, maxDim = Math.max(o.outWidth, o.outHeight);
            while (maxDim / sample > 1024) sample *= 2;

            // 3) فك فعلي بالصِغَر
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;

            Bitmap bmp;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(is, null, o);
            }

            // 4) ضغط JPEG جودة 70%
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();

            // 5) ترميز Base64 بدون أسطر
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "toBase64 error", e);
            return null;
        }
    }

    /*** جمع البيانات وحفظها ***/
    private void validData() {
        name = binding.nameEt.getText().toString().trim();
        dob = binding.dobEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        phoneCode = binding.countryCodePicker.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();

        if (imageUri == null) {
            // بدون صورة
            updateProfileDb(null);
        } else {
            progressDialog.setMessage("Encoding image...");
            progressDialog.show();
            String b64 = toBase64(imageUri);
            progressDialog.dismiss();
            if (b64 != null) {
                updateProfileDb(b64); // حفظ Base64
            } else {
                MyUtils.toast(this, "Failed to encode image");
            }
        }
    }

    /*** تحديث قاعدة البيانات (Realtime DB) ***/
    private void updateProfileDb(String imageBase64) {
        progressDialog.setMessage("Updating profile info...");
        progressDialog.show();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", name);
        hashMap.put("dob", dob);

        if (imageBase64 != null) {
            hashMap.put("profileImageBase64", imageBase64);
            // تنظيف أي URL قديم (اختياري)
            hashMap.put("profileImageUrl", null);
        }

        if (MyUtils.USER_TYPE_EMAIL.equals(myUserype) || MyUtils.USER_TYPE_GOOGLE.equals(myUserype)) {
            hashMap.put("phoneCode", phoneCode);
            hashMap.put("phoneNumber", phoneNumber);
        } else if (MyUtils.USER_TYPE_PHONE.equals(myUserype)) {
            hashMap.put("email", email);
        } else {
            if (email != null && !email.isEmpty()) hashMap.put("email", email);
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .updateChildren(hashMap)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "onSuccess: Info updated");
                    progressDialog.dismiss();
                    MyUtils.toast(ProfileEditActivity.this, "Profile updated...");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onFailure: ", e);
                    progressDialog.dismiss();
                    MyUtils.toast(ProfileEditActivity.this, "Failed to update due to " + e.getMessage());
                });
    }

    /*** تحميل بياناتي وعرض الصورة (Base64 أولاً ثم URL إن وجد) ***/
    private void loadMyinfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child("" + firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String dobSnap = String.valueOf(snapshot.child("dob").getValue());
                        String nameSnap = String.valueOf(snapshot.child("name").getValue());
                        // حدث المتغير العضوي، لا تعرّف محلي
                        email = String.valueOf(snapshot.child("email").getValue());
                        String phoneCodeSnap = String.valueOf(snapshot.child("phoneCode").getValue());
                        String phoneNumberSnap = String.valueOf(snapshot.child("phoneNumber").getValue());
                        String profileImageUrl = String.valueOf(snapshot.child("profileImageUrl").getValue());
                        String profileImageBase64 = String.valueOf(snapshot.child("profileImageBase64").getValue());
                        myUserype = String.valueOf(snapshot.child("userType").getValue());

                        // ضبط تمكين الحقول حسب نوع المستخدم
                        if (myUserype.equalsIgnoreCase(MyUtils.USER_TYPE_EMAIL) ||
                                myUserype.equalsIgnoreCase(MyUtils.USER_TYPE_GOOGLE)) {
                            binding.emailTitle.setEnabled(false);
                            binding.emailEt.setEnabled(false);
                        } else {
                            binding.phoneNumberTitle.setEnabled(false);
                            binding.phoneNumberEt.setEnabled(false);
                            binding.countryCodePicker.setEnabled(false);
                        }

                        // تعبئة الحقول
                        binding.emailEt.setText(email.equals("null") ? "" : email);
                        binding.dobEt.setText(dobSnap.equals("null") ? "" : dobSnap);
                        binding.nameEt.setText(nameSnap.equals("null") ? "" : nameSnap);
                        binding.fullNameTv.setText(nameSnap.equals("null") ? "" : nameSnap);
                        binding.phoneNumberEt.setText(phoneNumberSnap.equals("null") ? "" : phoneNumberSnap);
                        try {
                            if (phoneCodeSnap != null && !phoneCodeSnap.equals("null")) {
                                int phoneCodeInt = Integer.parseInt(phoneCodeSnap.replace("+", ""));
                                binding.countryCodePicker.setCountryForPhoneCode(phoneCodeInt);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "set phone code", e);
                        }

                        // عرض الصورة: Base64 أولاً، ثم URL، وإلا افتراضي
                        try {
                            if (profileImageBase64 != null && !profileImageBase64.equals("null") && !profileImageBase64.isEmpty()) {
                                String dataUrl = "data:image/jpeg;base64," + profileImageBase64;
                                Glide.with(ProfileEditActivity.this)
                                        .load(dataUrl)
                                        .placeholder(R.drawable.person_black)
                                        .into(binding.profileIv);
                            } else if (profileImageUrl != null && !profileImageUrl.equals("null") && !profileImageUrl.isEmpty()) {
                                Glide.with(ProfileEditActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.person_black)
                                        .into(binding.profileIv);
                            } else {
                                binding.profileIv.setImageResource(R.drawable.person_black);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "load image", e);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
    }
}
