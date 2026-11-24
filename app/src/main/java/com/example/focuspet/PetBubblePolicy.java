package com.example.focuspet;

import java.util.ArrayDeque;
import java.util.Deque;

final class PetBubblePolicy {
    private static final long AMBIENT_COOLDOWN_MS = 45_000L;
    private static final long AMBIENT_WINDOW_MS = 5 * 60_000L;
    private static final int MAX_AMBIENT_BUBBLES_PER_WINDOW = 3;

    private final Deque<Long> ambientBubbleTimes = new ArrayDeque<>();
    private boolean hasShownBubble;
    private long lastBubbleTime;

    boolean canShowAmbient(PetExpression expression, long now) {
        discardExpiredAmbientBubbles(now);
        return expression != PetExpression.NEUTRAL
                && (!hasShownBubble || now - lastBubbleTime >= AMBIENT_COOLDOWN_MS)
                && ambientBubbleTimes.size() < MAX_AMBIENT_BUBBLES_PER_WINDOW;
    }

    void recordBubble(long now) {
        hasShownBubble = true;
        lastBubbleTime = now;
    }

    void recordAmbientBubble(long now) {
        recordBubble(now);
        ambientBubbleTimes.addLast(now);
    }

    private void discardExpiredAmbientBubbles(long now) {
        while (!ambientBubbleTimes.isEmpty()
                && now - ambientBubbleTimes.peekFirst() >= AMBIENT_WINDOW_MS) {
            ambientBubbleTimes.removeFirst();
        }
    }
}
