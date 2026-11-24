package com.example.focuspet;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** Built-in, whole-pet art. Keys are saved per pet in Room. */
enum PetLook {
    ORIGINAL("original", R.string.pet_look_original, 0),
    CLASSIC("classic_cat", R.string.pet_look_classic, R.drawable.pet_look_classic),
    HEADPHONES("headphones_cat", R.string.pet_look_headphones, R.drawable.pet_look_headphones),
    WINTER("winter_cat", R.string.pet_look_winter, R.drawable.pet_look_winter),
    MINT_STAR("mint_star", R.string.pet_look_mint_star, R.drawable.pet_look_mint_star),
    CLOUD_WINGS("cloud_wings", R.string.pet_look_cloud_wings, R.drawable.pet_look_cloud_wings),
    MIDNIGHT_DJ("midnight_dj", R.string.pet_look_midnight_dj, R.drawable.pet_look_midnight_dj),
    GARDEN_CHILL("garden_chill", R.string.pet_look_garden_chill, R.drawable.pet_look_garden_chill),
    JUICE_BREAK("juice_break", R.string.pet_look_juice_break, R.drawable.pet_look_juice_break),
    BLUSH_WINTER("blush_winter", R.string.pet_look_blush_winter, R.drawable.pet_look_blush_winter),
    STRAWBERRY_NIGHT("strawberry_night", R.string.pet_look_strawberry_night,
            R.drawable.pet_look_strawberry_night);

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

    static PetLook fromKey(String key) {
        for (PetLook look : values()) {
            if (look.key.equals(key)) return look;
        }
        return null;
    }

    boolean canChoose(Pet pet) {
        // A cat's original art is already the classic look.
        return this != CLASSIC || !"Cat".equalsIgnoreCase(pet.species);
    }

    boolean isFree() {
        return this == CLASSIC || this == HEADPHONES || this == WINTER;
    }

    @DrawableRes int artworkFor(Pet pet) {
        if (this == ORIGINAL && pet != null && "Cat".equalsIgnoreCase(pet.species)) {
            return CLASSIC.artwork;
        }
        return artwork;
    }
}
