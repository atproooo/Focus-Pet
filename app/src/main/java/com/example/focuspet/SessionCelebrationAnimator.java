package com.example.focuspet;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

final class SessionCelebrationAnimator {
    private final View petLayer;
    private final View popup;
    private AnimatorSet petAnimation;
    private AnimatorSet popupAnimation;
    private boolean started;

    SessionCelebrationAnimator(View petLayer, View popup) {
        this.petLayer = petLayer;
        this.popup = popup;
    }

    void start() {
        if (started) return;
        started = true;
        popup.setVisibility(View.VISIBLE);
        if (!ValueAnimator.areAnimatorsEnabled()) return;

        float density = petLayer.getResources().getDisplayMetrics().density;
        petLayer.setAlpha(0f);
        petLayer.setScaleX(0.9f);
        petLayer.setScaleY(0.9f);
        popup.setAlpha(0f);
        popup.setScaleX(0.82f);
        popup.setScaleY(0.82f);

        petAnimation = new AnimatorSet();
        petAnimation.playTogether(
                ObjectAnimator.ofFloat(petLayer, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(petLayer, View.TRANSLATION_Y,
                        0f, -12f * density, 0f, -8f * density, 0f),
                ObjectAnimator.ofFloat(petLayer, View.SCALE_X,
                        0.9f, 1.08f, 0.96f, 1.04f, 1f),
                ObjectAnimator.ofFloat(petLayer, View.SCALE_Y,
                        0.9f, 0.94f, 1.05f, 0.98f, 1f),
                ObjectAnimator.ofFloat(petLayer, View.ROTATION, 0f, -4f, 4f, -2f, 0f));
        petAnimation.setDuration(1100L);
        petAnimation.setInterpolator(new DecelerateInterpolator());
        petAnimation.start();

        popupAnimation = new AnimatorSet();
        popupAnimation.playTogether(
                ObjectAnimator.ofFloat(popup, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(popup, View.SCALE_X, 0.82f, 1.06f, 1f),
                ObjectAnimator.ofFloat(popup, View.SCALE_Y, 0.82f, 1.06f, 1f));
        popupAnimation.setStartDelay(180L);
        popupAnimation.setDuration(300L);
        popupAnimation.setInterpolator(new DecelerateInterpolator());
        popupAnimation.start();
    }

    void cancel() {
        if (petAnimation != null) {
            petAnimation.cancel();
            petAnimation = null;
        }
        if (popupAnimation != null) {
            popupAnimation.cancel();
            popupAnimation = null;
        }
        petLayer.setAlpha(1f);
        petLayer.setTranslationY(0f);
        petLayer.setRotation(0f);
        petLayer.setScaleX(1f);
        petLayer.setScaleY(1f);
        popup.setAlpha(1f);
        popup.setScaleX(1f);
        popup.setScaleY(1f);
    }
}
