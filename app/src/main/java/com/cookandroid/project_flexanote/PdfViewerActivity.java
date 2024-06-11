package com.cookandroid.project_flexanote;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;
    private View annotationCanvas;
    private Button annotateButton;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);
        annotationCanvas = findViewById(R.id.annotationCanvas);
        annotateButton = findViewById(R.id.annotateButton);

        annotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                annotationCanvas.setVisibility(View.VISIBLE);
            }
        });

        annotationCanvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    canvas.drawCircle(event.getX(), event.getY(), 10, paint);
                    annotationCanvas.invalidate();
                    return true;
                }
                return false;
            }
        });

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        annotationCanvas.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bitmap == null) {
                    bitmap = Bitmap.createBitmap(annotationCanvas.getWidth(), annotationCanvas.getHeight(), Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(bitmap);
                    annotationCanvas.setBackground(new BitmapDrawable(getResources(), bitmap));
                }
            }
        });

        String filePath = getIntent().getStringExtra("filePath");
        if (filePath != null) {
            File file = new File(filePath);
            openPdfViewer(file);
        }
    }

    private void openPdfViewer(File file) {
        pdfView.fromFile(file)
                .defaultPage(0)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        showToast("PDF Loaded");
                    }
                })
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
