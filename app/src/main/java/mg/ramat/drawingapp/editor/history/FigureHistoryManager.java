package mg.ramat.drawingapp.editor.history;

import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;

import mg.ramat.drawingapp.data.Figure;

/**
 * Gestionnaire de l'historique des états de la toile.
 *
 * Enregistre des {@link CanvasSnapshot} (figures + tracés de gomme) pour
 * permettre l'annulation successive des actions. La taille de l'historique
 * est bornée par {@code maxSnapshots} pour contrôler l'empreinte mémoire.
 */
public final class FigureHistoryManager {

    private final int maxSnapshots;
    private final List<CanvasSnapshot> snapshots = new ArrayList<>();

    public FigureHistoryManager(int maxSnapshots) {
        this.maxSnapshots = maxSnapshots;
    }

    /**
     * Sauvegarde l'état courant AVANT une action (dessin, gomme, déplacement…).
     * Appeler cette méthode juste avant de modifier la toile garantit que
     * {@link #popSnapshot()} peut restaurer l'état d'avant l'action.
     *
     * Si la limite maxSnapshots est atteinte, le plus ancien instantané est supprimé.
     *
     * @param figures      liste des figures actuelles
     * @param eraserPaths  liste des tracés de gomme validés
     */
    public void saveSnapshot(List<Figure> figures, List<Path> eraserPaths) {
        CanvasSnapshot snapshot = new CanvasSnapshot(
                deepCopyFigures(figures),
                new ArrayList<>(eraserPaths)    // copie superficielle OK : les Path sont immuables
        );

        snapshots.add(snapshot);

        if (snapshots.size() > maxSnapshots) {
            snapshots.remove(0);
        }
    }

    /**
     * Dépile et retourne le dernier état sauvegardé.
     * Les figures sont re-copiées pour que la restauration soit indépendante
     * de l'instantané et protège l'historique des mutations ultérieures.
     *
     * @return le snapshot précédent, ou {@code null} si l'historique est vide
     */
    public CanvasSnapshot popSnapshot() {
        if (snapshots.isEmpty()) {
            return null;
        }

        CanvasSnapshot saved = snapshots.remove(snapshots.size() - 1);

        return new CanvasSnapshot(
                deepCopyFigures(saved.figures),
                new ArrayList<>(saved.eraserPaths)
        );
    }

    /** @return vrai s'il existe au moins un état à annuler */
    public boolean canUndo() {
        return !snapshots.isEmpty();
    }

    /** Copie profonde via {@link Figure#copy()} — nécessaire car les figures sont mutables. */
    private List<Figure> deepCopyFigures(List<Figure> source) {
        List<Figure> copy = new ArrayList<>(source.size());
        for (Figure figure : source) {
            copy.add(figure.copy());
        }
        return copy;
    }
}
