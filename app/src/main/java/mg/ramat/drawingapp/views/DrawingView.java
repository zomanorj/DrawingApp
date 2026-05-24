package mg.ramat.drawingapp.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;

import mg.ramat.drawingapp.R;
import mg.ramat.drawingapp.data.Figure;
import mg.ramat.drawingapp.editor.history.FigureHistoryManager;
import mg.ramat.drawingapp.editor.model.FigureFactory;
import mg.ramat.drawingapp.editor.model.FigureStyle;
import mg.ramat.drawingapp.editor.model.FigureType;
import mg.ramat.drawingapp.editor.model.ToolMode;

public class DrawingView extends View {

    private static final int MAX_HISTORY = 50;

    private final List<Figure> figures = new ArrayList<>();
    private final FigureHistoryManager historyManager = new FigureHistoryManager(MAX_HISTORY);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Figure currentFigure;
    private Figure selectedFigure;

    private FigureType figureType = FigureType.RECTANGLE;
    private ToolMode toolMode = ToolMode.DRAW;
    private FigureStyle currentStyle = new FigureStyle(Color.RED, Color.TRANSPARENT, 8f);

    private float lastTouchX;
    private float lastTouchY;
    private boolean movingSelection;
    private boolean moveStateSaved;

    private CanvasStateListener canvasStateListener;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(ContextCompat.getColor(getContext(), R.color.canvas_background));

