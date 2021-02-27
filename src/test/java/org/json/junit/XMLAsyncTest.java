package org.json.junit;

import org.json.*;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class XMLAsyncTest {

    public static JSONObject actual;
    public static String error;
    public static void copy(JSONObject jo){
        actual = jo;
    }
    public static void printExc(Exception e){
        System.out.println(e);
    }
    public static void copyExc(Exception e){
        error = e.getMessage();
    }

    @Test
    public void testXMLAsync(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple1.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                actual = new JSONObject();
                Thread parseThread = XML.toJSONObject(xmlReader, XMLAsyncTest::copy, XMLAsyncTest::printExc);
                assertEquals("pre-join", "{}", actual.toString());
                parseThread.join();
                String expected = "{\"foo\":{\"bar\":1234,\"baar\":2}}";
                assertEquals("simle1.xml JSON", expected, actual.toString());
            } catch (InterruptedException e){
                printExc(e);
            }
            finally {
                if (xmlStream != null){
                    xmlStream.close();
                }
            }
        } catch (IOException e){
            fail ("file writer error: " + e.getMessage());
        }
    }
    @Test
    public void testXMLAsyncFail(){
        try{
            InputStream xmlStream = null;
            try{
                String testXML = "<this is not a valid XML";
                xmlStream = new ByteArrayInputStream(testXML.getBytes());
                Reader xmlReader = new InputStreamReader(xmlStream);
                actual = new JSONObject();
                error = "";
                Thread parseThread = XML.toJSONObject(xmlReader, XMLAsyncTest::copy, XMLAsyncTest::copyExc);
                assertEquals("pre-join", "{}", actual.toString());
                assertEquals("pre-join exception", "", error);
                parseThread.join();
                String expected = "Misshaped element at 24 [character 25 line 1]";
                assertEquals("test string exception message", expected, error);
            } catch (InterruptedException e){
                printExc(e);
            }
            finally {
                if (xmlStream != null){
                    xmlStream.close();
                }
            }
        } catch (IOException e){
            fail ("file writer error: " + e.getMessage());
        }
    }


}
