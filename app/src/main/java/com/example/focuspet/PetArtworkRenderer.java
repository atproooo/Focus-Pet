package com.example.focuspet;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

final class PetArtworkRenderer {
    void bind(View host, Pet pet, PetLook look, String accessoryKey, String backgroundKey) {
        Context context = host.getContext();
        ImageView background = host.findViewById(R.id.pet_background_image);
        if (background != null) {
            if (backgroundKey == null) {
                background.setVisibility(View.GONE);
            } else {
                BackgroundCatalog.Background scene = BackgroundCatalog.fromKey(backgroundKey);
                if (scene == null) {
                    throw new IllegalStateException("Unknown pet background: " + backgroundKey);
                }
                background.setImageResource(scene.drawable);
                background.setVisibility(View.VISIBLE);
            }
        }
        ImageView art = host.findViewById(R.id.pet_animation_image);
        art.setTag(pet == null ? null : pet.imageKey);
        int artwork = look.artworkFor(pet);
        art.setImageResource(artwork);
        art.setVisibility(artwork != 0 ? View.VISIBLE : View.GONE);
        AccessoryCatalog.Accessory accessory = artwork == 0
                ? null : AccessoryCatalog.fromKey(accessoryKey);
        PetAccessoryLayer backLayer = host.findViewById(R.id.pet_accessory_back_layer);
        PetAccessoryLayer frontLayer = host.findViewById(R.id.pet_accessory_front_layer);
        if (backLayer != null) {
            backLayer.bind(art, accessory != null
                    && accessory.layer == AccessoryCatalog.Layer.BEHIND ? accessory : null);
        }
        if (frontLayer != null) {
            frontLayer.bind(art, accessory != null
                    && accessory.layer == AccessoryCatalog.Layer.IN_FRONT ? accessory : null);
        }

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
