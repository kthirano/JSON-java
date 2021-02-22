#Milestone 4: streaming

##Overview
1. New method in XML that takes a Reader and streams all top-level elements in the JSONObject. Supports most streaming functions such as filter and foreach.

2. New method in JSONObject that streams all nested objects inside the JSONObject along with a JSONPointer that points to the location of the object.

##Benefits
When using streaming methods, developers are able to manipulate the JSONObject with more elegant syntax.
The reader version lets the developer to the above without needing to read the entire JSONObject into memory.

##New test cases
Tests both implementations mentioed above, all in XMLStreamTest.java