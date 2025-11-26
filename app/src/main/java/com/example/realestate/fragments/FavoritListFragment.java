package com.example.realestate.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.realestate.adapters.AdapterProperty;
import com.example.realestate.databinding.FragmentFavoritListBinding;
import com.example.realestate.models.ModelProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FavoritListFragment extends Fragment {

    private static final String TAG = "FAVORIT_LIST_TAG";

    private FragmentFavoritListBinding binding;
    private Context mContext;
    private FirebaseAuth firebaseAuth;

    private ArrayList<ModelProperty> modelPropertyArrayList;
    private AdapterProperty adapterProperty;

    private DatabaseReference favRef;
    private ChildEventListener favChildListener;
    private final Map<String, Integer> indexById = new HashMap<>();
    private boolean isListenerAttached = false; // لمنع التكرار


    private SharedPreferences locationSp;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public FavoritListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoritListBinding.inflate(inflater, container, false);
       // Toast.makeText(mContext, "onCreateView:", Toast.LENGTH_SHORT).show();
        // إعداد RecyclerView
        binding.favoriteRv.setLayoutManager(new LinearLayoutManager(mContext));
        binding.favoriteRv.setHasFixedSize(true);
        modelPropertyArrayList = new ArrayList<>();
        indexById.clear(); // تنظيف الفهرس لمنع مشاكل التكرار

       // attachFavoritesListener();
        adapterProperty = new AdapterProperty(mContext, modelPropertyArrayList);
        binding.favoriteRv.setAdapter(adapterProperty);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
      //  Toast.makeText(mContext, "onViewCreated:", Toast.LENGTH_SHORT).show();
        super.onViewCreated(view, savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();

        attachFavoritesListener();

        // البحث الفوري
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (adapterProperty != null) {
                    adapterProperty.getFilter().filter(s == null ? "" : s.toString());
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void attachFavoritesListener() {
       // Toast.makeText(mContext, "attachFavoritesListener:", Toast.LENGTH_SHORT).show();

        if (isListenerAttached) return;
        isListenerAttached = true;

        if (firebaseAuth.getCurrentUser() == null) return;

//        favRef = FirebaseDatabase.getInstance()
//                .getReference("Users")
//                .child(firebaseAuth.getUid())
//                .child("Favorites");


//        favRef = FirebaseDatabase.getInstance()
//                .getReference("Users")
//                //.child(firebaseAuth.getUid())
//                .child("21QSwxHaLKMwPYf6FY9Cd1YbY2o2") // جرّب الـ UID من قاعدة بياناتك
//                .child("Favorites");

        favRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(firebaseAuth.getUid())
                .child("Favorites");


        //  FirebaseDatabase.getInstance().getReference("Properties")


        // تحميل كل المفضلات أول مرة
        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                modelPropertyArrayList.clear();
                indexById.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    loadProperty(ds.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // مراقبة الإضافات والحذف لاحقًا
        favChildListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) { loadProperty(snapshot.getKey()); }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String id = snapshot.getKey();
                if (id != null && indexById.containsKey(id)) {
                    modelPropertyArrayList.remove((int) indexById.get(id));
                    rebuildIndexMap();
                    adapterProperty.updateData(modelPropertyArrayList);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        favRef.addChildEventListener(favChildListener);
    }

    private void loadProperty(String propertyId) {
      //  Toast.makeText(mContext, "loadProperty:", Toast.LENGTH_SHORT).show();

        DatabaseReference propertyRef = FirebaseDatabase.getInstance()
                .getReference("Properties")
                .child(propertyId);

        propertyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ds) {
                if (!ds.exists()) return;

                ModelProperty property = snapshotToModel(ds);
                if (property == null) return;
                property.setId(propertyId);

                // تجنب التكرار باستخدام الفهرس
                if (indexById.containsKey(propertyId)) return;

                modelPropertyArrayList.add(property);
                rebuildIndexMap();
                //adapterProperty.updateData(modelPropertyArrayList);
              //  binding.favoriteRv.setAdapter(adapterProperty);

                adapterProperty.updateData(new ArrayList<>(modelPropertyArrayList));

                binding.favoriteRv.invalidate(); // إعادة رسم واجهة القائمة
                Log.d(TAG, "Recycler updated, count=" + adapterProperty.getItemCount());

                //  binding.favoriteRv.setAdapter(adapterProperty);

                Log.d(TAG, "Property loaded1: " + property.getTitle());
               // Toast.makeText(mContext, "تم تحميل: " + property.getTitle(), Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadProperty cancelled2: " + error.getMessage());
            }
        });
    }

    private void rebuildIndexMap() {
        indexById.clear(); // تنظيف الفهرس أولاً
        for (int i = 0; i < modelPropertyArrayList.size(); i++) {
            String id = modelPropertyArrayList.get(i).getId();
            if (id != null) indexById.put(id, i);
        }
    }

    private ModelProperty snapshotToModel(DataSnapshot ds) {
        try {
           // Toast.makeText(mContext, "snapshotToModel:", Toast.LENGTH_SHORT).show();

            ModelProperty m = new ModelProperty();
            m.setTitle(val(ds, "title", "عقار بدون عنوان"));
            m.setPurpose(val(ds, "purpose", ""));
            m.setCategory(val(ds, "category", ""));
            m.setSubCategory(val(ds, "subCategory", ""));
            m.setAreaSizeUnit(val(ds, "areaSizeUnit", ""));
            m.setDescription(val(ds, "description", ""));
            m.setEmail(val(ds, "email", ""));
            m.setPhoneCode(val(ds, "phoneCode", ""));
            m.setPhoneNumber(val(ds, "phoneNumber", ""));
            m.setCountry(val(ds, "country", ""));
            m.setCity(val(ds, "city", ""));
            m.setState(val(ds, "state", ""));
            m.setAddress(val(ds, "address", ""));
            m.setStatus(val(ds, "status", val(ds, "Status", "")));
            m.setFloors(safeLong(ds.child("floors").getValue()));
            m.setBedRooms(safeLong(ds.child("bedRooms").getValue()));
            m.setBathRooms(safeLong(ds.child("bathRooms").getValue()));
            m.setTimestamp(safeLong(ds.child("timestamp").getValue()));
            m.setAreaSize(safeDouble(ds.child("areaSize").getValue()));
            m.setPrice(safeDouble(ds.child("price").getValue()));
            m.setLatitude(safeDouble(ds.child("latitude").getValue()));
            m.setLongitude(safeDouble(ds.child("longitude").getValue()));
            return m;
        } catch (Exception e) {
            Log.e(TAG, "snapshotToModel error", e);
            return null;
        }
    }

    private String val(DataSnapshot ds, String key, String def) {
        Object v = ds.child(key).getValue();
        return v == null ? def : String.valueOf(v);
    }

    private Long safeLong(Object v) {
        try {
            if (v == null) return 0L;
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double safeDouble(Object v) {
        try {
            if (v == null) return 0d;
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return 0d;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
       // Toast.makeText(mContext, "onDestroyView:", Toast.LENGTH_SHORT).show();

        if (favRef != null && favChildListener != null) {
            favRef.removeEventListener(favChildListener);
        }
        binding = null;
        isListenerAttached = false; // إعادة ضبط المتغير
    }
}
