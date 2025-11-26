package com.example.realestate.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.activities.LocationPickerActivity;
import com.example.realestate.adapters.AdapterProperty;
import com.example.realestate.databinding.BsFilterCategoryBinding;
import com.example.realestate.databinding.FragmentHomeBinding;
import com.example.realestate.models.ModelProperty;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private static final String TAG = "HOME_TAG";

    private Context mContext;
    private final ArrayList<ModelProperty> modelPropertyArrayList = new ArrayList<>();
    private AdapterProperty adapterProperty;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentCity = "";
    private String currentAddress = "";

    private SharedPreferences locationSp;
    private String filterPurpose = MyUtils.PROPERTY_PURPOSE_ANY;
    private String filterCategory = "";
    private String filterSubCategory = "";
    private Double filterpriceMin = 0.0;
    private Double filterpriceMax = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.propertiesRv.setLayoutManager(new LinearLayoutManager(mContext));
        binding.propertiesRv.setHasFixedSize(true);

        adapterProperty = new AdapterProperty(mContext, modelPropertyArrayList);
        binding.propertiesRv.setAdapter(adapterProperty);

        // جلب أسماء المحافظات من resources
        String[] governorates = getResources().getStringArray(R.array.yemen_governorates);

// إضافة خيار "الكل" في البداية
        String[] spinnerItems = new String[governorates.length + 1];
        spinnerItems[0] = "اختر محافظة";
        System.arraycopy(governorates, 0, spinnerItems, 1, governorates.length);

// إنشاء ArrayAdapter للـ Spinner
        ArrayAdapter<String> spAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, spinnerItems);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

