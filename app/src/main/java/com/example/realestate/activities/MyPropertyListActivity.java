package com.example.realestate.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.realestate.R;
import com.example.realestate.adapters.AdapterProperty;
import com.example.realestate.databinding.ActivityMyPropertyListBinding;
import com.example.realestate.models.ModelProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MyPropertyListActivity extends AppCompatActivity {

    private ActivityMyPropertyListBinding binding;
    private static final String TAG="MYPROPERTY_LIST_TAG";

    private FirebaseAuth firebaseAuth;

    private ArrayList<ModelProperty>  modelPropertyArrayList;
    private AdapterProperty adapterProperty;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     //   EdgeToEdge.enable(this);

        binding=ActivityMyPropertyListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth=FirebaseAuth.getInstance();


        loadMyProperties();

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(TAG,"beforeTextChanged: Query: "+s);

                try {
                    String query=s.toString();
                    adapterProperty.getFilter().filter(query);

                }catch (Exception e){
                    Log.e(TAG,"beforeTextChanged: ",e);


                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }

    private void loadMyProperties(){

        modelPropertyArrayList=new ArrayList<>();
        String myUid=""+firebaseAuth.getUid();
        Log.d(TAG,"loadMyProperties: myUid: "+ myUid);

        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Properties");
        ref.orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        modelPropertyArrayList.clear();
                        for (DataSnapshot ds:snapshot.getChildren()){
                            try {
                                ModelProperty modelProperty=ds.getValue(ModelProperty.class);
                                modelPropertyArrayList.add(modelProperty);

                            }catch (Exception e){
                                Log.e(TAG,"onDataChange: ",e);
                            }
                        }
                        adapterProperty=new AdapterProperty(MyPropertyListActivity.this,modelPropertyArrayList);
                        binding.propertiesRv.setAdapter(adapterProperty);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}