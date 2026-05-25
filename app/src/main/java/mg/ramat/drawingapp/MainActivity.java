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

        File imageFile = new File(imagesFolder, "drawing_share.png");

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
