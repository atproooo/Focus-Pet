package com.example.focuspet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.graphics.Typeface;

final class PetAnimationController {
    enum Mode {
        AMBIENT,
        INTERACTIVE
    }

    private static final long EXPRESSION_DURATION_MS = 1600L;
    private static final long IDLE_DURATION_MS = 2200L;

    private final View host;
    private final View artworkLayer;
    private final TextView expressionView;
    private final Mode mode;
    private final Context context;
    private final String baseContentDescription;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final View.OnAttachStateChangeListener attachListener;

    private PetExpression restingExpression = PetExpression.NEUTRAL;
    private AnimatorSet idleAnimator;
    private AnimatorSet interactionAnimator;
    private boolean activityResumed;
    private boolean attached;
    private boolean disposed;
    private boolean transientExpression;

    private final Runnable restoreExpression = () -> {
        transientExpression = false;
        applyExpression(restingExpression);
    };

    PetAnimationController(View host, View artworkLayer, TextView expressionView, Mode mode) {
        this.host = host;
        this.artworkLayer = artworkLayer;
        this.expressionView = expressionView;
        this.mode = mode;
        this.context = host.getContext();
        CharSequence description = host.getContentDescription();
        this.baseContentDescription = description == null ? "" : description.toString();
        this.attached = host.isAttachedToWindow();
        expressionView.setTypeface(Typeface.MONOSPACE);
        expressionView.setVisibility(View.GONE);

        attachListener = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                attached = true;
                startIdleIfReady();
            }

            @Override public void onViewDetachedFromWindow(View view) {
                attached = false;
                clearTransientExpression();
                stopMotion();
                applyExpression(restingExpression);
            }
        };
        host.addOnAttachStateChangeListener(attachListener);
    }

    void setRestingExpression(PetExpression expression) {
        restingExpression = expression;
        if (!transientExpression) applyExpression(expression);
    }

    void resume() {
        if (disposed) return;
        activityResumed = true;
        attached = host.isAttachedToWindow();
        startIdleIfReady();
    }

    void pause() {
        activityResumed = false;
        clearTransientExpression();
        stopMotion();
        applyExpression(restingExpression);
    }

    void dispose() {
        if (disposed) return;
        pause();
        disposed = true;
        host.removeOnAttachStateChangeListener(attachListener);
    }

    void playInteraction(PetInteraction.Action action) {
        if (disposed || mode != Mode.INTERACTIVE) return;
        showTransientExpression(PetExpression.forAction(action), EXPRESSION_DURATION_MS);
        if (!ValueAnimator.areAnimatorsEnabled()) return;

        stopMotion();
        AnimatorSet animator = createInteractionAnimator(action);
        interactionAnimator = animator;
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (interactionAnimator != animator) return;
                interactionAnimator = null;
                resetArtworkTransform();
                startIdleIfReady();
            }
        });
        animator.start();
    }

    void showTransientExpression(PetExpression expression, long durationMs) {
        if (disposed) return;
        handler.removeCallbacks(restoreExpression);
        transientExpression = durationMs > 0;
        applyExpression(expression);
        if (transientExpression) {
            handler.postDelayed(restoreExpression, durationMs);
        } else {
            applyExpression(restingExpression);
        }
    }

    private void startIdleIfReady() {
        if (disposed || !attached || !activityResumed || idleAnimator != null
                || interactionAnimator != null || !ValueAnimator.areAnimatorsEnabled()) {
            return;
        }
        ObjectAnimator bob = ObjectAnimator.ofFloat(
                artworkLayer, View.TRANSLATION_Y, 0f, dp(-3f), 0f);
        ObjectAnimator breatheX = ObjectAnimator.ofFloat(
                artworkLayer, View.SCALE_X, 1f, 1.012f, 1f);
        ObjectAnimator breatheY = ObjectAnimator.ofFloat(
                artworkLayer, View.SCALE_Y, 1f, 1.012f, 1f);
        for (ObjectAnimator animator : new ObjectAnimator[]{bob, breatheX, breatheY}) {
            animator.setDuration(IDLE_DURATION_MS);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
        }
        idleAnimator = new AnimatorSet();
        idleAnimator.playTogether(bob, breatheX, breatheY);
        idleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        idleAnimator.start();
    }

    private AnimatorSet createInteractionAnimator(PetInteraction.Action action) {
        AnimatorSet animator = new AnimatorSet();
        switch (action) {
            case PET:
                ObjectAnimator wiggle = ObjectAnimator.ofFloat(
                        artworkLayer, View.ROTATION, 0f, -3f, 3f, -2f, 2f, 0f);
                wiggle.setDuration(360L);
                animator.playTogether(wiggle);
                break;
            case PLAY:
                ObjectAnimator hop = ObjectAnimator.ofFloat(
                        artworkLayer, View.TRANSLATION_Y, 0f, dp(-12f), 0f, dp(-7f), 0f);
                ObjectAnimator squashX = ObjectAnimator.ofFloat(
                        artworkLayer, View.SCALE_X, 1f, 1.04f, 1f, 1.02f, 1f);
                ObjectAnimator squashY = ObjectAnimator.ofFloat(
                        artworkLayer, View.SCALE_Y, 1f, 0.97f, 1f, 0.99f, 1f);
                hop.setDuration(520L);
                squashX.setDuration(520L);
                squashY.setDuration(520L);
                animator.playTogether(hop, squashX, squashY);
                break;
            case REST:
                ObjectAnimator settle = ObjectAnimator.ofFloat(
                        artworkLayer, View.TRANSLATION_Y, 0f, dp(3f), 0f);
                ObjectAnimator relax = ObjectAnimator.ofFloat(
                        artworkLayer, View.SCALE_X, 1f, 0.99f, 1f);
                settle.setDuration(620L);
                relax.setDuration(620L);
                animator.playTogether(settle, relax);
                break;
            default:
                throw new IllegalArgumentException("Unsupported pet action: " + action);
        }
        animator.setInterpolator(new DecelerateInterpolator());
        return animator;
    }

    private void applyExpression(PetExpression expression) {
        boolean visible = expression != PetExpression.NEUTRAL;
        expressionView.setText(expression.asciiArt);
        expressionView.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible || baseContentDescription.isEmpty()) {
            host.setContentDescription(baseContentDescription);
            return;
        }
        host.setContentDescription(context.getString(
                R.string.pet_expression_content_description,
                baseContentDescription, context.getString(expression.descriptionRes)));
    }

    private void clearTransientExpression() {
        handler.removeCallbacks(restoreExpression);
        transientExpression = false;
    }

    private void stopMotion() {
        AnimatorSet previousInteraction = interactionAnimator;
        interactionAnimator = null;
        if (previousInteraction != null) previousInteraction.cancel();

        AnimatorSet previousIdle = idleAnimator;
        idleAnimator = null;
        if (previousIdle != null) previousIdle.cancel();
        resetArtworkTransform();
    }

    private void resetArtworkTransform() {
        artworkLayer.setTranslationY(0f);
        artworkLayer.setRotation(0f);
        artworkLayer.setScaleX(1f);
        artworkLayer.setScaleY(1f);
    }

    private float dp(float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }
}