// ربط الـ Adapter بالـ Spinner
        binding.filterSelectedSp.setAdapter(spAdapter);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationSp = mContext.getSharedPreferences("LOCATION_SP", MODE_PRIVATE);
        currentLatitude = locationSp.getFloat("CURRENT_LATITUDE", 0.0f);
        currentLongitude = locationSp.getFloat("CURRENT_LONGITUDE", 0.0f);
        currentAddress = locationSp.getString("CURRENT_ADDRESS", "");
        currentCity = locationSp.getString("CURRENT_CITY", "");
        if (currentCity != null && !currentCity.isEmpty())
            binding.cityTv.setText(currentCity);

        loadProperties();

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterProperty.getFilter().filter(s);
                } catch (Exception e) {
                    Log.e(TAG, "filter error: ", e);
                }
            }
        });

        binding.cityTv.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, LocationPickerActivity.class);
            locationActivityResultLauncher.launch(intent);
        });

        binding.filterSelectedSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = parent.getItemAtPosition(position).toString();
                filterPropertiesByCity(selectedCity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterPropertiesByCity(""); // عرض كل البيانات
            }
        });

      //  binding.filterTV.setOnClickListener(v -> showFilterDialog());
    }

    private boolean isPropertyMatchingFilter(ModelProperty m) {
        if (m == null) return false;

        boolean purposeOk = MyUtils.PROPERTY_PURPOSE_ANY.equals(filterPurpose)
                || (m.getPurpose() != null && m.getPurpose().equalsIgnoreCase(filterPurpose));

        boolean categoryOk = filterCategory.isEmpty()
                || (m.getCategory() != null && m.getCategory().equalsIgnoreCase(filterCategory));

        boolean subCategoryOk = filterSubCategory.isEmpty()
                || (m.getSubCategory() != null && m.getSubCategory().equalsIgnoreCase(filterSubCategory));

        double price = m.getPrice();
        boolean priceOk = price >= filterpriceMin && (filterpriceMax == null || price <= filterpriceMax);

        return purposeOk && categoryOk && subCategoryOk && priceOk;
    }

    private final ActivityResultLauncher<Intent> locationActivityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                Intent data = result.getData();
                                if (data != null) {
                                    currentLatitude = data.getDoubleExtra("latitude", 0.0);
                                    currentLongitude = data.getDoubleExtra("longitude", 0.0);
                                    currentAddress = data.getStringExtra("address");
                                    currentCity = data.getStringExtra("city");

                                    locationSp.edit()
                                            .putFloat("CURRENT_LATITUDE", (float) currentLatitude)
                                            .putFloat("CURRENT_LONGITUDE", (float) currentLongitude)
                                            .putString("CURRENT_ADDRESS", currentAddress)
                                            .putString("CURRENT_CITY", currentCity)
                                            .apply();

                                    binding.cityTv.setText(currentCity);
                                    loadProperties();
                                }
                            } else {
                                Log.d(TAG, "onActivityResult: Cancelled");
                                MyUtils.toast(mContext, "Cancelled!");
                            }
                        }
                    }
            );

    private ArrayAdapter<String> arrayAdapterPropertyCategory;
    private ArrayAdapter<String> arrayAdapterPropertySubCategory;

    private final ArrayList<ModelProperty> allPropertiesList = new ArrayList<>(); // قائمة لجميع العقارات

    private void loadProperties() {
        Log.d(TAG, "loadProperties()");
        FirebaseDatabase.getInstance()
                .getReference("Properties")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allPropertiesList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ModelProperty m = snapshotToModel(ds);
                            if (m != null) {
                                allPropertiesList.add(m);
                            }
                        }
                        // عرض كل العقارات في البداية
                        adapterProperty.updateData(new ArrayList<>(allPropertiesList));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadProperties cancelled: " + error.getMessage());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private ModelProperty snapshotToModel(@NonNull DataSnapshot ds) {
        try {
            ModelProperty m = new ModelProperty();

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

            String status = firstNonEmpty(str(ds, "status"), str(ds, "Status"));
            m.setStatus(status);

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
            String s = v.toString().trim();
            if (s.isEmpty()) return 0L;
            return (long) Math.floor(Double.parseDouble(s));
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double safeDouble(Object v) {
        try {
            if (v == null) return 0d;
            String s = v.toString().trim();
            if (s.isEmpty()) return 0d;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
    }

    private void showFilterDialog() {
        BsFilterCategoryBinding bindingBs = BsFilterCategoryBinding.inflate(getLayoutInflater());
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mContext);
        bottomSheetDialog.setContentView(bindingBs.getRoot());
        bottomSheetDialog.show();

        if (!filterCategory.isEmpty()) bindingBs.propertyCategoryAct.setText(filterCategory);
        if (!filterSubCategory.isEmpty()) bindingBs.propertySubCategoryAct.setText(filterSubCategory);
        if (filterpriceMin != 0) bindingBs.priceMinEt.setText("" + filterpriceMin);
        if (filterpriceMax != null && filterpriceMax != 0) bindingBs.priceMaxEt.setText("" + filterpriceMax);

        bindingBs.tabBuyTv.setOnClickListener(v -> {
            filterPurpose = MyUtils.PROPERTY_PURPOSE_SELL;
            bindingBs.tabBuyTv.setBackgroundResource(R.drawable.shape_rounded_white);
            bindingBs.tabBuyTv.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
            bindingBs.tabRentTv.setBackground(null);
            bindingBs.tabRentTv.setTextColor(ContextCompat.getColor(mContext, R.color.black));
            bindingBs.tabRentTv.setTypeface(null, Typeface.NORMAL);
        });

        bindingBs.tabRentTv.setOnClickListener(v -> {
            filterPurpose = MyUtils.PROPERTY_PURPOSE_RENT;
            bindingBs.tabBuyTv.setBackground(null);
            bindingBs.tabBuyTv.setTextColor(ContextCompat.getColor(mContext, R.color.black));
            bindingBs.tabBuyTv.setTypeface(null, Typeface.NORMAL);
            bindingBs.tabRentTv.setBackgroundResource(R.drawable.shape_rounded_white);
            bindingBs.tabRentTv.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
            bindingBs.tabRentTv.setTypeface(null, Typeface.NORMAL);

            arrayAdapterPropertyCategory = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_list_item_1, MyUtils.propertyTypes);
            bindingBs.propertyCategoryAct.setAdapter(arrayAdapterPropertyCategory);
        });

        bindingBs.propertyCategoryAct.setOnItemClickListener((parent, view, position, id) -> {
            filterCategory = parent.getItemAtPosition(position).toString();
            filterSubCategory = "";
            bindingBs.propertySubCategoryAct.setText(filterSubCategory);

            if (filterCategory.equals(MyUtils.propertyTypes[0])) {
                arrayAdapterPropertySubCategory = new ArrayAdapter<>(mContext,
                        android.R.layout.simple_list_item_1, MyUtils.propertyTypesHomes);
            } else if (filterCategory.equals(MyUtils.propertyTypes[2])) {
                arrayAdapterPropertySubCategory = new ArrayAdapter<>(mContext,
                        android.R.layout.simple_list_item_1, MyUtils.propertyTypesPlots);
            } else if (filterCategory.equals(MyUtils.propertyTypes[1])) {
                arrayAdapterPropertySubCategory = new ArrayAdapter<>(mContext,
                        android.R.layout.simple_list_item_1, MyUtils.propertyTypesCommercial);
            }

            bindingBs.propertySubCategoryAct.setAdapter(arrayAdapterPropertySubCategory);
        });

        bindingBs.propertySubCategoryAct.setOnItemClickListener((parent, view, position, id) ->
                filterSubCategory = parent.getItemAtPosition(position).toString()
        );

        bindingBs.resetBtn.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            filterPurpose = MyUtils.PROPERTY_PURPOSE_ANY;
            filterCategory = "";
            filterSubCategory = "";
            filterpriceMax = null;
            filterpriceMin = 0.0;
            loadProperties();
        });

        bindingBs.applyBtn.setOnClickListener(v -> {
            if (filterPurpose.equals(MyUtils.PROPERTY_PURPOSE_ANY)) {
                filterPurpose = MyUtils.PROPERTY_PURPOSE_SELL;
            }

            if (filterCategory.isEmpty()) {
                bindingBs.propertyCategoryAct.setError("Choose Category");
                bindingBs.propertyCategoryAct.requestFocus();
                return;
            }
            if (filterSubCategory.isEmpty()) {
                bindingBs.propertySubCategoryAct.setError("Choose subCategory");
                bindingBs.propertySubCategoryAct.requestFocus();
                return;
            }

            String priceMin = bindingBs.priceMinEt.getText().toString().trim();
            String priceMax = bindingBs.priceMaxEt.getText().toString().trim();

            filterpriceMin = priceMin.isEmpty() ? 0.0 : Double.parseDouble(priceMin);
            filterpriceMax = priceMax.isEmpty() ? null : Double.parseDouble(priceMax);

            bottomSheetDialog.dismiss();
            loadProperties();
        });
    }

    private void filterPropertiesByCity(String city) {
        ArrayList<ModelProperty> filteredList = new ArrayList<>();
        if (city.equals("اختر محافظة") || city.isEmpty()) {
            filteredList.addAll(allPropertiesList); // عرض كل العقارات
        } else {
            for (ModelProperty property : allPropertiesList) {
                if (property.getCity() != null && property.getCity().equals(city)) {
                    filteredList.add(property);
                }
            }
        }
        adapterProperty.updateData(filteredList);
    }

}
