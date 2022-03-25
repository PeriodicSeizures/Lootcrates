package com.crazicrafter1.lootcrates;

import com.crazicrafter1.crutils.Metrics;

public class MetricWrap {

    public static void init(Main plugin, boolean update) {
        try {
            Metrics metrics = new Metrics(plugin, 10395);

            metrics.addCustomChart(new Metrics.SimplePie("update",
                    () -> "" + update));

            metrics.addCustomChart(new Metrics.SimplePie("crates",
                    () -> "" + plugin.data.crates.size()));

            metrics.addCustomChart(new Metrics.SimplePie("loot",
                    () -> "" + LootCratesAPI.lootClasses.size()));

            metrics.addCustomChart(new Metrics.SimplePie("language",
                    () -> plugin.language));

        } catch (Exception e) {
            plugin.error("Unable to enable bStats Metrics (" + e.getMessage() + ")");
        }
    }

}
