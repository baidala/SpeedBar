package ua.baidala.speedbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.github.anastr.speedviewlib.RaySpeedometer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RaySpeedometer speedometer = findViewById(R.id.raySpeedometer);
        speedometer.speedTo(50, 4000);
    }
}
