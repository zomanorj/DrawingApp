package mg.ramat.drawingapp.editor.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mg.ramat.drawingapp.R;
import mg.ramat.drawingapp.data.Figure;
import mg.ramat.drawingapp.databinding.ActivityMainBinding;
import mg.ramat.drawingapp.editor.model.ColorTarget;
import mg.ramat.drawingapp.editor.model.FigureStyle;
import mg.ramat.drawingapp.editor.model.FigureType;
import mg.ramat.drawingapp.editor.model.ToolMode;
import mg.ramat.drawingapp.views.CanvasStateListener;

public final class EditorUiController implements CanvasStateListener {

    // ── Constantes ─────────────────────────────────────────────────────────
    private static final int   DEFAULT_STROKE_WIDTH       = 8;
    /** Facteur de zoom du bouton lors d'une sélection (animation scale-pop). */
    private static final float ANIM_SCALE_PEAK            = 1.15f;
    /** Durée de la montée en echelle (ms). */
    private static final long  ANIM_SCALE_UP_DURATION_MS  = 120L;
    /** Durée de la descente en echelle (ms). */
    private static final long  ANIM_SCALE_DOWN_DURATION_MS = 80L;

    // ── Champs ─────────────────────────────────────────────────────────────
    private final Context              context;
    private final ActivityMainBinding  binding;
    private final Runnable             shareAction;
    private final List<PaletteColor>   palette        = PaletteColor.defaultPalette();
    private final Map<MaterialButton, PaletteColor> paletteButtons = new LinkedHashMap<>();
    private final Map<FigureType, Integer>          figureButtons  = new EnumMap<>(FigureType.class);
    private final Map<ToolMode, Integer>            modeButtons    = new EnumMap<>(ToolMode.class);

    private ColorTarget colorTarget = ColorTarget.STROKE;
    /** Vrai pendant render() pour bloquer les listeners d'UI qui déclencheraient une boucle. */
    private boolean isRenderingState;

    // ═══════════════════════════════════════════════════════════════════════
    // Constructeur
    // ═══════════════════════════════════════════════════════════════════════

