package com.example.focuspet;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Built-in, whole-pet art. Keys are saved per pet in Room. */
enum PetLook {
    ORIGINAL("original", R.string.pet_look_original, 0),
    CLASSIC("classic_cat", R.string.pet_look_classic, R.drawable.pet_look_classic),
    HEADPHONES("headphones_cat", R.string.pet_look_headphones, R.drawable.pet_look_headphones),
    WINTER("winter_cat", R.string.pet_look_winter, R.drawable.pet_look_winter);

    final String key;
    @StringRes final int label;
    @DrawableRes private final int artwork;

    PetLook(String key, @StringRes int label, @DrawableRes int artwork) {
        this.key = key;
        this.label = label;
        this.artwork = artwork;
    }

    static PetLook forPet(Pet pet) {
        if (pet != null) {
            for (PetLook look : values()) {
                if (look.key.equals(pet.appearanceData)) return look;
            }
        }
        return ORIGINAL;
    }

    boolean canChoose(Pet pet) {
        // A cat's original art is already the classic look.
        return this != CLASSIC || !"Cat".equalsIgnoreCase(pet.species);
    }

    @DrawableRes int artworkFor(Pet pet) {
        if (this == ORIGINAL && pet != null && "Cat".equalsIgnoreCase(pet.species)) {
            return CLASSIC.artwork;
        }
        return artwork;
    }
}
