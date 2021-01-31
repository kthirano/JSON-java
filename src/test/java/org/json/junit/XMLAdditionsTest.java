package org.json.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.function.Function;

import org.json.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XMLAdditionsTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    public static String keyTransform1(String x){
        return "262_" + x;
    }
    private Function<String, String> ktObj1 = XMLAdditionsTest::keyTransform1;

    public static String notValidTransform(String x) {
        return "";
    }
    private Function<String, String> failFunction = XMLAdditionsTest::notValidTransform;

    @Test
    public void testXMLReplaceKeySimple(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple1.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObject(xmlReader, ktObj1);
                String expected = "{\"262_foo\":{\"262_baar\":2,\"262_bar\":1234}}";
                assertEquals("simple1.xml with key transform", expected, actual.toString());
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
    public void testXMLReplaceKeySimpleWithArray(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple2.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObject(xmlReader, ktObj1);
                String expected = "{\"262_foo\":{\"262_bar\":[12,34,56]}}";
                assertEquals("simple2.xml with key transform", expected, actual.toString());
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
    public void testXMLReplaceKeyComplex(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("small1.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObject(xmlReader, ktObj1);
                String expected ="{\"262_catalog\":{\"262_book\":[" +
                        "{\"262_title\":\"XML Developer's Guide\",\"262_author\":\"Gambardella, Matthew\"," +
                            "\"262_publish_date\":\"2000-10-01\",\"262_description\":\"An in-depth look at creating applications \\r\\n      with XML.\"," +
                            "\"262_price\":44.95,\"262_genre\":\"Computer\",\"262_id\":\"bk101\"}," +
                        "{\"262_title\":\"Midnight Rain\",\"262_author\":\"Ralls, Kim\",\"262_publish_date\":\"2000-12-16\"," +
                            "\"262_description\":\"A former architect battles corporate zombies, \\r\\n      " +
                            "an evil sorceress, and her own childhood to become queen \\r\\n      of the world.\"," +
                            "\"262_price\":5.95,\"262_genre\":\"Fantasy\",\"262_id\":\"bk102\"}," +
                        "{\"262_title\":\"Maeve Ascendant\",\"262_author\":\"Corets, Eva\",\"262_publish_date\":\"2000-11-17\"," +
                            "\"262_description\":\"After the collapse of a nanotechnology \\r\\n      society in England, the young survivors lay the \\r\\n      " +
                            "foundation for a new society.\",\"262_price\":5.95,\"262_genre\":\"Fantasy\",\"262_id\":\"bk103\"}," +
                        "{\"262_title\":\"Oberon's Legacy\",\"262_author\":\"Corets, Eva\",\"262_publish_date\":\"2001-03-10\"," +
                            "\"262_description\":\"In post-apocalypse England, the mysterious \\r\\n      agent known only as Oberon helps to create a new life \\r\\n      " +
                            "for the inhabitants of London. Sequel to Maeve \\r\\n      Ascendant.\",\"262_price\":5.95,\"262_genre\":\"Fantasy\",\"262_id\":\"bk104\"}," +
                        "{\"262_title\":\"The Sundered Grail\",\"262_author\":\"Corets, Eva\",\"262_publish_date\":\"2001-09-10\"," +
                            "\"262_description\":\"The two daughters of Maeve, half-sisters, \\r\\n      battle one another for control of England. Sequel to \\r\\n      " +
                            "Oberon's Legacy.\",\"262_price\":5.95,\"262_genre\":\"Fantasy\",\"262_id\":\"bk105\"}," +
                        "{\"262_title\":\"Lover Birds\",\"262_author\":\"Randall, Cynthia\",\"262_publish_date\":\"2000-09-02\"," +
                            "\"262_description\":\"When Carla meets Paul at an ornithology \\r\\n      conference, tempers fly as feathers get ruffled.\"," +
                            "\"262_price\":4.95,\"262_genre\":\"Romance\",\"262_id\":\"bk106\"}," +
                        "{\"262_title\":\"Splish Splash\",\"262_author\":\"Thurman, Paula\",\"262_publish_date\":\"2000-11-02\"," +
                            "\"262_description\":\"A deep sea diver finds true love twenty \\r\\n      thousand leagues beneath the sea.\"," +
                            "\"262_price\":4.95,\"262_genre\":\"Romance\",\"262_id\":\"bk107\"}," +
                        "{\"262_title\":\"Creepy Crawlies\",\"262_author\":\"Knorr, Stefan\",\"262_publish_date\":\"2000-12-06\"," +
                            "\"262_description\":\"An anthology of horror stories about roaches,\\r\\n      centipedes, scorpions  and other insects.\"," +
                            "\"262_price\":4.95,\"262_genre\":\"Horror\",\"262_id\":\"bk108\"}," +
                        "{\"262_title\":\"Paradox Lost\",\"262_author\":\"Kress, Peter\",\"262_publish_date\":\"2000-11-02\"," +
                            "\"262_description\":\"After an inadvertant trip through a Heisenberg\\r\\n      " +
                            "Uncertainty Device, James Salway discovers the problems \\r\\n      of being quantum.\",\"262_price\":6.95," +
                            "\"262_genre\":\"Science Fiction\",\"262_id\":\"bk109\"}," +
                        "{\"262_title\":\"Microsoft .NET: The Programming Bible\",\"262_author\":\"O'Brien, Tim\"," +
                            "\"262_publish_date\":\"2000-12-09\",\"262_description\":\"Microsoft's .NET initiative is explored in \\r\\n      " +
                            "detail in this deep programmer's reference.\",\"262_price\":36.95,\"262_genre\":\"Computer\",\"262_id\":\"bk110\"}," +
                        "{\"262_title\":\"MSXML3: A Comprehensive Guide\",\"262_author\":\"O'Brien, Tim\",\"262_publish_date\":\"2000-12-01\"," +
                            "\"262_description\":\"The Microsoft MSXML3 parser is covered in \\r\\n      detail, with attention to XML DOM interfaces, " +
                            "XSLT processing, \\r\\n      SAX and more.\",\"262_price\":36.95,\"262_genre\":\"Computer\",\"262_id\":\"bk111\"}," +
                        "{\"262_title\":\"Visual Studio 7: A Comprehensive Guide\",\"262_author\":\"Galos, Mike\",\"262_publish_date\":\"2001-04-16\"," +
                            "\"262_description\":\"Microsoft Visual Studio 7 is explored in depth,\\r\\n      " +
                            "looking at how Visual Basic, Visual C++, C#, and ASP+ are \\r\\n      integrated into a comprehensive development \\r\\n      " +
                            "environment.\",\"262_price\":49.95,\"262_genre\":\"Computer\",\"262_id\":\"bk112\"}]}}" ;
                assertEquals("simple2.xml with key transform", expected, actual.toString());
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
    public void testXMLReplaceKeyEmpty(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("empty.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObject(xmlReader, ktObj1);
                String expected = "{}";
                assertEquals("Empty XML with key transform", expected, actual.toString());
            } finally {
                if (xmlStream != null){
                    xmlStream.close();
                }
            }
        } catch (IOException e){
            fail ("file writer error: " + e.getMessage());
        }
    }

    @Test(expected = JSONException.class)
    public void shouldThrowException(){
        try{
            InputStream xmlStream = null;
            try{
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("simple1.xml");
                Reader xmlReader = new InputStreamReader(xmlStream);
                JSONObject actual = XML.toJSONObject(xmlReader, failFunction);
                String expected = "{}";
                assertEquals("Function does not produce unique keys", expected, actual.toString());
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
