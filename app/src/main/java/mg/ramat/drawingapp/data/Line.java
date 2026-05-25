package mg.ramat.drawingapp.data;

import android.graphics.Canvas;

public class Line extends Figure {

    public Line(float sx, float sy, float ex, float ey, int strokeColor, int fillColor, float strokeWidth) {
        super(sx, sy, ex, ey, strokeColor, fillColor, strokeWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawLine(startX, startY, endX, endY, strokePaint);
    }

    @Override
    public boolean contains(float x, float y) {
        // On calcule la distance entre le doigt et le segment.
        float dx = endX - startX;
        float dy = endY - startY;
        float segmentLengthSquared = dx * dx + dy * dy;
        float tolerance = Math.max(strokeWidth, 18f);

        if (segmentLengthSquared == 0f) {
            float pointDx = x - startX;
            float pointDy = y - startY;
            return pointDx * pointDx + pointDy * pointDy <= tolerance * tolerance;
        }

        float projection = ((x - startX) * dx + (y - startY) * dy) / segmentLengthSquared;
        float clampedProjection = Math.max(0f, Math.min(1f, projection));

        float closestX = startX + clampedProjection * dx;
        float closestY = startY + clampedProjection * dy;

        float distanceX = x - closestX;
        float distanceY = y - closestY;

        return distanceX * distanceX + distanceY * distanceY <= tolerance * tolerance;
    }

    @Override
    public Figure copy() {
        return new Line(startX, startY, endX, endY, strokeColor, fillColor, strokeWidth);
    }
}
