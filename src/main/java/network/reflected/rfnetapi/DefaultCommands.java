package network.reflected.rfnetapi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.reflected.rfnetapi.commands.StringArg;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class DefaultCommands {
    static List<String> availableGames = List.of(
            "spleefrun",
            "survival"
    );

    public static void initialize() {
        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
                if (executor instanceof Player && arguments[0] instanceof StringArg && availableGames.contains(arguments[0].get())) {
                    if (arguments[0].get().equals("survival")) {
                        checkVoteAndSendToSurvival((Player) executor);
                    } else {
                        api.sendPlayer((Player) executor, ((StringArg) arguments[0]).get());
                    }
                }
        }, 1, "game")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> executor.sendMessage(
                Component.text("\n").append(
                        Component.text("Click the link below to join our discord!\n")
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
        ), 0, "discord")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommands((executor, arguments) -> {
            if (executor instanceof Player) {
               api.sendPlayer((Player) executor, "lobby");
            }
        }, 0, "hub", "lobby")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> executor.sendMessage(
                Component.text("\n")
                        .append(Component.text("Command List"))
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
                        ).append(Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                                .append(
                                        Component.text("/setpin")
                                                .clickEvent(ClickEvent.runCommand("/setpin"))
                                                .color(TextColor.color(255, 253, 68))
                                                .append(Component.text(" - Set or change the PIN on your account").color(TextColor.color(36, 198, 166)))
                                )
                        ).append(Component.text("\n @ ").color(TextColor.color(0, 0, 0))
                                .append(
                                        Component.text("/purchasehistory")
                                                .clickEvent(ClickEvent.runCommand("/purchasehistory"))
                                                .color(TextColor.color(255, 253, 68))
                                                .append(Component.text(" - See your receipts").color(TextColor.color(36, 198, 166)))
                                )
                        ).append(Component.text("\n"))
        ), 0, "help")));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand((executor, arguments) -> executor.sendMessage(Component.text(
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" +
                "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
        )), 0, "clear"));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            api.restart();
        }, "rfnet.restart", 0, "restart")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            api.restart();
        }, "rfnet.restart", 0, "fakerestart")));
    }

    private static void checkVoteAndSendToSurvival(Player player) {
        try {
            URL url = new URL("https://reflected.network/voted/" + player.getName());
            URLConnection connection = null;
            connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder builder = new StringBuilder();
            bufferedReader.lines().forEach(builder::append);

            JsonObject result = new Gson().fromJson(builder.toString(), JsonObject.class);
            Boolean voted = result.get("voted").getAsBoolean();

            if (voted) {
                ReflectedAPI.get(api -> api.sendPlayer(player, "survival"));
            } else {
                player.sendMessage(Component.text("You need to vote before joining survival."));
                player.openBook(
                        Book.book(
                                Component.text(""),
                                Component.text(""),
                                Component.text("\n\n\nUse the link below vote.\n   ")
                                        .color(NamedTextColor.BLACK)
                                        .append(Component.text("Click to open!")
                                                .clickEvent(ClickEvent.openUrl("https://reflected.network/vote"))
                                                .color(NamedTextColor.GREEN)
                                                .decoration(TextDecoration.UNDERLINED, TextDecoration.State.TRUE)
                                                .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                                        )
                        )
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
