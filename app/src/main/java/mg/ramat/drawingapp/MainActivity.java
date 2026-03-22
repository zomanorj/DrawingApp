package mg.ramat.drawingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import mg.ramat.drawingapp.views.DrawingView;
import mg.ramat.drawingapp.views.UndoStateListener;

/**
 * Activité principale - Drawing Whiteboard
 * Projet de développement mobile pour débutants
 * Gère l'interface utilisateur avec barres d'outils et SeekBar
 */
public class MainActivity extends AppCompatActivity {

    // Composants UI principaux
    private DrawingView drawingView;
    
    // Barre d'outils de formes
    private Button btnLine, btnRect, btnOval;
    
    // Barre de sélection de couleurs (10 couleurs)
    private Button btnRed, btnBlue, btnGreen, btnBlack;
    private Button btnYellow, btnOrange, btnPurple, btnPink;
    private Button btnCyan, btnLime;
    
    // Barre d'options de trait (avec SeekBar)
    private SeekBar seekBarStroke;
    private TextView tvStrokeValue;
    
    // Barre d'actions (annuler, effacer)
    private Button btnUndo, btnClear;

    // Constantes pour les types de formes
    private static final int TYPE_LIGNE = 1;
    private static final int TYPE_RECTANGLE = 2;
    private static final int TYPE_ELLIPSE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialisation de l'interface
        setContentView(R.layout.activity_main);
        
        // Références aux composants
        initialiserComposants();
        
        // Configuration des écouteurs d'événements
        configurerEcouteurs();
    }

    /**
     * Initialise les références aux composants UI
     * Méthode simple pour les débutants
     */
    private void initialiserComposants() {
        // Vue de dessin principale
        drawingView = findViewById(R.id.drawingView);
        
        // --- BARRE D'OUTILS DE FORMES ---
        btnLine = findViewById(R.id.btnLine);
        btnRect = findViewById(R.id.btnRect);
        btnOval = findViewById(R.id.btnOval);
        
        // --- BARRE DE SÉLECTION DE COULEURS (10 COULEURS) ---
        btnRed = findViewById(R.id.btnRed);
        btnBlue = findViewById(R.id.btnBlue);
        btnGreen = findViewById(R.id.btnGreen);
        btnBlack = findViewById(R.id.btnBlack);
        btnYellow = findViewById(R.id.btnYellow);
        btnOrange = findViewById(R.id.btnOrange);
        btnPurple = findViewById(R.id.btnPurple);
        btnPink = findViewById(R.id.btnPink);
        btnCyan = findViewById(R.id.btnCyan);
        btnLime = findViewById(R.id.btnLime);
        
        // --- BARRE D'OPTIONS DE TRAIT (NOUVEAU) ---
        seekBarStroke = findViewById(R.id.seekBarStroke);
        tvStrokeValue = findViewById(R.id.tvStrokeValue);
        
        // --- BARRE D'Actions (NOUVEAU) ---
        btnUndo = findViewById(R.id.btnUndo);
        btnClear = findViewById(R.id.btnClear);
        
        // Initialiser l'état du bouton annuler
        updateUndoButtonState();
    }

    /**
     * Configure les écouteurs d'événements pour tous les boutons
     * Code commenté pour faciliter la compréhension
     */
    private void configurerEcouteurs() {
        
        // ======== GESTION DES FORMES ========
        btnLine.setOnClickListener(v -> {
            drawingView.setFigureType(TYPE_LIGNE);
            afficherMessage("📐 Ligne sélectionnée");
        });

        btnRect.setOnClickListener(v -> {
            drawingView.setFigureType(TYPE_RECTANGLE);
            afficherMessage("📐 Rectangle sélectionné");
        });

        btnOval.setOnClickListener(v -> {
            drawingView.setFigureType(TYPE_ELLIPSE);
            afficherMessage("📐 Ellipse sélectionnée");
        });

        // ======== GESTION DES COULEURS (10 couleurs) ========
        btnRed.setOnClickListener(v -> {
            drawingView.setColor(Color.RED);
            afficherMessage("🎨 Couleur rouge");
        });

        btnBlue.setOnClickListener(v -> {
            drawingView.setColor(Color.BLUE);
            afficherMessage("🎨 Couleur bleue");
        });

        btnGreen.setOnClickListener(v -> {
            drawingView.setColor(Color.GREEN);
            afficherMessage("🎨 Couleur verte");
        });

        btnBlack.setOnClickListener(v -> {
            drawingView.setColor(Color.BLACK);
            afficherMessage("🎨 Couleur noire");
        });

        btnYellow.setOnClickListener(v -> {
            drawingView.setColor(Color.YELLOW);
            afficherMessage("🎨 Couleur jaune");
        });

        btnOrange.setOnClickListener(v -> {
            drawingView.setColor(Color.parseColor("#FF9800"));
            afficherMessage("🎨 Couleur orange");
        });

        btnPurple.setOnClickListener(v -> {
            drawingView.setColor(Color.parseColor("#9C27B0"));
            afficherMessage("🎨 Couleur violette");
        });

        btnPink.setOnClickListener(v -> {
            drawingView.setColor(Color.parseColor("#E91E63"));
            afficherMessage("🎨 Couleur rose");
        });

        btnCyan.setOnClickListener(v -> {
            drawingView.setColor(Color.parseColor("#00BCD4"));
            afficherMessage("🎨 Couleur cyan");
        });

        btnLime.setOnClickListener(v -> {
            drawingView.setColor(Color.parseColor("#CDDC39"));
            afficherMessage("🎨 Couleur vert clair");
        });

        // ======== GESTION DE L'ÉPAISSEUR (SEEKBAR) ========
        seekBarStroke.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Mettre à jour la valeur affichée
                tvStrokeValue.setText(progress + "dp");
                
                // Appliquer l'épaisseur au dessin
                drawingView.setStrokeWidth(progress);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Quand l'utilisateur commence à faire glisser
                afficherMessage("✏️ Ajustement de l'épaisseur...");
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Quand l'utilisateur arrête de faire glisser
                int epaisseur = seekBar.getProgress();
                afficherMessage("✏️ Épaisseur : " + epaisseur + "dp");
            }
        });
        
        // ======== GESTION DES ACTIONS (NOUVEAU) ========
        btnUndo.setOnClickListener(v -> {
            if (drawingView.undo()) {
                afficherMessage("↩️ Action annulée");
                updateUndoButtonState(); // Mettre à jour l'état du bouton
            } else {
                afficherMessage("❌ Rien à annuler");
            }
        });
        
        btnClear.setOnClickListener(v -> {
            drawingView.clearAll();
            afficherMessage("🗑️ Dessin effacé");
            updateUndoButtonState(); // Mettre à jour l'état du bouton
        });
        
        // Mettre à jour le bouton annuler après chaque dessin
        drawingView.setUndoStateListener(new UndoStateListener() {
            @Override
            public void onUndoStateChanged() {
                updateUndoButtonState();
            }
        });
    }
    
    /**
     * Met à jour l'état du bouton Annuler
     * Désactive le bouton s'il n'y a rien à annuler
     */
    private void updateUndoButtonState() {
        if (drawingView.canUndo()) {
            btnUndo.setEnabled(true);
            btnUndo.setAlpha(1.0f); // Bouton normal
        } else {
            btnUndo.setEnabled(false);
            btnUndo.setAlpha(0.5f); // Bouton grisé
        }
    }

    /**
     * Affiche un message Toast à l'utilisateur
     * @param message Le message à afficher
     */
    private void afficherMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}