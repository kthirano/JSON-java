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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
                assertEquals("simple1.xml JSON", expectedStr, actual.toString());
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
                assertEquals("simple3.xml JSON", expectedStr, actual.toString());
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
                assertEquals("simple3.xml JSON", expectedStr, actual.toString());
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
    public void testXMLStreamManyTopLevelForEach(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple3.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                Predicate<JSONObject> match = p->!p.toString().equals("{\"bar1\":12}");
                ArrayList<JSONObject> res = new ArrayList<>();
                XML.toJSONObjectStream(xmlReader).filter(match).forEach( j -> res.add((JSONObject) j));
                String expectedStr = "[{}, {\"bar2\":34}, {\"bar3\":56}]";
                assertEquals("simple3.xml JSON", expectedStr,res.toString());
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
    public void testXMLStreamPreBuiltObject(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple3.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject tester = XML.toJSONObject(xmlReader);
                List<Map.Entry<String, Object>> res = tester.toStream().collect(Collectors.toList());
                String expectedStr = "[bar1=12, bar2=34, bar3=56]";
                assertEquals("simple1.xml JSON", expectedStr,res.toString());
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
    public void testXMLStreamPreBuiltObjectWithFilter(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple3.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject tester = XML.toJSONObject(xmlReader);
                Predicate<Map.Entry<String, Object>> match = p->!p.getValue().toString().equals("12");
                List<Map.Entry<String, Object>> res = tester.toStream().filter(match).collect(Collectors.toList());
                String expectedStr = "[bar2=34, bar3=56]";
                assertEquals("simple1.xml JSON", expectedStr,res.toString());
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
