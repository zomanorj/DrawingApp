package mg.ramat.drawingapp.data;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Rectangle extends Figure {

    public Rectangle(float sx, float sy, float ex, float ey, Paint paint) {
        super(sx, sy, ex, ey, paint);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(
                Math.min(startX, endX),
                Math.min(startY, endY),
                Math.max(startX, endX),
                Math.max(startY, endY),
                paint
        );
    }
}