package sv.edu.itca.masterquizapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import sv.edu.itca.masterquizapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private ViewPager2 viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        configViewPager();
        configBottomNavigation();

    }

    private void configViewPager() {
        viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        //Agregar fragmentos en orden
        adapter.addFragment(new HomeFragment());      // Posición 0
        adapter.addFragment(new ScoreFragment());  // Posición 1
        adapter.addFragment(new TeacherFragment());   // Posición 2

        viewPager.setAdapter(adapter);
    }

    private void configBottomNavigation() {

        binding.btnNavView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.Home) {
                viewPager.setCurrentItem(0, true);
            } else if (item.getItemId() == R.id.Score) {
                viewPager.setCurrentItem(1, true);
            } else if (item.getItemId() == R.id.Teacher) {
                viewPager.setCurrentItem(2, true);
            }
            return true;
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                switch (position) {
                    case 0:
                        binding.btnNavView.setSelectedItemId(R.id.Home);
                        break;
                    case 1:
                        binding.btnNavView.setSelectedItemId(R.id.Score);
                        break;
                    case 2:
                        binding.btnNavView.setSelectedItemId(R.id.Teacher);
                        break;
                }
            }
        });

    }


}