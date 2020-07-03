package ua.baidala.speedbar;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import com.github.anastr.speedviewlib.DistributionSpeedometer;
import com.github.anastr.speedviewlib.IngredientSpeedometer;
import com.github.anastr.speedviewlib.RaySpeedometer;
import com.github.anastr.speedviewlib.components.Section;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int overLimitColor = getResources().getColor(R.color.error_bg_color, getApplicationContext().getTheme());

        DistributionSpeedometer speedometer = findViewById(R.id.raySpeedometer);
        speedometer.setTitleText("Ingredient");

        speedometer.clearSections();
        Section myNewSection2 = new Section(.1077f, 1f, Color.GREEN, speedometer.getWidth(), Section.Style.SQUARE);
        Section myNewSection1 = new Section(0f, .1077f, overLimitColor, speedometer.getWidth(), Section.Style.SQUARE);
        speedometer.addSections(myNewSection1, myNewSection2);
        float min = 12607 - (12607 / 89.23f * 100f);
        speedometer.setSpeedLimits(min, 12607, 12607);
        speedometer.setSpeedAt(5000);

//        speedometer.speedTo(0, 4000);
        speedometer.speedTo(-500, 4000);
    }
}
