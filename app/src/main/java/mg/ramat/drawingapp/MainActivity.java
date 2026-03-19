package mg.ramat.drawingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import mg.ramat.drawingapp.views.DrawingView;

public class MainActivity extends AppCompatActivity {

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMPORTANT : utiliser un layout XML maintenant
        setContentView(R.layout.activity_main);

        // récupérer le DrawingView depuis le XML
        drawingView = findViewById(R.id.drawingView);

        // récupérer boutons
        Button btnRed = findViewById(R.id.btnRed);
        Button btnBlue = findViewById(R.id.btnBlue);

        // actions
        btnRed.setOnClickListener(v -> drawingView.setColor(Color.RED));
        btnBlue.setOnClickListener(v -> drawingView.setColor(Color.BLUE));
    }
}