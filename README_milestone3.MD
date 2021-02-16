#Milestone 3: Key transformation

##Overview
New overloaded function toJSONObject that takes a Reader object and a Function<String, String> object.
All keys in the produced JSON will be replaced by the result of the original key applied to the given function.

###Benefits
When replacing the key during parse, we can avoid first reading the entire XML file and then recursively replacing the key.
This method requires the reading of the JSONObject twice: once during the initial parsing of the XML file and another during the key replacement process.
The newly implemented method replaces the keys during the parsing of the XML file, therefore improving efficiency.

##New Test Cases
For the key transformation unit tests, they are included in a new test class called XMLAdditionsTest.java.