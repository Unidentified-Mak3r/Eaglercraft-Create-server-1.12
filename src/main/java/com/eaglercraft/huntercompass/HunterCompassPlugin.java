package com.eaglercraft.huntercompass;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for the HunterCompass plugin.
 */
public final class HunterCompassPlugin extends JavaPlugin {
    private TrackingManager trackingManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        trackingManager = new TrackingManager(this);
        trackingManager.loadLastKnownTargets();

        CompassListener listener = new CompassListener(trackingManager);
        getServer().getPluginManager().registerEvents(listener, this);

        CompassCommand command = new CompassCommand(trackingManager);
        if (getCommand("unbindcompass") != null) {
            getCommand("unbindcompass").setExecutor(command);
        }

        trackingManager.start();
        getLogger().info("HunterCompass enabled with " + trackingManager.getTrackingIntervalTicks() + " tick updates.");
    }

    @Override
    public void onDisable() {
        if (trackingManager != null) {
            trackingManager.stop();
            trackingManager.saveLastKnownTargets();
        }
    }

    public TrackingManager getTrackingManager() {
        return trackingManager;
    }
}
