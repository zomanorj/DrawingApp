package mg.ramat.drawingapp.editor.history;

import android.graphics.Path;

import java.util.List;

import mg.ramat.drawingapp.data.Figure;

/**
 * Instantané complet de la toile à un instant t.
 * Stocke les figures ET les tracés de gomme pour permettre un undo fidèle.
 *
 * Les figures sont copiées en profondeur (elles sont mutables — couleur, position…).
 * Les Path de gomme sont copiés en référence : un Path committé n'est plus jamais
 * modifié, la copie superficielle de la liste est donc suffisante et moins coûteuse.
 */
public final class CanvasSnapshot {

    /** Copies profondes des figures au moment de la sauvegarde. */
    public final List<Figure> figures;

    /** Références aux tracés de gomme validés (immuables après commit). */
    public final List<Path> eraserPaths;

    // Accès restreint au package — seul FigureHistoryManager crée des snapshots.
    CanvasSnapshot(List<Figure> figures, List<Path> eraserPaths) {
        this.figures = figures;
        this.eraserPaths = eraserPaths;
    }
}
