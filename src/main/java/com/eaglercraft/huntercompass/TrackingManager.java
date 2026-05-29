package com.eaglercraft.huntercompass;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the once-per-second tracking task and the persistent last-known target cache.
 */
public final class TrackingManager {
    private final HunterCompassPlugin plugin;
    private final Map<UUID, TargetData> lastKnownTargets = new HashMap<UUID, TargetData>();
    private final Set<String> offlineNotifications = new HashSet<String>();
    private BukkitTask task;
    private File targetFile;
    private FileConfiguration targetConfig;

    public TrackingManager(HunterCompassPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tickOnlinePlayers();
            }
        }.runTaskTimer(plugin, 20L, getTrackingIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public long getTrackingIntervalTicks() {
        return Math.max(20L, plugin.getConfig().getLong("tracking-interval-ticks", 20L));
    }

    public void rememberTarget(Player target) {
        if (target != null && target.getWorld() != null) {
            lastKnownTargets.put(target.getUniqueId(), TargetData.fromLocation(target.getUniqueId(), target.getName(), target.getLocation()));
        }
    }

    public TargetData getBestTargetData(TargetData itemData) {
        if (itemData == null) {
            return null;
        }
        Player online = Bukkit.getPlayer(itemData.getTargetUuid());
        if (online != null && online.isOnline()) {
            TargetData live = TargetData.fromLocation(online.getUniqueId(), online.getName(), online.getLocation());
            lastKnownTargets.put(online.getUniqueId(), live);
            return live;
        }
        TargetData cached = lastKnownTargets.get(itemData.getTargetUuid());
        return cached != null ? cached : itemData;
    }

    public void bindCompass(Player hunter, Player target, ItemStack compass) {
        if (hunter == null || target == null || !CompassItemData.isCompass(compass)) {
            return;
        }
        TargetData data = TargetData.fromLocation(target.getUniqueId(), target.getName(), target.getLocation());
        lastKnownTargets.put(target.getUniqueId(), data);
        hunter.setItemInHand(CompassItemData.writeTarget(compass, data));
        updateCompassTarget(hunter, data, true);
        hunter.sendMessage(message("bound").replace("%target%", target.getName()));
    }

    public boolean unbindHeldCompass(Player player) {
        if (player == null) {
            return false;
        }
        ItemStack held = player.getItemInHand();
        if (!CompassItemData.isCompass(held) || CompassItemData.readTarget(held) == null) {
            return false;
        }
        player.setItemInHand(CompassItemData.clearTarget(held));
        return true;
    }

    public void sendTargetInfo(Player player, TargetData itemData) {
        if (player == null || itemData == null) {
            return;
        }
        TargetData data = getBestTargetData(itemData);
        Player target = Bukkit.getPlayer(itemData.getTargetUuid());
        boolean online = target != null && target.isOnline();
        Location location = online ? target.getLocation() : data.toLocation();
        String distance = formatDistance(player, location, data);
        List<String> lines = plugin.getConfig().getStringList("messages.target-info");
        for (String line : lines) {
            player.sendMessage(color(line
                    .replace("%target%", data.getTargetName())
                    .replace("%status%", online ? "Online" : "Offline")
                    .replace("%distance%", distance)
                    .replace("%world%", data.getWorldName())
                    .replace("%x%", String.valueOf(data.getX()))
                    .replace("%y%", String.valueOf(data.getY()))
                    .replace("%z%", String.valueOf(data.getZ()))));
        }
        if (!online) {
            player.sendMessage(message("target-offline").replace("%target%", data.getTargetName()));
        }
    }

    private void tickOnlinePlayers() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player online : onlinePlayers) {
            rememberTarget(online);
        }

        for (Player hunter : onlinePlayers) {
            if (hunter == null || !hunter.isOnline()) {
                continue;
            }
            ItemStack held = hunter.getItemInHand();
            TargetData itemData = CompassItemData.readTarget(held);
            if (itemData == null) {
                continue;
            }
            TargetData bestData = getBestTargetData(itemData);
            if (bestData == null) {
                continue;
            }

            ItemStack refreshed = CompassItemData.writeTarget(held, bestData);
            hunter.setItemInHand(refreshed);
            updateCompassTarget(hunter, bestData, false);
            notifyOfflineOnce(hunter, bestData);
        }
    }

    private void updateCompassTarget(Player hunter, TargetData data, boolean force) {
        if (hunter == null || data == null || hunter.getWorld() == null) {
            return;
        }
        Player target = Bukkit.getPlayer(data.getTargetUuid());
        if (target != null && target.isOnline() && target.getWorld() != null
                && target.getWorld().getName().equals(hunter.getWorld().getName())) {
            hunter.setCompassTarget(target.getLocation());
            offlineNotifications.remove(notificationKey(hunter.getUniqueId(), data.getTargetUuid()));
            return;
        }

        // Cross-world compass targets can desync on legacy/browser clients, so project the
        // target's last-known coordinates into the hunter's current world for stable needles.
        Location projected = data.toLocationInWorld(hunter.getWorld());
        if (projected != null) {
            Location current = hunter.getCompassTarget();
            boolean sameWorld = current != null && current.getWorld() != null
                    && current.getWorld().getName().equals(projected.getWorld().getName());
            if (force || current == null || !sameWorld || current.distanceSquared(projected) > 1.0D) {
                hunter.setCompassTarget(projected);
            }
        }
    }

    private void notifyOfflineOnce(Player hunter, TargetData data) {
        Player target = Bukkit.getPlayer(data.getTargetUuid());
        boolean online = target != null && target.isOnline();
        String key = notificationKey(hunter.getUniqueId(), data.getTargetUuid());
        if (online) {
            offlineNotifications.remove(key);
            return;
        }
        if (offlineNotifications.add(key)) {
            hunter.sendMessage(message("target-offline").replace("%target%", data.getTargetName()));
        }
    }

    private String formatDistance(Player hunter, Location targetLocation, TargetData data) {
        if (hunter == null || targetLocation == null || hunter.getWorld() == null || targetLocation.getWorld() == null) {
            return "Unknown";
        }
        if (!hunter.getWorld().getName().equals(targetLocation.getWorld().getName())) {
            World hunterWorld = hunter.getWorld();
            Location projected = data.toLocationInWorld(hunterWorld);
            if (projected == null) {
                return "Different world";
            }
            return Math.round(hunter.getLocation().distance(projected)) + " blocks (last known coordinates, different world)";
        }
        return Math.round(hunter.getLocation().distance(targetLocation)) + " blocks";
    }

    public String message(String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&6[HunterCompass]&r ");
        String body = plugin.getConfig().getString("messages." + key, "");
        return color(prefix + body);
    }

    public String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public void loadLastKnownTargets() {
        targetFile = new File(plugin.getDataFolder(), "targets.yml");
        targetConfig = YamlConfiguration.loadConfiguration(targetFile);
        ConfigurationSection section = targetConfig.getConfigurationSection("targets");
        if (section == null) {
            return;
        }
        for (String uuidText : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                String path = "targets." + uuidText + ".";
                TargetData data = new TargetData(uuid,
                        targetConfig.getString(path + "name", "Unknown"),
                        targetConfig.getString(path + "world", "world"),
                        targetConfig.getDouble(path + "x"),
                        targetConfig.getDouble(path + "y"),
                        targetConfig.getDouble(path + "z"));
                lastKnownTargets.put(uuid, data);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Skipping invalid target UUID in targets.yml: " + uuidText);
            }
        }
    }

    public void saveLastKnownTargets() {
        if (targetFile == null) {
            targetFile = new File(plugin.getDataFolder(), "targets.yml");
        }
        if (targetConfig == null) {
            targetConfig = new YamlConfiguration();
        }
        targetConfig.set("targets", null);
        for (Map.Entry<UUID, TargetData> entry : lastKnownTargets.entrySet()) {
            TargetData data = entry.getValue();
            String path = "targets." + entry.getKey().toString() + ".";
            targetConfig.set(path + "name", data.getTargetName());
            targetConfig.set(path + "world", data.getWorldName());
            targetConfig.set(path + "x", data.getX());
            targetConfig.set(path + "y", data.getY());
            targetConfig.set(path + "z", data.getZ());
        }
        try {
            targetConfig.save(targetFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save targets.yml: " + ex.getMessage());
        }
    }

    private String notificationKey(UUID hunter, UUID target) {
        return hunter.toString() + ':' + target.toString();
    }
}
