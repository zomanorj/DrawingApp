package mg.ramat.drawingapp.editor.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mg.ramat.drawingapp.R;
import mg.ramat.drawingapp.databinding.ActivityMainBinding;
import mg.ramat.drawingapp.editor.model.ColorTarget;
import mg.ramat.drawingapp.editor.model.FigureType;
import mg.ramat.drawingapp.editor.model.ToolMode;
import mg.ramat.drawingapp.views.CanvasStateListener;

public final class EditorUiController implements CanvasStateListener {

    private static final int DEFAULT_STROKE_WIDTH = 8;

    private final Context context;
    private final ActivityMainBinding binding;
    private final Runnable shareAction;
    private final List<PaletteColor> palette = PaletteColor.defaultPalette();
    private final Map<MaterialButton, PaletteColor> paletteButtons = new LinkedHashMap<>();
    private final Map<FigureType, Integer> figureButtons = new EnumMap<>(FigureType.class);
    private final Map<ToolMode, Integer> modeButtons = new EnumMap<>(ToolMode.class);

    private ColorTarget colorTarget = ColorTarget.STROKE;
    private boolean isRenderingState;

    public EditorUiController(Context context, ActivityMainBinding binding, Runnable shareAction) {
        this.context = context;
        this.binding = binding;
        this.shareAction = shareAction;
    }

    public void bind() {
        figureButtons.put(FigureType.LINE, R.id.btnLine);
        figureButtons.put(FigureType.RECTANGLE, R.id.btnRect);
        figureButtons.put(FigureType.OVAL, R.id.btnOval);

        modeButtons.put(ToolMode.DRAW, R.id.btnDrawMode);
        modeButtons.put(ToolMode.SELECT, R.id.btnSelectMode);
        modeButtons.put(ToolMode.ERASE, R.id.btnEraseMode);

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
        binding.seekBarStroke.setProgress(DEFAULT_STROKE_WIDTH);
        binding.drawingView.setFigureType(FigureType.RECTANGLE);
        binding.drawingView.setToolMode(ToolMode.DRAW);
        binding.drawingView.setStrokeColor(ContextCompat.getColor(context, R.color.drawing_red));
        binding.drawingView.setFillColor(Color.TRANSPARENT);
        binding.drawingView.setStrokeWidth(DEFAULT_STROKE_WIDTH);
    }

    private void configureShapeControls() {
        binding.shapeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || isRenderingState) {
                return;
            }

            FigureType selectedType = findFigureType(checkedId);
            if (selectedType == null) {
                return;
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

        // android:min="1" couvre API 26+. Pour API 24-25, on force le min ici.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            binding.seekBarStroke.setMin(1);
        }

