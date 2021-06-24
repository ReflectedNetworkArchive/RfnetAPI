package network.reflected.rfnetapi.purchases;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import network.reflected.rfnetapi.ReflectedAPI;
import network.reflected.rfnetapi.commands.BoolArg;
import network.reflected.rfnetapi.commands.IntArg;
import network.reflected.rfnetapi.commands.PlayerArg;
import network.reflected.rfnetapi.commands.StringArg;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

public class PurchaseAPI implements Listener {
    Map<UUID, Authentication> authenticationCallbacks = new HashMap<>();

    public PurchaseAPI() {
        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (executor instanceof Player) {
                getShardBalance((Player) executor, (result) -> executor.sendMessage(
                        Component.text("You currently have ")
                                .color(TextColor.color(36, 198, 166))
                                .append(Component.text(
                                    result + " shards"
                                ).color(TextColor.color(200, 255, 230)))
                ));
            }
        }, 0, "balance")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (executor instanceof Player) {
                resetPin((Player) executor);
            }
        }, 0, "setpin")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (executor instanceof Player && arguments[0] instanceof IntArg && authenticationCallbacks.containsKey(((Player) executor).getUniqueId())) {
                MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                MongoCollection<Document> pins;

                try {
                    pins = db.getCollection("pins");
                } catch (IllegalArgumentException e) {
                    db.createCollection("pins");
                    pins = db.getCollection("pins");
                }

                Bson pinFilter = and(eq("uuid", ((Player) executor).getUniqueId().toString()), exists("pin"));

                if (pins.countDocuments(pinFilter) == 1) {
                    if (
                            pins.countDocuments(and(pinFilter, eq("pin", arguments[0].get()))) == 1
                    ) {
                        finishAuth((Player) executor);
                    } else {
                        cancelAuth((Player) executor);
                    }
                }
            }
        }, 1, "login")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (executor instanceof Player && arguments[0] instanceof IntArg && authenticationCallbacks.containsKey(((Player) executor).getUniqueId())) {
                MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                MongoCollection<Document> pins;

                try {
                    pins = db.getCollection("pins");
                } catch (IllegalArgumentException e) {
                    db.createCollection("pins");
                    pins = db.getCollection("pins");
                }

                Bson pinFilter = and(eq("uuid", ((Player) executor).getUniqueId().toString()), exists("pin"));

                if (pins.countDocuments(pinFilter) == 0) {
                    pins.insertOne(new Document().append("uuid", ((Player) executor).getUniqueId().toString()).append("pin", arguments[0].get()));
                    finishAuth((Player) executor);
                }
            }
        }, 1, "newpin")));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand(((executor, arguments) -> {
            if (executor instanceof Player && arguments[0] instanceof PlayerArg && arguments[1] instanceof IntArg) {
                payShards((Player)executor, ((PlayerArg) arguments[0]).get(), ((IntArg) arguments[1]).get(), (success) -> {
                    if (success) {
                        executor.sendMessage("You paid " + arguments[1].get() + " to " + ((PlayerArg)arguments[0]).get().getName());
                        ((PlayerArg)arguments[0]).get().sendMessage("You got paid " + arguments[1].get() + " from " + executor.getName());
                    } else {
                        executor.sendMessage("Error! You couldn't pay " + ((PlayerArg)arguments[0]).get().getName() + ".");
                    }
                });
            }
        }), 2, "pay"));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand(((executor, arguments) -> {
            if (executor instanceof Player && authenticationCallbacks.containsKey(((Player) executor).getUniqueId())) {
                cancelAuth(((Player) executor));
            }
        }), 0, "cancel"));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (executor instanceof Player) {
                authenticate((Player) executor, () -> {
                    Player player = (Player)executor;
                    MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                    MongoCollection<Document> purchases;

                    try {
                        purchases = db.getCollection("purchases");
                        Bson purchaseFilter = and(eq("uuid", player.getUniqueId().toString()));

                        Document purchaseDocument = purchases.find(purchaseFilter).first();
                        if (purchaseDocument != null) {
                            List<String> purchaseHistory = purchaseDocument.getList("history", String.class);
                            if (purchaseHistory != null) {
                                player.sendMessage(Component.text("--- Oldest transactions").color(NamedTextColor.AQUA));
                                for (String line : purchaseHistory) {
                                    player.sendMessage(Component.text(line));
                                }
                                player.sendMessage(Component.text("--- Newest transactions").color(NamedTextColor.AQUA));
                            } else {
                                player.sendMessage(Component.text("No transactions!").color(NamedTextColor.AQUA));
                            }
                        } else {
                            player.sendMessage(Component.text("No transactions!").color(NamedTextColor.AQUA));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, false, "See purchase history");
            }
        }, 0, "purchasehistory"));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (executor instanceof Player && arguments[0] instanceof StringArg) {

                purchaseWithShards(((StringArg) arguments[0]).get(), (Player) executor, (success) -> {
                    if (!success) {
                        executor.sendMessage(Component.text("Purchase failed!"));
                    }
                });
            }
        }, 1, "buy"));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (arguments[0] instanceof PlayerArg && arguments[1] instanceof IntArg) {
                addShards(((PlayerArg)arguments[0]).get(), ((IntArg)arguments[1]).get(), (result) -> {
                    if (result) {
                        executor.sendMessage("Gave them shards.");
                    } else {
                        executor.sendMessage("Failed to give shards!");
                    }
                });
            }
        }, "rfnet.purchaseapi.give", 2, "giveshards")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (arguments[0] instanceof PlayerArg) {
                getShardBalance(((PlayerArg)arguments[0]).get(), (result) -> executor.sendMessage("They have " + result + " shards."));
            }
        }, "rfnet.purchaseapi.getother", 1, "getshards")));

        ReflectedAPI.get((api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (arguments[0] instanceof PlayerArg && arguments[1] instanceof StringArg) {
                givePlayerItem(((PlayerArg)arguments[0]).get(), ((StringArg)arguments[1]).get(), (result) -> {
                    if (result) {
                        executor.sendMessage("Purchase succeeded.");
                        ((PlayerArg)arguments[0]).get().sendMessage("You recieved an item from the store!");
                    } else {
                        ((PlayerArg)arguments[0]).get().sendMessage("Purchase failed! Use \"/discord\" and contact someone so they can restore your purchase!");
                        executor.sendMessage("Purchase failed!");
                    }
                });
            }
        }, "rfnet.purchaseapi.givepurchase", 2, "givepurchase")));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (arguments[0] instanceof PlayerArg) {
                Player player = ((PlayerArg)arguments[0]).get();
                MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                MongoCollection<Document> pins;

                try {
                    pins = db.getCollection("pins");
                } catch (IllegalArgumentException e) {
                    db.createCollection("pins");
                    pins = db.getCollection("pins");
                }

                Bson pinFilter = and(eq("uuid", player.getUniqueId().toString()), exists("pin"));
                pins.deleteMany(pinFilter);
                player.sendMessage(Component.text("An administrator has forced you to reset your PIN.").color(TextColor.color(36, 198, 166)));
                authenticate(player, () -> player.sendMessage(Component.text("Success! Your PIN has been reset.").color(TextColor.color(200, 255, 230))), false, "PIN change", true);
            }
        }, "rfnet.purchaseapi.resetotherpin", 1, "resetpin"));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (arguments[0] instanceof PlayerArg) {
                Player player = ((PlayerArg)arguments[0]).get();
                MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                MongoCollection<Document> pins;

                try {
                    pins = db.getCollection("pins");
                } catch (IllegalArgumentException e) {
                    db.createCollection("pins");
                    pins = db.getCollection("pins");
                }

                Bson pinFilter = and(eq("uuid", player.getUniqueId().toString()), exists("pin"));
                pins.deleteMany(pinFilter);
            }
        }, "rfnet.purchaseapi.clearotherpin", 1, "clearpin"));

        ReflectedAPI.get(api -> api.getCommandProvider().registerCommand((executor, arguments) -> {
            if (arguments[0] instanceof StringArg && arguments[1] instanceof IntArg && arguments[2] instanceof BoolArg) {
                createProduct(new Product(
                        ((StringArg) arguments[0]).get(),
                        ((IntArg) arguments[1]).get(),
                        ((BoolArg) arguments[2]).get()
                ), (success) -> {
                    if (success) {
                        executor.sendMessage(Component.text("Product created."));
                    } else {
                        executor.sendMessage(Component.text("Product could not be created!"));
                    }
                });
            }
        }, "rfnet.purchaseapi.createproduct", 3, "createproduct"));

        ReflectedAPI.get(api -> Bukkit.getScheduler().runTaskTimerAsynchronously(api.getPlugin(), () -> {
            if (api.getPlugin().getDatabase().isAvailable()) {
                MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                MongoCollection<Document> purchases;
                purchases = db.getCollection("purchases");

                for (Player player : Bukkit.getOnlinePlayers()) {
                    Bson purchaseFilter = and(eq("uuid", player.getUniqueId().toString()));
                    try {
                        Document purchaseDocument = purchases.find(purchaseFilter).first();
                        if (purchaseDocument != null) {
                            List<String> purchaseHistory = purchaseDocument.getList("history", String.class);
                            if (purchaseHistory != null && purchaseDocument.getBoolean("needsToSee")) {
                                showReceipt(player, purchaseHistory);
                                purchases.updateOne(purchaseFilter, set("needsToSee", false));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 600));
    }

    void finishAuth(Player player) {
        Runnable callback = authenticationCallbacks.get(player.getUniqueId()).getSuccessRunnable();
        if (!authenticationCallbacks.get(player.getUniqueId()).isDelayEffect()) {
            authSuccessEffect(player);
        }
        authenticationCallbacks.remove(player.getUniqueId());
        try {
            callback.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void cancelAuth(Player player) {
        if (authenticationCallbacks.containsKey(player.getUniqueId()) && authenticationCallbacks.get(player.getUniqueId()).isCancellable()) {
            authenticationCallbacks.remove(player.getUniqueId());
            authFailEffect(player);
        }
    }

    public void hasItem(String item, Player player, BooleanCallback callback) {
        howManyProductOwns(player, item, (amount) -> callback.run(amount > 0));
    }

    public void getShardBalance(Player player, IntegerCallback callback) {
        ReflectedAPI.get((api) -> {
            MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
            MongoCollection<Document> shards;

            try {
                shards = db.getCollection("shards");
            } catch (IllegalArgumentException e) {
                db.createCollection("shards");
                shards = db.getCollection("shards");
            }

            Bson shardFilter = and(eq("uuid", player.getUniqueId().toString()), gt("shards", 0));

            if (shards.countDocuments(shardFilter) > 0) {
                callback.run(Objects.requireNonNull(shards.find(shardFilter).first()).getInteger("shards"));
            } else {
                shards.deleteMany(and(eq("uuid", player.getUniqueId().toString()), exists("shards")));
                shards.insertOne(new Document().append("uuid", player.getUniqueId().toString()).append("shards", 0));
                callback.run(0);
            }
        });
    }

    public void purchaseWithShards(String item, Player player, BooleanCallback callback) {
        getProduct(item, (product) -> getShardBalance(player, (balance) -> {
            if (balance >= product.getPrice()) {
                hasItem(item, player, (has) -> {
                    if (product.isOneTimePurchase() && has) {
                        player.sendMessage(Component.text("You already own that item.").color(NamedTextColor.RED));
                        authFailEffect(player);
                        callback.run(false);
                    } else {
                        authenticate(player, () -> givePlayerItem(player, item, (succeeded) -> {
                            if (succeeded) {
                                addShards(player, -product.getPrice(), (tookShards) -> {
                                    if (tookShards) {
                                        authSuccessEffect(player);
                                        callback.run(true);
                                    } else {
                                        player.sendMessage(Component.text(
                                                "Error charging you. You have received the product for free!" +
                                                        " Please contact support with /discord so we can fix this problem" +
                                                        " for others. (You'll get to keep your free item)"
                                        ).color(NamedTextColor.RED));
                                        authFailEffect(player);
                                        callback.run(false);
                                    }
                                });
                            } else {
                                player.sendMessage(Component.text("Error buying that item. Do you already own it?").color(NamedTextColor.RED));
                                authFailEffect(player);
                                callback.run(false);
                            }
                        }), true, "Purchase " + item);
                    }
                });
            } else {
                authFailEffect(player);
                player.sendMessage(Component.text("You don't have enough to buy that!").color(NamedTextColor.RED));
                callback.run(false);
            }
        }), () -> {
            authFailEffect(player);
            player.sendMessage(Component.text("Can't find that product.").color(NamedTextColor.RED));
            callback.run(false);
        });
    }

    public void addShards(Player player, int amount, BooleanCallback callback) {
        ReflectedAPI.get((api) -> {
            MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
            MongoCollection<Document> shards;

            try {
                shards = db.getCollection("shards");
            } catch (IllegalArgumentException e) {
                db.createCollection("shards");
                shards = db.getCollection("shards");
            }

            Bson shardFilter = and(eq("uuid", player.getUniqueId().toString()), exists("shards"));

            MongoCollection<Document> finalShards = shards;
            getShardBalance(player, (balance) -> {
                int newBalance = balance + amount;
                if (finalShards.countDocuments(shardFilter) > 0) {
                    finalShards.replaceOne(shardFilter, new Document().append("uuid", player.getUniqueId().toString()).append("shards", newBalance));
                } else {
                    finalShards.insertOne(new Document().append("uuid", player.getUniqueId().toString()).append("shards", newBalance));
                }
                callback.run(true);
            });
        });
    }

    public void payShards(Player from, Player to, int amount, BooleanCallback callback) {
        getShardBalance(from, (balance) -> {
            if (balance >= amount) {
                authenticate(from, () -> addShards(to, amount, (transferToSuccess) -> {
                    if (transferToSuccess) {
                        addShards(from, -amount, (transferFromSuccess) -> {
                            if (transferFromSuccess) {
                                authSuccessEffect(from);
                                callback.run(true);
                            } else {
                                addShards(to, -amount, (rt) -> {
                                });
                                authFailEffect(from);
                                callback.run(false);
                            }
                        });
                    } else {
                        authFailEffect(from);
                        callback.run(false);
                    }
                }), true, "Pay " + amount + " shards to " + to.getName());
            } else {
                callback.run(false);
                from.sendMessage(Component.text("Sorry, you don't have that many gems.").color(TextColor.color(200, 255, 230)));
            }
        });
    }

    public void createProduct(Product product, BooleanCallback callback) {
        ReflectedAPI.get(api -> {
            MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
            MongoCollection<Document> products;

            try {
                products = db.getCollection("products");
            } catch (IllegalArgumentException e) {
                db.createCollection("products");
                products = db.getCollection("products");
            }

            Bson productFilter = eq("name", product.getName());

            if (products.countDocuments(productFilter) == 0) {
                products.insertOne(new Document()
                        .append("name", product.getName())
                        .append("price", product.getPrice())
                        .append("oneTimePurchase", product.isOneTimePurchase())
                );
                callback.run(true);
            } else {
                callback.run(false);
            }
        });
    }

    public void getProduct(String item, ProductCallback callback, Runnable failureCallback) {
        ReflectedAPI.get(api -> {
            MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
            MongoCollection<Document> products;

            try {
                products = db.getCollection("products");
            } catch (IllegalArgumentException e) {
                db.createCollection("products");
                products = db.getCollection("products");
            }

            Bson productFilter = and(eq("name", item), exists("price"), exists("oneTimePurchase"));

            if (products.countDocuments(productFilter) > 0) {
                Document document = products.find(productFilter).first();
                if (document != null){
                    callback.run(new Product(
                            item,
                            document.getInteger("price"),
                            document.getBoolean("oneTimePurchase")
                    ));
                } else {
                    failureCallback.run();
                }
            } else {
                failureCallback.run();
            }
        });
    }

    public void howManyProductOwns(Player player, String item, IntegerCallback callback) {
        ReflectedAPI.get(api -> {
            MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
            MongoCollection<Document> purchases;

            try {
                purchases = db.getCollection("purchases");
            } catch (IllegalArgumentException e) {
                db.createCollection("purchases");
                purchases = db.getCollection("purchases");
            }

            Bson purchaseFilter = and(eq("uuid", player.getUniqueId().toString()), exists(item));

            if (purchases.countDocuments(purchaseFilter) > 0) {
                Document document = purchases.find(purchaseFilter).first();
                if (document != null) {
                    callback.run(document.getInteger(item));
                } else {
                    callback.run(0);
                }
            } else {
                callback.run(0);
            }
        });
    }

    public void givePlayerItem(Player player, String item, BooleanCallback callback) {
        getProduct(item, (product) -> howManyProductOwns(player, item, (number) -> {
            if (product.isOneTimePurchase() && number > 0) {
                callback.run(false);
            } else {
                ReflectedAPI.get(api -> {
                    MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                    MongoCollection<Document> purchases;

                    try {
                        purchases = db.getCollection("purchases");
                    } catch (IllegalArgumentException e) {
                        db.createCollection("purchases");
                        purchases = db.getCollection("purchases");
                    }

                    Bson purchaseFilter = and(eq("uuid", player.getUniqueId().toString()));

                    Document purchaseDocument = purchases.find(purchaseFilter).first();
                    if (purchaseDocument == null) {
                        InsertOneResult result = purchases.insertOne(new Document("uuid", player.getUniqueId().toString()));
                        purchaseDocument = purchases.find(eq("_id", result.getInsertedId())).first();
                        assert purchaseDocument != null; // Since we just created it, it isn't null.
                    }

                    if (number > 0) {
                        purchases.updateOne(purchaseFilter, inc(item, 1));
                    } else {
                        purchases.updateOne(purchaseFilter, set(item, 1));
                    }


                    List<String> purchaseHistory = purchaseDocument.getList("history", String.class);
                    if (purchaseHistory == null) {
                        purchaseHistory = new ArrayList<>();
                    }
                    purchaseHistory.add(player.getName() + " purchased '" + item + "' on " + new Date());
                    purchases.updateOne(purchaseFilter, set("history", purchaseHistory));

                    purchases.updateOne(purchaseFilter, set("needsToSee", true));

                    PurchaseSuccessEvent event = new PurchaseSuccessEvent(product, player);
                    Bukkit.getPluginManager().callEvent(event);
                    callback.run(true);
                });
            }
        }), () -> callback.run(false));
    }

    public boolean isAuthenticating(Player player) {
        return authenticationCallbacks.containsKey(player.getUniqueId());
    }

    public void resetPin(Player player) {
        hasPin(player, (result) -> {
            if (result) {
                player.sendMessage(Component.text("First, you must enter your old PIN.").color(TextColor.color(36, 198, 166)));
                authenticate(player, () -> ReflectedAPI.get(api -> {
                    MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
                    MongoCollection<Document> pins;

                    try {
                        pins = db.getCollection("pins");
                    } catch (IllegalArgumentException e) {
                        db.createCollection("pins");
                        pins = db.getCollection("pins");
                    }

                    Bson pinFilter = and(eq("uuid", player.getUniqueId().toString()), exists("pin"));
                    pins.deleteMany(pinFilter);
                    player.sendMessage(Component.text("Now enter a new PIN.").color(TextColor.color(36, 198, 166)));
                    authenticate(player, () -> player.sendMessage(Component.text("Success! Your PIN has been reset.").color(TextColor.color(200, 255, 230))), false, "PIN change", true);
                }), true, "PIN reset", false);
            } else {
                player.sendMessage(Component.text("You don't have a PIN. Create one now.").color(TextColor.color(36, 198, 166)));
                authenticate(player, () -> player.sendMessage(Component.text("Success! Your PIN has been set.").color(TextColor.color(200, 255, 230))), false, "PIN change", false);
            }
        });
    }

    public void authenticate(Player player, Runnable success, boolean delayEffect, String action) {
        authenticate(player, success, delayEffect, action, false);
    }

    public void authenticate(Player player, Runnable success, boolean delayEffect, String action, boolean force) {
        if (!force && authenticationCallbacks.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Continue logging in first!").color(TextColor.color(200, 255, 230)));
            return;
        }

        authEffect(player, action, force);
        hasPin(player, (result) -> {
            if (result) {
                player.sendMessage(Component.text("Type /login <your PIN>").color(TextColor.color(227, 252, 199)).append(
                        Component.text("\nForgot PIN? Click here to join our discord and get support.").clickEvent(ClickEvent.runCommand("/discord"))
                ));
                if (force) {
                    authenticationCallbacks.remove(player.getUniqueId());
                }
                authenticationCallbacks.put(player.getUniqueId(), new Authentication(success, true, delayEffect));
            } else {
                player.sendMessage(Component.text("You don't have a PIN!\nType /newpin <your PIN>\nMake sure to double check your PIN.").color(TextColor.color(227, 252, 199)));
                if (force) {
                    authenticationCallbacks.remove(player.getUniqueId());
                    ReflectedAPI.get(api -> api.getPlugin().getDatabase().setBusyChangingPwd(player.getUniqueId(), true));
                    authenticationCallbacks.put(player.getUniqueId(), new Authentication(() -> {
                        ReflectedAPI.get(api -> api.getPlugin().getDatabase().setBusyChangingPwd(player.getUniqueId(), false));
                        success.run();
                    }, false, delayEffect));
                } else {
                    authenticationCallbacks.put(player.getUniqueId(), new Authentication(success, true, delayEffect));
                }
            }
        });
    }

    void hasPin(Player player, BooleanCallback callback) {
        ReflectedAPI.get((api) -> {
            MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
            MongoCollection<Document> pins;

            try {
                pins = db.getCollection("pins");
            } catch (IllegalArgumentException e) {
                db.createCollection("pins");
                pins = db.getCollection("pins");
            }

            Bson pinFilter = and(eq("uuid", player.getUniqueId().toString()), exists("pin"));

            callback.run(pins.countDocuments(pinFilter) > 0);
        });
    }

    void authEffect(Audience audience, String action, boolean noCancel) {
        if (!noCancel) {
            audience.sendMessage(Component.text("Authenticating \"" + action + "\". Click here to cancel.")
                    .color(NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/cancel"))
                    .hoverEvent(Component.text("Click to cancel!")));
        }
        audience.clearTitle();
        audience.showTitle(
                Title.title(
                        Component.text("Authentication"), Component.text("in progress").color(NamedTextColor.GRAY),
                        Title.Times.of(Ticks.duration(20), Ticks.duration(100000), Ticks.duration(20))
                )
        );
    }

    void authFailEffect(Audience audience) {
        ReflectedAPI.get((api -> {
            audience.clearTitle();
            audience.showTitle(
                    Title.title(
                            Component.text("Authentication"), Component.text("✗").color(NamedTextColor.RED),
                            Title.Times.of(Ticks.duration(0), Ticks.duration(40), Ticks.duration(20))
                    )
            );
            Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> audience.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 1.1f)), 1);
            Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> audience.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 0.7f)), 5);
            Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> audience.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 0.5f)), 9);
        }));
    }

    void authSuccessEffect(Audience audience) {
        ReflectedAPI.get((api -> {
            audience.clearTitle();
            audience.showTitle(
                    Title.title(
                            Component.text("Authentication"), Component.text("✔").color(NamedTextColor.GREEN),
                            Title.Times.of(Ticks.duration(0), Ticks.duration(40), Ticks.duration(20))
                    )
            );
            Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> audience.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 0.7f)), 1);
            Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> audience.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 1.1f)), 4);
        }));
    }

    public void showReceipt(Audience audience, List<String> purchaseHistory) {
        audience.sendMessage(Component.text("Thanks for your purchase! Receipt:").color(TextColor.color(36, 198, 166)));
        audience.sendMessage(Component.text(purchaseHistory.get(purchaseHistory.size() - 1)));
        audience.sendMessage(
                Component.text("Older receipts: ")
                        .color(TextColor.color(36, 198, 166))
                        .append(Component.text("[").color(NamedTextColor.GRAY))
                        .append(Component.text("View")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/purchasehistory"))
                        )
                        .append(Component.text("]").color(NamedTextColor.GRAY))
        );
        audience.sendMessage(
                Component.text("Current shard balance: ")
                        .color(TextColor.color(36, 198, 166))
                        .append(Component.text("[").color(NamedTextColor.GRAY))
                        .append(Component.text("View")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/balance"))
                        )
                        .append(Component.text("]").color(NamedTextColor.GRAY))
        );
        audience.playSound(Sound.sound(Key.key("minecraft:entity.player.levelup"), Sound.Source.PLAYER, 1f, 1.8f));
    }

    @EventHandler
    private void onPurchase(PurchaseSuccessEvent event) {
        MongoDatabase db = Objects.requireNonNull(ReflectedAPI.get()).getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
        MongoCollection<Document> purchases;
        purchases = db.getCollection("purchases");

        Bson purchaseFilter = and(eq("uuid", event.getPurchaser().getUniqueId().toString()));
        try {
            Document purchaseDocument = purchases.find(purchaseFilter).first();
            if (purchaseDocument != null) {
                List<String> purchaseHistory = purchaseDocument.getList("history", String.class);
                if (purchaseHistory != null && purchaseDocument.getBoolean("needsToSee")) {
                    showReceipt(event.getPurchaser(), purchaseHistory);
                    purchases.updateOne(purchaseFilter, set("needsToSee", false));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    private void join(PlayerJoinEvent event) {
        ReflectedAPI.get(api -> Bukkit.getScheduler().runTaskAsynchronously(api.getPlugin(), () -> hasPin(event.getPlayer(), (result) -> {
            if (result) {
                if (isAuthenticating(event.getPlayer())) {
                    event.getPlayer().sendMessage(Component.text("You left in the middle of an authentication, so it was cancelled.\nYou can always cancel a payment by running /cancel").color(TextColor.color(200, 255, 230)));
                    cancelAuth(event.getPlayer());
                }
            } else {
                if (isAuthenticating(event.getPlayer()) || api.getPlugin().getDatabase().getBusyChangingPwd(event.getPlayer().getUniqueId())) {
                    event.getPlayer().sendMessage(Component.text("It appears that you were trying to reset your PIN before leaving. Set it now.").color(TextColor.color(36, 198, 166)));
                    authenticate(event.getPlayer(), () -> event.getPlayer().sendMessage(Component.text("Success! Your PIN has been reset.").color(TextColor.color(200, 255, 230))),  false,"PIN change", true);
                } else {
                    getShardBalance(event.getPlayer(), (balance) -> {
                        if (balance > 0) {
                            event.getPlayer().sendMessage(Component.text("POLICY ENFORCEMENT").color(NamedTextColor.RED).append(Component.text(" > ")).append(Component.text("You must have a PIN on your Shard Wallet because it contains Shards.").color(TextColor.color(36, 198, 166))));
                            authenticate(event.getPlayer(), () -> event.getPlayer().sendMessage(Component.text("Success! Your PIN has been set.").color(TextColor.color(200, 255, 230))), false, "PIN change", true);
                        }
                    });

                }
            }

            // Check for any receipts they got while they were away
            MongoDatabase db = api.getPlugin().getDatabase().getMongoClient().getDatabase("purchases");
            MongoCollection<Document> purchases;

            try {
                purchases = db.getCollection("purchases");
                Bson purchaseFilter = and(eq("uuid", event.getPlayer().getUniqueId().toString()));

                Document purchaseDocument = purchases.find(purchaseFilter).first();
                if (purchaseDocument != null) {
                    List<String> purchaseHistory = purchaseDocument.getList("history", String.class);
                    if (purchaseHistory != null && purchaseDocument.getBoolean("needsToSee")) {
                        showReceipt(event.getPlayer(), purchaseHistory);
                        purchases.updateOne(purchaseFilter, set("needsToSee", false));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        })));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void chat(AsyncChatEvent event) {
        if (isAuthenticating(event.getPlayer())) {
            event.message(Component.text(""));
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You can't chat while authenticating for security reasons."));
        }
    }

    public interface IntegerCallback {
        void run(int result);
    }

    public interface BooleanCallback {
        void run(boolean result);
    }

    public interface ProductCallback {
        void run(Product result);
    }

    @RequiredArgsConstructor
    class Authentication {
        @Getter private final Runnable successRunnable;
        @Getter private final boolean cancellable;
        @Getter private final boolean delayEffect;
    }
}
