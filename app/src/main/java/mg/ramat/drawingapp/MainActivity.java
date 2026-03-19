package mg.ramat.drawingapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import mg.ramat.drawingapp.views.DrawingView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DrawingView drawingView = new DrawingView(this);
        setContentView(drawingView);
    }
}