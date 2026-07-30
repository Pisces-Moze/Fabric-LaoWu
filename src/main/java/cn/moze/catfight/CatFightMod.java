package cn.moze.catfight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class CatFightMod implements ModInitializer {
    public static final String MOD_ID = "catfight";
    public static final Identifier HISS_ID = Identifier.of(MOD_ID, "cat_hiss");
    public static final Identifier TUSSLE_ID = Identifier.of(MOD_ID, "cat_tussle");
    public static final SoundEvent CAT_HISS = SoundEvent.of(HISS_ID);
    public static final SoundEvent CAT_TUSSLE = SoundEvent.of(TUSSLE_ID);

    @Override
    public void onInitialize() {
        Registry.register(Registries.SOUND_EVENT, HISS_ID, CAT_HISS);
        Registry.register(Registries.SOUND_EVENT, TUSSLE_ID, CAT_TUSSLE);
        ServerTickEvents.END_WORLD_TICK.register(CatFightManager::tick);
    }
}
