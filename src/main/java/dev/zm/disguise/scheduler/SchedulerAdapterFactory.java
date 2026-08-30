package dev.zm.disguise.scheduler;

/**
 * Factory that detects at runtime whether the server runs Folia or standard
 * Paper/Spigot and returns the appropriate {@link SchedulerAdapter}.
 *
 * <p>Detection is performed by checking for the presence of Folia's
 * {@code io.papermc.paper.threadedregions.RegionizedServer} class, which
 * exists only on Folia builds.
 */
public final class SchedulerAdapterFactory {

    private static final boolean FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        FOLIA = folia;
    }

    private SchedulerAdapterFactory() {}

    /**
     * @return {@code true} if the server is running Folia.
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Creates and returns the correct scheduler adapter for this runtime.
     */
    public static SchedulerAdapter create() {
        return FOLIA ? new FoliaSchedulerAdapter() : new BukkitSchedulerAdapter();
    }
}
