package network.reflected.rfnetapi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import network.reflected.rfnetapi.commands.CommandRegistry;

public class DefaultCommands {
    public static void initialize() {
        CommandRegistry.getRegistry().registerCommand((executor, arguments) -> {
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
                                    Component.text(" <--").color(TextColor.color(36, 198, 166))
                            )
                    )
            );
        }, 0, "discord");
    }
}
