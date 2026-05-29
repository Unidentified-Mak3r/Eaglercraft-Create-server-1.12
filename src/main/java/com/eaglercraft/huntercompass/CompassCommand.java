package com.eaglercraft.huntercompass;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /unbindcompass command implementation.
 */
public final class CompassCommand implements CommandExecutor {
    private final TrackingManager trackingManager;

    public CompassCommand(TrackingManager trackingManager) {
        this.trackingManager = trackingManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("HunterCompass commands can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("huntercompass.unbind")) {
            player.sendMessage(trackingManager.color("&cYou do not have permission to use this command."));
            return true;
        }

        ItemStack held = player.getItemInHand();
        if (!CompassItemData.isCompass(held)) {
            player.sendMessage(trackingManager.message("hold-compass"));
            return true;
        }
        if (!trackingManager.unbindHeldCompass(player)) {
            player.sendMessage(trackingManager.message("not-bound"));
            return true;
        }
        player.sendMessage(trackingManager.message("unbound"));
        return true;
    }
}
