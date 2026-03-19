package mg.ramat.drawingapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
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
    private int currentColor = Color.RED;
    private float currentStrokeWidth = 8f;

    public void setFigureType(int type) {
        this.figureType = type;
    }

    public void setColor(int color) {
        this.currentColor = color;
        paint.setColor(color);
    }

    public void setStrokeWidth(float width) {
        this.currentStrokeWidth = width;
        paint.setStrokeWidth(width);
    }


    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(Color.WHITE);

        paint = new Paint();
        paint.setColor(currentColor);
        paint.setStrokeWidth(currentStrokeWidth);
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