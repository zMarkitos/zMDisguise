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
import dev.zm.disguise.storage.StorageService;
import dev.zm.disguise.storage.YamlStorage;
import dev.zm.disguise.tasks.ActionBarTask;
import dev.zm.disguise.tasks.BossBarTask;
import dev.zm.disguise.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

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

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

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

        // Commands
        ZMDisguiseCommand zmdCmd = new ZMDisguiseCommand(this);
        getCommand("zmdisguise").setExecutor(zmdCmd);
        getCommand("zmdisguise").setTabCompleter(zmdCmd);

        DisguiseCommand dCmd = new DisguiseCommand(this);
        getCommand("disguise").setExecutor(dCmd);
        getCommand("disguise").setTabCompleter(dCmd);

        UnDisguiseCommand undCmd = new UnDisguiseCommand(this);
        getCommand("undisguise").setExecutor(undCmd);
        getCommand("undisguise").setTabCompleter(undCmd);

        // Events
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuClickListener(this), this);

        // Repeating tasks
        int bossInterval = settingsManager.getBossBarInterval();
        int actionInterval = settingsManager.getActionBarInterval();
        bossBarTask = new BossBarTask(this);
        actionBarTask = new ActionBarTask(this);
        bossBarTask.runTaskTimer(this, 20L, bossInterval);
        actionBarTask.runTaskTimer(this, 20L, actionInterval);

        // Public API
        DisguiseAPIImpl.init(this);

        updateChecker = new UpdateChecker(this, 136779);
        updateChecker.fetch();

        long timeTaken = System.currentTimeMillis() - startTime;
        printStartupMessage(timeTaken);
    }

    @Override
    public void onDisable() {
        long startTime = System.currentTimeMillis();

        if (bossBarTask != null)
            bossBarTask.cancel();
        if (actionBarTask != null)
            actionBarTask.cancel();

        // Persist or remove disguises based on config
        if (settingsManager != null && settingsManager.keepDisguiseOnRelog()) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                if (disguiseService.isDisguised(player)) {
                    storageService.save(player.getUniqueId(), disguiseService.getDisguise(player));
                }
            }
        } else if (settingsManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                if (disguiseService.isDisguised(player)) {
                    disguiseRemover.removeDisguise(player);
                    storageService.delete(player.getUniqueId());
                }
            }
        }

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

        c.sendMessage(mm("<dark_gray>──────────────────────────────────────────"));
        c.sendMessage(mm("<gradient:#00D76B:#00FF7F><b>zMDisguise</b></gradient> <gray>v" + version
                + " <dark_gray>by <white>" + author));
        c.sendMessage(mm(""));
        c.sendMessage(mm("  <dark_gray>• <gray>LuckPerms:        " + lpStr));
        c.sendMessage(mm("  <dark_gray>• <gray>PlaceholderAPI:   " + papiStr));
        c.sendMessage(mm("  <dark_gray>• <gray>SkinsRestorer:    " + srStr));
        c.sendMessage(mm("  <dark_gray>• <gray>Keep on relog:    " + keepStr));
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
}
