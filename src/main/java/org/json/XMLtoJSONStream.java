package org.json;

import java.io.Writer;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class XMLtoJSONStream{

    private class JSONIterator implements Iterator{
        private XMLTokener x;
        public JSONIterator(XMLTokener tokener){
            x = tokener;
        }
        public boolean hasNext(){
            return x.more();
        }
        public JSONObject next(){
            x.skipPast("<");
            JSONObject jo = new JSONObject();
            parse(x, jo, null, XMLParserConfiguration.ORIGINAL);
            return jo;
        }
        private boolean parse(XMLTokener x, JSONObject context, String name, XMLParserConfiguration config)
                throws JSONException {
            char c;
            int i;
            JSONObject jsonObject = null;
            String string;
            String tagName;
            Object token;
            XMLXsiTypeConverter<?> xmlXsiTypeConverter;

            token = x.nextToken();

            // <!

            if (token == XML.BANG) {
                c = x.next();
                if (c == '-') {
                    if (x.next() == '-') {
                        x.skipPast("-->");
                        return false;
                    }
                    x.back();
                } else if (c == '[') {
                    token = x.nextToken();
                    if ("CDATA".equals(token)) {
                        if (x.next() == '[') {
                            string = x.nextCDATA();
                            if (string.length() > 0) {
                                context.accumulate(config.getcDataTagName(), string);
                            }
                            return false;
                        }
                    }
                    throw x.syntaxError("Expected 'CDATA['");
                }
                i = 1;
                do {
                    token = x.nextMeta();
                    if (token == null) {
                        throw x.syntaxError("Missing '>' after '<!'.");
                    } else if (token == XML.LT) {
                        i += 1;
                    } else if (token == XML.GT) {
                        i -= 1;
                    }
                } while (i > 0);
                return false;
            } else if (token == XML.QUEST) {

                // <?
                x.skipPast("?>");
                return false;
            } else if (token == XML.SLASH) {

                // Close tag </

                token = x.nextToken();
                if (name == null) {
                    throw x.syntaxError("Mismatched close tag " + token);
                }
                if (!token.equals(name)) {
                    throw x.syntaxError("Mismatched " + name + " and " + token);
                }
                if (x.nextToken() != XML.GT) {
                    throw x.syntaxError("Misshaped close tag");
                }
                return true;

            } else if (token instanceof Character) {
                //System.out.println(token);
                throw x.syntaxError("Misshaped tag");

                // Open tag <

            } else {
                tagName = (String) token;
                token = null;
                jsonObject = new JSONObject();
                boolean nilAttributeFound = false;
                xmlXsiTypeConverter = null;
                for (;;) {
                    if (token == null) {
                        token = x.nextToken();
                    }
                    // attribute = value
                    if (token instanceof String) {
                        string = (String) token;
                        token = x.nextToken();
                        if (token == XML.EQ) {
                            token = x.nextToken();
                            if (!(token instanceof String)) {
                                throw x.syntaxError("Missing value");
                            }

                            if (config.isConvertNilAttributeToNull()
                                    && XML.NULL_ATTR.equals(string)
                                    && Boolean.parseBoolean((String) token)) {
                                nilAttributeFound = true;
                            } else if(config.getXsiTypeMap() != null && !config.getXsiTypeMap().isEmpty()
                                    && XML.TYPE_ATTR.equals(string)) {
                                xmlXsiTypeConverter = config.getXsiTypeMap().get(token);
                            } else if (!nilAttributeFound) {
                                jsonObject.accumulate(string,
                                        config.isKeepStrings()
                                                ? ((String) token)
                                                : XML.stringToValue((String) token));
                            }
                            token = null;
                        } else {
                            jsonObject.accumulate(string, "");
                        }


                    } else if (token == XML.SLASH) {
                        // Empty tag <.../>
                        if (x.nextToken() != XML.GT) {
                            throw x.syntaxError("Misshaped tag");
                        }
                        if (nilAttributeFound) {
                            context.accumulate(tagName, JSONObject.NULL);
                        } else if (jsonObject.length() > 0) {
                            context.accumulate(tagName, jsonObject);
                        } else {
                            context.accumulate(tagName, "");
                        }
                        return false;

                    } else if (token == XML.GT) {
                        // Content, between <...> and </...>
                        for (;;) {
                            token = x.nextContent();
                            if (token == null) {
                                if (tagName != null) {
                                    throw x.syntaxError("Unclosed tag " + tagName);
                                }
                                return false;
                            } else if (token instanceof String) {
                                string = (String) token;
                                if (string.length() > 0) {
                                    if(xmlXsiTypeConverter != null) {
                                        jsonObject.accumulate(config.getcDataTagName(),
                                                XML.stringToValue(string, xmlXsiTypeConverter));
                                    } else {
                                        jsonObject.accumulate(config.getcDataTagName(),
                                                config.isKeepStrings() ? string : XML.stringToValue(string));
                                    }
                                }

                            } else if (token == XML.LT) {
                                // Nested element
                                if (parse(x, jsonObject, tagName, config)) {
                                    if (jsonObject.length() == 0) {
                                        context.accumulate(tagName, "");
                                    } else if (jsonObject.length() == 1
                                            && jsonObject.opt(config.getcDataTagName()) != null) {
                                        context.accumulate(tagName, jsonObject.opt(config.getcDataTagName()));
                                    } else {
                                        context.accumulate(tagName, jsonObject);
                                    }
                                    return false;
                                }
                            }
                        }//for
                    } else {
                        //System.out.println(token);
                        throw x.syntaxError("Misshaped tag");
                    }
                } // for
            }
        }

    }

    private Stream<JSONObject> jsonStream;


    public XMLtoJSONStream(XMLTokener x){
        Spliterator<JSONObject> tempSpliter = Spliterators.spliteratorUnknownSize(new JSONIterator(x), Spliterator.NONNULL);
        jsonStream = StreamSupport.stream(tempSpliter, false);
    }/*
    private XMLtoJSONStream buildFromStream(Stream<JSONObject> j){
        jsonStream = j;
        return this;
    }*/

    public XMLtoJSONStream filter(Predicate p){
        jsonStream = jsonStream.filter(p);
        return this;
    }

    private JSONObject collectToJSON(){
        JSONObject master = new JSONObject();
        Iterator<JSONObject> iter = jsonStream.iterator();
        while (iter.hasNext()){
            JSONObject temp = iter.next();
            for (String key : temp.keySet()){
                master.accumulate(key, temp.get(key));
            }
        }
        return master;
    }

    public Writer write(Writer a){
        return collectToJSON().write(a);
    }

    public JSONObject streamToJSON(){
        return collectToJSON();
    }

    public XMLtoJSONStream distinct(){
        jsonStream = jsonStream.distinct();
        return this;
    }
    public boolean allMatch(Predicate p){
        return jsonStream.allMatch(p);
    }
    public boolean anyMatch(Predicate p){
        return jsonStream.anyMatch(p);
    }
    public XMLtoJSONStream limit(long s){
        jsonStream = jsonStream.limit(s);
        return this;
    }
    public boolean noneMatch(Predicate p){
        return jsonStream.noneMatch(p);
    }
    public XMLtoJSONStream peek(Consumer c){
        jsonStream = jsonStream.peek(c);
        return this;
    }
    public XMLtoJSONStream skip(long n){
        jsonStream = jsonStream.skip(n);
        return this;
    }
    public XMLtoJSONStream sorted(Comparator c){
        jsonStream = jsonStream.sorted(c);
        return this;
    }
    public Object[] toArray(){
        return jsonStream.toArray();
    }
    public XMLtoJSONStream flatMap(Function f){
        jsonStream = jsonStream.flatMap(f);
        return this;
    }
    public DoubleStream flatMapToDouble(Function f){
        return jsonStream.flatMapToDouble(f);
    }
    public IntStream flatMapToInt(Function f){
        return jsonStream.flatMapToInt(f);
    }
    public LongStream flatMapToLong(Function f){
        return jsonStream.flatMapToLong(f);
    }
    public void forEach(Consumer a){
        jsonStream.forEach(a);
    }
    public void forEachOrdered(Consumer a){
        jsonStream.forEachOrdered(a);
    }
    public XMLtoJSONStream map(Function f){
        jsonStream = jsonStream.map(f);
        return this;
    }

}
