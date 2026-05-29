package com.eaglercraft.huntercompass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Immutable snapshot of a compass target stored on the item and in memory.
 */
public final class TargetData {
    private final UUID targetUuid;
    private final String targetName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    public TargetData(UUID targetUuid, String targetName, String worldName, double x, double y, double z) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static TargetData fromLocation(UUID targetUuid, String targetName, Location location) {
        String world = location != null && location.getWorld() != null ? location.getWorld().getName() : "world";
        return new TargetData(targetUuid, targetName, world,
                location != null ? location.getX() : 0.0D,
                location != null ? location.getY() : 64.0D,
                location != null ? location.getZ() : 0.0D);
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    public Location toLocationInWorld(World world) {
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
