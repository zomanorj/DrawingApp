package mg.ramat.drawingapp.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;

import mg.ramat.drawingapp.R;
import mg.ramat.drawingapp.data.Figure;
import mg.ramat.drawingapp.editor.history.CanvasSnapshot;
import mg.ramat.drawingapp.editor.history.FigureHistoryManager;
import mg.ramat.drawingapp.editor.model.FigureFactory;
import mg.ramat.drawingapp.editor.model.FigureStyle;
import mg.ramat.drawingapp.editor.model.FigureType;
import mg.ramat.drawingapp.editor.model.ToolMode;

public class DrawingView extends View {

    // ── Constantes ─────────────────────────────────────────────────────────
    private static final int   MAX_HISTORY             = 50;
    private static final float ERASER_WIDTH_MULTIPLIER = 3f;
    private static final float ERASER_MIN_WIDTH_PX     = 30f;
    private static final float SELECTION_MARGIN        = 12f;
    private static final float HANDLE_HALF_SIZE        = 10f;

    // ── État du canvas ──────────────────────────────────────────────────────
    private final List<Figure> figures       = new ArrayList<>();
    private final List<Path>   eraserStrokes = new ArrayList<>();

    private Figure currentFigure;
    private Figure selectedFigure;
    private Path   currentEraserStroke;

    // ── Outils actifs ───────────────────────────────────────────────────────
    private FigureType  figureType   = FigureType.RECTANGLE;
    private ToolMode    toolMode     = ToolMode.DRAW;
    private FigureStyle currentStyle = new FigureStyle(Color.RED, Color.TRANSPARENT, 8f);

    // ── État du toucher ─────────────────────────────────────────────────────
    private float   lastTouchX;
    private float   lastTouchY;
    private boolean movingSelection;
    private boolean moveStateSaved;

    // ── Historique ──────────────────────────────────────────────────────────
    private final FigureHistoryManager historyManager = new FigureHistoryManager(MAX_HISTORY);

    // ── Rendu bitmap intermédiaire ──────────────────────────────────────────
    /** Bitmap off-screen sur lequel on applique PorterDuff.CLEAR pour la gomme. */
    private Bitmap mDrawingBitmap;
    private Canvas mDrawingCanvas;
    private int    canvasBackgroundColor;

    // ── Paints ─────────────────────────────────────────────────────────────
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eraserPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Listener ────────────────────────────────────────────────────────────
    private CanvasStateListener canvasStateListener;

    // ═══════════════════════════════════════════════════════════════════════
    // Constructeurs
    // ═══════════════════════════════════════════════════════════════════════

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Obligatoire pour que PorterDuff.Mode.CLEAR fonctionne sur le bitmap intermédiaire.
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        canvasBackgroundColor = ContextCompat.getColor(getContext(), R.color.canvas_background);

        // Bordure de sélection pointillée
        selectionPaint.setColor(ContextCompat.getColor(getContext(), R.color.brand_accent));
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(3f);
        selectionPaint.setPathEffect(new DashPathEffect(new float[]{16f, 10f}, 0f));

        // Poignées de coin (carrés blancs)
        handlePaint.setColor(Color.WHITE);
        handlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        handlePaint.setStrokeWidth(2f);

