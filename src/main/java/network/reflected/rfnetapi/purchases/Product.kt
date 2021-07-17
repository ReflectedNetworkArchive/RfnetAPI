package network.reflected.rfnetapi.purchases

data class Product (
    val name: String,
    val price: Int,
    val oneTimePurchase: Boolean
)