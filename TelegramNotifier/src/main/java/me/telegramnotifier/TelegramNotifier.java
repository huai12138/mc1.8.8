package me.telegramnotifier;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class TelegramNotifier extends JavaPlugin implements Listener {

    private File configFile;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        setupConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TelegramNotifier 已启用！");
    }

    private void setupConfig() {
        File pluginFolder = new File(getDataFolder().getAbsolutePath());
        if (!pluginFolder.exists()) pluginFolder.mkdirs();

        configFile = new File(pluginFolder, "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players", Collections.singletonList("name"));
                config.set("bot_token", "Bot Token");
                config.set("chat_id", "Chat ID");
                config.save(configFile);
                
                // 提醒用户修改配置
                getLogger().info("请修改config.yml文件，填入正确的Telegram Bot Token和Chat ID");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> players = config.getStringList("players");
        if (players.contains(player.getName())) {
            String botToken = config.getString("bot_token");
            String chatId = config.getString("chat_id");

            if (botToken == null || chatId == null) {
                getLogger().warning("未正确设置 bot_token 或 chat_id！");
                return;
            }

            String message = "🎮 玩家 " + player.getName() + " 已上线！";
            sendTelegramMessage(botToken, chatId, message);
        }
    }

    private void sendTelegramMessage(String token, String chatId, String message) {
        try {
            String urlStr = "https://api.telegram.org/bot" + token + "/sendMessage";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // URL编码消息内容
            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
            String payload = "chat_id=" + chatId + "&text=" + encodedMessage;
            
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                getLogger().warning("发送 Telegram 消息失败！响应码：" + responseCode);
                // 输出详细错误信息
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                    String errorResponse = br.readLine();
                    getLogger().warning("错误详情：" + errorResponse);
                } catch (Exception e) {
                    getLogger().warning("无法读取错误详情");
                }
            }

            conn.disconnect();
        } catch (IOException e) {
            getLogger().warning("发送 Telegram 消息时出错！");
            e.printStackTrace();
        }
    }
}
