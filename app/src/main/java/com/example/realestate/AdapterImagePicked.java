package com.example.realestate;

//package com.example.realestate;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.realestate.databinding.RowImagesPickedBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class AdapterImagePicked extends RecyclerView.Adapter<AdapterImagePicked.HolderImagePicked> {

    private final Context context;
    private final ArrayList<ModelImagePicked> items;
    private final LayoutInflater inflater;
    private String propertyId;

    public AdapterImagePicked(Context context, ArrayList<ModelImagePicked> items,String propertyId) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
        this.propertyId=propertyId;
    }

    @NonNull
    @Override
    public HolderImagePicked onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowImagesPickedBinding binding = RowImagesPickedBinding.inflate(inflater, parent, false);
        return new HolderImagePicked(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderImagePicked holder, int position) {

        ModelImagePicked model = items.get(position);
        Uri imageUri = model.getImageUri();


        if(model.isFromInternet()){
            String imageUrl=model.imageUrl;
            Log.d(TAG,"onBindViewHolder: imageUrl:"+imageUrl);
         try {
             Glide.with(context)
                     .load(imageUrl).placeholder(R.drawable.image_gray)
                     .into(holder.binding.imageIv);
         }catch (Exception e){
             Log.e(TAG,"onBindViewHolder: ",e);
         }

        }else{

          try {
              Glide.with(holder.itemView)
                      .load(imageUri)
                      .centerCrop()
                      .into(holder.binding.imageIv);

              holder.binding.closeBtn.setOnClickListener(v -> {
                  int pos = holder.getBindingAdapterPosition();
                  if (pos != RecyclerView.NO_POSITION) {
                      items.remove(pos);
                      notifyItemRemoved(pos);
                  }
              });
          } catch (Exception e) {

              Log.e(TAG,"onBindViewHolder: ",e);
          }

        }

        holder.binding.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(model.isFromInternet()){
                    deleteImageFirebase(model,position);
                }else {
                    items.remove(model);
                    notifyItemRemoved(position);

                }
            }
        });


    }

    private void deleteImageFirebase(ModelImagePicked modelImagePicked,int position){

        String imageId=modelImagePicked.getId();
        Log.d(TAG,"deleteImageFirebase: properyId: "+ propertyId);
        Log.d(TAG,"deleteImageFirebase: imageId: "+ imageId);

        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Properties");

        ref.child(propertyId).child("images").child(imageId)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"onSuccess:");
                      MyUtils.toast(context,"Image deleted...");

                      try {
                          items.remove(modelImagePicked);
                          notifyItemRemoved(position);
                      }catch (Exception e){
                          Log.e(TAG,"onSuccess:",e);
                          MyUtils.toast(context,"Failed to delete due to "+e.getMessage());


                      }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"onFailure:",e);

                    }
                });

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HolderImagePicked extends RecyclerView.ViewHolder {
        final RowImagesPickedBinding binding;

        HolderImagePicked(@NonNull RowImagesPickedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
