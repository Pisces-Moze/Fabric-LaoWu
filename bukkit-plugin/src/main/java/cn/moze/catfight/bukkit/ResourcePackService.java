package cn.moze.catfight.bukkit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

final class ResourcePackService {
    private final CatFightPlugin plugin;
    private HttpServer server;
    private Path packPath;
    private byte[] hash;

    ResourcePackService(CatFightPlugin plugin) {
        this.plugin = plugin;
    }

    void start() throws IOException {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) return;
        packPath = plugin.getDataFolder().toPath().resolve("generated/catfight-pack.zip");
        ResourcePackBuilder.build(packPath, packFormat(), plugin.getClass().getClassLoader());
        hash = sha1(Files.readAllBytes(packPath));

        String bind = plugin.getConfig().getString("resource-pack.bind-address", "0.0.0.0");
        int port = plugin.getConfig().getInt("resource-pack.port", 8123);
        server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        server.createContext("/catfight-pack.zip", this::servePack);
        server.setExecutor(null);
        server.start();

        String publicUrl = publicUrl();
        if (publicUrl.isEmpty()) {
            plugin.getLogger().warning("resource-pack.public-url is empty. Pack generated at " + packPath
                + "; set a public URL before players join.");
        } else {
            plugin.getLogger().info("Serving CatFight resource pack at " + publicUrl);
        }
    }

    void stop() {
        if (server != null) server.stop(0);
    }

    void send(Player player) {
        if (hash == null || publicUrl().isEmpty()) return;
        player.setResourcePack(publicUrl(), hash);
    }

    Path packPath() {
        return packPath;
    }

    private void servePack(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        byte[] bytes = Files.readAllBytes(packPath);
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String publicUrl() {
        return plugin.getConfig().getString("resource-pack.public-url", "").trim();
    }

    private int packFormat() {
        String version = Bukkit.getBukkitVersion().toLowerCase(Locale.ROOT);
        if (version.startsWith("1.21")) return 34;
        if (version.startsWith("1.20.5") || version.startsWith("1.20.6")) return 32;
        if (version.startsWith("1.20.3") || version.startsWith("1.20.4")) return 22;
        if (version.startsWith("1.20.2")) return 18;
        if (version.startsWith("1.20")) return 15;
        if (version.startsWith("1.19.4")) return 13;
        if (version.startsWith("1.19.3")) return 12;
        if (version.startsWith("1.19")) return 9;
        return 8;
    }

    private static byte[] sha1(byte[] bytes) throws IOException {
        try {
            return MessageDigest.getInstance("SHA-1").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-1 unavailable", exception);
        }
    }
}
