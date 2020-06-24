package ua.baidala.speedbar;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import com.github.anastr.speedviewlib.RaySpeedometer;
import com.github.anastr.speedviewlib.components.Section;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Integer overLimitColor = getResources().getColor(R.color.error_bg_color, getApplicationContext().getTheme());

        RaySpeedometer speedometer = findViewById(R.id.raySpeedometer);
        speedometer.setTitleText("Ingredient");
        speedometer.clearSections();
        Section myNewSection1 = new Section(0f, .9f, Color.GREEN, speedometer.getWidth(), Section.Style.SQUARE);
        Section myNewSection2 = new Section(.9f, 1f, overLimitColor, speedometer.getWidth(), Section.Style.SQUARE);
        speedometer.setMaxSpeed(11300);
        speedometer.setSpeedLimit(11200);
        speedometer.addSections(myNewSection1, myNewSection2);

        speedometer.speedTo(11300, 4000);
    }
}
