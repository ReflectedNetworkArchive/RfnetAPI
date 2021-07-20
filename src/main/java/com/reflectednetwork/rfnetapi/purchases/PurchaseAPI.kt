package com.reflectednetwork.rfnetapi.purchases

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import com.reflectednetwork.rfnetapi.ReflectedAPI.Companion.get
import com.reflectednetwork.rfnetapi.async.async
import com.reflectednetwork.rfnetapi.bugs.ExceptionDispensary
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.*

class PurchaseAPI : Listener {
    fun hasItem(item: String, player: Player): Boolean {
        return howManyProductOwns(player, item) > 0
    }

    fun getShardBalance(player: Player):Int {
        return try {
            val shards = get().database.getCollection("purchases", "shards")

            shards.find(
                and(eq("uuid", player.uniqueId.toString()), gt("shards", 0))
            ).first()?.getInteger("shards") ?: 0
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "getting shard balance", player)
            0
        }
    }

    fun purchaseWithShards(item: String, friendlyName: String, player: Player) {
        try {
            val product = getProduct(item)
            val balance = getShardBalance(player)

            if (product == null) {
                player.sendMessage(Component.text("Can't find that product.").color(NamedTextColor.RED))
                return
            }

            if (balance >= product.price) {
                if (product.oneTimePurchase && hasItem(item, player)) {
                    player.sendMessage(Component.text("You already own that item.").color(NamedTextColor.RED))
                    return
                } else {
                    PurchaseGUI.confirm(player, {
                        try {
                            givePlayerItem(player, item, friendlyName)
                            addShards(player, -product.price)
                        } catch (e: Exception) {
                            ExceptionDispensary.reportAndNotify(e, "vending item", player)
                        }
                    }, "Purchase $friendlyName")
                }
            } else {
                player.sendMessage(Component.text("You don't have enough to buy that!").color(NamedTextColor.RED))
            }
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "purchasing product", player)
        }
    }

    fun addShards(player: Player, amount: Int) {
        try {
            val shards = get().database.getCollection("purchases", "shards")
            val shardFilter = and(eq("uuid", player.uniqueId.toString()), exists("shards"))

            val balance = getShardBalance(player) + amount
            if (shards.countDocuments(shardFilter) > 0) {
                shards.updateOne(
                    shardFilter,
                    set("shards", balance)
                )
            } else {
                shards.insertOne(
                    Document()
                        .append("uuid", player.uniqueId.toString())
                        .append("shards", balance)
                )
            }
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "adding shards", player)
        }
    }

    fun payShards(from: Player, to: Player, amount: Int) {
        try {
            if (getShardBalance(from) >= amount) {
                PurchaseGUI.confirm(from, {
                    addShards(to, amount)
                    addShards(from, -amount)
                    from.sendMessage(Component.text("Payment succeeded.").color(NamedTextColor.GREEN))
                    to.sendMessage(Component.text("${from.name} paid you $amount shards.").color(NamedTextColor.GREEN))
                }, "Pay $amount shards to ${to.name}")
            } else {
                from.sendMessage(
                    Component.text("You don't have that many shards.").color(NamedTextColor.RED)
                )
            }
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "paying other player", from)
        }
    }

    fun createProduct(product: Product) {
        try {
            val products = get().database.getCollection("purchases", "products")
            val productFilter = eq("name", product.name)

            if (products.find(productFilter).first() == null) {
                products.insertOne(
                    Document()
                        .append("name", product.name)
                        .append("price", product.price)
                        .append("oneTimePurchase", product.oneTimePurchase)
                )
            } else {
                throw RuntimeException("That product already exists!")
            }
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "creating product")
        }
    }

    fun getProduct(item: String): Product? {
        try {
            val products = get().database.getCollection("purchases", "products")

            val productDocument = products.find(
                and(eq("name", item), exists("price"), exists("oneTimePurchase"))
            ).first()

            return if (productDocument != null) {
                Product(
                    item,
                    productDocument.getInteger("price"),
                    productDocument.getBoolean("oneTimePurchase")
                )
            } else null
        } catch (e: Exception) {
            ExceptionDispensary.report(e, "retrieving product")
            return null
        }
    }

    fun howManyProductOwns(player: Player, item: String): Int {
        return try {
            val purchases = get().database.getCollection("purchases", "purchases")
            val purchaseFilter = and(eq("uuid", player.uniqueId.toString()), exists(item))

            purchases.find(purchaseFilter).first()?.getInteger(item) ?: 0
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "retrieving product ownership count", player)
            0
        }
    }

    fun givePlayerItem(player: Player, item: String, niceName: String) {
        try {
            val product = getProduct(item)
            if (product == null) {
                player.sendMessage(Component.text("Couldn't find that item.").color(NamedTextColor.RED))
                throw NullPointerException("Couldn't find that item")
            }

            val owns = hasItem(item, player)
            if (product.oneTimePurchase && owns) {
                player.sendMessage(Component.text("You already own that item!").color(NamedTextColor.RED))
                throw RuntimeException("Already owns that item")
            } else {
                val purchases = get().database.getCollection("purchases", "purchases")

                val purchaseFilter = and(eq("uuid", player.uniqueId.toString()))

                val purchaseDocument = purchases.find(
                    purchaseFilter
                ).first()?: purchases.find(
                    eq(
                        "_id",
                        purchases.insertOne(
                            Document()
                                .append("uuid", player.uniqueId.toString())
                        ).insertedId
                    )
                ).first()!!

                if (owns) {
                    purchases.updateOne(purchaseFilter, inc(item, 1))
                } else {
                    purchases.updateOne(purchaseFilter, set(item, 1))
                }

                var purchaseHistory = purchaseDocument.getList("history", String::class.java)
                if (purchaseHistory == null) {
                    purchaseHistory = ArrayList()
                }
                purchaseHistory.add("${player.name} purchased '$item' on ${Date()}")
                purchases.updateOne(purchaseFilter, set("history", purchaseHistory))
//                purchases.updateOne(purchaseFilter, set("needsToSee", true))

                val event = AsyncPurchaseSuccessEvent(
                    JProduct(
                        product.price,
                        product.name,
                        product.oneTimePurchase,
                        niceName
                    ),
                    player
                )
                async { Bukkit.getPluginManager().callEvent(event) }
            }
        } catch (e: Exception) {
            ExceptionDispensary.reportAndNotify(e, "dispensing item", player)
        }
    }
}