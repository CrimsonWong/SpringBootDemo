# Medical plans storage service with optimization 
This is my final project of INFO 7255, and I made some optimization. Below are key features of this project. 

This project includes the following parts:
- Built a medical plan service system to store medical plans, provided crud APIs and search APIs to the front end. 
- Used JSON Schema at the controller side to verify the format of the medical plans data in an HTTP request. 
- Used JWT to implement authentication on every endpoint; Generated e-Tag for each response to save bandwidth.
- Enhanced the scalability and performance of the data query and storage system by partitioning the data vertically under different column families and applied distributed cache mechanism using Redis. 
- Developed an indexing engine for the documents, flattened the data into parent-child mode in Elasticsearch. 
- Implemented a Message Queue by using Redis queue between storing and indexing, to provide high throughput and low latency of processed data.  
