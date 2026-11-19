package com.venky.visaphoto;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int OUTPUT_SIZE = 1000;
    private static final int MAX_BYTES = 240 * 1024;

    private Bitmap source;
    private ImageView preview;
    private SeekBar zoomBar, xBar, yBar;
    private TextView info;

    private final ActivityResultLauncher<String> picker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) loadImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preview = findViewById(R.id.preview);
        zoomBar = findViewById(R.id.zoomBar);
        xBar = findViewById(R.id.xBar);
        yBar = findViewById(R.id.yBar);
        info = findViewById(R.id.info);

        findViewById(R.id.btnPick).setOnClickListener(v -> picker.launch("image/*"));
        findViewById(R.id.btnSave).setOnClickListener(v -> saveImage());

        SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { updatePreview(); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };
        zoomBar.setOnSeekBarChangeListener(l);
        xBar.setOnSeekBarChangeListener(l);
        yBar.setOnSeekBarChangeListener(l);
    }

    private void loadImage(Uri uri) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int sample = 1;
            while (o.outWidth / sample > 2400 || o.outHeight / sample > 2400) sample *= 2;

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                bmp = BitmapFactory.decodeStream(in, null, o2);
            }

            int rot = 0;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                int ori = new ExifInterface(in).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (ori == ExifInterface.ORIENTATION_ROTATE_90) rot = 90;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_180) rot = 180;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_270) rot = 270;
            } catch (Exception ignored) {}

            if (rot != 0) {
                Matrix m = new Matrix();
                m.postRotate(rot);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            }

            source = bmp;
            zoomBar.setProgress(0);
            xBar.setProgress(50);
            yBar.setProgress(50);
            updatePreview();
            info.setText("Output: " + OUTPUT_SIZE + "x" + OUTPUT_SIZE + " px (2x2 inch), JPEG < 240 KB");
        } catch (Exception e) {
            Toast.makeText(this, "Photo load aagala: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap makeSquare(int size) {
        float zoom = 1f + zoomBar.getProgress() / 100f;
        int side = (int) (Math.min(source.getWidth(), source.getHeight()) / zoom);
        int x = (int) ((source.getWidth() - side) * xBar.getProgress() / 100f);
        int y = (int) ((source.getHeight() - side) * yBar.getProgress() / 100f);

        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(Color.WHITE);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        c.drawBitmap(source, new Rect(x, y, x + side, y + side), new Rect(0, 0, size, size), p);
        return out;
    }

    private void updatePreview() {
        if (source == null) return;
        preview.setImageBitmap(makeSquare(600));
    }

    private void saveImage() {
        if (source == null) {
            Toast.makeText(this, "Mudhalla photo select pannunga", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap bmp = makeSquare(OUTPUT_SIZE);
            ByteArrayOutputStream bos;
            int q = 95;
            do {
                bos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, q, bos);
                q -= 5;
            } while (bos.size() > MAX_BYTES && q >= 40);

            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, "visa_photo_" + System.currentTimeMillis() + ".jpg");
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VisaPhoto");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                os.write(bos.toByteArray());
            }
            Toast.makeText(this, "Saved: Pictures/VisaPhoto (" + bos.size() / 1024 + " KB)",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save aagala: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
