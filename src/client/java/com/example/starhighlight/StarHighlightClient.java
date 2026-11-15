package com.example.starhighlight;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class StarHighlightClient implements ClientModInitializer {
    // U+272F PINWHEEL STAR
    public static final String STAR = "\u272F";
    // Gold outline
    public static final int COLOR = 0xFFAA00;

    private static volatile IntSet highlighted = new IntOpenHashSet();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(StarHighlightClient::scan);
    }

    public static boolean isHighlighted(Entity entity) {
        return highlighted.contains(entity.getId());
    }

    private static boolean hasStarName(Entity e) {
        if (!e.hasCustomName()) return false;
        var name = e.getCustomName();
        return name != null && name.getString().contains(STAR);
    }

    private static void scan(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            highlighted = new IntOpenHashSet();
            return;
        }

        IntSet next = new IntOpenHashSet();

        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof Player || !hasStarName(e)) continue;

            if (e instanceof ArmorStand stand) {
                // SkyBlock puts the name on an invisible armor stand above the real mob.
                // Highlight the closest real mob under the nametag instead.
                AABB box = new AABB(
                        stand.getX() - 1.0, stand.getY() - 3.0, stand.getZ() - 1.0,
                        stand.getX() + 1.0, stand.getY() + 0.5, stand.getZ() + 1.0);
                List<Entity> nearby = level.getEntities(stand, box,
                        o -> o instanceof LivingEntity && !(o instanceof ArmorStand) && !(o instanceof Player));
                Entity best = null;
                double bestDist = Double.MAX_VALUE;
                for (Entity o : nearby) {
                    double d = o.distanceToSqr(stand);
                    if (d < bestDist) {
                        bestDist = d;
                        best = o;
                    }
                }
                if (best != null) next.add(best.getId());
            } else {
                // Name is on the mob itself
                next.add(e.getId());
            }
        }

        highlighted = next;
    }
}
