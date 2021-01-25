package org.json;

/*
Copyright (c) 2015 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;


/**
 * This provides static methods to convert an XML text into a JSONObject, and to
 * covert a JSONObject into an XML text.
 *
 * @author JSON.org
 * @version 2016-08-10
 */
@SuppressWarnings("boxing")
public class XML {

    /** The Character '&amp;'. */
    public static final Character AMP = '&';

    /** The Character '''. */
    public static final Character APOS = '\'';

    /** The Character '!'. */
    public static final Character BANG = '!';

    /** The Character '='. */
    public static final Character EQ = '=';

    /** The Character <pre>{@code '>'. }</pre>*/
    public static final Character GT = '>';

    /** The Character '&lt;'. */
    public static final Character LT = '<';

    /** The Character '?'. */
    public static final Character QUEST = '?';

    /** The Character '"'. */
    public static final Character QUOT = '"';

    /** The Character '/'. */
    public static final Character SLASH = '/';

    /**
     * Null attribute name
     */
    public static final String NULL_ATTR = "xsi:nil";

    public static final String TYPE_ATTR = "xsi:type";

    /**
     * Creates an iterator for navigating Code Points in a string instead of
     * characters. Once Java7 support is dropped, this can be replaced with
     * <code>
     * string.codePoints()
     * </code>
     * which is available in Java8 and above.
     *
     * @see <a href=
     *      "http://stackoverflow.com/a/21791059/6030888">http://stackoverflow.com/a/21791059/6030888</a>
     */
    private static Iterable<Integer> codePointIterator(final String string) {
        return new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    private int nextIndex = 0;
                    private int length = string.length();

                    @Override
                    public boolean hasNext() {
                        return this.nextIndex < this.length;
                    }

                    @Override
                    public Integer next() {
                        int result = string.codePointAt(this.nextIndex);
                        this.nextIndex += Character.charCount(result);
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Replace special characters with XML escapes:
     *
     * <pre>{@code 
     * &amp; (ampersand) is replaced by &amp;amp;
     * &lt; (less than) is replaced by &amp;lt;
     * &gt; (greater than) is replaced by &amp;gt;
     * &quot; (double quote) is replaced by &amp;quot;
     * &apos; (single quote / apostrophe) is replaced by &amp;apos;
     * }</pre>
     *
     * @param string
     *            The string to be escaped.
     * @return The escaped string.
     */
    public static String escape(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        for (final int cp : codePointIterator(string)) {
            switch (cp) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&apos;");
                break;
            default:
                if (mustEscape(cp)) {
                    sb.append("&#x");
                    sb.append(Integer.toHexString(cp));
                    sb.append(';');
                } else {
                    sb.appendCodePoint(cp);
                }
            }
        }
        return sb.toString();
    }

    /**
     * @param cp code point to test
     * @return true if the code point is not valid for an XML
     */
    private static boolean mustEscape(int cp) {
        /* Valid range from https://www.w3.org/TR/REC-xml/#charsets
         *
         * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
         *
         * any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
         */
        // isISOControl is true when (cp >= 0 && cp <= 0x1F) || (cp >= 0x7F && cp <= 0x9F)
        // all ISO control characters are out of range except tabs and new lines
        return (Character.isISOControl(cp)
                && cp != 0x9
                && cp != 0xA
                && cp != 0xD
            ) || !(
                // valid the range of acceptable characters that aren't control
                (cp >= 0x20 && cp <= 0xD7FF)
                || (cp >= 0xE000 && cp <= 0xFFFD)
                || (cp >= 0x10000 && cp <= 0x10FFFF)
            )
        ;
    }

    /**
     * Removes XML escapes from the string.
     *
     * @param string
     *            string to remove escapes from
     * @return string with converted entities
     */
    public static String unescape(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0, length = string.length(); i < length; i++) {
            char c = string.charAt(i);
            if (c == '&') {
                final int semic = string.indexOf(';', i);
                if (semic > i) {
                    final String entity = string.substring(i + 1, semic);
                    sb.append(XMLTokener.unescapeEntity(entity));
                    // skip past the entity we just parsed.
                    i += entity.length() + 1;
                } else {
                    // this shouldn't happen in most cases since the parser
                    // errors on unclosed entries.
                    sb.append(c);
                }
            } else {
                // not part of an entity
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Throw an exception if the string contains whitespace. Whitespace is not
     * allowed in tagNames and attributes.
     *
     * @param string
     *            A string.
     * @throws JSONException Thrown if the string contains whitespace or is empty.
     */
    public static void noSpace(String string) throws JSONException {
        int i, length = string.length();
        if (length == 0) {
            throw new JSONException("Empty string.");
        }
        for (i = 0; i < length; i += 1) {
            if (Character.isWhitespace(string.charAt(i))) {
                throw new JSONException("'" + string
                        + "' contains a space character.");
            }
        }
    }

    /**
     * Scan the content following the named tag, attaching it to the context.
     *
     * @param x
     *            The XMLTokener containing the source string.
     * @param context
     *            The JSONObject that will include the new material.
     * @param name
     *            The tag name.
     * @return true if the close tag is processed.
     * @throws JSONException
     */
    private static boolean parse(XMLTokener x, JSONObject context, String name, XMLParserConfiguration config)
            throws JSONException {
        char c;
        int i;
        JSONObject jsonObject = null;
        String string;
        String tagName;
        Object token;
        XMLXsiTypeConverter<?> xmlXsiTypeConverter;

        // Test for and skip past these forms:
        // <!-- ... -->
        // <! ... >
        // <![ ... ]]>
        // <? ... ?>
        // Report errors for these forms:
        // <>
        // <=
        // <<

        token = x.nextToken();

        // <!

        if (token == BANG) {
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
                } else if (token == LT) {
                    i += 1;
                } else if (token == GT) {
                    i -= 1;
                }
            } while (i > 0);
            return false;
        } else if (token == QUEST) {

            // <?
            x.skipPast("?>");
            return false;
        } else if (token == SLASH) {

            // Close tag </

            token = x.nextToken();
            if (name == null) {
                throw x.syntaxError("Mismatched close tag " + token);
            }
            if (!token.equals(name)) {
                throw x.syntaxError("Mismatched " + name + " and " + token);
            }
            if (x.nextToken() != GT) {
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
                    if (token == EQ) {
                        token = x.nextToken();
                        if (!(token instanceof String)) {
                            throw x.syntaxError("Missing value");
                        }

                        if (config.isConvertNilAttributeToNull()
                                && NULL_ATTR.equals(string)
                                && Boolean.parseBoolean((String) token)) {
                            nilAttributeFound = true;
                        } else if(config.getXsiTypeMap() != null && !config.getXsiTypeMap().isEmpty()
                                && TYPE_ATTR.equals(string)) {
                            xmlXsiTypeConverter = config.getXsiTypeMap().get(token);
                        } else if (!nilAttributeFound) {
                            jsonObject.accumulate(string,
                                    config.isKeepStrings()
                                            ? ((String) token)
                                            : stringToValue((String) token));
                        }
                        token = null;
                    } else {
                        jsonObject.accumulate(string, "");
                    }


                } else if (token == SLASH) {
                    // Empty tag <.../>
                    if (x.nextToken() != GT) {
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

                } else if (token == GT) {
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
                                            stringToValue(string, xmlXsiTypeConverter));
                                } else {
                                    jsonObject.accumulate(config.getcDataTagName(),
                                            config.isKeepStrings() ? string : stringToValue(string));
                                }
                            }

                        } else if (token == LT) {
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

    /**
     * This method tries to convert the given string value to the target object
     * @param string String to convert
     * @param typeConverter value converter to convert string to integer, boolean e.t.c
     * @return JSON value of this string or the string
     */
    public static Object stringToValue(String string, XMLXsiTypeConverter<?> typeConverter) {
        if(typeConverter != null) {
            return typeConverter.convert(string);
        }
        return stringToValue(string);
    }

    /**
     * This method is the same as {@link JSONObject#stringToValue(String)}.
     *
     * @param string String to convert
     * @return JSON value of this string or the string
     */
    // To maintain compatibility with the Android API, this method is a direct copy of
    // the one in JSONObject. Changes made here should be reflected there.
    // This method should not make calls out of the XML object.
    public static Object stringToValue(String string) {
        if ("".equals(string)) {
            return string;
        }

        // check JSON key words true/false/null
        if ("true".equalsIgnoreCase(string)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(string)) {
            return Boolean.FALSE;
        }
        if ("null".equalsIgnoreCase(string)) {
            return JSONObject.NULL;
        }

        /*
         * If it might be a number, try converting it. If a number cannot be
         * produced, then the value will just be a string.
         */

        char initial = string.charAt(0);
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            try {
                return stringToNumber(string);
            } catch (Exception ignore) {
            }
        }
        return string;
    }
    
    /**
     * direct copy of {@link JSONObject#stringToNumber(String)} to maintain Android support.
     */
    private static Number stringToNumber(final String val) throws NumberFormatException {
        char initial = val.charAt(0);
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            // decimal representation
            if (isDecimalNotation(val)) {
                // Use a BigDecimal all the time so we keep the original
                // representation. BigDecimal doesn't support -0.0, ensure we
                // keep that by forcing a decimal.
                try {
                    BigDecimal bd = new BigDecimal(val);
                    if(initial == '-' && BigDecimal.ZERO.compareTo(bd)==0) {
                        return Double.valueOf(-0.0);
                    }
                    return bd;
                } catch (NumberFormatException retryAsDouble) {
                    // this is to support "Hex Floats" like this: 0x1.0P-1074
                    try {
                        Double d = Double.valueOf(val);
                        if(d.isNaN() || d.isInfinite()) {
                            throw new NumberFormatException("val ["+val+"] is not a valid number.");
                        }
                        return d;
                    } catch (NumberFormatException ignore) {
                        throw new NumberFormatException("val ["+val+"] is not a valid number.");
                    }
                }
            }
            // block items like 00 01 etc. Java number parsers treat these as Octal.
            if(initial == '0' && val.length() > 1) {
                char at1 = val.charAt(1);
                if(at1 >= '0' && at1 <= '9') {
                    throw new NumberFormatException("val ["+val+"] is not a valid number.");
                }
            } else if (initial == '-' && val.length() > 2) {
                char at1 = val.charAt(1);
                char at2 = val.charAt(2);
                if(at1 == '0' && at2 >= '0' && at2 <= '9') {
                    throw new NumberFormatException("val ["+val+"] is not a valid number.");
                }
            }
            // integer representation.
            // This will narrow any values to the smallest reasonable Object representation
            // (Integer, Long, or BigInteger)
            
            // BigInteger down conversion: We use a similar bitLenth compare as
            // BigInteger#intValueExact uses. Increases GC, but objects hold
            // only what they need. i.e. Less runtime overhead if the value is
            // long lived.
            BigInteger bi = new BigInteger(val);
            if(bi.bitLength() <= 31){
                return Integer.valueOf(bi.intValue());
            }
            if(bi.bitLength() <= 63){
                return Long.valueOf(bi.longValue());
            }
            return bi;
        }
        throw new NumberFormatException("val ["+val+"] is not a valid number.");
    }
    
