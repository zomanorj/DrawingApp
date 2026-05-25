package mg.ramat.drawingapp.editor.model;

public final class FigureStyle {

    private final int strokeColor;
    private final int fillColor;
    private final float strokeWidth;

    public FigureStyle(int strokeColor, int fillColor, float strokeWidth) {
        this.strokeColor = strokeColor;
        this.fillColor = fillColor;
        this.strokeWidth = strokeWidth;
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

    public FigureStyle withStrokeColor(int color) {
        return new FigureStyle(color, fillColor, strokeWidth);
    }

    public FigureStyle withFillColor(int color) {
        return new FigureStyle(strokeColor, color, strokeWidth);
    }

    public FigureStyle withStrokeWidth(float width) {
        return new FigureStyle(strokeColor, fillColor, width);
    }
}
