package org.json.junit;

import org.json.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import static org.junit.Assert.*;
import java.util.function.Predicate;

public class XMLStreamTest {

    @Test
    public void testXMLStreamSimple1(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple1.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObjectStream(xmlReader).streamToJSON();
                String expected = "{\"foo\":{\"bar\":1234,\"baar\":2}}";
                assertEquals("simle1.xml JSON", expected, actual.toString());
            } finally {
                if (xmlStream != null){
                    xmlStream.close();
                }
            }
        } catch (IOException e){
            fail ("file writer error: " + e.getMessage());
        }
    }
    @Test
    public void testXMLStreamComplex(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("small1.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObjectStream(xmlReader).streamToJSON();
                InputStream xmlStream2 = XMLTest.class.getClassLoader().getResourceAsStream("small1.xml");
                Reader xmlReader2 = new InputStreamReader(xmlStream2);
                JSONObject expected = XML.toJSONObject(xmlReader2);
                assertEquals("Small1.xml should just return entire XML", expected.toString(), actual.toString());
            } finally {
                if (xmlStream != null) {
                    xmlStream.close();
                }
            }
        } catch (IOException e) {
            fail("file writer error: " +e.getMessage());
        }
    }

    @Test
    public void testXMLStreamSimpleFilter(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple1.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                Predicate<JSONObject> match = p->p.toString().equals("{\"foo\":{\"bar\":1234,\"baar\":2}}");
                JSONObject actual = XML.toJSONObjectStream(xmlReader).filter(match).streamToJSON();
                String expectedStr = "{\"foo\":{\"bar\":1234,\"baar\":2}}";
                assertEquals("simle1.xml JSON", expectedStr, actual.toString());
            } finally {
                if (xmlStream != null){
                    xmlStream.close();
                }
            }
        } catch (IOException e){
            fail ("file writer error: " + e.getMessage());
        }
    }
    @Test
    public void testXMLStreamManyTopLevel(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple3.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObjectStream(xmlReader).streamToJSON();
                String expectedStr = "{\"bar1\":12,\"bar2\":34,\"bar3\":56}";
                assertEquals("simle1.xml JSON", expectedStr, actual.toString());
            } finally {
                if (xmlStream != null){
                    xmlStream.close();
                }
            }
        } catch (IOException e){
            fail ("file writer error: " + e.getMessage());
        }
    }
    @Test
    public void testXMLStreamManyTopLevelFilter(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple3.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                Predicate<JSONObject> match = p->!p.toString().equals("{\"bar1\":12}");
                JSONObject actual = XML.toJSONObjectStream(xmlReader).filter(match).streamToJSON();
                String expectedStr = "{\"bar2\":34,\"bar3\":56}";
                assertEquals("simle1.xml JSON", expectedStr, actual.toString());
            } finally {
                if (xmlStream != null){
                    xmlStream.close();
                }
            }
        } catch (IOException e){
            fail ("file writer error: " + e.getMessage());
        }
    }

}
