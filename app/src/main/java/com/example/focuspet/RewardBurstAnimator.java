package com.example.focuspet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Typeface;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class RewardBurstAnimator {
    private static final long PARTICLE_DURATION_MS = 820L;
    private static final long PARTICLE_STAGGER_MS = 24L;

    private final FrameLayout particleLayer;
    private final Context context;
    private final Typeface pixelFont;
    private final List<AnimatorSet> activeAnimations = new ArrayList<>();
    private final Random random = new Random();
    private final Runnable startAfterLayout = this::createBurst;
    private boolean started;
    private boolean cancelled;
    private final List<RewardParticlePlan.Particle> particles;

    RewardBurstAnimator(FrameLayout particleLayer, int xp, int coins, int essence,
                        boolean egg, Typeface pixelFont) {
        this.particleLayer = particleLayer;
        this.context = particleLayer.getContext();
        this.pixelFont = pixelFont;
        this.particles = RewardParticlePlan.create(xp, coins, essence, egg);
    }

    void start() {
        if (started || cancelled || particles.isEmpty()) return;
        started = true;
        particleLayer.post(startAfterLayout);
    }

    void cancel() {
        cancelled = true;
        particleLayer.removeCallbacks(startAfterLayout);
        for (AnimatorSet animator : new ArrayList<>(activeAnimations)) {
            animator.cancel();
        }
        activeAnimations.clear();
        particleLayer.removeAllViews();
    }

    private void createBurst() {
        if (cancelled || !ValueAnimator.areAnimatorsEnabled()
                || particleLayer.getWidth() == 0 || particleLayer.getHeight() == 0) {
            return;
        }
        for (int i = 0; i < particles.size(); i++) {
            addParticle(particles.get(i), i);
        }
    }

    private void addParticle(RewardParticlePlan.Particle particle, int index) {
        View particleView = createParticleView(particle);
        FrameLayout.LayoutParams params;
        if (particle.kind == RewardParticlePlan.Kind.XP) {
            params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
        } else {
            params = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
        }
        particleLayer.addView(particleView, params);

        float maxX = Math.min(dp(70), particleLayer.getWidth() * 0.34f);
        float maxY = Math.min(dp(38), particleLayer.getHeight() * 0.30f);
        float targetX = (random.nextFloat() * 2f - 1f) * maxX;
        float targetY = (random.nextFloat() * 2f - 1f) * maxY - dp(4);
        float rotation = random.nextFloat() * 36f - 18f;
        float peakScale = 0.8f + 0.25f * particle.intensity;

        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(
                ObjectAnimator.ofFloat(particleView, View.ALPHA,
                        0f, particle.intensity, 0f),
                ObjectAnimator.ofFloat(particleView, View.TRANSLATION_X, 0f, targetX),
                ObjectAnimator.ofFloat(particleView, View.TRANSLATION_Y, 0f, targetY),
                ObjectAnimator.ofFloat(particleView, View.SCALE_X, 0.45f, peakScale, 0.75f),
                ObjectAnimator.ofFloat(particleView, View.SCALE_Y, 0.45f, peakScale, 0.75f),
                ObjectAnimator.ofFloat(particleView, View.ROTATION, 0f, rotation));
        animator.setDuration(PARTICLE_DURATION_MS);
        animator.setStartDelay(index * PARTICLE_STAGGER_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        activeAnimations.add(animator);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                activeAnimations.remove(animator);
                if (particleView.getParent() == particleLayer) {
                    particleLayer.removeView(particleView);
                }
            }
        });
        animator.start();
    }

    private View createParticleView(RewardParticlePlan.Particle particle) {
        if (particle.kind == RewardParticlePlan.Kind.XP) {
            TextView xp = new TextView(context);
            xp.setText(context.getString(R.string.session_reward_xp, particle.amount));
            xp.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            xp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            xp.setTypeface(pixelFont == null ? Typeface.MONOSPACE : pixelFont);
            xp.setGravity(Gravity.CENTER);
            xp.setPadding(dp(5), dp(3), dp(5), dp(3));
            xp.setBackground(particleBadge());
            xp.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            return xp;
        }

        ImageView icon = new ImageView(context);
        icon.setImageResource(drawableFor(particle.kind));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setPadding(dp(2), dp(2), dp(2), dp(2));
        icon.setBackground(particleBadge());
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (icon.getDrawable() instanceof BitmapDrawable) {
            ((BitmapDrawable) icon.getDrawable()).setFilterBitmap(false);
        }
        return icon;
    }

    private int drawableFor(RewardParticlePlan.Kind kind) {
        switch (kind) {
            case COIN: return R.drawable.icon_coin;
            case ESSENCE: return R.drawable.icon_essence;
            case EGG: return R.drawable.icon_eggs;
            default: throw new IllegalArgumentException("No image for particle kind: " + kind);
        }
    }

    private GradientDrawable particleBadge() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(context, R.color.background_app));
        background.setCornerRadius(dp(6));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
