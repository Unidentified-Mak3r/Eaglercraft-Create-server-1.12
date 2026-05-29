package com.eaglercraft.huntercompass;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player combat binding and right-click target information.
 */
public final class CompassListener implements Listener {
    private final TrackingManager trackingManager;

    public CompassListener(TrackingManager trackingManager) {
        this.trackingManager = trackingManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getEntity();
        Player hunter = getAttackingPlayer(event.getDamager());
        if (hunter == null || hunter.equals(target) || !hunter.hasPermission("huntercompass.use")) {
            return;
        }

        ItemStack held = hunter.getItemInHand();
        if (!CompassItemData.isCompass(held)) {
            return;
        }

        TargetData existing = CompassItemData.readTarget(held);
        if (existing != null) {
            hunter.sendMessage(trackingManager.message("already-bound").replace("%target%", existing.getTargetName()));
            return;
        }

        trackingManager.bindCompass(hunter, target, held);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCompassRightClick(PlayerInteractEvent event) {
        if (event.getPlayer() == null || !event.hasItem() || !CompassItemData.isCompass(event.getItem())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        TargetData targetData = CompassItemData.readTarget(event.getItem());
        if (targetData == null) {
            return;
        }
        event.setCancelled(true);
        trackingManager.sendTargetInfo(event.getPlayer(), targetData);
    }

    private Player getAttackingPlayer(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        // Legacy Bukkit projectile APIs differ across Eaglercraft forks; reflection keeps this
        // listener compatible without importing newer ProjectileSource classes unnecessarily.
        try {
            Object shooter = damager.getClass().getMethod("getShooter").invoke(damager);
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
            if (shooter instanceof String) {
                return Bukkit.getPlayerExact((String) shooter);
            }
        } catch (Exception ignored) {
            // Non-projectile damage source; no player to bind.
        }
        return null;
    }
}
