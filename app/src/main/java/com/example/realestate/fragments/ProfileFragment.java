package com.example.realestate.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.activities.ChangePasswordActivity;
import com.example.realestate.activities.DeleteAccountActivity;
import com.example.realestate.activities.MainActivity;
import com.example.realestate.activities.MyPropertyListActivity;
import com.example.realestate.activities.PostAddActivity;
import com.example.realestate.activities.ProfileEditActivity;
import com.example.realestate.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private static final String TAG="PROFILE_TAG";

    private Context mContext;
    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    public void onAttach(@NonNull Context context) {
        mContext=context;
        super.onAttach(context);
    }

    public ProfileFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding= FragmentProfileBinding.inflate(inflater,container,false);
        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
         progressDialog=new ProgressDialog(mContext);
         progressDialog.setTitle("Please wait");
         progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth=FirebaseAuth.getInstance();
        loadMyInfo();
        binding.postAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent=new Intent(mContext, PostAddActivity.class);
                startActivity(intent);
            }
        });
        binding.LogputCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                startActivity(new Intent(mContext, MainActivity.class));
                getActivity().finishAffinity();
            }
        });
        binding.myProfileCV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, MyPropertyListActivity.class));
            }
        });
        binding.editProfileCV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, ProfileEditActivity.class));
            }
        });

        binding.changePasswordCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, ChangePasswordActivity.class));
            }
        });

        binding.deletAcountCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, DeleteAccountActivity.class));
            }
        });


    }
    private void loadMyInfo() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child("" + firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String dob = "" + snapshot.child("dob").getValue();
                        String email = "" + snapshot.child("email").getValue();
                        String name = "" + snapshot.child("name").getValue();
                        String phoneCode = "" + snapshot.child("phoneCode").getValue();
                        String phoneNumber = "" + snapshot.child("phoneNumber").getValue();
                        String profileImageBase64 = "" + snapshot.child("profileImageBase64").getValue(); // التأكد من أن قاعدة البيانات تحتوي على الصورة بتنسيق Base64
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        String userType = "" + snapshot.child("userType").getValue();
                        String phone = phoneCode + phoneNumber;

                        // تحويل Base64 إلى Bitmap
                        try {
                            if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                                byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                // تعيين الصورة إلى ImageView
                                binding.profileIv.setImageBitmap(decodedByte);
                            } else {
                                binding.profileIv.setImageResource(R.drawable.person_black); // صورة افتراضية إذا كانت الصورة فارغة أو غير موجودة
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error decoding Base64 image", e);
                            binding.profileIv.setImageResource(R.drawable.person_black); // صورة افتراضية في حالة حدوث خطأ
                        }

                        if (timestamp.equals("null")) {
                            timestamp = "0";
                        }

                        String formattedDate = MyUtils.formatTimestampDate(Long.parseLong(timestamp));

                        binding.emailTv.setText(email);
                        binding.fullNameTv.setText(name);
                        binding.dobTv.setText(dob);
                        binding.phoneTv.setText(phone);
                        binding.membersinceTv.setText(formattedDate);

                        // التحقق من نوع المستخدم وإظهار الحالة
                        if (userType.equals(MyUtils.USER_TYPE_EMAIL)) {
                            boolean isVerified = firebaseAuth.getCurrentUser().isEmailVerified();
                            if (isVerified) {
                                binding.verifyAcountCv.setVisibility(View.GONE);
                                binding.verificationTv.setText("Verified");
                            } else {
                                binding.verifyAcountCv.setVisibility(View.VISIBLE);
                                binding.verificationTv.setText("Not Verified");
                            }
                        } else {
                            binding.verifyAcountCv.setVisibility(View.GONE);
                            binding.verificationTv.setText("Verified");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading data: " + error.getMessage());
                    }
                });
    }


//    private void loadMyInfo(){
//
//        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
//        ref.child(""+ firebaseAuth.getUid())
//                .addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        String dob=""+snapshot.child("dob").getValue();
//                        String email=""+snapshot.child("email").getValue();
//                        String name=""+snapshot.child("name").getValue();
//                        String phoneCode=""+snapshot.child("phoneCode").getValue();
//                        String phoneNumber=""+snapshot.child("phoneNumber").getValue();
//                        String profileImageUrl=""+snapshot.child("profileImageBase64").getValue();
//                        String timestamp=""+snapshot.child("timestamp").getValue();
//                        String userType=""+snapshot.child("userType").getValue();
//                        String phone=phoneCode+phoneNumber;
//
//
//                        // تحويل Base64 إلى Bitmap
//                        try {
//                            byte[] decodedString = Base64.decode(profileImageUrl, Base64.DEFAULT);
//                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//                            // تعيين الصورة إلى ImageView
//                            binding.profileIv.setImageBitmap(decodedByte);
//                        } catch (Exception e) {
//                            Log.e(TAG, "Error decoding Base64 image", e);
//                            binding.profileIv.setImageResource(R.drawable.person_black);  // صورة افتراضية
//                        }
//
//
//
//                        if(timestamp.equals("null")){
//                            timestamp="0";
//                        }
//
//                        String formattedDate= MyUtils.formatTimestampDate(Long.parseLong(timestamp));
//
//                        binding.emailTv.setText(email);
//                        binding.fullNameTv.setText(name);
//                        binding.dobTv.setText(dob);
//                        binding.phoneTv.setText(phone);
//                        binding.membersinceTv.setText(formattedDate);
//
//
//
//
//
//                        Log.d(TAG, "Profile Image URL: " + profileImageUrl);
//
//
//
////                        Glide.with(mContext)
////                                .load(decodedByte)  // تحميل الصورة بعد التحويل
////                                .placeholder(R.drawable.person_black)  // صورة افتراضية أثناء التحميل
////                                .into(binding.profileIv);  // تعيين الصورة إلى ImageView
//
//
////                        // تحميل الصورة عبر Glide
////                        Glide.with(mContext)
////                                .load(profileImageUrl)  // رابط الصورة
////                                .placeholder(R.drawable.person_black)  // صورة افتراضية تظهر أثناء تحميل الصورة
////                                .error(R.drawable.person_black)  // صورة تظهر إذا فشل تحميل الصورة
////                                .into(binding.profileIv);  // تعيين الصورة إلى ImageView
//
//                        if(userType.equals(MyUtils.USER_TYPE_EMAIL)){
//                            boolean isVerified=firebaseAuth.getCurrentUser().isEmailVerified();
//                            if(isVerified){
//                                binding.verifyAcountCv.setVisibility(View.GONE);
//                                binding.verificationTv.setText("Verified");
//                            }
//                            else{
//                                binding.verifyAcountCv.setVisibility(View.VISIBLE);
//                                binding.verificationTv.setText("Not Verified");
//                            }
//                        }
//                        else{
//                            binding.verifyAcountCv.setVisibility(View.GONE);
//                            binding.verificationTv.setText("Verified");
//                        }
//                        try {
//                            Glide.with(mContext)
//                                    .load(profileImageUrl)
//                                    .placeholder(R.drawable.person_black)
//                                    .into(binding.profileIv);
//
//                        }catch (Exception e){
//                            Log.e(TAG,"onDataChanege" ,e);
//                        }
//
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//
//                    }
//                });
//    }
}