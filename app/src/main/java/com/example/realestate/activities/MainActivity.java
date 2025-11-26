package com.example.realestate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.realestate.MyUtils;
import com.example.realestate.R;
import com.example.realestate.databinding.ActivityMainBinding;
import com.example.realestate.fragments.ChatsListFragment;
import com.example.realestate.fragments.FavoritListFragment;
import com.example.realestate.fragments.HomeFragment;
import com.example.realestate.fragments.ProfileFragment;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {


    private ActivityMainBinding binding;

    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);

        binding=ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        firebaseAuth=FirebaseAuth.getInstance();
        showHomeFragment();
        if(firebaseAuth.getCurrentUser()==null){
            startLoginOptionsActivity();

        }

        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
             int itemId=menuItem.getItemId();
             if(itemId== R.id.item_home){
                 showHomeFragment();

                 return true;
             }
//             else if (itemId==R.id.item_cahts) {
//
//                 if(firebaseAuth.getCurrentUser()==null){
//                     MyUtils.toast(MainActivity.this,"Login Required...!");
//                     return false;
//
//                 }else{
//                    // showChatsFragment();
//                     return true;
//                 }
             //}
             else if (itemId==R.id.item_profile) {

                 if(firebaseAuth.getCurrentUser()==null){
                     MyUtils.toast(MainActivity.this,"Login Required...!");
                     return false;
                 }else{
                     showProfileFragment();
                     return true;
                 }

             }
             else if (itemId==R.id.item_favorite) {


                 if(firebaseAuth.getCurrentUser()==null){
                     MyUtils.toast(MainActivity.this,"Login Required...!");
                     return false;
                 }else{
                     showFavoritListFragment();
                     return true;
                 }
             }

             else{
                 return false;
             }

            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }



    private  void showHomeFragment(){
        binding.toolbarTitle.setText(getString(R.string.home));
        HomeFragment hf=new HomeFragment();
        FragmentTransaction  ft=getSupportFragmentManager().beginTransaction();
      ft.replace(binding.framentsFl.getId(),hf,"HomeFragment");
      ft.commit();

    }
    private  void showChatsFragment(){
        binding.toolbarTitle.setText(getString(R.string.chat));
        ChatsListFragment cf=new ChatsListFragment();
        FragmentTransaction  ft=getSupportFragmentManager().beginTransaction();
      ft.replace(binding.framentsFl.getId(),cf,"ChatsListFragment");
      ft.commit();

    }
    private  void showFavoritListFragment(){
        binding.toolbarTitle.setText(getString(R.string.favorites));
       FavoritListFragment ff=new FavoritListFragment();
        FragmentTransaction  ft=getSupportFragmentManager().beginTransaction();
      ft.replace(binding.framentsFl.getId(),ff,"FavoritListFragment");
      ft.commit();

    } private  void showProfileFragment(){
        binding.toolbarTitle.setText(getString(R.string.profile));
      ProfileFragment pf=new ProfileFragment();
        FragmentTransaction  ft=getSupportFragmentManager().beginTransaction();
      ft.replace(binding.framentsFl.getId(),pf,"ProfileFragment");
      ft.commit();

    }

    private void startLoginOptionsActivity() {
        startActivity(new Intent(this, LoginOptionsActivity.class));
    }
}