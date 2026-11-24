package com.example.focuspet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Typeface;

import java.util.Random;
import java.util.Collections;
import java.util.Set;

final class PetAnimationController {
    enum Mode {
        AMBIENT,
        INTERACTIVE
    }

    private static final long EXPRESSION_DURATION_MS = 1600L;
    private static final long IDLE_DURATION_MS = 2200L;
    private static final long AMBIENT_BUBBLE_DURATION_MS = 2000L;
    private static final long AMBIENT_INITIAL_DELAY_MIN_MS = 15_000L;
    private static final long AMBIENT_INITIAL_DELAY_MAX_MS = 30_000L;
    private static final long AMBIENT_CHECK_MIN_MS = 30_000L;
    private static final long AMBIENT_CHECK_MAX_MS = 60_000L;
    private static final float AMBIENT_BUBBLE_CHANCE = 0.25f;

    private final View host;
    private final View artworkLayer;
    private final View expressionBubble;
    private final ImageView expressionBubbleImage;
    private final TextView expressionView;
    private final Mode mode;
    private final Context context;
    private final String baseContentDescription;
    private Set<String> enabledExpressionKeys = Collections.emptySet();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random ambientRandom = new Random();
    private final PetBubblePolicy bubblePolicy = new PetBubblePolicy();
    private final View.OnAttachStateChangeListener attachListener;

    private PetExpression restingExpression = PetExpression.NEUTRAL;
    private AnimatorSet idleAnimator;
    private AnimatorSet interactionAnimator;
    private AnimatorSet bubbleEntranceAnimator;
    private boolean activityResumed;
    private boolean attached;
    private boolean disposed;
    private boolean transientExpression;

    private final Runnable restoreExpression = () -> {
        transientExpression = false;
        hideExpression();
    };
    private final Runnable ambientBubbleCheck = () -> {
        if (!canRunAmbientBubbleSchedule()) return;
        long now = SystemClock.elapsedRealtime();
        PetExpression expression = PetExpressionSelector.choose(
                restingExpression, enabledExpressionKeys,
                PetExpression.CONTEXT_AMBIENT, ambientRandom);
        if (!transientExpression && interactionAnimator == null
                && bubblePolicy.canShowAmbient(expression, now)
                && ambientRandom.nextFloat() < AMBIENT_BUBBLE_CHANCE) {
            showTransientExpression(expression, AMBIENT_BUBBLE_DURATION_MS, true);
        }
        scheduleNextAmbientBubbleCheck();
    };

    PetAnimationController(View host, View artworkLayer, View expressionBubble,
                           ImageView expressionBubbleImage, TextView expressionView, Mode mode,
                           Set<String> enabledExpressionKeys) {
        this.host = host;
        this.artworkLayer = artworkLayer;
        this.expressionBubble = expressionBubble;
        this.expressionBubbleImage = expressionBubbleImage;
        this.expressionView = expressionView;
        this.mode = mode;
        this.context = host.getContext();
        this.enabledExpressionKeys = enabledExpressionKeys == null
                ? Collections.emptySet() : enabledExpressionKeys;
        CharSequence description = host.getContentDescription();
        this.baseContentDescription = description == null ? "" : description.toString();
        this.attached = host.isAttachedToWindow();
        expressionView.setTypeface(Typeface.MONOSPACE);
        expressionView.setMaxLines(2);
        expressionView.setAutoSizeTextTypeUniformWithConfiguration(
                6, 16, 1, TypedValue.COMPLEX_UNIT_SP);
        hideExpression();

        attachListener = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                attached = true;
                startIdleIfReady();
                startAmbientBubbleSchedule();
            }

