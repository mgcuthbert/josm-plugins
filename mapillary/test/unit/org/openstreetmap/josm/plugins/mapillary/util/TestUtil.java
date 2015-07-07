package org.openstreetmap.josm.plugins.mapillary.util;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.tools.I18n;

public class TestUtil {
    private static boolean isInitialized;

    private TestUtil() {
        // Prevent instantiation
    }

    /**
     * Initializes the {@link Main} class of JOSM and the mapillary plugin
     * with the preferences from test/data/preferences.
     *
     * That is needed e.g. to use {@link MapillaryLayer#getInstance()}
     */
    public static final void initPlugin() {
        if (!isInitialized) {
            System.setProperty("josm.home", "test/data/preferences");
            Main.initApplicationPreferences();
            Main.pref.enableSaveOnPut(false);
            I18n.init();
            Main.determinePlatformHook();
            Main.platform.preStartupHook();
            Main.pref.init(false);
            I18n.set(Main.pref.get("language", "en"));
            Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator

            isInitialized = true;
        }
    }

}