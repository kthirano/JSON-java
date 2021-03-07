#Milestone 5: Asyncronous Methods

##Overview
New method in XML that takes a Reader, one consumer object to operate on a JSONObject, and one consumer object to execute if the parsing fails.
This method returns a Thread object. I used this object in the test cases to join, to test the method's asyncronous feature.

##Benefits
Developers can perform other duties when waiting for the reader to parse the XML file into a JSONObject

##New Test Cases
Test cases in XMLAsyncTest.java