        binding.seekBarStroke.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isRenderingState) {
                    return;
                }

                int strokeValue = Math.max(1, progress);

                if (strokeValue != progress) {
                    seekBar.setProgress(strokeValue);
                    return;
                }

                binding.drawingView.setStrokeWidth(strokeValue);
                binding.tvStrokeValue.setText(context.getString(R.string.stroke_value_format, strokeValue));
                render();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                showMessage(context.getString(
                        R.string.message_stroke_width,
                        Math.max(1, seekBar.getProgress())
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

        binding.btnDelete.setOnClickListener(v -> {
            if (binding.drawingView.deleteSelectedFigure()) {
                showMessage(context.getString(R.string.message_delete_done));
            } else {
                showMessage(context.getString(R.string.message_delete_empty));
            }

            render();
        });

        binding.btnClear.setOnClickListener(v -> {
            if (binding.drawingView.hasFigures()) {
                binding.drawingView.clearAll();
                showMessage(context.getString(R.string.message_canvas_cleared));
            } else {
                showMessage(context.getString(R.string.message_canvas_already_empty));
            }

            render();
        });

        binding.btnShare.setOnClickListener(v -> shareAction.run());
    }

    private void buildPaletteButtons() {
        int swatchSize = context.getResources().getDimensionPixelSize(R.dimen.palette_swatch_size);
        int swatchSpacing = context.getResources().getDimensionPixelSize(R.dimen.margin_small);

        for (PaletteColor colorOption : palette) {
            MaterialButton colorButton = new MaterialButton(
                    new ContextThemeWrapper(context, R.style.Widget_DrawingApp_PaletteSwatch),
                    null,
                    0
            );

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            layoutParams.setMarginEnd(swatchSpacing);
            colorButton.setLayoutParams(layoutParams);
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

    private void render() {
        isRenderingState = true;

        FigureType currentFigureType = binding.drawingView.getFigureType();
        ToolMode currentToolMode = binding.drawingView.getToolMode();
        int strokeWidth = Math.round(binding.drawingView.getCurrentStrokeWidth());
        int currentStrokeColor = binding.drawingView.getCurrentStrokeColor();
        int currentFillColor = binding.drawingView.getCurrentFillColor();

        Integer checkedFigureButtonId = figureButtons.get(currentFigureType);
        if (checkedFigureButtonId != null && binding.shapeToggleGroup.getCheckedButtonId() != checkedFigureButtonId) {
            binding.shapeToggleGroup.check(checkedFigureButtonId);
        }

        Integer checkedModeButtonId = modeButtons.get(currentToolMode);
        if (checkedModeButtonId != null && binding.modeToggleGroup.getCheckedButtonId() != checkedModeButtonId) {
            binding.modeToggleGroup.check(checkedModeButtonId);
        }

        int checkedTargetButtonId = colorTarget == ColorTarget.FILL ? R.id.btnFillTarget : R.id.btnStrokeTarget;
        if (binding.targetToggleGroup.getCheckedButtonId() != checkedTargetButtonId) {
            binding.targetToggleGroup.check(checkedTargetButtonId);
        }

        if (binding.seekBarStroke.getProgress() != strokeWidth) {
            binding.seekBarStroke.setProgress(strokeWidth);
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

        updateButtonState(binding.btnUndo, binding.drawingView.canUndo());
        updateButtonState(binding.btnDelete, binding.drawingView.hasSelectedFigure());
        updateNoFillState(currentFillColor == Color.TRANSPARENT);
        updatePaletteSelection(colorTarget == ColorTarget.STROKE ? currentStrokeColor : currentFillColor);

        isRenderingState = false;
    }

    private void updateButtonState(MaterialButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.55f);
    }

    private void updateNoFillState(boolean isActive) {
        int backgroundColor = ContextCompat.getColor(
                context,
                isActive ? R.color.brand_primary : R.color.surface_soft
        );
        int textColor = ContextCompat.getColor(
                context,
                isActive ? R.color.white : R.color.ink_strong
        );
        int strokeColor = ContextCompat.getColor(
                context,
                isActive ? R.color.brand_primary : R.color.stroke_soft
        );

        binding.btnNoFill.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        binding.btnNoFill.setStrokeColor(ColorStateList.valueOf(strokeColor));
        binding.btnNoFill.setTextColor(textColor);
    }

    private void updatePaletteSelection(int activeColor) {
        int activeStrokeColor = ContextCompat.getColor(context, R.color.brand_primary);
        int defaultStrokeColor = ContextCompat.getColor(context, R.color.stroke_soft);
        int activeStrokeWidth = context.getResources().getDimensionPixelSize(R.dimen.outline_width_active);
        int defaultStrokeWidth = context.getResources().getDimensionPixelSize(R.dimen.outline_width_regular);

        for (Map.Entry<MaterialButton, PaletteColor> entry : paletteButtons.entrySet()) {
            MaterialButton button = entry.getKey();
            boolean isActive = entry.getValue().resolveColor(context) == activeColor;

            button.setStrokeColor(ColorStateList.valueOf(isActive ? activeStrokeColor : defaultStrokeColor));
            button.setStrokeWidth(isActive ? activeStrokeWidth : defaultStrokeWidth);
            button.setScaleX(isActive ? 1.05f : 1f);
            button.setScaleY(isActive ? 1.05f : 1f);
        }
    }

    private CharSequence getStatusText(ToolMode toolMode, boolean hasSelection) {
        if (toolMode == ToolMode.SELECT) {
            return context.getText(hasSelection ? R.string.status_select_mode_active : R.string.status_select_mode_idle);
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

        for (PaletteColor paletteColor : palette) {
            if (paletteColor.resolveColor(context) == color) {
                return context.getString(paletteColor.getLabelResId());
            }
        }

        return context.getString(R.string.custom_color_label);
    }

    private String getFillSummary(int fillColor) {
        if (fillColor == Color.TRANSPARENT) {
            return context.getString(R.string.summary_fill_none);
        }

        return getColorName(fillColor);
    }

    private FigureType findFigureType(int checkedId) {
        for (Map.Entry<FigureType, Integer> entry : figureButtons.entrySet()) {
            if (entry.getValue() == checkedId) {
                return entry.getKey();
            }
        }

        return null;
    }

    private ToolMode findToolMode(int checkedId) {
        for (Map.Entry<ToolMode, Integer> entry : modeButtons.entrySet()) {
            if (entry.getValue() == checkedId) {
                return entry.getKey();
            }
        }

        return null;
    }

    private String getFigureLabel(FigureType figureType) {
        if (figureType == FigureType.LINE) {
            return context.getString(R.string.line_tool);
        }

        if (figureType == FigureType.OVAL) {
            return context.getString(R.string.ellipse_tool);
        }

        return context.getString(R.string.rectangle_tool);
    }

    private String getModeLabel(ToolMode toolMode) {
        if (toolMode == ToolMode.SELECT) {
            return context.getString(R.string.select_mode);
        }

        if (toolMode == ToolMode.ERASE) {
            return context.getString(R.string.erase_mode);
        }

        return context.getString(R.string.draw_mode);
    }

    private String getTargetLabel(ColorTarget target) {
        return context.getString(target == ColorTarget.FILL ? R.string.fill_target : R.string.stroke_target);
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Libère la référence circulaire DrawingView → EditorUiController → binding → Activity.
     * À appeler depuis Activity.onDestroy() pour permettre au GC de collecter l'Activity.
     */
    public void detach() {
        binding.drawingView.removeCanvasStateListener();
    }

    @Override
    public void onCanvasStateChanged() {
        render();
    }
}