        // Gomme : mode CLEAR = efface les pixels jusqu'à la transparence dans le bitmap
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraserPaint.setStyle(Paint.Style.STROKE);
        eraserPaint.setStrokeCap(Paint.Cap.ROUND);
        eraserPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cycle de vie de la View
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            recreateDrawingBitmap(w, h);
        }
    }

    /**
     * Recrée le bitmap intermédiaire lors d'un changement de taille.
     * Le contenu existant est recopié pour ne pas perdre le dessin.
     */
    private void recreateDrawingBitmap(int w, int h) {
        Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas newCanvas = new Canvas(newBitmap);
        newCanvas.drawColor(canvasBackgroundColor);

        if (mDrawingBitmap != null) {
            newCanvas.drawBitmap(mDrawingBitmap, 0, 0, null);
            mDrawingBitmap.recycle();
        }

        mDrawingBitmap = newBitmap;
        mDrawingCanvas = newCanvas;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Rendu
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Pipeline de rendu :
     * 1. Fond du canvas matériel (visible à travers les trous de la gomme)
     * 2. Rendu dans le bitmap intermédiaire (figures + gomme CLEAR)
     * 3. Composite du bitmap sur le canvas matériel
     * 4. Bordure de sélection et poignées (dessinées sur le canvas, pas sur le bitmap)
     *
     * Pourquoi deux passes ?
     * PorterDuff.CLEAR crée des "trous" transparents dans le bitmap. Ces trous laissent
     * apparaître la couleur dessinée à l'étape 1 — qui est identique au fond de la toile.
     * Résultat : les zones gommées ont visuellement la couleur du fond.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawingBitmap == null) {
            return;
        }

        // Étape 1 — fond sur le canvas matériel (visible sous les trous CLEAR)
        canvas.drawColor(canvasBackgroundColor);

        // Étape 2 — rendu complet dans le bitmap off-screen

        // 2a. Effacement du frame précédent
        mDrawingCanvas.drawColor(canvasBackgroundColor);

        // 2b. Figures validées
        for (Figure figure : figures) {
            figure.draw(mDrawingCanvas);
        }

        // 2c. Aperçu de la figure en cours (avant commit dans ACTION_UP)
        if (currentFigure != null) {
            currentFigure.draw(mDrawingCanvas);
        }

        // 2d. Tracés de gomme (CLEAR = pixels transparents dans le bitmap)
        float eraserWidth = computeEraserWidth();
        eraserPaint.setStrokeWidth(eraserWidth);

        for (Path stroke : eraserStrokes) {
            mDrawingCanvas.drawPath(stroke, eraserPaint);
        }
        if (currentEraserStroke != null) {
            mDrawingCanvas.drawPath(currentEraserStroke, eraserPaint);
        }

        // Étape 3 — composite : les trous du bitmap laissent voir le fond de l'étape 1
        canvas.drawBitmap(mDrawingBitmap, 0, 0, null);

        // Étape 4 — sélection par-dessus (ne doit pas être gommable)
        if (selectedFigure != null) {
            drawSelection(canvas, selectedFigure);
        }
    }

    /** Largeur de la gomme en px, proportionnelle à l'épaisseur courante du trait. */
    private float computeEraserWidth() {
        return Math.max(ERASER_MIN_WIDTH_PX, currentStyle.getStrokeWidth() * ERASER_WIDTH_MULTIPLIER);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Gestion du toucher
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;

                if (toolMode == ToolMode.ERASE) {
                    startErasing(x, y);
                    return true;
                }
                if (toolMode == ToolMode.SELECT) {
                    startSelection(x, y);
                    return true;
                }
                startDrawing(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (toolMode == ToolMode.ERASE) {
                    continueErasing(x, y);
                    return true;
                }
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
                commitCurrentAction();
                return true;

            default:
                return true;
        }
    }

    /**
     * Valide l'action en cours lors du ACTION_UP.
     * Le snapshot est pris AVANT le commit : undo revient à l'état d'avant l'action.
     */
    private void commitCurrentAction() {
        if (toolMode == ToolMode.DRAW && currentFigure != null) {
            historyManager.saveSnapshot(figures, eraserStrokes);
            figures.add(currentFigure);

        } else if (toolMode == ToolMode.ERASE && currentEraserStroke != null) {
            historyManager.saveSnapshot(figures, eraserStrokes);
            eraserStrokes.add(currentEraserStroke);
            currentEraserStroke = null;
        }

        currentFigure = null;
        movingSelection = false;
        moveStateSaved = false;
        notifyCanvasStateChanged();
        invalidate();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Actions selon le mode
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Mode DRAW — crée la figure comme aperçu sans l'ajouter à la liste.
     * Elle sera commitée dans {@link #commitCurrentAction()}.
     */
    private void startDrawing(float x, float y) {
        selectedFigure = null;
        currentFigure = FigureFactory.create(figureType, x, y, currentStyle);
        invalidate();
    }

    /**
     * Mode ERASE — démarre un nouveau tracé de gomme au point (x, y).
     * Un micro-segment est ajouté pour qu'un simple tap efface aussi.
     */
    private void startErasing(float x, float y) {
        currentEraserStroke = new Path();
        currentEraserStroke.moveTo(x, y);
        currentEraserStroke.lineTo(x + 0.1f, y + 0.1f);
        invalidate();
    }

    /**
     * Mode ERASE — prolonge le tracé de gomme en cours jusqu'au point (x, y).
     */
    private void continueErasing(float x, float y) {
        if (currentEraserStroke == null) {
            return;
        }
        currentEraserStroke.lineTo(x, y);
        invalidate();
    }

    /** Mode SELECT — sélectionne la figure au point (x, y), ou désélectionne. */
    private void startSelection(float x, float y) {
        selectedFigure = findFigureAt(x, y);
        movingSelection = selectedFigure != null;
        moveStateSaved = false;
        syncStyleFromSelection();
        notifyCanvasStateChanged();
        invalidate();
    }

    /**
     * Mode SELECT — déplace la figure sélectionnée.
     * Seul {@link #invalidate()} est appelé ici pour ne pas déclencher
     * de render() à chaque frame (BUG 2 fix).
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

    // ═══════════════════════════════════════════════════════════════════════
    // Historique
    // ═══════════════════════════════════════════════════════════════════════

    /** Sauvegarde l'état complet (figures + gomme) sans déclencher de render(). */
    private void saveToHistory() {
        historyManager.saveSnapshot(figures, eraserStrokes);
    }

    /**
     * Annule la dernière action en restaurant le snapshot précédent.
     * Efface aussi la figure/gomme en cours de tracé le cas échéant.
     *
     * @return vrai si l'annulation a eu lieu, faux si l'historique est vide
     */
    public boolean undo() {
        CanvasSnapshot snapshot = historyManager.popSnapshot();
        if (snapshot == null) {
            return false;
        }

        figures.clear();
        figures.addAll(snapshot.figures);

        eraserStrokes.clear();
        eraserStrokes.addAll(snapshot.eraserPaths);

        selectedFigure = null;
        currentFigure = null;
        currentEraserStroke = null;
        movingSelection = false;
        moveStateSaved = false;

        notifyCanvasStateChanged();
        invalidate();
        return true;
    }

    /** @return vrai s'il existe au moins un état à annuler */
    public boolean canUndo() {
        return historyManager.canUndo();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Actions publiques
    // ═══════════════════════════════════════════════════════════════════════

    /** Efface toute la toile (figures + gomme) et sauvegarde pour undo. */
    public void clearAll() {
        if (figures.isEmpty() && eraserStrokes.isEmpty()) {
            return;
        }

        saveToHistory();
        figures.clear();
        eraserStrokes.clear();
        selectedFigure = null;
        currentFigure = null;
        currentEraserStroke = null;
        notifyCanvasStateChanged();
        invalidate();
    }

    /**
     * Supprime la figure sélectionnée avec support de l'annulation.
     *
     * @return vrai si une figure a été supprimée
     */
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

    /**
     * Exporte la toile en {@link Bitmap} pour le partage.
     * Les tracés de gomme (CLEAR) créent des pixels transparents dans le PNG final —
     * comportement attendu pour un fichier image avec canal alpha.
     *
     * @return le bitmap exporté, ou {@code null} si la vue n'a pas encore été mesurée
     */
    public Bitmap exportToBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }

        Bitmap exportBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas exportCanvas = new Canvas(exportBitmap);
        exportCanvas.drawColor(canvasBackgroundColor);

        for (Figure figure : figures) {
            figure.draw(exportCanvas);
        }

        // Appliquer les gommes — les zones effacées seront transparentes dans le PNG
        eraserPaint.setStrokeWidth(computeEraserWidth());
        for (Path stroke : eraserStrokes) {
            exportCanvas.drawPath(stroke, eraserPaint);
        }

        return exportBitmap;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Accesseurs de style
    // ═══════════════════════════════════════════════════════════════════════

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
        currentEraserStroke = null;

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

    // ═══════════════════════════════════════════════════════════════════════
    // Listener
    // ═══════════════════════════════════════════════════════════════════════

    public void setCanvasStateListener(CanvasStateListener listener) {
        canvasStateListener = listener;
    }

    /** Coupe le lien vers le listener — appeler dans onDestroy() pour éviter les fuites mémoire. */
    public void removeCanvasStateListener() {
        canvasStateListener = null;
    }

    private void notifyCanvasStateChanged() {
        if (canvasStateListener != null) {
            canvasStateListener.onCanvasStateChanged();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Utilitaires privés
    // ═══════════════════════════════════════════════════════════════════════

    public boolean hasSelectedFigure() {
        return selectedFigure != null;
    }

    /** @return vrai si la toile a au moins une figure ou un tracé de gomme */
    public boolean hasContent() {
        return !figures.isEmpty() || !eraserStrokes.isEmpty();
    }

    /** @return vrai si la toile a au moins une figure dessinée (hors gomme) */
    public boolean hasFigures() {
        return !figures.isEmpty();
    }

    /** Cherche la figure visible au-dessus sous le doigt, de la plus récente à la plus ancienne. */
    private Figure findFigureAt(float x, float y) {
        for (int i = figures.size() - 1; i >= 0; i--) {
            Figure figure = figures.get(i);
            if (figure.contains(x, y)) {
                return figure;
            }
        }
        return null;
    }

    /**
     * Dessine la bordure de sélection (rectangle pointillé) et les 4 poignées de coin.
     * Dessiné sur le canvas matériel, pas sur le bitmap, pour ne pas être effaçable.
     */
    private void drawSelection(Canvas canvas, Figure figure) {
        float left   = figure.getLeft()   - SELECTION_MARGIN;
        float top    = figure.getTop()    - SELECTION_MARGIN;
        float right  = figure.getRight()  + SELECTION_MARGIN;
        float bottom = figure.getBottom() + SELECTION_MARGIN;

        // Bordure pointillée
        canvas.drawRect(left, top, right, bottom, selectionPaint);

        // Poignées de coin (carrés blancs avec bordure de la couleur d'accent)
        drawHandle(canvas, left,  top);
        drawHandle(canvas, right, top);
        drawHandle(canvas, left,  bottom);
        drawHandle(canvas, right, bottom);
    }

    /** Dessine un carré blanc centré en (cx, cy) pour matérialiser une poignée de coin. */
    private void drawHandle(Canvas canvas, float cx, float cy) {
        RectF handle = new RectF(
                cx - HANDLE_HALF_SIZE,
                cy - HANDLE_HALF_SIZE,
                cx + HANDLE_HALF_SIZE,
                cy + HANDLE_HALF_SIZE
        );
        canvas.drawRect(handle, handlePaint);
        canvas.drawRect(handle, selectionPaint);  // bordure de la couleur d'accent
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