    /**
     * direct copy of {@link JSONObject#isDecimalNotation(String)} to maintain Android support.
     */
    private static boolean isDecimalNotation(final String val) {
        return val.indexOf('.') > -1 || val.indexOf('e') > -1
                || val.indexOf('E') > -1 || "-0".equals(val);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONObject. Some information may be lost in this transformation because
     * JSON is a data format and XML is a document format. XML uses elements,
     * attributes, and content text, while JSON uses unordered collections of
     * name/value pairs and arrays of values. JSON does not does not like to
     * distinguish between elements and attributes. Sequences of similar
     * elements are represented as JSONArrays. Content text may be placed in a
     * "content" member. Comments, prologs, DTDs, and <pre>{@code 
     * &lt;[ [ ]]>}</pre>
     * are ignored.
     *
     * @param string
     *            The source string.
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(String string) throws JSONException {
        return toJSONObject(string, XMLParserConfiguration.ORIGINAL);
    }

    /**
     * Convert a well-formed (but not necessarily valid) XML into a
     * JSONObject. Some information may be lost in this transformation because
     * JSON is a data format and XML is a document format. XML uses elements,
     * attributes, and content text, while JSON uses unordered collections of
     * name/value pairs and arrays of values. JSON does not does not like to
     * distinguish between elements and attributes. Sequences of similar
     * elements are represented as JSONArrays. Content text may be placed in a
     * "content" member. Comments, prologs, DTDs, and <pre>{@code 
     * &lt;[ [ ]]>}</pre>
     * are ignored.
     *
     * @param reader The XML source reader.
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(Reader reader) throws JSONException {
        return toJSONObject(reader, XMLParserConfiguration.ORIGINAL);
    }

    /**
     * Convert a well-formed (but not necessarily valid) XML into a
     * JSONObject. Some information may be lost in this transformation because
     * JSON is a data format and XML is a document format. XML uses elements,
     * attributes, and content text, while JSON uses unordered collections of
     * name/value pairs and arrays of values. JSON does not does not like to
     * distinguish between elements and attributes. Sequences of similar
     * elements are represented as JSONArrays. Content text may be placed in a
     * "content" member. Comments, prologs, DTDs, and <pre>{@code
     * &lt;[ [ ]]>}</pre>
     * are ignored.
     *
     * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
     * numbers but will instead be the exact value as seen in the XML document.
     *
     * @param reader The XML source reader.
     * @param keepStrings If true, then values will not be coerced into boolean
     *  or numeric values and will instead be left as strings
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(Reader reader, boolean keepStrings) throws JSONException {
        if(keepStrings) {
            return toJSONObject(reader, XMLParserConfiguration.KEEP_STRINGS);
        }
        return toJSONObject(reader, XMLParserConfiguration.ORIGINAL);
    }

    /**
     * Convert a well-formed (but not necessarily valid) XML into a
     * JSONObject. Some information may be lost in this transformation because
     * JSON is a data format and XML is a document format. XML uses elements,
     * attributes, and content text, while JSON uses unordered collections of
     * name/value pairs and arrays of values. JSON does not does not like to
     * distinguish between elements and attributes. Sequences of similar
     * elements are represented as JSONArrays. Content text may be placed in a
     * "content" member. Comments, prologs, DTDs, and <pre>{@code
     * &lt;[ [ ]]>}</pre>
     * are ignored.
     *
     * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
     * numbers but will instead be the exact value as seen in the XML document.
     *
     * @param reader The XML source reader.
     * @param config Configuration options for the parser
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(Reader reader, XMLParserConfiguration config) throws JSONException {
        JSONObject jo = new JSONObject();
        XMLTokener x = new XMLTokener(reader);
        while (x.more()) {
            x.skipPast("<");
            if(x.more()) {
                parse(x, jo, null, config);
            }
        }
        return jo;
    }

    public static JSONObject toJSONObject(Reader reader, JSONPointer path, JSONObject replacement) throws JSONException{
        String[] keys = path.toString().split("/");
        if (keys[0] == ""){
            String[] temp = new String[keys.length - 1];
            for (int i = 0; i < temp.length; i++){
                temp[i] = keys[i+1];
            }
            keys = temp;
        }
        JSONObject jo = new JSONObject();
        XMLTokener x = new XMLTokener(reader);
        while (x.more()){
            x.skipPast("<");
            if (x.more()){
                parseWithReplace(x,jo,null,XMLParserConfiguration.ORIGINAL, keys, 0, replacement);
            }
        }
        return jo;
    }

    private static boolean parseWithReplace(XMLTokener x, JSONObject context, String name, XMLParserConfiguration config, String[] keys, int keyIndex,
                                         JSONObject replacement) throws JSONException{
        char c;
        int i;
        JSONObject jsonObject = null;
        String string;
        String tagName;
        Object token;
        XMLXsiTypeConverter<?> xmlXsiTypeConverter;

        // Test for and skip past these forms:
        // <!-- ... -->
        // <! ... >
        // <![ ... ]]>
        // <? ... ?>
        // Report errors for these forms:
        // <>
        // <=
        // <<
        token = x.nextToken();


        if (token == BANG) {
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
                } else if (token == LT) {
                    i += 1;
                } else if (token == GT) {
                    i -= 1;
                }
            } while (i > 0);
            return false;
        } else if (token == QUEST) {

            // <?
            x.skipPast("?>");
            return false;
        } else if (token == SLASH) {

            // Close tag </

            token = x.nextToken();
            if (name == null) {
                throw x.syntaxError("Mismatched close tag " + token);
            }
            if (!token.equals(name)) {
                throw x.syntaxError("Mismatched " + name + " and " + token);
            }
            if (x.nextToken() != GT) {
                throw x.syntaxError("Misshaped close tag");
            }
            return true;

        } else if (token instanceof Character) {
            throw x.syntaxError("Misshaped tag");

            // Open tag <

        } else {
            tagName = (String) token;
            token = null;
            jsonObject = new JSONObject();
            boolean nilAttributeFound = false;
            xmlXsiTypeConverter = null;
            boolean isArray;
            int iters = 0;
            int newKeyIndex = keyIndex;
            try{
                iters = Integer.parseInt(keys[keyIndex + 1]);
                isArray = true;
            } catch (NumberFormatException e){isArray = false;
            } catch (ArrayIndexOutOfBoundsException e) {
                isArray = false;
            }
            if (isArray){/*
                if (!tagName.equals(keys[keyIndex])){
                    iters++;
                }
                for (int skips = 0; skips < iters; skips++){
                    parseWithTag(x, context, tagName, config);
                    x.skipPast("<");
                    token = x.nextToken();
                    System.out.println(token);
                    tagName = (String) token;
                    if (!tagName.equals(keys[keyIndex])){
                        skips--;
                    }
                }
                if (keyIndex == keys.length - 2){
                    System.out.println("in isArray replacing w tagname: " + tagName);
                    context.accumulate(tagName, replacement);
                    skipSameLevel(x,0);
                    if (x.more()){
                        return parse(x,context,name,config);
                    }
                    else{
                        return true;
                    }
                } else{
                    newKeyIndex++;
                }
                newKeyIndex++;*/
                //there is actually no way to optimize JSONArray interactions
                //i legit worked on this for 72+ hours i can't figure it out
                /*
                JSONArray temp = new JSONArray();
                boolean parseResult = getArray(x, temp, tagName, config);
                Object tempchild;
                JSONPointer.Builder b = new JSONPointer.Builder();
                for (int k = keyIndex + 1; k < keys.length - 1; k++){
                    b.append(keys[k]);
                }
                tempchild = temp.query(b.build());
                if (tempchild instanceof JSONObject){
                    JSONObject child = (JSONObject) tempchild;
                    child.remove(keys[keys.length - 1]);
                    child.put(keys[keys.length - 1], replacement);
                }
                else if (tempchild instanceof JSONArray){
                    ((JSONArray)tempchild).put(Integer.parseInt(keys[keys.length - 1]), replacement);
                }
                context.accumulate(tagName, temp);
                System.out.println(temp.toString());
                System.out.println();
                return parseResult;*/
            }
            if (newKeyIndex == keys.length || (tagName.equals(keys[newKeyIndex]) && newKeyIndex == keys.length - 1)){
                context.accumulate(tagName, replacement);
                skipSameLevel(x,0);
                if (x.more()){
                    return parse(x,context, name, config);
                }
                else{
                    return true;
                }
            }
            for (;;) {
                if (token == null) {
                    token = x.nextToken();
                }
                // attribute = value
                if (token instanceof String) {
                    string = (String) token;
                    token = x.nextToken();
                    if (token == EQ) {
                        token = x.nextToken();
                        if (!(token instanceof String)) {
                            throw x.syntaxError("Missing value");
                        }
                        if (config.isConvertNilAttributeToNull()
                                && NULL_ATTR.equals(string)
                                && Boolean.parseBoolean((String) token)) {
                            nilAttributeFound = true;
                        } else if(config.getXsiTypeMap() != null && !config.getXsiTypeMap().isEmpty()
                                && TYPE_ATTR.equals(string)) {
                            xmlXsiTypeConverter = config.getXsiTypeMap().get(token);
                        } else if (!nilAttributeFound) {
                            jsonObject.accumulate(string,
                                    config.isKeepStrings()
                                            ? ((String) token)
                                            : stringToValue((String) token));
                        }
                        token = null;
                    } else {
                        jsonObject.accumulate(string, "");
                    }
                } else if (token == SLASH) {
                    // Empty tag <.../>
                    if (x.nextToken() != GT) {
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
                } else if (token == GT) {
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
                                            stringToValue(string, xmlXsiTypeConverter));
                                } else {
                                    jsonObject.accumulate(config.getcDataTagName(),
                                            config.isKeepStrings() ? string : stringToValue(string));
                                }
                            }
                        } else if (token == LT) {
                            // Nested element
                            if (keys[keyIndex].equals(tagName)){
                                if (parseWithReplace(x, jsonObject, tagName, config, keys, newKeyIndex + 1, replacement)){
                                    try{
                                        Integer.parseInt(keys[keyIndex + 2]);
                                        isArray = true;
                                    } catch (NumberFormatException e){isArray = false;
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        isArray = false;
                                    }
                                    if (isArray){
                                        JSONObject temp = jsonObject;
                                        Object tempchild;
                                        JSONPointer.Builder b = new JSONPointer.Builder();
                                        for (int k = keyIndex + 1; k < keys.length - 1; k++){
                                            b.append(keys[k]);
                                        }
                                        tempchild = temp.query(b.build());
                                        if (tempchild instanceof JSONObject){
                                            JSONObject child = (JSONObject) tempchild;
                                            child.put(keys[keys.length - 1], replacement);
                                        }
                                        else if (tempchild instanceof JSONArray){
                                            ((JSONArray)tempchild).put(Integer.parseInt(keys[keys.length - 1]), replacement);
                                        }
                                    }
                                    if (jsonObject.length() == 0){
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
                            else if (parse(x, jsonObject, tagName, config)) {
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
/*
    private static boolean getArray(XMLTokener x, JSONArray ja, String name, XMLParserConfiguration config){
        Object token = null;
        JSONObject jsonObject = new JSONObject();
        boolean nilAttributeFound = false;
        XMLXsiTypeConverter<?> xmlXsiTypeConverter = null;
        String string;
        for(;;){
            if (token == null) {
                token = x.nextToken();
            }
            // attribute = value
            if (token instanceof String) {
                string = (String) token;
                token = x.nextToken();
                if (token == EQ) {
                    token = x.nextToken();
                    if (!(token instanceof String)) {
                        throw x.syntaxError("Missing value");
                    }

                    if (config.isConvertNilAttributeToNull()
                            && NULL_ATTR.equals(string)
                            && Boolean.parseBoolean((String) token)) {
                        nilAttributeFound = true;
                    } else if(config.getXsiTypeMap() != null && !config.getXsiTypeMap().isEmpty()
                            && TYPE_ATTR.equals(string)) {
                        xmlXsiTypeConverter = config.getXsiTypeMap().get(token);
                    } else if (!nilAttributeFound) {
                        jsonObject.accumulate(string,
                                config.isKeepStrings()
                                        ? ((String) token)
                                        : stringToValue((String) token));
                    }
                    token = null;
                } else {
                    jsonObject.accumulate(string, "");
                }


            } else if (token == SLASH) {
                // Empty tag <.../>
                if (x.nextToken() != GT) {
                    throw x.syntaxError("Misshaped tag");
                }
                if (nilAttributeFound) {
                    ja.put(JSONObject.NULL);
                } else if (jsonObject.length() > 0) {
                    ja.put(jsonObject);
                } else {
                    ja.put("");
                }
                return true;

            } else if (token == GT) {
                // Content, between <...> and </...>
                for (;;) {
                    token = x.nextContent();
                    if (token == null) {
                        if (name != null) {
                            throw x.syntaxError("Unclosed tag " + name);
                        }
                        return false;
                    } else if (token instanceof String) {
                        string = (String) token;
                        if (string.length() > 0) {
                            if(xmlXsiTypeConverter != null) {
                                jsonObject.accumulate(config.getcDataTagName(),
                                        stringToValue(string, xmlXsiTypeConverter));
                            } else {
                                jsonObject.accumulate(config.getcDataTagName(),
                                        config.isKeepStrings() ? string : stringToValue(string));
                            }
                        }

                    } else if (token == LT) {
                        // Nested element
                        if (parse(x, jsonObject, name, config)) {
                            if (jsonObject.length() == 0) {
                                ja.put("");
                            } else if (jsonObject.length() == 1
                                    && jsonObject.opt(config.getcDataTagName()) != null) {
                                ja.put(jsonObject.opt(config.getcDataTagName()));
                            } else {
                                ja.put(jsonObject);
                            }
                            return false;
                        }
                    }
                }//for
            } else {
                throw x.syntaxError("Misshaped tag");
            }
        }
    }
*/

    /**
     * ADDED IN MILESTONE 2
     * Extracts a jsonobject in a certain path.
     *
     */
    public static JSONObject toJSONObject(Reader reader, JSONPointer path) throws JSONException {
        String[] keys = path.toString().split("/");
        if (keys[0] == ""){
            String[] temp = new String[keys.length - 1];
            for (int i = 0; i < temp.length; i++){
                temp[i] = keys[i+1];
            }
            keys = temp;
        }
        XMLTokener x = new XMLTokener(reader);
        JSONObject jo = new JSONObject();
        try{
            while(x.more()){
                x.skipPast("<");
                if (x.more()){
                    if (findObject(x, jo, keys,0, null, XMLParserConfiguration.ORIGINAL)){
                        break;
                    }
                }
            }
        } catch (JSONException j){
            jo = new JSONObject();
        }


        return jo;
    }

    private static boolean findObject(XMLTokener x, JSONObject context, String[] keys, int keyIndex, String name, XMLParserConfiguration config)
        throws JSONException{
        char c;
        int i;
        String tagName;
        Object token;

        token = x.nextToken();

        if (token == BANG){
            c = x.next();
            if (c == '-') {
                if (x.next() == '-') {
                    x.skipPast("-->");
                    return false;
                }
                x.back();
            } else if (c == '['){return false;}
            i = 1;
            do {
                token = x.nextMeta();
                if (token == null) {
                    throw x.syntaxError("Missing '>' after '<!'.");
                } else if (token == LT) {
                    i += 1;
                } else if (token == GT) {
                    i -= 1;
                }
            } while (i > 0);
            return false;
        } else if (token == QUEST){
            x.skipPast("?>");
            return false;
        } else if (token == SLASH){
            token = x.nextToken();
            if (name == null){
                throw x.syntaxError("Mismatched close tag " + token);
            }
            if (!token.equals(name)){
                throw x.syntaxError("Mismatched " + name + " and " + token);
            }
            if (x.nextToken() != GT){
                throw x.syntaxError("Misshaped close tag");
            }
            return false;
        } else if (token instanceof Character){
            throw x.syntaxError("Misshaped tag");
        } else { //open tag <
            tagName = (String) token;
            boolean isArray = false;
            try{
                Integer.parseInt(keys[keyIndex + 1]);
                isArray = true;
            } catch (NumberFormatException e){isArray = false;
            } catch (ArrayIndexOutOfBoundsException e) {
                if (keyIndex == keys.length){
                    parseWithTag(x,context,tagName,config);
                    return true;
                }
            }
            //if (isArray && Integer.parseInt(keys[keyIndex + 1]) > 0){
            if (isArray){
                int iters = Integer.parseInt(keys[keyIndex+1]);
                if (!tagName.equals(keys[keyIndex])){
                    iters++;
                }
                for (int skips = 0; skips < iters; skips++){
                    skipSameLevel(x,0);
                    token = x.nextToken();
                    String temp = (String) token;
                    if (!temp.equals(keys[keyIndex])){
                        skips--;
                    }
                }
                if (keyIndex == keys.length -2){
                    parseWithTag(x,context,tagName,config);
                    return true;
                }
            }
            int kIndex = keyIndex;
            if (isArray){
                kIndex+=2;
            }
            String string;
            boolean nilAttributeFound = false;
            XMLXsiTypeConverter<?> xmlXsiTypeConverter = null;
            token = null;
            if (kIndex >= keys.length || (tagName.equals(keys[kIndex]) && kIndex == keys.length - 1)){
                parseWithTag(x, context, tagName, config);
                return true;
            }
            else if (kIndex <= keys.length - 1){
                for (;;) {
                    if (token == null) {
                        token = x.nextToken();
                    }
                    // attribute = value
                    if (token instanceof String) {
                        string = (String) token;
                        token = x.nextToken();
                        if (token == EQ) {
                            token = x.nextToken();
                            if (!(token instanceof String)) {
                                throw x.syntaxError("Missing value");
                            }
                            if (config.isConvertNilAttributeToNull()
                                    && NULL_ATTR.equals(string)
                                    && Boolean.parseBoolean((String) token)) {
                                nilAttributeFound = true;
                            } else if (config.getXsiTypeMap() != null && !config.getXsiTypeMap().isEmpty()
                                    && TYPE_ATTR.equals(string)) {
                                xmlXsiTypeConverter = config.getXsiTypeMap().get(token);
                            } else if (!nilAttributeFound) {
                                if (string.equals(keys[kIndex])){
                                    context.accumulate(string,
                                            config.isKeepStrings()
                                                    ? ((String) token)
                                                    : stringToValue((String) token));
                                    return true;
                                }
                            }
                            token = null;
                        } else {
                            if (string.equals(keys[kIndex])){
                                context.accumulate(string, "");
                                return true;
                            }
                            token = null;
                        }


                    } else if (token == SLASH) {
                        // Empty tag <.../>
                        if (x.nextToken() != GT) {
                            throw x.syntaxError("Misshaped tag");
                        }
                        return false;
                    } else if (token == GT) {
                        // Content, between <...> and </...>
                        for (; ; ) {
                            token = x.nextContent();
                            if (token == null) {
                                if (tagName != null) {
                                    throw x.syntaxError("Unclosed tag " + tagName);
                                }
                                return false;
                            } else if (token instanceof String) {
                                string = (String) token;
                            } else if (token == LT) {
                                if (keys[kIndex].equals(tagName) && kIndex < keys.length - 1){
                                    return findObject(x, context, keys, kIndex + 1, null, config);
                                }
                                else if (kIndex >= keys.length || (tagName.equals(keys[kIndex]) && kIndex  == keys.length - 1)){
                                    parseWithTag(x, context, tagName, config);
                                    return true;
                                }
                                else{
                                    skipSameLevel(x,0);
                                    return findObject(x,context,keys,kIndex,tagName,config);
                                }
                            }
                        }//for
                    } else {
                        throw x.syntaxError("Misshaped tag");
                    }
                } // for
            } else{
                skipSameLevel(x, 0);
                return findObject(x,context,keys,kIndex,tagName,config);
            }
        }
    }

    private static boolean parseWithTag( XMLTokener x, JSONObject context, String tag, XMLParserConfiguration config){
        Object token = null;
        JSONObject jsonObject = new JSONObject();
        boolean nilAttributeFound = false;
        XMLXsiTypeConverter<?> xmlXsiTypeConverter = null;
        String string;
        String tagName = tag;
        for(;;){
            if (token == null) {
                token = x.nextToken();
            }
            // attribute = value
            if (token instanceof String) {
                string = (String) token;
                token = x.nextToken();
                if (token == EQ) {
                    token = x.nextToken();
                    if (!(token instanceof String)) {
                        throw x.syntaxError("Missing value");
                    }

                    if (config.isConvertNilAttributeToNull()
                            && NULL_ATTR.equals(string)
                            && Boolean.parseBoolean((String) token)) {
                        nilAttributeFound = true;
                    } else if(config.getXsiTypeMap() != null && !config.getXsiTypeMap().isEmpty()
                            && TYPE_ATTR.equals(string)) {
                        xmlXsiTypeConverter = config.getXsiTypeMap().get(token);
                    } else if (!nilAttributeFound) {
                        jsonObject.accumulate(string,
                                config.isKeepStrings()
                                        ? ((String) token)
                                        : stringToValue((String) token));
                    }
                    token = null;
                } else {
                    jsonObject.accumulate(string, "");
                }


            } else if (token == SLASH) {
                // Empty tag <.../>
                if (x.nextToken() != GT) {
                    throw x.syntaxError("Misshaped tag");
                }
                if (nilAttributeFound) {
                    context.accumulate(tagName, JSONObject.NULL);
                } else if (jsonObject.length() > 0) {
                    context.accumulate(tagName, jsonObject);
                } else {
                    context.accumulate(tagName, "");
                }
                return true;

            } else if (token == GT) {
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
                                        stringToValue(string, xmlXsiTypeConverter));
                            } else {
                                jsonObject.accumulate(config.getcDataTagName(),
                                        config.isKeepStrings() ? string : stringToValue(string));
                            }
                        }

                    } else if (token == LT) {
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
                throw x.syntaxError("Misshaped tag");
            }
        }
    }

    private static void skipSameLevel(XMLTokener x,int offset){
        boolean foundInteresting = false;
        Object token;
        while (!foundInteresting){
            token = x.nextToken();
            if (token == SLASH){
                x.skipPast("<");
                return;
            }
            if (token == GT){
                foundInteresting = true;
            }
        }
        int level = 1 + offset;
        while (level > 0){
            x.skipPast("<");
            token = x.nextToken();
            if (token == BANG){
                char c = x.next();
                if (c == '-') {
                    if (x.next() == '-') {
                        x.skipPast("-->");
                    }
                    x.back();
                } else if (c == '['){x.skipPast("]>");}
                else{
                    int i = 1;
                    do {
                        token = x.nextMeta();
                        if (token == null) {
                            throw x.syntaxError("Missing '>' after '<!'.");
                        } else if (token == LT) {
                            i += 1;
                        } else if (token == GT) {
                            i -= 1;
                        }
                    } while (i > 0);
                }
            } else if (token == QUEST){
                x.skipPast("?>");
            } else if (token == SLASH){
                level--;
            } else if (token instanceof Character) {
                throw x.syntaxError("Misshaped tag");
            }else{
                boolean foundInteresting2 = false;
                while (!foundInteresting2){
                    token = x.nextToken();
                    if (token == SLASH){
                        foundInteresting2 = true;
                    }
                    if (token == GT){
                        foundInteresting2 = true;
                        level++;
                    }
                }
            }
        }
        x.skipPast("<");
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONObject. Some information may be lost in this transformation because
     * JSON is a data format and XML is a document format. XML uses elements,
     * attributes, and content text, while JSON uses unordered collections of
     * name/value pairs and arrays of values. JSON does not does not like to
     * distinguish between elements and attributes. Sequences of similar
     * elements are represented as JSONArrays. Content text may be placed in a
     * "content" member. Comments, prologs, DTDs, and <pre>{@code 
     * &lt;[ [ ]]>}</pre>
     * are ignored.
     *
     * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
     * numbers but will instead be the exact value as seen in the XML document.
     *
     * @param string
     *            The source string.
     * @param keepStrings If true, then values will not be coerced into boolean
     *  or numeric values and will instead be left as strings
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(String string, boolean keepStrings) throws JSONException {
        return toJSONObject(new StringReader(string), keepStrings);
    }

    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONObject. Some information may be lost in this transformation because
     * JSON is a data format and XML is a document format. XML uses elements,
     * attributes, and content text, while JSON uses unordered collections of
     * name/value pairs and arrays of values. JSON does not does not like to
     * distinguish between elements and attributes. Sequences of similar
     * elements are represented as JSONArrays. Content text may be placed in a
     * "content" member. Comments, prologs, DTDs, and <pre>{@code 
     * &lt;[ [ ]]>}</pre>
     * are ignored.
     *
     * All values are converted as strings, for 1, 01, 29.0 will not be coerced to
     * numbers but will instead be the exact value as seen in the XML document.
     *
     * @param string
     *            The source string.
     * @param config Configuration options for the parser.
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException Thrown if there is an errors while parsing the string
     */
    public static JSONObject toJSONObject(String string, XMLParserConfiguration config) throws JSONException {
        return toJSONObject(new StringReader(string), config);
    }

    /**
     * Convert a JSONObject into a well-formed, element-normal XML string.
     *
     * @param object
     *            A JSONObject.
     * @return A string.
     * @throws JSONException Thrown if there is an error parsing the string
     */
    public static String toString(Object object) throws JSONException {
        return toString(object, null, XMLParserConfiguration.ORIGINAL);
    }

    /**
     * Convert a JSONObject into a well-formed, element-normal XML string.
     *
     * @param object
     *            A JSONObject.
     * @param tagName
     *            The optional name of the enclosing tag.
     * @return A string.
     * @throws JSONException Thrown if there is an error parsing the string
     */
    public static String toString(final Object object, final String tagName) {
        return toString(object, tagName, XMLParserConfiguration.ORIGINAL);
    }

    /**
     * Convert a JSONObject into a well-formed, element-normal XML string.
     *
     * @param object
     *            A JSONObject.
     * @param tagName
     *            The optional name of the enclosing tag.
     * @param config
     *            Configuration that can control output to XML.
     * @return A string.
     * @throws JSONException Thrown if there is an error parsing the string
     */
    public static String toString(final Object object, final String tagName, final XMLParserConfiguration config)
            throws JSONException {
        StringBuilder sb = new StringBuilder();
        JSONArray ja;
        JSONObject jo;
        String string;

        if (object instanceof JSONObject) {

            // Emit <tagName>
            if (tagName != null) {
                sb.append('<');
                sb.append(tagName);
                sb.append('>');
            }

            // Loop thru the keys.
            // don't use the new entrySet accessor to maintain Android Support
            jo = (JSONObject) object;
            for (final String key : jo.keySet()) {
                Object value = jo.opt(key);
                if (value == null) {
                    value = "";
                } else if (value.getClass().isArray()) {
                    value = new JSONArray(value);
                }

                // Emit content in body
                if (key.equals(config.getcDataTagName())) {
                    if (value instanceof JSONArray) {
                        ja = (JSONArray) value;
                        int jaLength = ja.length();
                        // don't use the new iterator API to maintain support for Android
						for (int i = 0; i < jaLength; i++) {
                            if (i > 0) {
                                sb.append('\n');
                            }
                            Object val = ja.opt(i);
                            sb.append(escape(val.toString()));
                        }
                    } else {
                        sb.append(escape(value.toString()));
                    }

                    // Emit an array of similar keys

                } else if (value instanceof JSONArray) {
                    ja = (JSONArray) value;
                    int jaLength = ja.length();
                    // don't use the new iterator API to maintain support for Android
					for (int i = 0; i < jaLength; i++) {
                        Object val = ja.opt(i);
                        if (val instanceof JSONArray) {
                            sb.append('<');
                            sb.append(key);
                            sb.append('>');
                            sb.append(toString(val, null, config));
                            sb.append("</");
                            sb.append(key);
                            sb.append('>');
                        } else {
                            sb.append(toString(val, key, config));
                        }
                    }
                } else if ("".equals(value)) {
                    sb.append('<');
                    sb.append(key);
                    sb.append("/>");

                    // Emit a new tag <k>

                } else {
                    sb.append(toString(value, key, config));
                }
            }
            if (tagName != null) {

                // Emit the </tagName> close tag
                sb.append("</");
                sb.append(tagName);
                sb.append('>');
            }
            return sb.toString();

        }

        if (object != null && (object instanceof JSONArray ||  object.getClass().isArray())) {
            if(object.getClass().isArray()) {
                ja = new JSONArray(object);
            } else {
                ja = (JSONArray) object;
            }
            int jaLength = ja.length();
            // don't use the new iterator API to maintain support for Android
			for (int i = 0; i < jaLength; i++) {
                Object val = ja.opt(i);
                // XML does not have good support for arrays. If an array
                // appears in a place where XML is lacking, synthesize an
                // <array> element.
                sb.append(toString(val, tagName == null ? "array" : tagName, config));
            }
            return sb.toString();
        }

        string = (object == null) ? "null" : escape(object.toString());
        return (tagName == null) ? "\"" + string + "\""
                : (string.length() == 0) ? "<" + tagName + "/>" : "<" + tagName
                        + ">" + string + "</" + tagName + ">";

    }
}
