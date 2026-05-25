package mg.ramat.drawingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mg.ramat.drawingapp.databinding.ActivityMainBinding;
import mg.ramat.drawingapp.editor.ui.EditorUiController;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private EditorUiController editorUiController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.topAppBar);

        editorUiController = new EditorUiController(this, binding, this::partagerDessin);
        editorUiController.bind();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Coupe le lien DrawingView → EditorUiController pour éviter la fuite mémoire.
        editorUiController.detach();
    }

    /**
     * Exporte la toile en PNG puis lance le sélecteur de partage Android.
     *
     * Flux :
     *  1. DrawingView.exportToBitmap() → rendu avec figures + tracés de gomme
     *  2. Sauvegarde dans getCacheDir()/shared_images/drawing_<timestamp>.png
     *  3. Uri via FileProvider (pas de permission WRITE_EXTERNAL_STORAGE nécessaire)
     *  4. Intent ACTION_SEND → chooser natif Android
     *
     * Le nom de fichier inclut un timestamp pour ne pas écraser un export précédent.
     */
    private void partagerDessin() {
        Bitmap bitmap = binding.drawingView.exportToBitmap();

        if (bitmap == null) {
            afficherMessage(getString(R.string.message_share_unavailable));
            return;
        }

        File imagesFolder = new File(getCacheDir(), "shared_images");
        if (!imagesFolder.exists() && !imagesFolder.mkdirs()) {
            afficherMessage(getString(R.string.message_share_prepare_error));
            return;
        }

        // Nom horodaté pour ne pas écraser un partage précédent
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File imageFile = new File(imagesFolder, "drawing_" + timestamp + ".png");

        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException exception) {
            afficherMessage(getString(R.string.message_share_export_error));
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                imageFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser)));
    }

    private void afficherMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
