package com.example.focuspet;

import android.icu.text.BreakIterator;
import java.util.Locale;

final class SessionLabels {
    private SessionLabels() { }

    /** Keep complete grapheme clusters, including emoji, combining marks and ZWJ sequences. */
    static String abbreviate(String label, int maxCharacters) {
        if (maxCharacters < 1) throw new IllegalArgumentException("maxCharacters must be positive");
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(label);
        int boundary = iterator.first();
        for (int i = 0; i < maxCharacters; i++) {
            int next = iterator.next();
            if (next == BreakIterator.DONE) return label;
            boundary = next;
        }
        return iterator.next() == BreakIterator.DONE ? label : label.substring(0, boundary) + "...";
    }
}