        selectionPaint.setColor(ContextCompat.getColor(getContext(), R.color.brand_accent));
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(3f);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{16f, 10f}, 0f));
    }

    public void setFigureType(FigureType type) {
        figureType = type;
        notifyCanvasStateChanged();
    }

    public FigureType getFigureType() {
        return figureType;
    }

    public void setToolMode(ToolMode mode) {
        toolMode = mode;
        currentFigure = null;

        if (toolMode != ToolMode.SELECT) {
            selectedFigure = null;
        }

        notifyCanvasStateChanged();
        invalidate();
    }

    public ToolMode getToolMode() {
        return toolMode;
    }

    public void setStrokeColor(int color) {
        currentStyle = currentStyle.withStrokeColor(color);

        if (toolMode == ToolMode.SELECT && selectedFigure != null
                && selectedFigure.getStrokeColor() != color) {
            saveToHistory();
            selectedFigure.setStrokeColor(color);
        }

        notifyCanvasStateChanged();
        invalidate();
    }

    public int getCurrentStrokeColor() {
        return currentStyle.getStrokeColor();
    }

    public void setFillColor(int color) {
        currentStyle = currentStyle.withFillColor(color);

        if (toolMode == ToolMode.SELECT && selectedFigure != null
                && selectedFigure.getFillColor() != color) {
            saveToHistory();
            selectedFigure.setFillColor(color);
        }

        notifyCanvasStateChanged();
        invalidate();
    }

    public int getCurrentFillColor() {
        return currentStyle.getFillColor();
    }

    public void setStrokeWidth(float width) {
        currentStyle = currentStyle.withStrokeWidth(Math.max(1f, width));

        if (toolMode == ToolMode.SELECT
                && selectedFigure != null
                && selectedFigure.getStrokeWidth() != currentStyle.getStrokeWidth()) {
            saveToHistory();
            selectedFigure.setStrokeWidth(currentStyle.getStrokeWidth());
        }

        notifyCanvasStateChanged();
        invalidate();
    }

    public float getCurrentStrokeWidth() {
        return currentStyle.getStrokeWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Figure figure : figures) {
            figure.draw(canvas);
        }

        // BUG 1 FIX : currentFigure n'est plus dans figures pendant le tracé.
        // On le dessine ici comme aperçu en temps réel avant la validation (ACTION_UP).
        if (currentFigure != null) {
            currentFigure.draw(canvas);
        }

        if (selectedFigure != null) {
            drawSelection(canvas, selectedFigure);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;

                if (toolMode == ToolMode.ERASE) {
                    eraseFigureAt(x, y);
                    return true;
                }
                if (toolMode == ToolMode.SELECT) {
                    startSelection(x, y);
                    return true;
                }
                startDrawing(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (toolMode == ToolMode.SELECT) {
                    moveSelection(x, y);
                    return true;
                }
                if (currentFigure != null) {
                    currentFigure.setEnd(x, y);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // BUG 1 FIX : la figure est validée ici, après que le tracé est
                // terminé. Le snapshot est pris AVANT d'ajouter la figure : undo
                // restaure donc l'état sans elle — comportement correct.
                if (toolMode == ToolMode.DRAW && currentFigure != null) {
                    historyManager.saveSnapshot(figures);
                    figures.add(currentFigure);
                }

                currentFigure = null;
                movingSelection = false;
                moveStateSaved = false;
                // BUG 2 FIX : un seul appel à render() par geste, dans ACTION_UP.
                notifyCanvasStateChanged();
                invalidate();
                return true;

            default:
                return true;
        }
    }

    /**
     * Démarre le tracé : crée la figure en aperçu sans l'ajouter à la liste.
     * La validation (commit + snapshot) se fait dans ACTION_UP — c'est là que
     * le undo peut fonctionner correctement.
     */
    private void startDrawing(float x, float y) {
        selectedFigure = null;
        currentFigure = createFigure(x, y);
        invalidate();
    }

    private Figure createFigure(float x, float y) {
        return FigureFactory.create(figureType, x, y, currentStyle);
    }

    private void startSelection(float x, float y) {
        selectedFigure = findFigureAt(x, y);
        movingSelection = selectedFigure != null;
        moveStateSaved = false;
        syncStyleFromSelection();
        notifyCanvasStateChanged();
        invalidate();
    }

    /**
     * Déplace la figure sélectionnée.
     * BUG 2 FIX : on n'appelle plus notifyCanvasStateChanged() ici. Pendant un
     * déplacement, seul invalidate() est nécessaire. Le render() UI est déclenché
     * une seule fois dans ACTION_UP.
     */
    private void moveSelection(float x, float y) {
        if (!movingSelection || selectedFigure == null) {
            return;
        }

        float dx = x - lastTouchX;
        float dy = y - lastTouchY;

        if (dx == 0f && dy == 0f) {
            return;
        }

        if (!moveStateSaved) {
            saveToHistory();
            moveStateSaved = true;
        }

        selectedFigure.moveBy(dx, dy);
        lastTouchX = x;
        lastTouchY = y;
        invalidate();
    }

    private void eraseFigureAt(float x, float y) {
        Figure figureToErase = findFigureAt(x, y);

        if (figureToErase == null) {
            selectedFigure = null;
            notifyCanvasStateChanged();
            invalidate();
            return;
        }

        saveToHistory();
        figures.remove(figureToErase);
        selectedFigure = null;
        notifyCanvasStateChanged();
        invalidate();
    }

    /**
     * Sauvegarde l'état courant dans l'historique SANS déclencher de render().
     * BUG 2 FIX : l'appelant choisit lui-même quand notifier l'UI.
     */
    private void saveToHistory() {
        historyManager.saveSnapshot(figures);
    }

    public boolean undo() {
        List<Figure> previousState = historyManager.popSnapshot();
        if (previousState == null) {
            return false;
        }

        figures.clear();
        figures.addAll(previousState);

        selectedFigure = null;
        currentFigure = null;
        movingSelection = false;
        moveStateSaved = false;

        notifyCanvasStateChanged();
        invalidate();
        return true;
    }

    public boolean canUndo() {
        return historyManager.canUndo();
    }

    public void clearAll() {
        if (figures.isEmpty()) {
            return;
        }

        saveToHistory();
        figures.clear();
        selectedFigure = null;
        currentFigure = null;
        notifyCanvasStateChanged();
        invalidate();
    }

    public void setCanvasStateListener(CanvasStateListener listener) {
        canvasStateListener = listener;
    }

    /** Supprime le listener — à appeler dans onDestroy() pour éviter la fuite mémoire. */
    public void removeCanvasStateListener() {
        canvasStateListener = null;
    }

    private void notifyCanvasStateChanged() {
        if (canvasStateListener != null) {
            canvasStateListener.onCanvasStateChanged();
        }
    }

    public boolean deleteSelectedFigure() {
        if (selectedFigure == null) {
            return false;
        }

        saveToHistory();
        figures.remove(selectedFigure);
        selectedFigure = null;
        notifyCanvasStateChanged();
        invalidate();
        return true;
    }

    public boolean hasSelectedFigure() {
        return selectedFigure != null;
    }

    public Bitmap exportToBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(ContextCompat.getColor(getContext(), R.color.canvas_background));

        for (Figure figure : figures) {
            figure.draw(canvas);
        }

        return bitmap;
    }

    public boolean hasFigures() {
        return !figures.isEmpty();
    }

    private Figure findFigureAt(float x, float y) {
        for (int i = figures.size() - 1; i >= 0; i--) {
            Figure figure = figures.get(i);
            if (figure.contains(x, y)) {
                return figure;
            }
        }
        return null;
    }

    private void drawSelection(Canvas canvas, Figure figure) {
        float margin = 12f;
        canvas.drawRect(
                figure.getLeft() - margin,
                figure.getTop() - margin,
                figure.getRight() + margin,
                figure.getBottom() + margin,
                selectionPaint
        );
    }

    private void syncStyleFromSelection() {
        if (selectedFigure == null) {
            return;
        }

        currentStyle = new FigureStyle(
                selectedFigure.getStrokeColor(),
                selectedFigure.getFillColor(),
                selectedFigure.getStrokeWidth()
        );
    }
}
