package mg.ramat.drawingapp.views;

import mg.ramat.drawingapp.data.Figure;

/**
 * Contrat d'observation de la toile.
 *
 * {@link #onCanvasStateChanged()} : état général (mode, sélection, undo…)
 * {@link #onEditFigureRequested(Figure)} : double-tap sur une figure sélectionnée
 *  → ouvrir le panneau d'édition de style (BottomSheet).
 *
 * La méthode onEditFigureRequested() a une implémentation par défaut (no-op)
 * pour ne pas forcer chaque implémenteur à la surcharger s'il n'en a pas besoin.
 */
public interface CanvasStateListener {

    /** Appelé à chaque changement d'état du canvas (hors mouvement continu). */
    void onCanvasStateChanged();

    /**
     * Appelé quand l'utilisateur double-tape sur une figure sélectionnée.
     * Implémentation par défaut : rien (Java 8 default method).
     *
     * @param figure la figure sélectionnée à éditer
     */
    default void onEditFigureRequested(Figure figure) {
        // No-op — l'implémenteur surcharge si nécessaire
    }
}
