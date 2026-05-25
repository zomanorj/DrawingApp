package mg.ramat.drawingapp.data;

import android.graphics.Canvas;

public class Oval extends Figure {

    public Oval(float sx, float sy, float ex, float ey, int strokeColor, int fillColor, float strokeWidth) {
        super(sx, sy, ex, ey, strokeColor, fillColor, strokeWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        if (hasVisibleFill()) {
            canvas.drawOval(getLeft(), getTop(), getRight(), getBottom(), fillPaint);
        }

        canvas.drawOval(getLeft(), getTop(), getRight(), getBottom(), strokePaint);
    }

    @Override
    public boolean contains(float x, float y) {
        float cx = (getLeft() + getRight()) / 2f;
        float cy = (getTop() + getBottom()) / 2f;
        float radiusX = (getRight() - getLeft()) / 2f;
        float radiusY = (getBottom() - getTop()) / 2f;

        if (radiusX == 0f || radiusY == 0f) {
            return false;
        }

        float tolerance = Math.max(strokeWidth, 12f);
        float expandedRadiusX = radiusX + tolerance;
        float expandedRadiusY = radiusY + tolerance;

        float dx = (x - cx) / expandedRadiusX;
        float dy = (y - cy) / expandedRadiusY;

        return dx * dx + dy * dy <= 1f;
    }

    @Override
    public Figure copy() {
        return new Oval(startX, startY, endX, endY, strokeColor, fillColor, strokeWidth);
    }
}
