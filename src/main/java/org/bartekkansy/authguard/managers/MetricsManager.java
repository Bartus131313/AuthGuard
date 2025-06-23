package org.bartekkansy.authguard.managers;

import org.bartekkansy.authguard.AuthGuard;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;

public class MetricsManager {
    public static final int PLUGIN_ID = 26233;

    private static Metrics metrics;

    public static void start(AuthGuard plugin) {
        metrics = new Metrics(plugin, PLUGIN_ID);

        // Players count chart
        metrics.addCustomChart(new SingleLineChart("registered_players", () -> plugin.getDatabaseManager().getRegisteredPlayers().size()));
    }

    public static void shutdown() {
        metrics.shutdown();
    }
}
