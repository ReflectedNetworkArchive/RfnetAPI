package network.reflected.rfnetapi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public class DefaultCommands {
    public static void initialize() {
        ReflectedAPI.getCommandProvider().registerCommand((executor, arguments) -> {
            executor.sendMessage(
                    Component.text("\n").append(
                            Component.text("Click the link below to join our discord!\n")
                                    .color(TextColor.color(200, 255, 230))
                    ).append(
                            Component.text("--> ")
                                    .color(TextColor.color(36, 198, 166))
                                    .append(
                                    Component.text("https://discord.gg/avECNchTCf").clickEvent(
                                            ClickEvent.openUrl("https://discord.gg/avECNchTCf")
                                    ).color(TextColor.color(255, 253, 68))
                            ).append(
                                    Component.text(" <--\n").color(TextColor.color(36, 198, 166))
                            )
                    )
            );
        }, 0, "discord");

        ReflectedAPI.getCommandProvider().registerCommands((executor, arguments) -> {
            if (executor instanceof Player) {
                ((RfnetAPI) Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("RfnetAPI"))).sendPlayer((Player) executor, "lobby");
            }
        }, 0, "hub", "lobby");

        ReflectedAPI.getCommandProvider().registerCommand((executor, arguments) -> {
            executor.sendMessage(
                    Component.text("\n")
                            .append(Component.text("Command List").color(TextColor.color(200, 255, 230)))
                            .append(Component.text(" Click one to run it.").color(NamedTextColor.GRAY))
                            .append(Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                                    .append(
                                            Component.text("/discord")
                                                    .clickEvent(ClickEvent.runCommand("/discord"))
                                                    .color(TextColor.color(255, 253, 68))
                                                    .append(Component.text(" - Get help from our discord").color(TextColor.color(36, 198, 166)))
                                    )
                            )
                            .append(Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                                    .append(
                                            Component.text("/balance")
                                                    .clickEvent(ClickEvent.runCommand("/balance"))
                                                    .color(TextColor.color(255, 253, 68))
                                                    .append(Component.text(" - Check how many shards you have").color(TextColor.color(36, 198, 166)))
                                    )
                            )
                            .append(Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                                    .append(
                                            Component.text("/lobby")
                                                    .clickEvent(ClickEvent.runCommand("/lobby"))
                                                    .color(TextColor.color(255, 253, 68))
                                                    .append(Component.text(" - Get back to the main lobby").color(TextColor.color(36, 198, 166)))
                                    )
                            ).append(Component.text("\n"))
            );
        }, 0, "help");
    }
}
