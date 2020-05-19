package org.olat.core.util.openxml;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.core.commons.services.image.Size;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class OpenXMLUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void convertPixelToEMUs() {
        int result = OpenXMLUtils.convertPixelToEMUs(1080, 92, 2);
        assertEquals(result, 21468521);
    }

    @Test
    public void convertPixelToEMUs_0() {
        int result = OpenXMLUtils.convertPixelToEMUs(0, 92, 2);
        assertEquals(result, 0);
    }

    @Test
    public void convertPixelToEMUs_Size() {
        Size img = new Size(1080, 920, true);
        OpenXMLSize result = OpenXMLUtils.convertPixelToEMUs(img, 76, 5);
        assertEquals(result.getWidthPx(), 1080);
        assertEquals(result.getHeightPx(), 920);
        assertEquals(result.getHeightEmu(), 1533333);
        assertEquals(result.getWidthEmu(), 1800000);
        assertEquals(result.getResizeRatio(), 0.138524354, 0.00000001);
    }

    @Test(expected = org.xml.sax.SAXParseException.class)
    public void getDocumentBuilder_XXE() throws Exception {
        DocumentBuilder db = OpenXMLUtils.getDocumentBuilder(true, true, false);
        InputStream is = getClass().getResourceAsStream("malicious.xml");
        Document doc = db.parse(is);
        String result = xml_to_string(doc);
        assertFalse(result, result.contains("root"));
    }

    @Test
    public void getSpanAttribute() throws Exception {
        DocumentBuilder db = OpenXMLUtils.getDocumentBuilder(true, true, false);
        InputStream is = getClass().getResourceAsStream("malicious.xml");
        Document doc = db.parse(is);
        // Attributes attr = new Attributes();
        // OpenXMLUtils.getSpanAttribute("abc", );
    }

    @Test
    public void contains() {
    }

    @Test
    public void createStreamWriter() {
    }

    @Test
    public void createDocument() {
    }

    @Test
    public void testCreateDocument() {
    }

    @Test
    public void testCreateDocument1() {
    }

    @Test
    public void writeTo() {
    }

    // --- Helper methods --- //

    private static String xml_to_string(Document document) throws Exception{
        StringWriter writer = new StringWriter();
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        Source source = new DOMSource(document);
        transformer.transform(source, new StreamResult(writer));
        writer.flush();
        return writer.toString();
    }
}