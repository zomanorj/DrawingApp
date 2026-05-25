package mg.ramat.drawingapp.data;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public abstract class Figure {

    protected float startX;
    protected float startY;
    protected float endX;
    protected float endY;

    protected final Paint strokePaint;
    protected final Paint fillPaint;

    protected int strokeColor;
    protected int fillColor;
    protected float strokeWidth;

    public Figure(
            float sx,
            float sy,
            float ex,
            float ey,
            int strokeColor,
            int fillColor,
            float strokeWidth
    ) {
        this.startX = sx;
        this.startY = sy;
        this.endX = ex;
        this.endY = ey;
        this.strokeColor = strokeColor;
        this.fillColor = fillColor;
        this.strokeWidth = strokeWidth;

        strokePaint = createBasePaint();
        fillPaint = createBasePaint();

        strokePaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStyle(Paint.Style.FILL);

        applyStyle();
    }

    private Paint createBasePaint() {
        Paint localPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        localPaint.setStrokeCap(Paint.Cap.ROUND);
        localPaint.setStrokeJoin(Paint.Join.ROUND);
        return localPaint;
    }

    protected void applyStyle() {
        strokePaint.setColor(strokeColor);
        strokePaint.setStrokeWidth(strokeWidth);
        fillPaint.setColor(fillColor);
    }

    public void setEnd(float x, float y) {
        endX = x;
        endY = y;
    }

    public void moveBy(float dx, float dy) {
        startX += dx;
        endX += dx;
        startY += dy;
        endY += dy;
    }

    public void setStrokeColor(int color) {
        strokeColor = color;
        strokePaint.setColor(color);
    }

    public void setFillColor(int color) {
        fillColor = color;
        fillPaint.setColor(color);
    }

    public void setStrokeWidth(float width) {
        strokeWidth = width;
        strokePaint.setStrokeWidth(width);
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public int getFillColor() {
        return fillColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    protected boolean hasVisibleFill() {
        return fillColor != Color.TRANSPARENT;
    }

    public abstract void draw(Canvas canvas);

    public abstract boolean contains(float x, float y);

    public abstract Figure copy();

    public float getLeft() {
        return Math.min(startX, endX);
    }

    public float getTop() {
        return Math.min(startY, endY);
    }

    public float getRight() {
        return Math.max(startX, endX);
    }

    public float getBottom() {
        return Math.max(startY, endY);
    }

    public RectF getBounds() {
        return new RectF(getLeft(), getTop(), getRight(), getBottom());
    }
}
