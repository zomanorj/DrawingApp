package mg.ramat.drawingapp.editor.ui;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;

import mg.ramat.drawingapp.R;

public final class PaletteColor {

    private final int colorResId;
    private final int labelResId;

    public PaletteColor(@ColorRes int colorResId, @StringRes int labelResId) {
        this.colorResId = colorResId;
        this.labelResId = labelResId;
    }

    /** @return l'identifiant de la ressource de chaîne pour le nom de cette couleur */
    public int getLabelResId() {
        return labelResId;
    }

    /** Résout la couleur via ContextCompat pour supporter les qualificatifs de ressource (-night). */
    public int resolveColor(Context context) {
        return ContextCompat.getColor(context, colorResId);
    }

    /**
     * Palette par défaut — 13 couleurs couvrant les usages courants du dessin.
     * Les couleurs dark sont définies dans values-night/colors.xml pour la lisibilité sur fond sombre.
     */
    public static List<PaletteColor> defaultPalette() {
        return Arrays.asList(
                new PaletteColor(R.color.drawing_red,    R.string.color_name_red),
                new PaletteColor(R.color.drawing_blue,   R.string.color_name_blue),
                new PaletteColor(R.color.drawing_green,  R.string.color_name_green),
                new PaletteColor(R.color.drawing_black,  R.string.color_name_black),
                new PaletteColor(R.color.drawing_yellow, R.string.color_name_yellow),
                new PaletteColor(R.color.drawing_orange, R.string.color_name_orange),
                new PaletteColor(R.color.drawing_purple, R.string.color_name_purple),
                new PaletteColor(R.color.drawing_pink,   R.string.color_name_pink),
                new PaletteColor(R.color.drawing_cyan,   R.string.color_name_cyan),
                new PaletteColor(R.color.drawing_lime,   R.string.color_name_lime),
                new PaletteColor(R.color.drawing_white,  R.string.color_name_white),
                new PaletteColor(R.color.drawing_brown,  R.string.color_name_brown),
                new PaletteColor(R.color.drawing_grey,   R.string.color_name_grey)
        );
    }
}
