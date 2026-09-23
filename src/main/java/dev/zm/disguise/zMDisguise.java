package dev.zm.disguise;

import dev.zm.disguise.api.DisguiseAPIImpl;
import dev.zm.disguise.commands.DisguiseCommand;
import dev.zm.disguise.commands.UnDisguiseCommand;
import dev.zm.disguise.commands.ZMDisguiseCommand;
import dev.zm.disguise.config.MessageManager;
import dev.zm.disguise.config.SettingsManager;
import dev.zm.disguise.disguise.DisguiseApplier;
import dev.zm.disguise.disguise.DisguiseRemover;
import dev.zm.disguise.disguise.DisguiseService;
import dev.zm.disguise.disguise.NameResolver;
import dev.zm.disguise.hooks.HookManager;
import dev.zm.disguise.listeners.ChatListener;
import dev.zm.disguise.listeners.MenuClickListener;
import dev.zm.disguise.listeners.PlayerJoinListener;
import dev.zm.disguise.listeners.PlayerQuitListener;
import dev.zm.disguise.refresh.RefreshManager;
import dev.zm.disguise.scheduler.SchedulerAdapter;
import dev.zm.disguise.scheduler.SchedulerAdapterFactory;
import dev.zm.disguise.storage.StorageService;
import dev.zm.disguise.storage.YamlStorage;
import dev.zm.disguise.tasks.ActionBarTask;
import dev.zm.disguise.tasks.BossBarTask;
import dev.zm.disguise.utils.UpdateChecker;
import dev.zm.disguise.velocity.VelocityMessenger;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

public class zMDisguise extends JavaPlugin {

    private static zMDisguise instance;

    private SettingsManager settingsManager;
    private MessageManager messageManager;
    private HookManager hookManager;
    private StorageService storageService;
    private DisguiseService disguiseService;
    private NameResolver nameResolver;
    private RefreshManager refreshManager;
    private DisguiseApplier disguiseApplier;
    private DisguiseRemover disguiseRemover;
    private BossBarTask bossBarTask;
    private ActionBarTask actionBarTask;
    private UpdateChecker updateChecker;
    private SchedulerAdapter schedulerAdapter;
    private VelocityMessenger velocityMessenger;
    private Metrics metrics;

    // Task handles for cancellation on disable
    private SchedulerAdapter.TaskHandle bossBarHandle;
    private SchedulerAdapter.TaskHandle actionBarHandle;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        // Detect runtime environment (Folia vs Paper)
        schedulerAdapter = SchedulerAdapterFactory.create();

        // Config & language files
        settingsManager = new SettingsManager(this);
        messageManager = new MessageManager(this);

        // Storage
        storageService = new YamlStorage(this);
        storageService.initialize();

        hookManager = new HookManager(this);

        // Core services
        disguiseService = new DisguiseService();
        nameResolver = new NameResolver(disguiseService);
        refreshManager = new RefreshManager(this);
        disguiseApplier = new DisguiseApplier(disguiseService, refreshManager);
        disguiseRemover = new DisguiseRemover(disguiseService, refreshManager);

        velocityMessenger = new VelocityMessenger(this);
        velocityMessenger.register();