    public EditorUiController(Context context, ActivityMainBinding binding, Runnable shareAction) {
        this.context     = context;
        this.binding     = binding;
        this.shareAction = shareAction;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Initialisation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Connecte tous les listeners et initialise l'état de départ de la toile.
     * À appeler depuis Activity.onCreate() après inflate du binding.
     */
    public void bind() {
        figureButtons.put(FigureType.LINE,      R.id.btnLine);
        figureButtons.put(FigureType.RECTANGLE, R.id.btnRect);
        figureButtons.put(FigureType.OVAL,      R.id.btnOval);

        modeButtons.put(ToolMode.DRAW,   R.id.btnDrawMode);
        modeButtons.put(ToolMode.SELECT, R.id.btnSelectMode);
        modeButtons.put(ToolMode.ERASE,  R.id.btnEraseMode);

        configureShapeControls();
        configureModeControls();
        configureAppearanceControls();
        configureActionButtons();
        buildPaletteButtons();
        configureInitialState();

        binding.drawingView.setCanvasStateListener(this);
        render();
    }

    private void configureInitialState() {
        binding.sliderStroke.setValue(DEFAULT_STROKE_WIDTH);
        binding.drawingView.setFigureType(FigureType.RECTANGLE);
        binding.drawingView.setToolMode(ToolMode.DRAW);
        binding.drawingView.setStrokeColor(ContextCompat.getColor(context, R.color.drawing_red));
        binding.drawingView.setFillColor(Color.TRANSPARENT);
        binding.drawingView.setStrokeWidth(DEFAULT_STROKE_WIDTH);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Configuration des contrôles
    // ═══════════════════════════════════════════════════════════════════════

    private void configureShapeControls() {
        binding.shapeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || isRenderingState) {
                return;
            }

            FigureType selectedType = findFigureType(checkedId);
            if (selectedType == null) {
                return;
            }

            // UI 2 — animation scale-pop sur le bouton sélectionné
            View btn = group.findViewById(checkedId);
            if (btn != null) {
                animateButtonScale(btn);
            }

            binding.drawingView.setFigureType(selectedType);
            showMessage(context.getString(R.string.message_shape_selected, getFigureLabel(selectedType)));
            render();
        });
    }

    private void configureModeControls() {
        binding.modeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || isRenderingState) {
                return;
            }

            ToolMode selectedMode = findToolMode(checkedId);
            if (selectedMode == null) {
                return;
            }

            // UI 2 — animation scale-pop sur le bouton sélectionné
            View btn = group.findViewById(checkedId);
            if (btn != null) {
                animateButtonScale(btn);
            }

            binding.drawingView.setToolMode(selectedMode);
            showMessage(context.getString(R.string.message_mode_selected, getModeLabel(selectedMode)));
            render();
        });
    }

    private void configureAppearanceControls() {
        binding.targetToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || isRenderingState) {
                return;
            }

            colorTarget = checkedId == R.id.btnFillTarget ? ColorTarget.FILL : ColorTarget.STROKE;
            showMessage(context.getString(R.string.message_palette_target, getTargetLabel(colorTarget)));
            render();
        });

        binding.btnNoFill.setOnClickListener(v -> {
            colorTarget = ColorTarget.FILL;
            binding.drawingView.setFillColor(Color.TRANSPARENT);
            showMessage(context.getString(R.string.message_fill_disabled));
            render();
        });

        // UI 6 — Slider MD3 remplace SeekBar
        binding.sliderStroke.addOnChangeListener((slider, value, fromUser) -> {
            if (isRenderingState || !fromUser) {
                return;
            }

            int strokeValue = Math.round(value);
            binding.drawingView.setStrokeWidth(strokeValue);
            binding.tvStrokeValue.setText(context.getString(R.string.stroke_value_format, strokeValue));
            render();
        });

        binding.sliderStroke.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) { }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                showMessage(context.getString(
                        R.string.message_stroke_width,
                        Math.round(slider.getValue())
                ));
            }
        });
    }

    private void configureActionButtons() {
        binding.btnUndo.setOnClickListener(v -> {
            if (binding.drawingView.undo()) {
                showMessage(context.getString(R.string.message_undo_done));
            } else {
                showMessage(context.getString(R.string.message_undo_empty));
            }
            render();
        });

        // UI 5 — Snackbar avec action Annuler sur suppression
        binding.btnDelete.setOnClickListener(v -> {
            if (binding.drawingView.deleteSelectedFigure()) {
                Snackbar.make(binding.getRoot(), R.string.message_delete_done, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo_action, undoView -> {
                            binding.drawingView.undo();
                            render();
                        })
                        .show();
            } else {
                showMessage(context.getString(R.string.message_delete_empty));
            }
            render();
        });

        binding.btnClear.setOnClickListener(v -> {
            if (binding.drawingView.hasContent()) {
                binding.drawingView.clearAll();
                showMessage(context.getString(R.string.message_canvas_cleared));
            } else {
                showMessage(context.getString(R.string.message_canvas_already_empty));
            }
            render();
        });

        binding.btnShare.setOnClickListener(v -> shareAction.run());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Palette
    // ═══════════════════════════════════════════════════════════════════════

    private void buildPaletteButtons() {
        int swatchSize    = context.getResources().getDimensionPixelSize(R.dimen.palette_swatch_size);
        int swatchSpacing = context.getResources().getDimensionPixelSize(R.dimen.margin_small);

        for (PaletteColor colorOption : palette) {
            MaterialButton colorButton = new MaterialButton(
                    new ContextThemeWrapper(context, R.style.Widget_DrawingApp_PaletteSwatch),
                    null,
                    0
            );

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMarginEnd(swatchSpacing);
            colorButton.setLayoutParams(lp);
            colorButton.setContentDescription(context.getString(
                    R.string.select_color_description,
                    context.getString(colorOption.getLabelResId())
            ));
            colorButton.setBackgroundTintList(ColorStateList.valueOf(colorOption.resolveColor(context)));
            colorButton.setOnClickListener(v -> applyColor(colorOption));

            paletteButtons.put(colorButton, colorOption);
            binding.paletteContainer.addView(colorButton);
        }
    }

    private void applyColor(PaletteColor colorOption) {
        int resolvedColor = colorOption.resolveColor(context);

        if (colorTarget == ColorTarget.STROKE) {
            binding.drawingView.setStrokeColor(resolvedColor);
            showMessage(context.getString(
                    R.string.message_stroke_color,
                    context.getString(colorOption.getLabelResId())
            ));
        } else {
            binding.drawingView.setFillColor(resolvedColor);
            showMessage(context.getString(
                    R.string.message_fill_color,
                    context.getString(colorOption.getLabelResId())
            ));
        }

        render();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Rendu de l'état de l'UI
    // ═══════════════════════════════════════════════════════════════════════

    private void render() {
        isRenderingState = true;

        FigureType currentFigureType  = binding.drawingView.getFigureType();
        ToolMode   currentToolMode    = binding.drawingView.getToolMode();
        int        strokeWidth        = Math.round(binding.drawingView.getCurrentStrokeWidth());
        int        currentStrokeColor = binding.drawingView.getCurrentStrokeColor();
        int        currentFillColor   = binding.drawingView.getCurrentFillColor();

        Integer checkedFigureButtonId = figureButtons.get(currentFigureType);
        if (checkedFigureButtonId != null
                && binding.shapeToggleGroup.getCheckedButtonId() != checkedFigureButtonId) {
            binding.shapeToggleGroup.check(checkedFigureButtonId);
        }

        Integer checkedModeButtonId = modeButtons.get(currentToolMode);
        if (checkedModeButtonId != null
                && binding.modeToggleGroup.getCheckedButtonId() != checkedModeButtonId) {
            binding.modeToggleGroup.check(checkedModeButtonId);
        }

        int checkedTargetButtonId = colorTarget == ColorTarget.FILL
                ? R.id.btnFillTarget : R.id.btnStrokeTarget;
        if (binding.targetToggleGroup.getCheckedButtonId() != checkedTargetButtonId) {
            binding.targetToggleGroup.check(checkedTargetButtonId);
        }

        // Slider : setValue uniquement si la valeur a changé (évite un callback inutile)
        if (Math.round(binding.sliderStroke.getValue()) != strokeWidth) {
            float clamped = Math.max(binding.sliderStroke.getValueFrom(),
                    Math.min(binding.sliderStroke.getValueTo(), strokeWidth));
            binding.sliderStroke.setValue(clamped);
        }

        binding.tvStrokeValue.setText(context.getString(R.string.stroke_value_format, strokeWidth));
        binding.chipCurrentMode.setText(context.getString(R.string.summary_mode, getModeLabel(currentToolMode)));
        binding.chipCurrentShape.setText(context.getString(R.string.summary_shape, getFigureLabel(currentFigureType)));
        binding.chipCurrentStyle.setText(context.getString(
                R.string.summary_style,
                getColorName(currentStrokeColor),
                strokeWidth,
                getFillSummary(currentFillColor)
        ));
        binding.chipSelectionState.setText(
                binding.drawingView.hasSelectedFigure()
                        ? R.string.selection_active
                        : R.string.selection_inactive
        );
        binding.tvStatus.setText(getStatusText(currentToolMode, binding.drawingView.hasSelectedFigure()));

        updateButtonState(binding.btnUndo,   binding.drawingView.canUndo());
        updateButtonState(binding.btnDelete, binding.drawingView.hasSelectedFigure());
        updateNoFillState(currentFillColor == Color.TRANSPARENT);
        updatePaletteSelection(
                colorTarget == ColorTarget.STROKE ? currentStrokeColor : currentFillColor
        );

        isRenderingState = false;
    }

    private void updateButtonState(MaterialButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.55f);
    }

    private void updateNoFillState(boolean isActive) {
        int bgColor = ContextCompat.getColor(
                context, isActive ? R.color.brand_primary : R.color.surface_soft);
        int textColor = ContextCompat.getColor(
                context, isActive ? R.color.white : R.color.ink_strong);
        int strokeColor = ContextCompat.getColor(
                context, isActive ? R.color.brand_primary : R.color.stroke_soft);

        binding.btnNoFill.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        binding.btnNoFill.setStrokeColor(ColorStateList.valueOf(strokeColor));
        binding.btnNoFill.setTextColor(textColor);
    }

    /**
     * Met en valeur le swatch de la couleur active (contour renforcé + légère montée en échelle).
     * UI 3 : indicateur d'état visuel dans la palette.
     */
    private void updatePaletteSelection(int activeColor) {
        int activeStrokeColor  = ContextCompat.getColor(context, R.color.brand_primary);
        int defaultStrokeColor = ContextCompat.getColor(context, R.color.stroke_soft);
        int activeStrokeWidth  = context.getResources().getDimensionPixelSize(R.dimen.outline_width_active);
        int defaultStrokeWidth = context.getResources().getDimensionPixelSize(R.dimen.outline_width_regular);

        for (Map.Entry<MaterialButton, PaletteColor> entry : paletteButtons.entrySet()) {
            MaterialButton button  = entry.getKey();
            boolean        isActive = entry.getValue().resolveColor(context) == activeColor;

            button.setStrokeColor(ColorStateList.valueOf(isActive ? activeStrokeColor : defaultStrokeColor));
            button.setStrokeWidth(isActive ? activeStrokeWidth : defaultStrokeWidth);
            button.setElevation(isActive ? 4f : 0f);
            button.setScaleX(isActive ? 1.05f : 1f);
            button.setScaleY(isActive ? 1.05f : 1f);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers — labels et textes
    // ═══════════════════════════════════════════════════════════════════════

    private CharSequence getStatusText(ToolMode toolMode, boolean hasSelection) {
        if (toolMode == ToolMode.SELECT) {
            return context.getText(
                    hasSelection ? R.string.status_select_mode_active : R.string.status_select_mode_idle);
        }
        if (toolMode == ToolMode.ERASE) {
            return context.getText(R.string.status_erase_mode);
        }
        return context.getText(R.string.status_draw_mode);
    }

    private String getColorName(int color) {
        if (color == Color.TRANSPARENT) {
            return context.getString(R.string.summary_fill_none);
        }
        for (PaletteColor pc : palette) {
            if (pc.resolveColor(context) == color) {
                return context.getString(pc.getLabelResId());
            }
        }
        return context.getString(R.string.custom_color_label);
    }

    private String getFillSummary(int fillColor) {
        return fillColor == Color.TRANSPARENT
                ? context.getString(R.string.summary_fill_none)
                : getColorName(fillColor);
    }

    private FigureType findFigureType(int checkedId) {
        for (Map.Entry<FigureType, Integer> e : figureButtons.entrySet()) {
            if (e.getValue() == checkedId) return e.getKey();
        }
        return null;
    }

    private ToolMode findToolMode(int checkedId) {
        for (Map.Entry<ToolMode, Integer> e : modeButtons.entrySet()) {
            if (e.getValue() == checkedId) return e.getKey();
        }
        return null;
    }

    private String getFigureLabel(FigureType figureType) {
        if (figureType == FigureType.LINE) return context.getString(R.string.line_tool);
        if (figureType == FigureType.OVAL) return context.getString(R.string.ellipse_tool);
        return context.getString(R.string.rectangle_tool);
    }

    private String getModeLabel(ToolMode toolMode) {
        if (toolMode == ToolMode.SELECT) return context.getString(R.string.select_mode);
        if (toolMode == ToolMode.ERASE)  return context.getString(R.string.erase_mode);
        return context.getString(R.string.draw_mode);
    }

    private String getTargetLabel(ColorTarget target) {
        return context.getString(
                target == ColorTarget.FILL ? R.string.fill_target : R.string.stroke_target);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Feedback utilisateur — Snackbar (UI 5)
    // ═══════════════════════════════════════════════════════════════════════

    /** Affiche un message court via Snackbar ancré sur la racine du layout. */
    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Animation scale-pop (UI 2)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Anime un bouton avec un effet "pop" : 1.0 → 1.15 → 1.0.
     * Durées : montée 120 ms, descente 80 ms (total 200 ms).
     *
     * @param view le bouton à animer
     */
    private static void animateButtonScale(View view) {
        ObjectAnimator upX   = ObjectAnimator.ofFloat(view, "scaleX", 1f, ANIM_SCALE_PEAK);
        ObjectAnimator upY   = ObjectAnimator.ofFloat(view, "scaleY", 1f, ANIM_SCALE_PEAK);
        ObjectAnimator downX = ObjectAnimator.ofFloat(view, "scaleX", ANIM_SCALE_PEAK, 1f);
        ObjectAnimator downY = ObjectAnimator.ofFloat(view, "scaleY", ANIM_SCALE_PEAK, 1f);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(upX, upY);
        scaleUp.setDuration(ANIM_SCALE_UP_DURATION_MS);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(downX, downY);
        scaleDown.setDuration(ANIM_SCALE_DOWN_DURATION_MS);

        AnimatorSet pop = new AnimatorSet();
        pop.playSequentially(scaleUp, scaleDown);
        pop.start();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BottomSheet d'édition de style (double-tap sur figure)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Ouvre un BottomSheetDialog pré-rempli avec le style courant de la figure sélectionnée.
     * Déclenché par un double-tap en mode SELECT (DrawingView → GestureDetector).
     *
     * Sur "Appliquer" : {@link mg.ramat.drawingapp.views.DrawingView#applyStyleToSelected}
     * crée un seul snapshot undo pour les trois changements.
     *
     * @param figure la figure dont on veut éditer le style
     */
    @Override
    public void onEditFigureRequested(Figure figure) {
        FigureStyle originalStyle = binding.drawingView.getSelectedFigureStyle();
        if (originalStyle == null) {
            return;
        }

        BottomSheetDialog sheet    = new BottomSheetDialog(context);
        View              sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_edit_figure, null);
        sheet.setContentView(sheetView);

        // État temporaire du style (tableau 1 élément = mutable dans lambda)
        int[]   pendingStrokeColor = { originalStyle.getStrokeColor() };
        int[]   pendingFillColor   = { originalStyle.getFillColor() };
        float[] pendingStrokeWidth = { originalStyle.getStrokeWidth() };

        // ── Palette contour ────────────────────────────────────────────────
        LinearLayout paletteStroke = sheetView.findViewById(R.id.sheetPaletteStroke);
        buildSheetPalette(paletteStroke, pendingStrokeColor[0], newColor -> {
            pendingStrokeColor[0] = newColor;
            refreshSheetPalette(paletteStroke, newColor);
        });

        // ── Palette fond + bouton "Sans fond" ──────────────────────────────
        LinearLayout paletteFill = sheetView.findViewById(R.id.sheetPaletteFill);
        buildSheetPalette(paletteFill, pendingFillColor[0], newColor -> {
            pendingFillColor[0] = newColor;
            refreshSheetPalette(paletteFill, newColor);
        });

        MaterialButton btnNoFill = sheetView.findViewById(R.id.sheetBtnNoFill);
        btnNoFill.setOnClickListener(v -> {
            pendingFillColor[0] = Color.TRANSPARENT;
            refreshSheetPalette(paletteFill, Color.TRANSPARENT);
        });

        // ── Slider épaisseur (UI 6) ────────────────────────────────────────
        Slider   sheetSlider = sheetView.findViewById(R.id.sheetSlider);
        TextView tvStroke    = sheetView.findViewById(R.id.sheetTvStrokeValue);

        float initialWidth = originalStyle.getStrokeWidth();
        sheetSlider.setValue(Math.max(sheetSlider.getValueFrom(),
                Math.min(sheetSlider.getValueTo(), Math.round(initialWidth))));
        tvStroke.setText(context.getString(R.string.stroke_value_format, Math.round(initialWidth)));

        sheetSlider.addOnChangeListener((slider, value, fromUser) -> {
            int width = Math.round(value);
            pendingStrokeWidth[0] = width;
            tvStroke.setText(context.getString(R.string.stroke_value_format, width));
        });

        // ── Boutons Annuler / Appliquer ────────────────────────────────────
        sheetView.findViewById(R.id.sheetBtnCancel)
                .setOnClickListener(v -> sheet.dismiss());

        sheetView.findViewById(R.id.sheetBtnApply).setOnClickListener(v -> {
            FigureStyle newStyle = new FigureStyle(
                    pendingStrokeColor[0],
                    pendingFillColor[0],
                    pendingStrokeWidth[0]
            );
            binding.drawingView.applyStyleToSelected(newStyle);
            showMessage(context.getString(R.string.message_figure_updated));
            sheet.dismiss();
            render();
        });

        sheet.show();
    }

    /**
     * Construit une rangée de swatches de couleur dans le conteneur donné.
     * Chaque swatch appelle {@code onColorPicked} avec la couleur résolue.
     *
     * @param container    conteneur horizontal où ajouter les boutons
     * @param activeColor  couleur initialement sélectionnée (reçoit le contour actif)
     * @param onColorPicked callback appelé quand l'utilisateur choisit une couleur
     */
    private void buildSheetPalette(LinearLayout container, int activeColor,
                                   ColorPickedCallback onColorPicked) {
        int swatchSize    = context.getResources().getDimensionPixelSize(R.dimen.palette_swatch_size);
        int swatchSpacing = context.getResources().getDimensionPixelSize(R.dimen.margin_small);
        int activeStroke  = ContextCompat.getColor(context, R.color.brand_primary);
        int defaultStroke = ContextCompat.getColor(context, R.color.stroke_soft);
        int activeWidth   = context.getResources().getDimensionPixelSize(R.dimen.outline_width_active);
        int defaultWidth  = context.getResources().getDimensionPixelSize(R.dimen.outline_width_regular);

        container.removeAllViews();

        for (PaletteColor colorOption : palette) {
            int resolved = colorOption.resolveColor(context);

            MaterialButton btn = new MaterialButton(
                    new ContextThemeWrapper(context, R.style.Widget_DrawingApp_PaletteSwatch), null, 0);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMarginEnd(swatchSpacing);
            btn.setLayoutParams(lp);
            btn.setBackgroundTintList(ColorStateList.valueOf(resolved));
            btn.setContentDescription(context.getString(
                    R.string.select_color_description,
                    context.getString(colorOption.getLabelResId())
            ));

            boolean isActive = resolved == activeColor;
            btn.setStrokeColor(ColorStateList.valueOf(isActive ? activeStroke : defaultStroke));
            btn.setStrokeWidth(isActive ? activeWidth : defaultWidth);
            btn.setElevation(isActive ? 4f : 0f);
            btn.setScaleX(isActive ? 1.05f : 1f);
            btn.setScaleY(isActive ? 1.05f : 1f);

            btn.setOnClickListener(v -> onColorPicked.onColorPicked(resolved));
            container.addView(btn);
        }
    }

    /**
     * Met à jour visuellement les bordures et l'élévation des swatches
     * pour refléter la nouvelle couleur active.
     *
     * @param container   palette à rafraîchir
     * @param activeColor couleur désormais sélectionnée
     */
    private void refreshSheetPalette(LinearLayout container, int activeColor) {
        int activeStroke  = ContextCompat.getColor(context, R.color.brand_primary);
        int defaultStroke = ContextCompat.getColor(context, R.color.stroke_soft);
        int activeWidth   = context.getResources().getDimensionPixelSize(R.dimen.outline_width_active);
        int defaultWidth  = context.getResources().getDimensionPixelSize(R.dimen.outline_width_regular);

        for (int i = 0; i < container.getChildCount(); i++) {
            if (!(container.getChildAt(i) instanceof MaterialButton)) continue;
            MaterialButton btn      = (MaterialButton) container.getChildAt(i);
            int btnColor = btn.getBackgroundTintList() != null
                    ? btn.getBackgroundTintList().getDefaultColor()
                    : Color.TRANSPARENT;

            boolean isActive = btnColor == activeColor;
            btn.setStrokeColor(ColorStateList.valueOf(isActive ? activeStroke : defaultStroke));
            btn.setStrokeWidth(isActive ? activeWidth : defaultWidth);
            btn.setElevation(isActive ? 4f : 0f);
            btn.setScaleX(isActive ? 1.05f : 1f);
            btn.setScaleY(isActive ? 1.05f : 1f);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cycle de vie
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Libère la référence circulaire DrawingView → EditorUiController → Activity.
     * À appeler depuis Activity.onDestroy() pour permettre au GC de collecter l'Activity.
     */
    public void detach() {
        binding.drawingView.removeCanvasStateListener();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CanvasStateListener
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onCanvasStateChanged() {
        render();
    }

    // ── Interface fonctionnelle privée ─────────────────────────────────────

    /** Callback pour la sélection d'une couleur dans le BottomSheet. */
    private interface ColorPickedCallback {
        void onColorPicked(int color);
    }
}
