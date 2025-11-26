package com.example.realestate.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.activities.PropertyDetailsActivity;
import com.example.realestate.databinding.RowPropertyBinding;
import com.example.realestate.models.ModelProperty;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdapterProperty extends RecyclerView.Adapter<AdapterProperty.HolderProperty> implements Filterable {

    private static final String TAG = "PROPERTY_TAG";
    private final Context context;
    private final ArrayList<ModelProperty> propertyArrayList; // القائمة المعروضة
    private final ArrayList<ModelProperty> propertyListOriginal; // القائمة الأصلية للفلترة
    private final FirebaseAuth firebaseAuth;
    private Filter filter;

    public AdapterProperty(Context context, ArrayList<ModelProperty> propertyArrayList) {
        this.context = context;
        this.propertyArrayList = propertyArrayList; // القائمة المعروضة
        // هذه هي القائمة الأصلية التي نعتمد عليها في البحث
        this.propertyListOriginal = new ArrayList<>(propertyArrayList);
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderProperty onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowPropertyBinding binding = RowPropertyBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new HolderProperty(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderProperty holder, int position) {
        ModelProperty m = propertyArrayList.get(position);

        Double price = m.getPrice() != null ? m.getPrice() : 0.0;
        Long   ts    = m.getTimestamp() != null ? m.getTimestamp() : 0L;

        String title    = nz(m.getTitle());
        String desc     = nz(m.getDescription());
        String addr     = nz(m.getAddress());
        String purpose  = nz(m.getPurpose());
        String cat      = nz(m.getCategory());
        String subCat   = nz(m.getSubCategory());

        String formattedPrice = MyUtils.formatteCurrency(price);
        String formattedDate  = MyUtils.formatTimestampDate(ts);
        String propertyId     = m.getId();

        holder.binding.titleTv.setText(title);
        holder.binding.descriptionTv.setText(desc);
        holder.binding.purposeTv.setText(purpose);
        holder.binding.categoryTv.setText(cat);
        holder.binding.subCategoryTv.setText(subCat);
        holder.binding.addressTv.setText(addr);
        holder.binding.dateTv.setText(formattedDate);
        holder.binding.priceTv.setText(formattedPrice);

        // ✅ صفّر الصورة أولاً لمنع بقايا من ViewHolder قديم
        holder.binding.propertyIv.setImageResource(R.drawable.buliding_asset01);
        // ✅ حمّل من Base64 فقط وبشكل آمن
        loadPropertyFirstImage(m, holder);

        // حالة المفضلة الافتراضية
        holder.binding.favoriteBtn.setImageResource(R.drawable.fav_no_black);

        // فحص حالة المفضلة
        if (firebaseAuth.getCurrentUser() != null && propertyId != null && !propertyId.isEmpty()) {
            checkIsFavorite(propertyId, holder);
        }

        // قلب المفضلة
        holder.binding.favoriteBtn.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                MyUtils.toast(context, "Please login to use favorites");
                return;
            }
            if (propertyId == null || propertyId.isEmpty()) return;

            DatabaseReference favRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(firebaseAuth.getUid())
                    .child("Favorites")
                    .child(propertyId);

            favRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot s) {
                    if (s.exists()) {
                        // لا نغير الأيقونة هنا، بل نعتمد على ChildEventListener في Fragment
                        favRef.removeValue();
                    } else {
                        // لا نغير الأيقونة هنا
                        favRef.setValue(true); // نخزن boolean
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { }
            });
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(context, PropertyDetailsActivity.class);
                intent.putExtra("propertyId", m.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return propertyArrayList.size();
    }

    // ✅ تحميل أول صورة (Base64) بأمان — يمنع ENOENT/null
    private void loadPropertyFirstImage(@NonNull ModelProperty modelProperty, @NonNull HolderProperty holder) {
        String propertyId = modelProperty.getId();
        if (propertyId == null || propertyId.trim().isEmpty()) {
            // نخلي الـ placeholder اللي ضبطناه فوق
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId)
                .child("images")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String b64 = null;

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            b64 = ds.child("imageBase64").getValue(String.class);
                            break; // أول صورة فقط
                        }

                        if (b64 == null) return;
                        String trimmed = b64.trim();
                        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) return;

                        try {
                            byte[] bytes = Base64.decode(trimmed, Base64.DEFAULT);
                            if (bytes == null || bytes.length == 0) return;

                            Glide.with(context)
                                    .asBitmap()
                                    .load(bytes) // من bytes فقط (لا Uri محلي)
                                    .placeholder(R.drawable.buliding_asset01)
                                    .error(R.drawable.buliding_asset01)
                                    .fallback(R.drawable.buliding_asset01)
                                    .into(holder.binding.propertyIv);

                        } catch (Exception e) {
                            Log.e(TAG, "decode/load error", e);
                            // نترك الـ placeholder
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadPropertyFirstImage cancelled: " + error.getMessage());
                        // نترك الـ placeholder
                    }
                });
    }

    private void checkIsFavorite(@NonNull String propertyId, @NonNull HolderProperty holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(firebaseAuth.getUid())
                .child("Favorites")
                .child(propertyId);

        // استخدم addValueEventListener لضمان تحديث الأيقونة فورًا عند التغيير
        ref.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean favorite = snapshot.exists();
                holder.binding.favoriteBtn.setImageResource(
                        favorite ? R.drawable.fav_yes_black : R.drawable.fav_no_black
                );
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private static String nz(String s){ return s==null ? "" : s; }
    private static String nzLower(String s){ return s==null ? "" : s.toLowerCase(); }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    String q = (constraint == null) ? "" : constraint.toString().toLowerCase().trim();
                    ArrayList<ModelProperty> filteredList = new ArrayList<>();

                    if (q.isEmpty()) {
                        filteredList.addAll(propertyListOriginal); // ✅ ارجع الكل
                    } else {
                        for (ModelProperty p : propertyListOriginal) {
                            String t  = nzLower(p.getTitle());
                            String d  = nzLower(p.getDescription());
                            String c  = nzLower(p.getCategory());
                            String sc = nzLower(p.getSubCategory());
                            String a  = nzLower(p.getAddress());

                            if (t.contains(q) || d.contains(q) || c.contains(q) || sc.contains(q) || a.contains(q)) {
                                filteredList.add(p); // ✅ أضف للنتائج الصحيحة
                            }
                        }
                    }

                    FilterResults res = new FilterResults();
                    res.values = filteredList;
                    res.count  = filteredList.size();
                    return res;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    propertyArrayList.clear();
                    if (results.values != null) {
                        propertyArrayList.addAll((ArrayList<ModelProperty>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
        return filter;
    }

    static class HolderProperty extends RecyclerView.ViewHolder {
        final RowPropertyBinding binding;
        HolderProperty(@NonNull RowPropertyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // لتحديث بيانات المحول من خارج (Home/Favorites..)
    // هذه الدالة مهمة لتحديث القائمة الأصلية والاحتياطية معًا
//    public void updateData(ArrayList<ModelProperty> newList) {
//        propertyArrayList.clear();
//        propertyArrayList.addAll(newList);
//        propertyListOriginal.clear();
//        propertyListOriginal.addAll(newList);
//        notifyDataSetChanged();
//    }


    public void updateData(ArrayList<ModelProperty> newList) {
        // اعمل نسخة سطحية من newList حتى نتجنب مسح المصدر إذا كان نفس المرجع
        ArrayList<ModelProperty> copy = new ArrayList<>(newList);

        // افرغ القوائم داخل الـ Adapter ثم املأها من النسخة
        propertyArrayList.clear();
        propertyArrayList.addAll(copy);

        propertyListOriginal.clear();
        propertyListOriginal.addAll(copy);

        notifyDataSetChanged();
    }







}