        int pluginId = 33747;
        metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new org.bstats.charts.SimplePie("platform", () -> {
            return SchedulerAdapterFactory.isFolia() ? "Folia" : "Paper";
        }));
        metrics.addCustomChart(new org.bstats.charts.SimplePie("keep_on_relog", () -> {
            return settingsManager.keepDisguiseOnRelog() ? "Enabled" : "Disabled";
        }));

        ZMDisguiseCommand zmdCmd = new ZMDisguiseCommand(this);
        getCommand("zmdisguise").setExecutor(zmdCmd);
        getCommand("zmdisguise").setTabCompleter(zmdCmd);

        DisguiseCommand dCmd = new DisguiseCommand(this);
        getCommand("disguise").setExecutor(dCmd);
        getCommand("disguise").setTabCompleter(dCmd);

        UnDisguiseCommand undCmd = new UnDisguiseCommand(this);
        getCommand("undisguise").setExecutor(undCmd);
        getCommand("undisguise").setTabCompleter(undCmd);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuClickListener(this), this);

        int bossInterval = settingsManager.getBossBarInterval();
        int actionInterval = settingsManager.getActionBarInterval();
        bossBarTask = new BossBarTask(this);
        actionBarTask = new ActionBarTask(this);
        bossBarHandle = schedulerAdapter.runTaskTimer(this, bossBarTask::run, 20L, bossInterval);
        actionBarHandle = schedulerAdapter.runTaskTimer(this, actionBarTask::run, 20L, actionInterval);

        DisguiseAPIImpl.init(this);

        updateChecker = new UpdateChecker(this, 136779);
        updateChecker.fetch();

        long timeTaken = System.currentTimeMillis() - startTime;
        printStartupMessage(timeTaken);
    }

    @Override
    public void onDisable() {
        long startTime = System.currentTimeMillis();

        if (bossBarHandle != null)
            bossBarHandle.cancel();
        if (actionBarHandle != null)
            actionBarHandle.cancel();
        if (settingsManager != null && disguiseService != null) {
            boolean keep = settingsManager.keepDisguiseOnRelog();
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                if (!disguiseService.isDisguised(player))
                    continue;
                if (keep) {
                    storageService.save(player.getUniqueId(), disguiseService.getDisguise(player));
                } else {
                    storageService.delete(player.getUniqueId());
                }
                // Restores the profile, the names and the LuckPerms groups: nothing disguised
                // may survive a shutdown, otherwise another plugin can persist it.
                disguiseRemover.restoreIdentity(player, true);
                if (velocityMessenger != null) {
                    velocityMessenger.sendUndisguise(player);
                }
            }
        }

        if (velocityMessenger != null)
            velocityMessenger.unregister();

        if (storageService != null)
            storageService.close();

        long timeTaken = System.currentTimeMillis() - startTime;
        printShutdownMessage(timeTaken);
    }

    private void printStartupMessage(long ms) {
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        String version = getDescription().getVersion();
        String author = getDescription().getAuthors().isEmpty() ? "zMarkitos_" : getDescription().getAuthors().get(0);
        String lpStr = hookManager.hasLuckPerms() ? "<color:#2ECC71>Hooked" : "<color:#E74C3C>Not found";
        String papiStr = hookManager.hasPlaceholderAPI() ? "<color:#2ECC71>Hooked" : "<color:#E74C3C>Not found";
        String srStr = hookManager.hasSkinsRestorer() ? "<color:#2ECC71>Hooked" : "<color:#E74C3C>Not found";
        String keepStr = settingsManager.keepDisguiseOnRelog() ? "<color:#2ECC71>Enabled" : "<color:#E74C3C>Disabled";
        String foliaStr = SchedulerAdapterFactory.isFolia() ? "<color:#2ECC71>Folia" : "<color:#6C8EBF>Paper";

        c.sendMessage(mm("<dark_gray>──────────────────────────────────────────"));
        c.sendMessage(mm("<gradient:#00D76B:#00FF7F><b>zMDisguise</b></gradient> <gray>v" + version
                + " <dark_gray>by <white>" + author));
        c.sendMessage(mm(""));
        c.sendMessage(mm("  <dark_gray>• <gray>Platform:         " + foliaStr));
        c.sendMessage(mm("  <dark_gray>• <gray>LuckPerms:        " + lpStr));
        c.sendMessage(mm("  <dark_gray>• <gray>PlaceholderAPI:   " + papiStr));
        c.sendMessage(mm("  <dark_gray>• <gray>SkinsRestorer:    " + srStr));
        c.sendMessage(mm("  <dark_gray>• <gray>Keep on relog:    " + keepStr));
        c.sendMessage(mm("  <dark_gray>• <gray>Velocity channel: <color:#2ECC71>Registered"));
        c.sendMessage(mm(""));
        c.sendMessage(mm("<color:#2ECC71>✔ <white>Plugin enabled successfully <gray>(" + ms + "ms)"));
        c.sendMessage(mm("<dark_gray>──────────────────────────────────────────"));
    }

    private void printShutdownMessage(long ms) {
        ConsoleCommandSender c = Bukkit.getConsoleSender();
        String version = getDescription().getVersion();
        c.sendMessage(mm("<dark_gray>──────────────────────────────────────────"));
        c.sendMessage(mm("<gradient:#00D76B:#00FF7F><b>zMDisguise</b></gradient> <gray>v" + version
                + " <dark_gray>is shutting down..."));
        c.sendMessage(mm("<color:#E74C3C>✗ <white>Plugin disabled successfully <gray>(" + ms + "ms)"));
        c.sendMessage(mm("<dark_gray>──────────────────────────────────────────"));
    }

    private static net.kyori.adventure.text.Component mm(String s) {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(s);
    }

    public static zMDisguise getInstance() {
        return instance;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public DisguiseService getDisguiseService() {
        return disguiseService;
    }

    public NameResolver getNameResolver() {
        return nameResolver;
    }

    public DisguiseApplier getDisguiseApplier() {
        return disguiseApplier;
    }

    public DisguiseRemover getDisguiseRemover() {
        return disguiseRemover;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public BossBarTask getBossBarTask() {
        return bossBarTask;
    }

    public SchedulerAdapter getScheduler() {
        return schedulerAdapter;
    }

    public VelocityMessenger getVelocityMessenger() {
        return velocityMessenger;
    }
}
