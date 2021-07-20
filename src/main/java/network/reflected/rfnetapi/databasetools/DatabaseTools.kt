package network.reflected.rfnetapi.databasetools

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import network.reflected.rfnetapi.getReflectedAPI
import org.bson.Document
import org.bson.conversions.Bson

fun getCollection(databaseName: String, collectionName: String): MongoCollection<Document?> {
    return getReflectedAPI().database.getCollection(databaseName, collectionName)
}

fun <TDocument> MongoCollection<TDocument>.updateOneOrCreate(filter: Bson, update: Bson, create: () -> TDocument) {
    if (countDocuments(filter) > 0) {
        findOneAndUpdate(filter, update)
    } else {
        insertOne(create.invoke())
    }
}

fun <TDocument> MongoCollection<TDocument>.findOneOrCreate(filter: Bson, create: () -> TDocument): TDocument {
    return find(filter).first() ?: find(Filters.eq("_id", insertOne(create.invoke()).insertedId)).first()!!
}
