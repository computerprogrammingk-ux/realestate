// App.java
package com.example.realestate;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // تفعيل تخزين محلي — يعرض البيانات فوراً من الديسك حتى قبل الشبكة
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
