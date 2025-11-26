package com.example.realestate.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.realestate.AdapterImagePicked;
import com.example.realestate.ModelImagePicked;
import com.example.realestate.MyUtils;
import com.example.realestate.databinding.ActivityPostAddBinding;
import com.example.realestate.models.ModelProperty;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostAddActivity extends AppCompatActivity {

    private ActivityPostAddBinding binding;
    private static final String TAG = "POST_ADD_TAG";

    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    private Uri imageUri = null;

    private ArrayList<ModelImagePicked> imagePickedArrayList;
    private AdapterImagePicked adapterImagePicked;

    private boolean isEditMode=false;
    private String propertyIdForEdit;

    private ArrayAdapter<String> adapterPropertySubcategory;

    private String category = MyUtils.propertyTypes[0];
    private String purpose = MyUtils.PROPERTY_PURPOSE_SELL;

    private String subCategory = "";
    private String floors = "";
    private String bedRooms = "";
    private String bathRooms = "";
    private String areaSizeUnit = "";
    private String areaSize = "";
    private String price = "";
    private String title = "";
    private String description = "";
    private String email = "";
    private String phoneCode = "";
    private String phoneNumber = "";
    private String country = "";
    private String city = "";
    private String state = "";
    private String address = "";
    private double latitude = 0;
    private double longitude = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isEditMode=getIntent().getBooleanExtra("isEditMode",false);
        propertyIdForEdit=getIntent().getStringExtra("propertyIdForEditing");
        Log.d(TAG,"onCreate:isEditMode: "+isEditMode);
        Log.d(TAG,"onCreate:propertyIdForEdit"+propertyIdForEdit);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...!");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();

        ArrayAdapter<String> adapterAreaSize =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyAreaSizeUnit);
        binding.areaSizeUnitAc.setAdapter(adapterAreaSize);

        if(isEditMode){

            loadPropertyDetails();

            binding.toolbarTitleTv.setText("Update Property");
            binding.submitBtn.setText("Update Property");

        }else {

            binding.toolbarTitleTv.setText("Add Property");
            binding.submitBtn.setText("Post Property");

        }

        imagePickedArrayList = new ArrayList<>();
        adapterImagePicked = new AdapterImagePicked(this, imagePickedArrayList,propertyIdForEdit);
        binding.imagesRv.setAdapter(adapterImagePicked);

        propertyCategoryHomes();

        binding.propertyCategoryTabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    category = MyUtils.propertyTypes[0];
                    propertyCategoryHomes();
                } else if (position == 1) {
                    category = MyUtils.propertyTypes[1];
                    propertyCategoryPlot();
                } else if (position == 2) {
                    category = MyUtils.propertyTypes[2];
                    propertyCategoryCommercial();
                }
                Log.d(TAG, "onTabSelected: category: " + category);
                binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.purposeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                RadioButton selectedRadioButton = findViewById(checkedId);
                purpose = selectedRadioButton.getText().toString();
                Log.d(TAG, "onCheckedChanged:purpose " + purpose);
            }
        });

        binding.pickImagesTv.setOnClickListener(v -> showImagePickOptions());
        binding.submitBtn.setOnClickListener(v -> validateData());
        binding.locationAc.setOnClickListener(v -> {
            Intent intent = new Intent(PostAddActivity.this, LocationPickerActivity.class);
            locationPickerActivityResultLauncher.launch(intent);
        });
    }

    private final ActivityResultLauncher<Intent> locationPickerActivityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            Log.d(TAG, "onActivityResult: result:" + result);
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                Intent data = result.getData();
                                if (data != null) {
                                    latitude = data.getDoubleExtra("latiude", 0);
                                    longitude = data.getDoubleExtra("longitude", 0);
                                    address = data.getStringExtra("address");
                                    city = data.getStringExtra("city");
                                    country = data.getStringExtra("country");
                                    state = data.getStringExtra("state");

                                    Log.d(TAG, "onActivityResult: latitude: " + latitude);
                                    Log.d(TAG, "onActivityResult: longitude: " + longitude);
                                    Log.d(TAG, "onActivityResult: address: " + address);
                                    Log.d(TAG, "onActivityResult: city: " + city);
                                    Log.d(TAG, "onActivityResult: country: " + country);
                                    Log.d(TAG, "onActivityResult: state: " + state);
                                    binding.locationAc.setText(address);
                                }
                            }
                        }
                    }
            );

    private void propertyCategoryHomes() {
        binding.floorsTil.setVisibility(VISIBLE);
        binding.bedroomsTil.setVisibility(VISIBLE);
        binding.bathRoomsTil.setVisibility(VISIBLE);

        adapterPropertySubcategory =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesHomes);
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
        binding.propertySubcategoryAct.setText("");
    }

    private void propertyCategoryPlot() {
        binding.floorsTil.setVisibility(GONE);
        binding.bedroomsTil.setVisibility(GONE);
        binding.bathRoomsTil.setVisibility(GONE);

        adapterPropertySubcategory =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesPlots);
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
        binding.propertySubcategoryAct.setText("");
    }

    private void propertyCategoryCommercial() {
        binding.floorsTil.setVisibility(VISIBLE);
        binding.bedroomsTil.setVisibility(VISIBLE);
        binding.bathRoomsTil.setVisibility(VISIBLE);

        adapterPropertySubcategory =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MyUtils.propertyTypesCommercial);
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
        binding.propertySubcategoryAct.setText("");
    }

    private void showImagePickOptions() {
        Log.d(TAG, "showImagePickOptions: ");
        PopupMenu popupMenu = new PopupMenu(this, binding.pickImagesTv);

        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    String[] permission = new String[]{Manifest.permission.CAMERA};
                    resultCameraPermission.launch(permission);
                } else {
                    String[] permission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    resultCameraPermission.launch(permission);
                }
                return true;
            } else if (itemId == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    resultStoragePermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
                } else {
                    resultStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                return true;
            }
            return false;
        });
    }

    private final ActivityResultLauncher<String> resultStoragePermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    (Boolean isGranted) -> {
                        Log.d(TAG, "onActivityResult: " + isGranted);
                        if (isGranted) {
                            pickImageGallery();
                        } else {
                            MyUtils.toast(PostAddActivity.this, "Storage permission!");
                        }
                    }
            );

    private final ActivityResultLauncher<String[]> resultCameraPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    (ActivityResultCallback<Map<String, Boolean>>) result -> {
                        Log.d(TAG, "onActivityResult: result: " + result);
                        boolean areAllGranted = true;
                        for (boolean isGranted : result.values()) {
                            areAllGranted &= isGranted;
                        }
                        if (areAllGranted) {
                            pickImageCamera();
                        } else {
                            MyUtils.toast(PostAddActivity.this, "Camera or Storage or both permission denied!");
                        }
                    }
            );

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        Log.d(TAG, "onActivityResult: ");
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            imageUri = data != null ? data.getData() : null;
                            if (imageUri != null) {
                                String timestamp = "" + MyUtils.timestamp();
                                ModelImagePicked modelImagePicked =
                                        new ModelImagePicked(timestamp, imageUri, null, false);
                                imagePickedArrayList.add(modelImagePicked);
                                adapterImagePicked.notifyItemInserted(imagePickedArrayList.size() - 1);
                            }
                        } else {
                            MyUtils.toast(PostAddActivity.this, "Cancelled!");
                        }
                    }
            );

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_TITLE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_DESCRIPTION");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        Log.d(TAG, "onActivityResult: result: " + result);
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            String timestamp = "" + MyUtils.timestamp();
                            ModelImagePicked modelImagePicked =
                                    new ModelImagePicked(timestamp, imageUri, null, false);
                            imagePickedArrayList.add(modelImagePicked);
                            adapterImagePicked.notifyItemInserted(imagePickedArrayList.size() - 1);
                        } else {
                            MyUtils.toast(PostAddActivity.this, "Cancelled!");
                        }
                    }
            );

    private void validateData() {
        Log.d(TAG, "validateData: ");
        subCategory = binding.propertySubcategoryAct.getText().toString().trim();
        floors = binding.foorsEt.getText().toString().trim();
        bedRooms = binding.bedroomsEt.getText().toString().trim();
        bathRooms = binding.bathRoomsEt.getText().toString().trim();
        areaSize = binding.areaSizeEt.getText().toString().trim();
        areaSizeUnit = binding.areaSizeUnitAc.getText().toString().trim();
        address = binding.locationAc.getText().toString().trim();
        price = binding.priceEt.getText().toString().trim();
        title = binding.titleEt.getText().toString().trim();
        description = binding.titledescriptionEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();

        if (subCategory.isEmpty()) {
            binding.propertySubcategoryAct.setError("Choose Subcategory");
            binding.propertySubcategoryAct.requestFocus();
        }
        else if (category.equals(MyUtils.propertyTypes[0]) && floors.isEmpty()) {
            binding.foorsEt.setError("Enter Floors Count...!");
            binding.foorsEt.requestFocus();
        }
        else if (category.equals(MyUtils.propertyTypes[0]) && bedRooms.isEmpty()) {
            binding.bedroomsEt.setError("Enter Bedrooms Count...!");
            binding.bedroomsEt.requestFocus();
        }
        else if (category.equals(MyUtils.propertyTypes[0]) && bathRooms.isEmpty()) {
            binding.bathRoomsEt.setError("Enter BathRooms Count...!");
            binding.bathRoomsEt.requestFocus();
        }
        else if (areaSize.isEmpty()) {
            binding.areaSizeEt.setError("Enter Area Size...!");
            binding.areaSizeEt.requestFocus();
        }
        else if (address.isEmpty()) {
            binding.locationAc.setError("Enter Pick Location...!");
            binding.locationAc.requestFocus();
        }
        else if (areaSizeUnit.isEmpty()) {
            binding.areaSizeUnitAc.setError("Choose  Area Size Unit...!");
            binding.areaSizeUnitAc.requestFocus();
        }
        else if (price.isEmpty()) {
            binding.priceEt.setError("Enter Price...!");
            binding.priceEt.requestFocus();
        }
        else if (title.isEmpty()) {
            binding.titleEt.setError("Enter Title...!");
            binding.titleEt.requestFocus();
        }
        else if (description.isEmpty()) {
            binding.titledescriptionEt.setError("Enter Description...!");
            binding.titledescriptionEt.requestFocus();
        }
        else if (phoneNumber.isEmpty()) {
            binding.phoneNumberEt.setError("Enter Phone Number...!");
            binding.phoneNumberEt.requestFocus();
        }
        else if (imagePickedArrayList.isEmpty()) {
            MyUtils.toast(this, "Pick at-least one Image...!");
        } else {

            if(isEditMode){
                updateProperty();
            }
            else{
                postAd();
            }

        }
    }




    private void updateProperty(){
        Log.d(TAG, "updateProperty: ");

        progressDialog.setMessage("Update Property ");
        progressDialog.show();

        if(floors.isEmpty())
            floors="0";
        if (bedRooms.isEmpty()) bedRooms = "0";
        if (bathRooms.isEmpty()) bathRooms = "0";

        long floorsL  = safeLong(floors);
        long bedsL    = safeLong(bedRooms);
        long bathsL   = safeLong(bathRooms);
        double areaD  = Double.parseDouble(areaSize);
        double priceD = Double.parseDouble(price);


        HashMap<String,Object> hashMap=new HashMap<>();



        hashMap.put("purpose",     "" + purpose);
        hashMap.put("category",    "" + category);
        hashMap.put("subCategory", "" + subCategory);
        hashMap.put("areaSizeUnit","" + areaSizeUnit);
        hashMap.put("title",       "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("email",       "" + email);
        hashMap.put("phoneCode",   "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("country",     "" + country);
        hashMap.put("city",        "" + city);
        hashMap.put("state",       "" + state);
        hashMap.put("address",     "" + address);
        // أرقام كأرقام
        hashMap.put("areaSize",  areaD);
        hashMap.put("floors",    floorsL);
        hashMap.put("bedRooms",  bedsL);
        hashMap.put("bathRooms", bathsL);
        hashMap.put("price",     priceD);
        hashMap.put("Status",    MyUtils.AD_STATUS_RENTED); // مفتاح بحرف كبير ليتطابق مع الموديل/البيانات القديمة
        hashMap.put("latitude",  latitude);
        hashMap.put("longitude", longitude);

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyIdForEdit)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();

                        uploadImagesAsBase64(propertyIdForEdit);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ",e);

                        MyUtils.toast(PostAddActivity.this,"Failed to update due to"+e.getMessage());
                        progressDialog.dismiss();


                    }
                });



    }
    private void postAd() {
        Log.d(TAG, "postAd: ");

        progressDialog.setMessage("Publishing Ad");
        progressDialog.show();

         if(floors.isEmpty())
             floors="0";
        if (bedRooms.isEmpty()) bedRooms = "0";
        if (bathRooms.isEmpty()) bathRooms = "0";

        long timestamp = MyUtils.timestamp();
        DatabaseReference refProperties = FirebaseDatabase.getInstance().getReference("Properties");
        String keyId = refProperties.push().getKey();

        long floorsL  = safeLong(floors);
        long bedsL    = safeLong(bedRooms);
        long bathsL   = safeLong(bathRooms);
        double areaD  = Double.parseDouble(areaSize);
        double priceD = Double.parseDouble(price);

        if (category.equals(MyUtils.propertyTypes[1])) { // 0=Homes, 1=Plot, 2=Commercial
            floorsL = 0;
            bedsL   = 0;
            bathsL  = 0;
        }

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id",          "" + keyId);
        hashMap.put("uid",         "" + firebaseAuth.getUid());
        hashMap.put("purpose",     "" + purpose);
        hashMap.put("category",    "" + category);
        hashMap.put("subCategory", "" + subCategory);
        hashMap.put("areaSizeUnit","" + areaSizeUnit);
        hashMap.put("title",       "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("email",       "" + email);
        hashMap.put("phoneCode",   "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("country",     "" + country);
        hashMap.put("city",        "" + city);
        hashMap.put("state",       "" + state);
        hashMap.put("address",     "" + address);

        // أرقام كأرقام
        hashMap.put("areaSize",  areaD);
        hashMap.put("floors",    floorsL);
        hashMap.put("bedRooms",  bedsL);
        hashMap.put("bathRooms", bathsL);
        hashMap.put("price",     priceD);


        hashMap.put("timestamp", timestamp);
        hashMap.put("Status",    MyUtils.AD_STATUS_RENTED); // مفتاح بحرف كبير ليتطابق مع الموديل/البيانات القديمة
        hashMap.put("latitude",  latitude);
        hashMap.put("longitude", longitude);

        refProperties.child(keyId).setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "onSuccess: Ad Published");
                    uploadImagesAsBase64(keyId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onFailure: ", e);
                    progressDialog.dismiss();
                    MyUtils.toast(PostAddActivity.this, "Failed to publish due to " + e.getMessage());
                });
    }

    private long safeLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }

    private String uriToBase64(@NonNull Uri uri) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream probe = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(probe, null, o);
            }
            int sample = 1, maxDim = Math.max(o.outWidth, o.outHeight);
            while (maxDim / sample > 1024) sample *= 2;

            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;

            Bitmap bmp;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(is, null, o);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "uriToBase64 error", e);
            return null;
        }
    }

    private void uploadImagesAsBase64(String propertyId) {
        Log.d(TAG, "uploadImagesAsBase64: propertyId=" + propertyId);
        progressDialog.setMessage("Encoding & saving images…");
        progressDialog.show();

        DatabaseReference imagesRef = FirebaseDatabase.getInstance()
                .getReference("Properties").child(propertyId).child("images");

        int total = imagePickedArrayList.size();
        int[] done = {0};

        for (ModelImagePicked m : imagePickedArrayList) {
            if (m.isFromInternet()) {
                done[0]++;
                if (done[0] == total) {
                    progressDialog.dismiss();
                    MyUtils.toast(this, "Ad & images saved.");
                    finish();
                }
                continue;
            }

            String b64 = uriToBase64(m.getImageUri());
            if (b64 == null) {
                Log.e(TAG, "Failed to encode image: " + m.getId());
                done[0]++;
                if (done[0] == total) {
                    progressDialog.dismiss();
                    MyUtils.toast(this, "Ad saved, but some images failed to encode.");
                }
                continue;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("imageBase64", b64);

            imagesRef.child(m.getId())
                    .setValue(map)
                    .addOnSuccessListener(unused -> {
                        done[0]++;
                        progressDialog.setMessage("Saved " + done[0] + " / " + total + " images");
                        if (done[0] == total) {
                            progressDialog.dismiss();
                            MyUtils.toast(this, "Ad & images saved.");
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Save image error", e);
                        done[0]++;
                        if (done[0] == total) {
                            progressDialog.dismiss();
                            MyUtils.toast(this, "Ad saved, but some images failed to save.");
                        }
                    });
        }
    }

    private void loadPropertyDetails(){
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Properties");

        ref.child(propertyIdForEdit)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            ModelProperty modelProperty=snapshot.getValue(ModelProperty.class);
                            String puroose=""+ modelProperty.getPurpose();
                            String category=""+ modelProperty.getCategory();
                            String subcategory=""+ modelProperty.getSubCategory();
                            String bedRooms=""+ modelProperty.getBedRooms();
                            String bethRomms=""+ modelProperty.getBathRooms();
                            String areaSize=""+ modelProperty.getAreaSize();
                            String areaSizeUnit=""+ modelProperty.getAreaSizeUnit();
                            String price=""+ modelProperty.getPrice();
                            String title=""+ modelProperty.getTitle();
                            String description=""+ modelProperty.getDescription();
                            String email=""+ modelProperty.getEmail();
                            String phoneCode=""+ modelProperty.getPhoneCode();
                            String phoneNumber=""+ modelProperty.getPhoneNumber();
                            address=""+modelProperty.getAddress();
                            //String addrass=""+ modelProperty.getAddress();
                            city=""+modelProperty.getCity();
                            state=""+modelProperty.getStatus();
                            country=""+modelProperty.getCountry();

                            String timestamp=""+ modelProperty.getCategory();
                            latitude=modelProperty.getLatitude();
                            longitude=modelProperty.getLongitude();
                            if(puroose.equalsIgnoreCase(MyUtils.PROPERTY_PURPOSE_SELL)){
                                binding.purposeSellRb.setChecked(true);
                            }else if (puroose.equalsIgnoreCase(MyUtils.PROPERTY_PURPOSE_RENT)){
                                binding.purposeRentRb.setChecked(true);


                            }
                            if(category.equalsIgnoreCase(MyUtils.propertyTypes[0])){
                                binding.propertyCategoryTabLayout.selectTab(binding.propertyCategoryTabLayout.getTabAt(0));

                            }

                            else if(category.equalsIgnoreCase(MyUtils.propertyTypes[1])){

                                binding.propertyCategoryTabLayout.selectTab(binding.propertyCategoryTabLayout.getTabAt(1));

                            }

                            else if(category.equalsIgnoreCase(MyUtils.propertyTypes[2])){

                                binding.propertyCategoryTabLayout.selectTab(binding.propertyCategoryTabLayout.getTabAt(2));

                            }

                            binding.propertySubcategoryAct.setText(subcategory);
                            binding.foorsEt.setText(floors);
                            binding.bedroomsEt.setText(bedRooms);
                            binding.bathRoomsEt.setText(bathRooms);
                            binding.areaSizeEt.setText(areaSize);
                            binding.areaSizeUnitAc.setText(areaSizeUnit);
                            binding.locationAc.setText(address);
                            binding.priceEt.setText(price);
                            binding.titleEt.setText(title);
                            binding.titledescriptionEt.setText(description);
                            binding.emailEt.setText(email);
                            binding.phoneNumberEt.setText(phoneNumber);
                            binding.phoneCodeTil.getTextView_selectedCountry().setText(phoneCode);

                            DatabaseReference refImages=snapshot.child("images").getRef();
                            refImages.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for(DataSnapshot ds:snapshot.getChildren()){
                                        String id=""+ds.child("id").getValue();
                                        String imageUrl=""+ds.child("imageUrl").getValue();
                                        ModelImagePicked modelImagePicked=new ModelImagePicked(id,null,imageUrl,true);
                                        imagePickedArrayList.add(modelImagePicked);
                                    }

                                    loadImages();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });


                        } catch (Exception e) {
                            Log.e(TAG,"onDataChange: ",e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }




    private void loadImages() {
        // تحقق إذا كانت هناك صور في القائمة
        if (imagePickedArrayList.isEmpty()) {
            // إذا القائمة فارغة، إخفاء RecyclerView أو عرض رسالة مناسبة
            binding.imagesRv.setVisibility(GONE);
        } else {
            // إذا كانت القائمة تحتوي على صور، إظهار RecyclerView
            binding.imagesRv.setVisibility(VISIBLE);
            // تنبيه الأدابتر لإعادة تحميل البيانات
            adapterImagePicked.notifyDataSetChanged();
        }

        // (اختياري) إذا كنت ترغب في تمرير الـRecyclerView إلى آخر عنصر تم إضافته
        if (!imagePickedArrayList.isEmpty()) {
            binding.imagesRv.smoothScrollToPosition(imagePickedArrayList.size() - 1);
        }
    }

}
