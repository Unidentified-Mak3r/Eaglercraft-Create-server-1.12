package com.eaglercraft.huntercompass;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Version-specific NBT adapter for legacy Eaglercraft/CraftBukkit 1.8_R3 servers.
 *
 * Keeping all NMS calls in this class makes the rest of the plugin clean and avoids
 * newer Paper PersistentDataContainer APIs that are not available on Eaglercraft servers.
 */
public final class CompassItemData {
    private static final String TAG_ROOT = "HunterCompass";
    private static final String TAG_UUID = "TargetUuid";
    private static final String TAG_NAME = "TargetName";
    private static final String TAG_WORLD = "TargetWorld";
    private static final String TAG_X = "LastX";
    private static final String TAG_Y = "LastY";
    private static final String TAG_Z = "LastZ";

    private CompassItemData() {
    }

    public static boolean isCompass(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.getAmount() > 0;
    }

    public static TargetData readTarget(ItemStack item) {
        if (!isCompass(item)) {
            return null;
        }
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        if (nms == null || !nms.hasTag()) {
            return null;
        }
        NBTTagCompound tag = nms.getTag();
        if (tag == null || !tag.hasKey(TAG_ROOT)) {
            return null;
        }
        NBTTagCompound root = tag.getCompound(TAG_ROOT);
        String uuidValue = root.getString(TAG_UUID);
        if (uuidValue == null || uuidValue.length() == 0) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(uuidValue);
            return new TargetData(uuid, root.getString(TAG_NAME), root.getString(TAG_WORLD),
                    root.getDouble(TAG_X), root.getDouble(TAG_Y), root.getDouble(TAG_Z));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static ItemStack writeTarget(ItemStack item, TargetData targetData) {
        if (!isCompass(item) || targetData == null) {
            return item;
        }
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nms.hasTag() ? nms.getTag() : new NBTTagCompound();
        NBTTagCompound root = new NBTTagCompound();
        root.setString(TAG_UUID, targetData.getTargetUuid().toString());
        root.setString(TAG_NAME, safe(targetData.getTargetName()));
        root.setString(TAG_WORLD, safe(targetData.getWorldName()));
        root.setDouble(TAG_X, targetData.getX());
        root.setDouble(TAG_Y, targetData.getY());
        root.setDouble(TAG_Z, targetData.getZ());
        tag.set(TAG_ROOT, root);
        nms.setTag(tag);
        return CraftItemStack.asBukkitCopy(nms);
    }

    public static ItemStack clearTarget(ItemStack item) {
        if (!isCompass(item)) {
            return item;
        }
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        if (nms == null || !nms.hasTag()) {
            return item;
        }
        NBTTagCompound tag = nms.getTag();
        tag.remove(TAG_ROOT);
        nms.setTag(tag);
        return CraftItemStack.asBukkitCopy(nms);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
