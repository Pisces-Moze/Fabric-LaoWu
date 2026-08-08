package cn.moze.catfight.bukkit;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CatFightPlugin extends JavaPlugin implements Listener, TabExecutor {
    private CatStateService states;
    private CatVisualManager visuals;
    private CatFightManager fights;
    private ResourcePackService resourcePack;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        states = new CatStateService(this);
        visuals = new CatVisualManager(this);
        visuals.cleanupAll(true);
        fights = new CatFightManager(this);
        resourcePack = new ResourcePackService(this);

        try {
            resourcePack.start();
        } catch (IOException exception) {
            getLogger().severe("Could not create/start the resource-pack service: " + exception.getMessage());
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new CatInteractionListener(this), this);
        Bukkit.getScheduler().runTaskTimer(this, fights, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(this, visuals, 1L, 1L);
        getCommand("catfight").setExecutor(this);
        getCommand("catfight").setTabCompleter(this);
        checkServerVersion();
        getLogger().info("CatFight Bukkit enabled with cross-version model renderer.");
    }

    @Override
    public void onDisable() {
        if (fights != null) fights.shutdown();
        if (visuals != null) visuals.cleanupAll(true);
        if (resourcePack != null) resourcePack.stop();
    }

    CatStateService states() { return states; }
    CatVisualManager visuals() { return visuals; }
    CatFightManager fights() { return fights; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> resourcePack.send(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        String status = event.getStatus().name();
        if (getConfig().getBoolean("resource-pack.required", true)
            && (status.equals("DECLINED") || status.equals("FAILED_DOWNLOAD"))) {
            Bukkit.getScheduler().runTask(this, () -> event.getPlayer().kickPlayer(
                ChatColor.RED + "本服务器需要 CatFight 资源包来显示猫的动画。"));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "CatFight " + getDescription().getVersion()
                + ChatColor.GRAY + " — /catfight reload|pack|cleanup");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                reloadConfig();
                resourcePack.stop();
                try {
                    resourcePack.start();
                    sender.sendMessage(ChatColor.GREEN + "CatFight 配置和资源包已重新加载。");
                } catch (IOException exception) {
                    sender.sendMessage(ChatColor.RED + "资源包服务启动失败：" + exception.getMessage());
                }
                return true;
            case "pack":
                if (sender instanceof Player) {
                    resourcePack.send((Player) sender);
                    sender.sendMessage(ChatColor.GREEN + "已重新发送资源包请求。");
                } else {
                    sender.sendMessage("Generated pack: " + resourcePack.packPath());
                }
                return true;
            case "cleanup":
                fights.shutdown();
                visuals.cleanupAll(true);
                sender.sendMessage(ChatColor.GREEN + "已清理全部 CatFight 模型载体并恢复猫。重新加载插件可继续配对。");
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Arrays.asList("reload", "pack", "cleanup").stream()
            .filter(value -> value.startsWith(prefix)).collect(Collectors.toList());
    }

    private void checkServerVersion() {
        String minecraft = Bukkit.getBukkitVersion().split("-")[0];
        if (!minecraft.matches("1\\.(18\\.2|19(?:\\.\\d+)?|20(?:\\.\\d+)?|21(?:\\.[01])?)")) {
            getLogger().warning("This build targets Minecraft 1.18.2 through 1.21.1; detected " + minecraft + '.');
        }
    }
}