            @Override public void onViewDetachedFromWindow(View view) {
                attached = false;
                stopAmbientBubbleSchedule();
                clearTransientExpression();
                stopMotion();
                hideExpression();
            }
        };
        host.addOnAttachStateChangeListener(attachListener);
    }

    void setRestingExpression(PetExpression expression) {
        restingExpression = expression;
        if (expression == PetExpression.NEUTRAL && !hasAmbientExpression()) {
            stopAmbientBubbleSchedule();
        }
        else startAmbientBubbleSchedule();
    }

    void resume() {
        if (disposed) return;
        activityResumed = true;
        attached = host.isAttachedToWindow();
        startIdleIfReady();
        startAmbientBubbleSchedule();
    }

    void pause() {
        activityResumed = false;
        stopAmbientBubbleSchedule();
        clearTransientExpression();
        stopMotion();
        hideExpression();
    }

    void dispose() {
        if (disposed) return;
        pause();
        disposed = true;
        host.removeOnAttachStateChangeListener(attachListener);
    }

    void playInteraction(PetInteraction.Action action) {
        if (disposed || mode != Mode.INTERACTIVE) return;
        PetExpression expression = PetExpressionSelector.choose(
                PetExpression.forAction(action), enabledExpressionKeys,
                PetExpression.contextForAction(action), ambientRandom);
        showTransientExpression(expression, EXPRESSION_DURATION_MS);
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
        showTransientExpression(expression, durationMs, false);
    }

    private void showTransientExpression(
            PetExpression expression, long durationMs, boolean ambient) {
        if (disposed) return;
        handler.removeCallbacks(restoreExpression);
        if (durationMs <= 0 || expression == PetExpression.NEUTRAL) {
            transientExpression = false;
            hideExpression();
            return;
        }
        transientExpression = true;
        showExpression(expression);
        long now = SystemClock.elapsedRealtime();
        if (ambient) bubblePolicy.recordAmbientBubble(now);
        else bubblePolicy.recordBubble(now);
        handler.postDelayed(restoreExpression, durationMs);
    }

    private boolean canRunAmbientBubbleSchedule() {
        return (restingExpression != PetExpression.NEUTRAL || hasAmbientExpression())
                && !disposed && attached && activityResumed
                && host.isShown() && host.getWindowVisibility() == View.VISIBLE;
    }

    private boolean hasAmbientExpression() {
        for (String key : enabledExpressionKeys) {
            PetExpression expression = PetExpression.fromKey(key);
            if (expression != null && expression.learnable
                    && expression.supportsContext(PetExpression.CONTEXT_AMBIENT)) {
                return true;
            }
        }
        return false;
    }

    private void startAmbientBubbleSchedule() {
        if (!canRunAmbientBubbleSchedule()) return;
        handler.removeCallbacks(ambientBubbleCheck);
        handler.postDelayed(ambientBubbleCheck,
                randomDelay(AMBIENT_INITIAL_DELAY_MIN_MS, AMBIENT_INITIAL_DELAY_MAX_MS));
    }

    private void scheduleNextAmbientBubbleCheck() {
        if (!canRunAmbientBubbleSchedule()) return;
        handler.postDelayed(ambientBubbleCheck,
                randomDelay(AMBIENT_CHECK_MIN_MS, AMBIENT_CHECK_MAX_MS));
    }

    private void stopAmbientBubbleSchedule() {
        handler.removeCallbacks(ambientBubbleCheck);
    }

    private long randomDelay(long minimum, long maximum) {
        return minimum + ambientRandom.nextInt((int) (maximum - minimum + 1));
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

    private void showExpression(PetExpression expression) {
        if (expression == PetExpression.NEUTRAL) {
            hideExpression();
            return;
        }
        if (expressionBubbleImage.getDrawable() == null) {
            expressionBubbleImage.setImageResource(R.drawable.bubble_thinking);
        }
        expressionView.setText(expression.asciiArt);
        expressionView.setVisibility(View.VISIBLE);
        expressionBubble.setVisibility(View.VISIBLE);
        animateBubbleEntrance();
        if (baseContentDescription.isEmpty()) return;
        host.setContentDescription(context.getString(
                R.string.pet_expression_content_description,
                baseContentDescription, context.getString(expression.descriptionRes)));
    }

    private void hideExpression() {
        cancelBubbleEntrance();
        expressionView.setVisibility(View.GONE);
        expressionBubble.setVisibility(View.GONE);
        expressionBubble.setAlpha(1f);
        expressionBubble.setScaleX(1f);
        expressionBubble.setScaleY(1f);
        host.setContentDescription(baseContentDescription);
    }

    private void animateBubbleEntrance() {
        cancelBubbleEntrance();
        expressionBubble.setAlpha(0f);
        expressionBubble.setScaleX(0.9f);
        expressionBubble.setScaleY(0.9f);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            expressionBubble.setAlpha(1f);
            expressionBubble.setScaleX(1f);
            expressionBubble.setScaleY(1f);
            return;
        }

        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(
                ObjectAnimator.ofFloat(expressionBubble, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(expressionBubble, View.SCALE_X, 0.9f, 1f),
                ObjectAnimator.ofFloat(expressionBubble, View.SCALE_Y, 0.9f, 1f));
        animator.setDuration(220L);
        animator.setInterpolator(new DecelerateInterpolator());
        bubbleEntranceAnimator = animator;
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (bubbleEntranceAnimator == animator) bubbleEntranceAnimator = null;
            }
        });
        animator.start();
    }

    private void cancelBubbleEntrance() {
        AnimatorSet previous = bubbleEntranceAnimator;
        bubbleEntranceAnimator = null;
        if (previous != null) previous.cancel();
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
