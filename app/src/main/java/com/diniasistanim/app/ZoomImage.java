package com.diniasistanim.app;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomImage extends ImageView {
    private final Matrix matrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;
    private Bitmap bitmap;
    private float zoom = 1f, lastX, lastY;
    public ZoomImage(Context ctx) {
        super(ctx);
        setScaleType(ScaleType.MATRIX);
        setBackgroundColor(0xFFF7F3E9);
        scaleDetector = new ScaleGestureDetector(ctx, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float next = Math.max(1f, Math.min(4f, zoom * detector.getScaleFactor()));
                float factor = next / zoom;
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                zoom = next;
                setImageMatrix(matrix);
                return true;
            }
        });
    }
    public void setPage(Bitmap image) {
        bitmap = image;
        setImageBitmap(image);
        post(this::fit);
    }
    private void fit() {
        if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float s = Math.min((float) getWidth()/bitmap.getWidth(), (float) getHeight()/bitmap.getHeight());
        matrix.reset();
        matrix.postScale(s,s);
        matrix.postTranslate((getWidth()-bitmap.getWidth()*s)/2f,(getHeight()-bitmap.getHeight()*s)/2f);
        zoom=1f;
        setImageMatrix(matrix);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastX=e.getX(); lastY=e.getY(); return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true); return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && e.getPointerCount() == 1 && zoom > 1f) {
            matrix.postTranslate(e.getX()-lastX,e.getY()-lastY);
            setImageMatrix(matrix);
            lastX=e.getX(); lastY=e.getY(); return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP) return performClick();
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
