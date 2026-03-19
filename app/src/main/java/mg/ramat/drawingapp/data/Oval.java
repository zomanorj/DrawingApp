package mg.ramat.drawingapp.data;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Oval extends Figure {

    public Oval(float sx, float sy, float ex, float ey, Paint paint) {
        super(sx, sy, ex, ey, paint);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawOval(
                Math.min(startX, endX),
                Math.min(startY, endY),
                Math.max(startX, endX),
                Math.max(startY, endY),
                paint
        );
    }
}