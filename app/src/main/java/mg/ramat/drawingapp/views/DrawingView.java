package mg.ramat.drawingapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;
import mg.ramat.drawingapp.data.Figure;
import mg.ramat.drawingapp.data.Line;
import mg.ramat.drawingapp.data.Rectangle;
import mg.ramat.drawingapp.data.Oval;

public class DrawingView extends View {

    private  Paint paint;
    private  float startX, startY, endX, endY;
    private List<Figure> figures = new ArrayList<>();
    private Figure currentFigure = null;
    private int figureType = 2;
    private int currentColor = Color.RED;
    private float currentStrokeWidth = 8f;
    
    // ======== SYSTÈME D'ANNULATION (NOUVEAU) ========
    // Historique pour pouvoir annuler les actions
    private List<List<Figure>> history = new ArrayList<>();
    private static final int MAX_HISTORY = 50; // Limiter l'historique à 50 actions
    private UndoStateListener undoStateListener; // Listener pour l'état du bouton

    public void setFigureType(int type) {
        this.figureType = type;
    }

    public void setColor(int color) {
        this.currentColor = color;
        paint.setColor(color);
    }

    public void setStrokeWidth(float width) {
        this.currentStrokeWidth = width;
        paint.setStrokeWidth(width);
    }

    /**
     * Retourne l'épaisseur du trait actuelle
     * @return Épaisseur du trait en pixels
     */
    public float getCurrentStrokeWidth() {
        return currentStrokeWidth;
    }

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(Color.WHITE);

        paint = new Paint();
        paint.setColor(currentColor);
        paint.setStrokeWidth(currentStrokeWidth);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Figure f : figures) {
            f.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                // Sauvegarder l'état AVANT d'ajouter la nouvelle figure
                saveToHistory();

                if (figureType == 1) {
                    currentFigure = new Line(x, y, x, y, new Paint(paint));
                } else if (figureType == 2) {
                    currentFigure = new Rectangle(x, y, x, y, new Paint(paint));
                } else {
                    currentFigure = new Oval(x, y, x, y, new Paint(paint));
                }

                figures.add(currentFigure);
                break;

            case MotionEvent.ACTION_MOVE:

                if (currentFigure != null) {
                    currentFigure.setEnd(x, y);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                currentFigure = null;
                break;
        }

        return true;
    }
    
    // ======== MÉTHODES D'ANNULATION (NOUVEAU) ========
    
    /**
     * Sauvegarde l'état actuel dans l'historique
     * Appelé chaque fois qu'une figure est terminée
     */
    private void saveToHistory() {
        // Créer une copie de la liste actuelle de figures
        List<Figure> currentState = new ArrayList<>(figures);
        
        // Ajouter à l'historique
        history.add(currentState);
        
        // Limiter la taille de l'historique
        if (history.size() > MAX_HISTORY) {
            history.remove(0); // Supprimer le plus ancien
        }
        
        // Notifier que l'état a changé
        notifyUndoStateChanged();
    }
    
    /**
     * Annule la dernière action
     * @return true si l'annulation a réussi, false si rien à annuler
     */
    public boolean undo() {
        if (history.size() > 0) {
            // Récupérer le dernier état
            List<Figure> previousState = history.get(history.size() - 1);
            
            // Restaurer cet état
            figures = new ArrayList<>(previousState);
            
            // Supprimer de l'historique
            history.remove(history.size() - 1);
            
            // Redessiner
            invalidate();
            
            // Notifier que l'état a changé
            notifyUndoStateChanged();
            
            return true;
        }
        return false; // Rien à annuler
    }
    
    /**
     * Vérifie si on peut annuler
     * @return true s'il y a des actions à annuler
     */
    public boolean canUndo() {
        return history.size() > 0;
    }
    
    /**
     * Efface tout le dessin et l'historique
     */
    public void clearAll() {
        figures.clear();
        history.clear();
        invalidate();
        notifyUndoStateChanged();
    }
    
    /**
     * Définit le listener pour les changements d'état d'annulation
     */
    public void setUndoStateListener(UndoStateListener listener) {
        this.undoStateListener = listener;
    }
    
    /**
     * Notifie le listener que l'état d'annulation a changé
     */
    private void notifyUndoStateChanged() {
        if (undoStateListener != null) {
            undoStateListener.onUndoStateChanged();
        }
    }
}
