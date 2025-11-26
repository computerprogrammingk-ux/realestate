package com.example.realestate.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.realestate.R;
import com.example.realestate.databinding.RowImageSliderBinding;
import com.example.realestate.models.ModelImageSlider;

import java.util.ArrayList;

public class AdapterImageSlider extends RecyclerView.Adapter<AdapterImageSlider.HolderImageSlider> {

    private static final String TAG = "IMAGE_SLIDER_TAG";
    private final Context context;
    private final ArrayList<ModelImageSlider> imageSliderArrayList;

    public AdapterImageSlider(Context context, ArrayList<ModelImageSlider> imageSliderArrayList) {
        this.context = context;
        this.imageSliderArrayList = imageSliderArrayList;
    }

    @NonNull
    @Override
    public HolderImageSlider onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowImageSliderBinding itemBinding = RowImageSliderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new HolderImageSlider(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderImageSlider holder, int position) {
        ModelImageSlider modelImageSlider = imageSliderArrayList.get(position);
        String imageUrl = modelImageSlider.getImageUrl();
        String imageCount = (position + 1) + "/" + imageSliderArrayList.size();
        holder.binding.imageCountTv.setText(imageCount);

        try {
            if (imageUrl != null &&
                    (imageUrl.startsWith("/9j/") || imageUrl.startsWith("iVBORw0KGgo"))) {
                // this is Base64 (JPEG starts بـ /9j/ ، والـ PNG يبدأ iVBORw0K)
                byte[] bytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT);
                Glide.with(context)
                        .asBitmap()
                        .load(bytes)
                        .placeholder(R.drawable.image_gray)
                        .error(R.drawable.image_gray)
                        .into(holder.binding.imageIv);
            } else {
                // this is a normal https url
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.image_gray)
                        .error(R.drawable.image_gray)
                        .into(holder.binding.imageIv);
            }
        } catch (Exception e) {
            Log.e(TAG, "onBindViewHolder: ", e);
            holder.binding.imageIv.setImageResource(R.drawable.image_gray);
        }
    }


//    @Override
//    public void onBindViewHolder(@NonNull HolderImageSlider holder, int position) {
//        ModelImageSlider m = imageSliderArrayList.get(position);
//        String imageUrl = m.getImageUrl() == null ? "" : m.getImageUrl().trim();
//
//        // عدّاد الصور 1/N
//        String count = (position + 1) + "/" + imageSliderArrayList.size();
//        holder.binding.imageCountTv.setText(count);
//
//        try {
//            Glide.with(context)
//                    .load(imageUrl.isEmpty() ? null : imageUrl)
//                    .placeholder(R.drawable.image_gray)
//                    .error(R.drawable.image_gray)
//                    .into(holder.binding.imageIv);
//        } catch (Exception e) {
//            Log.e(TAG, "onBindViewHolder load error", e);
//            holder.binding.imageIv.setImageResource(R.drawable.image_gray);
//        }
//    }

    @Override
    public int getItemCount() {
        return imageSliderArrayList.size();
    }

    static class HolderImageSlider extends RecyclerView.ViewHolder {
        final RowImageSliderBinding binding;
        HolderImageSlider(@NonNull RowImageSliderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
