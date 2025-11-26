package com.example.realestate;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;

import com.example.realestate.models.ModelProperty;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SharedPropertiesViewModel extends ViewModel {

    private static final String TAG = "SharedPropsVM";

    private final MutableLiveData<List<ModelProperty>> _items = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ModelProperty>> items = _items;

    private final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
    private ValueEventListener listener;

    /** استدعِها مرة واحدة مثلاً في onViewCreated */
    public void loadOnce() {
        if (listener != null) return; // لا تكرّر السماع

        listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ModelProperty> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelProperty m = snapshotToModel(ds);
                    if (m == null) continue;

                    if (m.getId() == null || m.getId().isEmpty()) {
                        m.setId(ds.getKey());
                    }
                    list.add(m);
                }
                _items.setValue(list);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: " + error.getMessage());
            }
        };
        ref.addValueEventListener(listener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listener != null) ref.removeEventListener(listener);
    }

    // --------- تحويل آمن من DataSnapshot إلى ModelProperty ---------

    private ModelProperty snapshotToModel(@NonNull DataSnapshot ds) {
        try {
            ModelProperty m = new ModelProperty();

            // نصوص
            m.setId(str(ds, "id"));
            m.setUid(str(ds, "uid"));
            m.setPurpose(str(ds, "purpose"));
            m.setCategory(str(ds, "category"));
            m.setSubCategory(str(ds, "subCategory"));
            m.setAreaSizeUnit(str(ds, "areaSizeUnit"));
            m.setTitle(str(ds, "title"));
            m.setDescription(str(ds, "description"));
            m.setEmail(str(ds, "email"));
            m.setPhoneCode(str(ds, "phoneCode"));
            m.setPhoneNumber(str(ds, "phoneNumber"));
            m.setCountry(str(ds, "country"));
            m.setCity(str(ds, "city"));
            m.setState(str(ds, "state"));
            m.setAddress(str(ds, "address"));

            // "status" أو "Status"
            String status = firstNonEmpty(str(ds, "status"), str(ds, "Status"));
            m.setStatus(status);

            // أرقام (قد تأتي String أو Number)
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

    private String str(@NonNull DataSnapshot ds, String key) {
        Object v = ds.child(key).getValue();
        return v == null ? null : String.valueOf(v);
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        return (b != null && !b.isEmpty()) ? b : null;
    }

    private Long safeLong(Object v) {
        try {
            if (v == null) return 0L;
            if (v instanceof Number) return ((Number) v).longValue();
            String s = v.toString().trim();
            if (s.isEmpty()) return 0L;
            // يدعم "5" و "5.0"
            return (long) Math.floor(Double.parseDouble(s));
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double safeDouble(Object v) {
        try {
            if (v == null) return 0d;
            if (v instanceof Number) return ((Number) v).doubleValue();
            String s = v.toString().trim();
            if (s.isEmpty()) return 0d;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
    }
}
