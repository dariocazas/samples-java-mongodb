package dcazas.samplesjavamongodb.minimaldeps;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonArray;
import org.bson.BsonDateTime;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.ZonedDateTime;
import java.util.*;

public class Main {

    /**
     * We don't manually create this database, will be created on the first iteration
     */
    private static final String DATABASE_NAME = "samples_java_mongodb";

    /**
     * We don't manually create this collection, will be created on the first iteration
     */
    private static final String COLLECTION_NAME = "minimal_deps";

    public static void main(String[] args) throws Exception {
        try (MongoClient mongoClient = initClient()) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
            ObjectId fromJSON = insert(collection, initFromJSON());
            ObjectId fromCode = insert(collection, initFromCode());
            long count = count(collection);

            System.out.println("########################################################################");
            System.out.println("\tThe collection " + DATABASE_NAME + "." + COLLECTION_NAME +
                    " has " + count + " documents");

            // Review the collection from http://localhost:8081/db/samples_java_mongodb/minimal_deps/
            //
            // Review document with menu.id 'fileFromCode'. Locate the menu.popup.menuItem.value as ISODate().
            // Review document with menu.id 'fileFromJSON'. Locate the menu.popup.menuItem.value with a date...
            // Are the same data type? No... One is a Date, other is a string.
            // BSON has many data types that JSON don't manage, take care when insert by parsing JSON.
            //
            // This is a low level example. For these reason, only can manage the date recognition programmatically,
            // non in Document.parse() operation. Using other strategies as Jackson open other possibilities.

            updateSampleAddField(collection, fromJSON);
            updateSampleAddDate(collection, fromJSON);
            updateInArray(collection, fromJSON);
        }
    }

    private static MongoClient initClient() {
        // List of servers
        List<ServerAddress> seeds = new ArrayList<>();
        seeds.add(new ServerAddress("localhost", 27017));

        // Manage access credentials (CAN'T use root user... only in local test)
        MongoCredential credential = MongoCredential.createCredential("root", "admin", "example".toCharArray());

        // To manage other configuration options, as replicaSet if you use, or read/write concern
        MongoClientOptions.Builder builder = MongoClientOptions.builder();
        // If write fails, retry (recommended enable)
        builder.retryWrites(true);

        // Instance client (the package is com.mongodb, not com.mongodb.client)
        return new MongoClient(seeds, credential, builder.build());
    }

    private static Document initFromJSON() {
        String json = "{\"menu\": {\n" +
                "  \"id\": \"fileFromJSON\",\n" +
                "  \"value\": \"File\",\n" +
                "  \"popup\": {\n" +
                "    \"menuitem\": [\n" +
                "      {\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"},\n" +
                "      {\"value\": \"2020-06-28T08:18:56Z\", \"onclick\": \"ShowDateTime()\"}\n" +
                "    ]\n" +
                "  }\n" +
                "}}";
        return Document.parse(json);
    }

    private static Document initFromCode() {
        Map<String, Object> menuItem1Map = new HashMap<>();
        menuItem1Map.put("value", "New");
        menuItem1Map.put("onclick", "CreateNewDoc()");
        Document menuItem1Doc = new Document(menuItem1Map);

        Map<String, Object> menuItem2Map = new HashMap<>();
        menuItem2Map.put("value", new Date());
        menuItem2Map.put("onclick", "ShowDateTime()");
        Document menuItem2Doc = new Document(menuItem2Map);

        Document menuItem = new Document("menuItem", Arrays.asList(menuItem1Doc, menuItem2Doc));
        Document menu = new Document("id", "fileFromCode").append("value", "File").append("popup", menuItem);

        return new Document("menu", menu);
    }


    private static ObjectId insert(MongoCollection<Document> collection, Document data) {
        collection.insertOne(data);
        // When insert, if _id field don't exist, auto fill with the generated value
        ObjectId id = (ObjectId) data.get("_id");
        System.out.println("################## Inserted a document with _id: " + id);
        return id;
    }

    private static long count(MongoCollection<Document> collection) {
        return collection.countDocuments();
    }

    /**
     * This is equivalent to run:
     * <code>
     * db.collection.update({_id: ObjectId(xxxx)}, { $set: {"menu.id2":"setNewField"}})
     * </code>
     *
     * @param collection
     * @param id
     */
    private static void updateSampleAddField(MongoCollection<Document> collection, ObjectId id) {
        Document filter = new Document("_id", id);
        Document updateField = new Document("menu.id2", "setNewField");
        collection.updateOne(filter, new Document("$set", updateField));
        System.out.println("################## update document with _id: " + id + ", adding a field menu.id2");
    }

    /**
     * This is equivalent to run:
     * <code>
     * db.collection.updateone({_id: ObjectId(xxxx)}, { $set: {"menu.date": "ISODate('2020-06-28T08:18:56Z')"}})
     * </code>
     *
     * @param collection
     * @param id
     */
    private static void updateSampleAddDate(MongoCollection<Document> collection, ObjectId id) {
        Document filter = new Document("_id", id);
        Document updateField = new Document("menu.date", parseIso8601("2020-06-28T08:18:56Z"));
        collection.updateOne(filter, new Document("$set", updateField));
        System.out.println("################## update document with _id: " + id + ", adding a field menu.date (as ISODate type)");
    }

    /**
     * This is equivalent to run:
     * <code>
     * db.collection.updateone( {_id: ObjectId(xxxx) },
     * { $set: {
     * "menu.popup.menuitem.1.onclick" : "ShowDateTime()Up",
     * "menu.popup.menuitem.0": { "value2" : "updateForeachArray", "value": "NewUp" },
     * } })
     * </code>
     *
     * @param collection
     * @param id
     */
    private static void updateInArray(MongoCollection<Document> collection, ObjectId id) {
        Document filter = new Document("_id", id);
        Document update1 = new Document("menu.popup.menuitem.1.onclick", "ShowDateTime()Up");
        collection.updateOne(filter, new Document("$set", update1));
        //collection.updateOne(filter, new Document("$set", update1));
        System.out.println("################## update document with _id: " + id + ", update menu.popup.menuitem: position 0 overrided, position 1 update onclick");
    }

    private static Date parseIso8601(String date) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2020-06-28T08:18:56Z");
        return Date.from(zonedDateTime.toInstant());
    }

}

