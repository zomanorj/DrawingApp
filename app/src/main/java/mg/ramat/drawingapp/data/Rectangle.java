package mg.ramat.drawingapp.data;

import android.graphics.Canvas;

public class Rectangle extends Figure {

    public Rectangle(
            float sx,
            float sy,
            float ex,
            float ey,
            int strokeColor,
            int fillColor,
            float strokeWidth
    ) {
        super(sx, sy, ex, ey, strokeColor, fillColor, strokeWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        if (hasVisibleFill()) {
            canvas.drawRect(getLeft(), getTop(), getRight(), getBottom(), fillPaint);
        }

        canvas.drawRect(getLeft(), getTop(), getRight(), getBottom(), strokePaint);
    }

    /**
     * Détecte si le point (x, y) touche ce rectangle.
     * — Si le fond est visible : tout l'intérieur est cliquable.
     * — Si le fond est transparent : seul le bord (contour) est cliquable.
     * Cette logique est cohérente avec Line et Oval.
     */
    @Override
    public boolean contains(float x, float y) {
        float tolerance = Math.max(strokeWidth / 2f, 12f);
        float left   = getLeft();
        float top    = getTop();
        float right  = getRight();
        float bottom = getBottom();

        // En dehors de la boîte englobante étendue → pas de collision
        if (x < left - tolerance || x > right + tolerance
                || y < top - tolerance || y > bottom + tolerance) {
            return false;
        }

        // Fond visible → l'intérieur entier est cliquable
        if (hasVisibleFill()) {
            return true;
        }

        // Sans fond → uniquement le bord (zone de largeur = tolerance)
        boolean surBordHorizontal = x >= left - tolerance && x <= right + tolerance
                && (y <= top + tolerance || y >= bottom - tolerance);
        boolean surBordVertical   = y >= top - tolerance && y <= bottom + tolerance
                && (x <= left + tolerance || x >= right - tolerance);

        return surBordHorizontal || surBordVertical;
    }

    @Override
    public Figure copy() {
        return new Rectangle(startX, startY, endX, endY, strokeColor, fillColor, strokeWidth);
    }
}
