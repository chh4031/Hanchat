package me.user.hanchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> korModeUsers = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("ko") != null) {
            getCommand("ko").setExecutor(this);
        }
        getLogger().info("한글 채팅 플러그인 활성화 (태그 모드) by chobojjal");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        UUID uuid = player.getUniqueId();
        if (korModeUsers.contains(uuid)) {
            korModeUsers.remove(uuid);
            player.sendMessage(Component.text("한글 채팅 모드 꺼짐", NamedTextColor.RED));
        } else {
            korModeUsers.add(uuid);
            player.sendMessage(Component.text("한글 채팅 모드 켜짐", NamedTextColor.GREEN));
        }
        return true;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        // 한글 모드가 꺼진 유저 암거도 안하게 설정
        if (!korModeUsers.contains(player.getUniqueId())) return;

        // 메시지 추출
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        
        // 영어와 공백으로만 이루어진 경우에만 변환 (기호/숫자 포함 시 변환 제외하여 오작동 방지) 숫자 기호는 동일하게 쓰니깐 ㅇㅇ
        if (rawMessage.matches("^[a-zA-Z\\s]+$")) {
            String converted = HangulConverter.translate(rawMessage);
            
            //[한] (하늘색) + 한글 메시지 (흰색) => 하늘색이 보기 나음
            Component formattedMessage = Component.text("[한] ", NamedTextColor.AQUA)
                    .append(Component.text(converted, NamedTextColor.WHITE));
            
            event.message(formattedMessage);
        }
    }
}