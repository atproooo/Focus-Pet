package com.example.focuspet;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

final class PetArtworkRenderer {
    void bind(View host, Pet pet, PetLook look) {
        Context context = host.getContext();
        ImageView art = host.findViewById(R.id.pet_animation_image);
        art.setTag(pet == null ? null : pet.imageKey);
        int artwork = look.artworkFor(pet);
        art.setImageResource(artwork);
        art.setVisibility(artwork != 0 ? View.VISIBLE : View.GONE);

        TextView placeholder = host.findViewById(R.id.pet_animation_placeholder);
        String name = pet == null || pet.name == null
                ? context.getString(R.string.pet_look_generic_name) : pet.name;
        placeholder.setText(context.getString(R.string.pet_look_placeholder, name));
        placeholder.setVisibility(artwork == 0 ? View.VISIBLE : View.GONE);
        host.setTag(pet == null ? null : pet.id);
        host.setContentDescription(
                context.getString(R.string.pet_look_description, name, context.getString(look.label)));
    }
}
