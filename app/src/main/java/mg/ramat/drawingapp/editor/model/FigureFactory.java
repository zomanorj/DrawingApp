package mg.ramat.drawingapp.editor.model;

import mg.ramat.drawingapp.data.Figure;
import mg.ramat.drawingapp.data.Line;
import mg.ramat.drawingapp.data.Oval;
import mg.ramat.drawingapp.data.Rectangle;

public final class FigureFactory {

    private FigureFactory() {
    }

    public static Figure create(FigureType figureType, float startX, float startY, FigureStyle style) {
        if (figureType == FigureType.LINE) {
            return new Line(
                    startX,
                    startY,
                    startX,
                    startY,
                    style.getStrokeColor(),
                    style.getFillColor(),
                    style.getStrokeWidth()
            );
        }

        if (figureType == FigureType.OVAL) {
            return new Oval(
                    startX,
                    startY,
                    startX,
                    startY,
                    style.getStrokeColor(),
                    style.getFillColor(),
                    style.getStrokeWidth()
            );
        }

        return new Rectangle(
                startX,
                startY,
                startX,
                startY,
                style.getStrokeColor(),
                style.getFillColor(),
                style.getStrokeWidth()
        );
    }
}
