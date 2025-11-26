package com.example.realestate.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.adapters.AdapterImageSlider;
import com.example.realestate.databinding.ActivityPropertyDetailsBinding;
import com.example.realestate.models.ModelImageSlider;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PropertyDetailsActivity extends AppCompatActivity {

    private ActivityPropertyDetailsBinding binding;
    private String propertyId = "";
    private FirebaseAuth firebaseAuth;
    private double propertyLatitude = 0.0;
    private double propertyLongitude = 0.0;
    private String sellerUid = null;
    private String sellerPhone = "";
    private String propertyStatus = "";
    private ArrayList<ModelImageSlider> imageSliderArrayList;
    private AdapterImageSlider adapterImageSlider;
    private boolean favorite = false;

    private static final String TAG = "PROPERTY_DETAILS_TAG";

    // ===== Helpers =====
    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static boolean eqi(String a, String b) {
        return nz(a).equalsIgnoreCase(nz(b));
    }

    private long asLong(Object v) {
        try {
            if (v == null) return 0L;
            if (v instanceof Number) return ((Number) v).longValue();
            String s = v.toString().trim();
            if (s.isEmpty() || s.equalsIgnoreCase("null")) return 0L;
            return (long) Math.floor(Double.parseDouble(s));
        } catch (Exception e) {
            return 0L;
        }
    }

    private double asDouble(Object v) {
        try {
            if (v == null) return 0d;
            if (v instanceof Number) return ((Number) v).doubleValue();
            String s = v.toString().trim();
            if (s.isEmpty() || s.equalsIgnoreCase("null")) return 0d;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPropertyDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // إخفاء افتراضيًا أزرار معيّنة لين ما نعرف هل المستخدم هو صاحب الإعلان
        binding.toolbarBackBtn.setVisibility(GONE);
        binding.toolbarDeleteBtn.setVisibility(GONE);
        //binding.chatBtn.setVisibility(GONE);
        binding.callBtn.setVisibility(GONE);
        binding.smsBtn.setVisibility(GONE);

        propertyId = getIntent().getStringExtra("propertyId");
        Log.d(TAG, "onCreate: property: " + propertyId);

        firebaseAuth = FirebaseAuth.getInstance();

        // تحقق من الـ ID
        if (propertyId == null || propertyId.trim().isEmpty()) {
            Log.e(TAG, "Property ID is null — cannot load details");
            MyUtils.toast(this, "No property selected");
            finish();
            return;
        }

        if (firebaseAuth.getCurrentUser() != null) {
            chickIsFavories();
        }

        loadPropertyDetails();
        loadPropertyImages();

        // أزرار الواجهة
        binding.toolbarBackBtn.setOnClickListener(v -> finish());

        binding.toolbarDeleteBtn.setOnClickListener(v -> {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(PropertyDetailsActivity.this);
            materialAlertDialogBuilder.setTitle("Delete")
                    .setMessage("Are you sure you want to delete this property?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "onClick: Delete clicked");
                            deleteProperty();
                        }
                    }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // no-op
                        }
                    }).show();
        });

        binding.toolbarFavBtn.setOnClickListener(v -> {
            // تأكد أن المستخدم مسجل
            if (firebaseAuth.getCurrentUser() == null) {
                MyUtils.toast(PropertyDetailsActivity.this, "Please login to use favorites");
                return;
            }

            if (propertyId == null || propertyId.trim().isEmpty()) return;

            // تعطيل الزر مؤقتًا لمنع نقرات مزدوجة
            binding.toolbarFavBtn.setEnabled(false);

            if (favorite) {
                // حذف من المفضلة
                MyUtils.removeFromFavorite(PropertyDetailsActivity.this, propertyId);
                binding.toolbarFavBtn.setImageResource(R.drawable.fav_no_black);
            } else {
                // إضافة للمفضلة
                MyUtils.addToFavorite(PropertyDetailsActivity.this, propertyId);
                binding.toolbarFavBtn.setImageResource(R.drawable.fav_yes_black);
            }

            // إعادة تمكين الزر بعد نصف ثانية
            binding.toolbarFavBtn.postDelayed(() -> binding.toolbarFavBtn.setEnabled(true), 600);
        });

        binding.callBtn.setOnClickListener(v -> MyUtils.callIntent(PropertyDetailsActivity.this, sellerPhone));

        binding.smsBtn.setOnClickListener(v -> MyUtils.smsIntent(PropertyDetailsActivity.this, sellerPhone));

        binding.mapBtn.setOnClickListener(v -> MyUtils.mapIntent(PropertyDetailsActivity.this, propertyLatitude, propertyLongitude));

        binding.toolbarEditBtn.setOnClickListener(v -> editOptions());

        binding.sellerPerfileCv.setOnClickListener(v -> {
            Intent intent = new Intent(PropertyDetailsActivity.this, SellerProfileActivity.class);
            intent.putExtra("sellerUid", sellerUid);
            startActivity(intent);
        });
    }

    private void editOptions() {
        PopupMenu popupMenu = new PopupMenu(this, binding.toolbarEditBtn);
        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Edit");
        Log.d(TAG, "propertyStatus: " + propertyStatus);

        // إذا كانت الحالة "متاح" أو "مستأجر" اعرض خيار "مباع"
        if (propertyStatus.equalsIgnoreCase(MyUtils.AD_STATUS_AVAILABE) ||
                propertyStatus.equalsIgnoreCase(MyUtils.AD_STATUS_RENTED)) {

            Log.d(TAG, "Adding 'Mark as Sold' option to the menu.");
            popupMenu.getMenu().add(Menu.NONE, 1, 1, "Mark as Sold");
        } else {
            Log.d(TAG, "Property is not available or rented, 'Mark as Sold' option will not be added.");
        }

        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            Log.d(TAG, "onMenuItemClick: itemId: " + itemId);

            if (itemId == 0) {
                Log.d(TAG, "onMenuItemClick: Edit clicked ");
                Intent intent = new Intent(PropertyDetailsActivity.this, PostAddActivity.class);
                intent.putExtra("isEditMode", true);
                intent.putExtra("propertyIdForEditing", propertyId);
                startActivity(intent);

            } else if (itemId == 1) {

                Log.d(TAG, "onMenuItemClick: Mark as sold clicked");
                showMarkAsSoldDialog();
            }
            return true;
        });

    }

    private void showMarkAsSoldDialog() {
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this);
        materialAlertDialogBuilder.setTitle("Mark as Sold")
                .setMessage("Are you sure you want to mark this property as Sold?")
                .setPositiveButton("YES", (dialog, which) -> {
                    Log.d(TAG, "onClick: Marking as sold");
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("status", MyUtils.AD_STATUS_SOLD);

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Properties");
                    reference.child(propertyId)
                            .updateChildren(hashMap)
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "onSuccess: Marked as sold");
                                MyUtils.toast(PropertyDetailsActivity.this, "Marked as sold");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "onFailure", e);
                                MyUtils.toast(PropertyDetailsActivity.this, "Failed to mark as sold due to " + e.getMessage());
                            });

                }).setNegativeButton("CANCEL", (dialog, which) -> {
                    Log.d(TAG, "onClick: Cancelled");
                    dialog.dismiss();
                }).show();
    }

    private void loadPropertyDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                try {
                    // ===== نصوص =====
                    String uid = nz(s.child("uid").getValue(String.class));
                    String purpose = nz(s.child("purpose").getValue(String.class));
                    String category = nz(s.child("category").getValue(String.class));
                    String subCategory = nz(s.child("subCategory").getValue(String.class));
                    String areaUnit = nz(s.child("areaSizeUnit").getValue(String.class));
                    String title = nz(s.child("title").getValue(String.class));
                    String description = nz(s.child("description").getValue(String.class));
                    String address = nz(s.child("address").getValue(String.class));
                    String profileImageBase64 = nz(s.child("profileImageBase64").getValue(String.class));

                    // بعض الإعلانات تخزنها "status" أو "Status"
                    String statusLower = nz(s.child("status").getValue(String.class));
                    String statusUpper = nz(s.child("Status").getValue(String.class));
                    propertyStatus = !statusLower.isEmpty() ? statusLower : statusUpper;

                    Log.d(TAG, "propertyStatus: " + propertyStatus);

                    // ===== أرقام (آمنة لأي نوع) =====
                    double price = asDouble(s.child("price").getValue());
                    long timestamp = asLong(s.child("timestamp").getValue());
                    long floors = asLong(s.child("floors").getValue());
                    long beds = asLong(s.child("bedRooms").getValue());
                    long baths = asLong(s.child("bathRooms").getValue());
                    double areaSize = asDouble(s.child("areaSize").getValue());
                    double lat = asDouble(s.child("latitude").getValue());
                    double lng = asDouble(s.child("longitude").getValue());

                    // خزّن قيم نحتاجها لاحقًا
                    sellerUid = uid;
                    propertyLatitude = lat;
                    propertyLongitude = lng;

                    // ===== تحكم الأزرار حسب المالك =====
                    if (!sellerUid.isEmpty() && sellerUid.equals(nz(firebaseAuth.getUid()))) {
                        // المعلن هو نفس المستخدم الحالي
                        binding.toolbarEditBtn.setVisibility(VISIBLE);
                        binding.toolbarDeleteBtn.setVisibility(VISIBLE);

                        //binding.chatBtn.setVisibility(GONE);
                        binding.callBtn.setVisibility(GONE);
                        binding.smsBtn.setVisibility(GONE);

                        binding.sellerPerfileCv.setVisibility(GONE);
                        binding.sellerProfileLabeTv.setVisibility(GONE);
                    } else {
                        // المعلن شخص ثاني
                        binding.toolbarEditBtn.setVisibility(GONE);
                        binding.toolbarDeleteBtn.setVisibility(GONE);

                       // binding.chatBtn.setVisibility(VISIBLE);
                        binding.callBtn.setVisibility(VISIBLE);
                        binding.smsBtn.setVisibility(VISIBLE);

                        binding.sellerPerfileCv.setVisibility(VISIBLE);
                        binding.sellerProfileLabeTv.setVisibility(VISIBLE);
                    }

                    // ===== Badge "مباع" =====
                    binding.soldCv.setVisibility(
                            eqi(propertyStatus, MyUtils.AD_STATUS_SOLD) ? VISIBLE : GONE
                    );

                    // ===== تعبئة الحقول العامة =====
                    binding.priceTv.setText(MyUtils.formatteCurrency(price));
                    binding.dateTv.setText(MyUtils.formatTimestampDate(timestamp));
                    binding.purposeTv.setText(purpose);
                    binding.categoryTv.setText(category);
                    binding.subCategoryTv.setText(subCategory);
                    binding.titleTv.setText(title);
                    binding.descriptionTv.setText(description);
                    binding.addressTv.setText(address);

                    // المساحة دايمًا نعرضها (حتى لو أرض)
                    binding.areasizeTv.setText(
                            getString(R.string.area_size) + " " + areaSize + " " + areaUnit
                    );

                    // ===== منطق إظهار/إخفاء غرف/حمامات/طوابق =====
                    // "قطع أراض" هي MyUtils.propertyTypes[1]
                    boolean isLand = eqi(category, MyUtils.propertyTypes[1]);

                    if (isLand) {
                        // إعلان أرض → ما نعرض غرف/حمام/طوابق
                        binding.floorsTv.setVisibility(GONE);
                        binding.bedsTv.setVisibility(GONE);
                        binding.bathroomsTv.setVisibility(GONE);
                    } else {
                        // إعلان منزل / تجاري → نظهرهم ونحط القيم
                        binding.floorsTv.setVisibility(VISIBLE);
                        binding.bedsTv.setVisibility(VISIBLE);
                        binding.bathroomsTv.setVisibility(VISIBLE);

                        binding.floorsTv.setText(getString(R.string.floors) + " " + floors);
                        binding.bedsTv.setText(getString(R.string.bedrooms) + " " + beds);
                        binding.bathroomsTv.setText(getString(R.string.bathrooms) + " " + baths);
                    }

                    // (اختياري) تحميل بيانات المعلن
                    if (!sellerUid.isEmpty()) loadSellerDetails();

                } catch (Exception e) {
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ممكن تضيف Toast هنا لو تحب تعرض خطأ التحميل
            }
        });
    }

    private void loadSellerDetails() {
        if (sellerUid == null || sellerUid.trim().isEmpty()) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(sellerUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String phoneCode = nz(s.child("phoneCode").getValue(String.class));
                String phoneNumber = nz(s.child("phoneNumber").getValue(String.class));
                String name = nz(s.child("name").getValue(String.class));
                String profileUrl = nz(s.child("profileImageBase64").getValue(String.class));
                long ts = asLong(s.child("timestamp").getValue());

                sellerPhone = phoneCode + phoneNumber;

                binding.sellerName.setText(name);
                binding.memberSinceTv.setText(MyUtils.formatTimestampDate(ts));

                Log.d(TAG, "Seller Name: " + name);
                Log.d(TAG, "Profile Image URL: " + profileUrl);

                // تحويل Base64 إلى Bitmap
                try {
                    if (!profileUrl.isEmpty()) {
                        byte[] decodedString = Base64.decode(profileUrl, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        binding.sellerProfileIv.setImageBitmap(decodedByte);
                    } else {
                        binding.sellerProfileIv.setImageResource(R.drawable.person_black);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding Base64 image", e);
                    binding.sellerProfileIv.setImageResource(R.drawable.person_black);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // تجاهل أو اعرض رسالة خطأ لو تحب
            }
        });
    }

    private void chickIsFavories() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(propertyId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favorite = snapshot.exists();
                        Log.d(TAG, "onDataChange: favorite: " + favorite);
                        if (favorite) {
                            binding.toolbarFavBtn.setImageResource(R.drawable.fav_yes_black);
                        } else {
                            binding.toolbarFavBtn.setImageResource(R.drawable.fav_no_black);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // لا شي
                    }
                });
    }

    private void loadPropertyImages() {
        imageSliderArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId).child("images")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        imageSliderArrayList.clear();

                        java.util.HashSet<String> seen = new java.util.HashSet<>();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                String url = ds.child("imageUrl").getValue(String.class);

                                if (url == null || url.trim().isEmpty() || "null".equalsIgnoreCase(url)) {
                                    String b64 = ds.child("imageBase64").getValue(String.class);
                                    if (b64 != null) url = b64.trim();
                                }

                                if (url == null || url.isEmpty()) continue;

                                // امنع التكرار
                                if (!seen.add(url)) continue;

                                ModelImageSlider m = new ModelImageSlider();
                                m.setImageUrl(url);

                                imageSliderArrayList.add(m);

                            } catch (Exception e) {
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        if (adapterImageSlider == null) {
                            adapterImageSlider = new AdapterImageSlider(PropertyDetailsActivity.this, imageSliderArrayList);
                            binding.imageSliderVp.setAdapter(adapterImageSlider);
                        } else {
                            adapterImageSlider.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // تقدر تضيف Toast لو تحب
                    }
                });
    }

    private void deleteProperty() {
        Log.d(TAG, "deleteProperty:Deleting " + propertyId);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Properties");

        reference.child(propertyId)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "onSuccess:Deleted " + propertyId);
                    MyUtils.toast(PropertyDetailsActivity.this, "Deleted...!");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onFailure:", e);
                    MyUtils.toast(PropertyDetailsActivity.this, "Failed to delete due to " + e.getMessage());
                });
    }
}
