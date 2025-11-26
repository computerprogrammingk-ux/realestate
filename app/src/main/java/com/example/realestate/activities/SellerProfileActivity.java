package com.example.realestate.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.adapters.AdapterProperty;
import com.example.realestate.databinding.ActivitySellerProfileBinding;
import com.example.realestate.models.ModelProperty;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SellerProfileActivity extends AppCompatActivity {


    private ActivitySellerProfileBinding binding;
    private static final String TAG="SELLER_INFO_TAG";
    private String sellerUid="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        binding=ActivitySellerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

       // sellerUid=getIntent().getStringExtra("sellerUid");
       // Log.d(TAG,"onCreate: sellerUid: "+sellerUid);

        sellerUid = getIntent().getStringExtra("sellerUid");
        Log.d(TAG, "onCreate: sellerUid: " + sellerUid);

        if (sellerUid == null || sellerUid.trim().isEmpty()) {
            MyUtils.toast(this, "No seller selected");
            finish();
            return;
        }


        laodSellerDetails();
        loadSellerProperties();

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }
    private void laodSellerDetails(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(sellerUid).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = String.valueOf(snapshot.child("name").getValue());
                String profileImageUrl = String.valueOf(snapshot.child("profileImageBase64").getValue());

                // <<<<<< تعديل آمن للتاريخ >>>>>
                String tsStr = String.valueOf(snapshot.child("timestamp").getValue());
                long ts = 0L;
                try {
                    if (tsStr != null && !tsStr.trim().isEmpty() && !"null".equalsIgnoreCase(tsStr)) {
                        ts = Long.parseLong(tsStr.trim());
                    }
                } catch (Exception ignored) {}
                // <<<<<< نهاية التعديل >>>>>

                binding.sellerName.setText(name);
                binding.memberSinceTv.setText(MyUtils.formatTimestampDate(ts));

                try {
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        byte[] decodedString = Base64.decode(profileImageUrl, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        // تعيين الصورة إلى ImageView
                        binding.sellerProfileIv.setImageBitmap(decodedByte);
                    } else {
                        binding.sellerProfileIv.setImageResource(R.drawable.person_black); // صورة افتراضية إذا كانت الصورة فارغة أو غير موجودة
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding Base64 image", e);
                    binding.sellerProfileIv.setImageResource(R.drawable.person_black); // صورة افتراضية في حالة حدوث خطأ
                }



//                try {
//                    Glide.with(SellerProfileActivity.this)
//                            .load((profileImageUrl == null || profileImageUrl.trim().isEmpty()) ? null : profileImageUrl)
//                            .placeholder(R.drawable.person_white)
//                            .error(R.drawable.person_white)
//                            .into(binding.sellerProfileIv);
//                } catch (Exception e){
//                    Log.e(TAG,"onDataChange: ", e);
//                }



            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }



    private void loadSellerProperties(){

        ArrayList<ModelProperty> modelPropertyArrayList=new ArrayList<>();

        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Properties");
      //  reference.child("uid").equalTo(sellerUid)
        reference.orderByChild("uid").equalTo(sellerUid)

                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        modelPropertyArrayList.clear();
                        for(DataSnapshot ds:snapshot.getChildren()){
                            try {
                                ModelProperty modelProperty=ds.getValue(ModelProperty.class);
                                modelPropertyArrayList.add(modelProperty);
                            }
                            catch (Exception e){
                                Log.e(TAG,"onDataChange: ",e);

                            }
                        }
                        AdapterProperty adapterProperty=new AdapterProperty(SellerProfileActivity.this,modelPropertyArrayList);
                        binding.propertiesRv.setAdapter(adapterProperty);

                        String propertiesCount=""+ modelPropertyArrayList.size();
                        binding.puplishedAdsCountTv.setText(propertiesCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}