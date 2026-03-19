package mg.ramat.drawingapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;
import mg.ramat.drawingapp.data.Figure;
import mg.ramat.drawingapp.data.Line;
import mg.ramat.drawingapp.data.Rectangle;
import mg.ramat.drawingapp.data.Oval;

public class DrawingView extends View {

    private  Paint paint;
    private  float startX, startY, endX, endY;
    private List<Figure> figures = new ArrayList<>();
    private Figure currentFigure = null;
    private int figureType = 2;


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

        for (Figure f : figures) {
            f.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                if (figureType == 1) {
                    currentFigure = new Line(x, y, x, y, new Paint(paint));
                } else if (figureType == 2) {
                    currentFigure = new Rectangle(x, y, x, y, new Paint(paint));
                } else {
                    currentFigure = new Oval(x, y, x, y, new Paint(paint));
                }

                figures.add(currentFigure);
                break;

            case MotionEvent.ACTION_MOVE:

                if (currentFigure != null) {
                    currentFigure.setEnd(x, y);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                currentFigure = null;
                break;
        }

        return true;
    }
}