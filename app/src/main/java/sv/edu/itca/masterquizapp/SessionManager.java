package sv.edu.itca.masterquizapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "QuizAppSession";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_PROFESSOR_CODE = "professor_code";
    private static final String KEY_USER_ID = "user_id";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Guardar solo rol (para estudiantes y profesores sin código)
    public void saveUserRole(String userId, String role) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    // Guardar código de profesor
    public void saveProfessorCode(String code) {
        editor.putString(KEY_PROFESSOR_CODE, code);
        editor.apply();
    }

    // Obtener datos
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    public String getProfessorCode() {
        return sharedPreferences.getString(KEY_PROFESSOR_CODE, null);
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    // Verificar si hay sesión guardada
    public boolean hasSessionData() {
        return sharedPreferences.getString(KEY_USER_ROLE, null) != null;
    }

    // Limpiar sesión (logout)
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}