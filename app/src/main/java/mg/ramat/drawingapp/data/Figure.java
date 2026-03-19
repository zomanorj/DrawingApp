package mg.ramat.drawingapp.data;

import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class Figure {

    protected float startX, startY, endX, endY;
    protected Paint paint;

    public Figure(float sx, float sy, float ex, float ey, Paint paint) {
        this.startX = sx;
        this.startY = sy;
        this.endX = ex;
        this.endY = ey;
        this.paint = paint;
    }

    public void setEnd(float x, float y) {
        this.endX = x;
        this.endY = y;
    }

    public abstract void draw(Canvas canvas);
}