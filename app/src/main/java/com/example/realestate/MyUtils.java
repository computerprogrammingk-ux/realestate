package com.example.realestate;

//import static androidx.appcompat.graphics.drawable.DrawableContainerCompat.Api21Impl.getResources;

import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.util.HashMap;

public class MyUtils {

//    public static final String AD_STATUS_AVAILABE = "AVAILABLE";
//    public static final String AD_STATUS_SOLD = "SOLD";
//    public static final String AD_STATUS_RENTED = "RENTED";
//    public static final String USER_TYPE_GOOGLE = "Google";
//    public static final String USER_TYPE_EMAIL = "Email";
//
//    public static String USER_TYPE_PHONE = "Phone";


//    public static final String[] propertyTypes = {"Homes", "Plots", "Commercial"};
//    public static final String[] propertyTypesHomes = {"House", "Flat", "Portion", "Lower Portion", "Farm House", "Room", "Penthouse"};
//    public static final String[] propertyTypesPlots = {"Residential Plot", "Commercial Plots", "Agricultual Plot", "Industrial Plot", "Plot File", "Plot Form"};
//    public static final String[] propertyTypesCommercial = {"Office", "Shop", "Warehouse", "Building", "other"};
//    public static final String[] propertyAreaSizeUnit = {"Square Feet", "Square Yards", "Square Meters", "Marla", "Kanal"};


    public static final String PROPERTY_PURPOSE_ANY = "أي";
    public static final String PROPERTY_PURPOSE_SELL = "بيع";
    public static final String PROPERTY_PURPOSE_RENT = "إيجار";

    public static final String AD_STATUS_AVAILABE = "متاح";
    public static final String AD_STATUS_SOLD = "مباع";
    public static final String AD_STATUS_RENTED = "مستأجر";

    public static final String USER_TYPE_GOOGLE = "جوجل";
    public static final String USER_TYPE_EMAIL = "بريد إلكتروني";

    public static String USER_TYPE_PHONE = "هاتف";

    public static final String[] propertyTypes = {"منازل", "قطع أراض", "تجاري"};
    public static final String[] propertyTypesHomes = {"منزل", "شقة", "جزء", "الجزء السفلي", "بيت ريفي", "غرفة", "الشقة العلوية"};
    public static final String[] propertyTypesPlots = {"قطعة أرض سكنية", "قطع أراض تجارية", "أرض زراعية", "أرض صناعية", "ملف قطعة أرض", "نموذج قطعة أرض"};
    public static final String[] propertyTypesCommercial = {"مكتب", "محل", "مستودع", "مبنى", "آخر"};
    public static final String[] propertyAreaSizeUnit = {"قدم مربع", "ياردة مربعة", "متر مربع", "مارلا", "كانال"};

//    public static final String PROPERTY_PURPOSE_ANY = "Any";
//    public static final String PROPERTY_PURPOSE_SELL = "Sell";
//    public static final String PROPERTY_PURPOSE_RENT = "Rent";
    public static final int MAX_DISTANCE_TO_LOAD_PROPERTIES=10;

    /**
     * A function
     *
     * @param context the context o
     * @param message the context o
     */
    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static long timestamp() {
        return System.currentTimeMillis();
    }

    /**
     * A function
     *
     * @param timestamp the context o
     */
    public static String formatTimestampDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        String date = DateFormat.format("dd/MM//yyyy", calendar.getTime()).toString();
        return date;
    }

    public static String formatteCurrency(Double price) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(price);
    }

    public static double calculateDistanceMK(double currentLatitude,double currentLongitude,double propertyLatitude,double propertyLongitude){


        Location startPoint=new Location(LocationManager.NETWORK_PROVIDER);
        startPoint.setLatitude(currentLatitude);
        startPoint.setLongitude(currentLongitude);


        Location sendPoint=new Location(LocationManager.NETWORK_PROVIDER);
        sendPoint.setLatitude(propertyLatitude);
        sendPoint.setLongitude(propertyLongitude);

        double distanceInMeters=startPoint.distanceTo(sendPoint);
        double distanceInKm=distanceInMeters/1000;

        return distanceInKm;


    }


    public static void addToFavorite(Context context, String propertyId) {
        FirebaseAuth fa = FirebaseAuth.getInstance();
        if (fa.getCurrentUser() == null) {
            toast(context, "You're not logged in!");
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(fa.getUid())
                .child("Favorites")
                .child(propertyId)
                .setValue(true) // ✅ Boolean بدل كائن
                .addOnSuccessListener(u -> toast(context, "Added to favorites!"))
                .addOnFailureListener(e -> toast(context, "Failed: " + e.getMessage()));
    }

    public static void removeFromFavorite(Context context, String propertyId) {
        FirebaseAuth fa = FirebaseAuth.getInstance();
        if (fa.getCurrentUser() == null) {
            toast(context, "You're not logged in!");
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(fa.getUid())
                .child("Favorites")
                .child(propertyId)
                .removeValue()
                .addOnSuccessListener(u -> toast(context, "Removed from favorites!"))
                .addOnFailureListener(e -> toast(context, "Failed: " + e.getMessage()));
    }


    public static void callIntent(Context context,String phoneNumber ){
        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + Uri.encode(phoneNumber)));
        context.startActivity(intent);

    }

    public static void smsIntent(Context context,String phoneNumber){
        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + Uri.encode(phoneNumber)));
        context.startActivity(intent);
    }


    public static void mapIntent(Context context,double latitude,double longitude){

        Uri mapIntentUrl=Uri.parse("https://maps.google.com/maps?daddr=" + latitude +"," + longitude);
        Intent intent=new Intent(Intent.ACTION_VIEW,mapIntentUrl);
        intent.setPackage("com.google.android.apps.maps");
        if(intent.resolveActivity(context.getPackageManager())!=null){
            context.startActivity(intent);
        }else{
            MyUtils.toast(context,"Google mao not installed!");
        }
    }

}

