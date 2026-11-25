package com.example.focuspet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

public final class PetAccessoryLayer extends View {
    private ImageView petImage;
    private AccessoryCatalog.Accessory accessory;
    private Drawable accessoryDrawable;

    public PetAccessoryLayer(Context context, AttributeSet attributes) {
        super(context, attributes);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void bind(ImageView petImage, AccessoryCatalog.Accessory accessory) {
        this.petImage = petImage;
        this.accessory = accessory;
        accessoryDrawable = accessory == null
                ? null : ContextCompat.getDrawable(getContext(), accessory.drawable);
        if (accessoryDrawable != null) accessoryDrawable = accessoryDrawable.mutate();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (petImage == null || accessory == null || accessoryDrawable == null) return;
        Drawable petDrawable = petImage.getDrawable();
        if (petDrawable == null || petDrawable.getIntrinsicWidth() <= 0
                || petDrawable.getIntrinsicHeight() <= 0
                || accessoryDrawable.getIntrinsicWidth() <= 0
                || accessoryDrawable.getIntrinsicHeight() <= 0
                || getWidth() <= 0 || getHeight() <= 0) return;

        float petWidth = petImage.getWidth() - petImage.getPaddingLeft()
                - petImage.getPaddingRight();
        float petHeight = petImage.getHeight() - petImage.getPaddingTop()
                - petImage.getPaddingBottom();
        float scale = Math.min(petWidth / petDrawable.getIntrinsicWidth(),
                petHeight / petDrawable.getIntrinsicHeight());
        float fittedWidth = petDrawable.getIntrinsicWidth() * scale;
        float fittedHeight = petDrawable.getIntrinsicHeight() * scale;
        float fittedLeft = petImage.getLeft() + petImage.getPaddingLeft()
                + (petWidth - fittedWidth) / 2f;
        float fittedTop = petImage.getTop() + petImage.getPaddingTop()
                + (petHeight - fittedHeight) / 2f;

        float contentWidth = accessory.contentRight - accessory.contentLeft;
        float contentHeight = accessory.contentBottom - accessory.contentTop;
        if (contentWidth <= 0f || contentHeight <= 0f) return;
        float visibleWidth = fittedWidth * accessory.widthFraction;
        float imageWidth = visibleWidth / contentWidth;
        float imageHeight = imageWidth * accessoryDrawable.getIntrinsicHeight()
                / accessoryDrawable.getIntrinsicWidth();
        float centerX = fittedLeft + fittedWidth * accessory.centerX;
        float centerY = fittedTop + fittedHeight * accessory.centerY;
        float visibleCenterX = (accessory.contentLeft + accessory.contentRight) / 2f;
        float visibleCenterY = (accessory.contentTop + accessory.contentBottom) / 2f;

        accessoryDrawable.setBounds(
                Math.round(centerX - visibleCenterX * imageWidth),
                Math.round(centerY - visibleCenterY * imageHeight),
                Math.round(centerX + (1f - visibleCenterX) * imageWidth),
                Math.round(centerY + (1f - visibleCenterY) * imageHeight));
        accessoryDrawable.draw(canvas);
    }
}
