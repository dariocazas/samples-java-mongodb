# Minimal dependencies

This a sample of read and write over MongoDB from Java with minimal dependencies.

Contains one Main that:

- Connect with secured MongoDB (standalone, using the [docker-compose of this repo](../mongodb-docker/docker-run.sh))
- Insert document from JSON and retrieve autogenerated `_id`
- Insert document from code and retrieve autogenerated `_id`
- Count inserted documents 
- Update document adding `menu.id2` field
- Update document adding a date in `menu.date` field (strong typed)
- Update concrete position in an array

## How to run

```sh
mvn verify exec:java
```


## Managing dates

When use low level API, all operations translated as JSON string representation (similar as mongo-shell use)
JSON manage a few types of data. MongoDB work with BSON, that is binary and strong typed. 
Review the code, and find comments to see the sample (take care about this).

This problem can be managed including Jackson library, and implement some serializers. 
