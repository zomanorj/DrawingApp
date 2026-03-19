package mg.ramat.drawingapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private  Paint paint;
    private  float startX, startY, endX, endY;
    private List<float[]> lines = new ArrayList<>();

    public DrawingView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setBackgroundColor(Color.WHITE);

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Dessiner les anciennes lignes
        for (float[] line : lines) {
            canvas.drawLine(line[0], line[1], line[2], line[3], paint);
        }

        // Dessiner la ligne actuelle
        canvas.drawLine(startX, startY, endX, endY, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                endX = x;
                endY = y;
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                // On sauvegarde la ligne
                lines.add(new float[]{startX, startY, endX, endY});
                break;
        }

        return true;
    }
}