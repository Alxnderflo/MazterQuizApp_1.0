package sv.edu.itca.masterquizapp;

import android.app.Application;

import com.cloudinary.android.MediaManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Configurar Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // Configurar Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dbfk97qdp");
        config.put("api_key", "126584225936658");
        config.put("api_secret", "pPBDsuiBntzAknVLCNyjeilMHsY");
        MediaManager.init(this, config);
    }
}