# SpringBootDemo
This is my final project of INFO 7255.

This project includes the following parts:
- Used JSON Schema to verify JSON format, traversed to separate the specific data by the object and stored each object as one document in Redis, then implemented CRUD function.
- Generated a bare token and used token verification on any endpoints before sending Http request.
- Generated e-Tag for each response, if the client requested cached content, then respond not-modified. 
- Created an event to trigger the Redis queue to store documents, retrieved it from the queue when listening to the event and indexed this document in Elasticsearch (using parent-child mode to save the hierarchical relationship). Then used the Kibana console to send a query request to Elasticsearch to query the index.
