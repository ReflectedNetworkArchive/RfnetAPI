package com.reflectednetwork.rfnetapi.databasetools

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.reflectednetwork.rfnetapi.getReflectedAPI
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

fun <T> Document.getMutableSet(key: String): MutableSet<T> {
    val set = mutableSetOf<T>()
    val list = this.get(key) as ArrayList<T>
    for (value in list) {
        set.add(value)
    }
    return set
}