package mg.ramat.drawingapp.data;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Line extends Figure {

    public Line(float sx, float sy, float ex, float ey, Paint paint) {
        super(sx, sy, ex, ey, paint);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawLine(startX, startY, endX, endY, paint);
    }
}