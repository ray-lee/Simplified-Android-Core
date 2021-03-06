package org.nypl.simplified.tests.opds;

import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nypl.simplified.opds.core.OPDSXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class OPDSXMLTest {

  private static Document parseStream(
    final InputStream s)
    throws ParserConfigurationException,
    SAXException,
    IOException {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();
    final Document d = NullCheck.notNull(db.parse(s));
    return d;
  }

  private static InputStream getResource(
    final String name)
    throws Exception {

    final String path = "/org/nypl/simplified/tests/opds/" + name;
    final URL url = OPDSFeedEntryParserTest.class.getResource(path);
    if (url == null) {
      throw new FileNotFoundException(path);
    }
    return url.openStream();
  }

  @Test
  public void testNamespaces_0()
    throws Exception {
    final Document d =
      OPDSXMLTest.parseStream(OPDSXMLTest.getResource("namespaces-0.xml"));
    final Element r = d.getDocumentElement();
    Assertions.assertEquals("feed", r.getNodeName());

    final NodeList p = r.getElementsByTagName("dcterms:publisher");
    Assertions.assertEquals(1, p.getLength());
    final Element pub_actual = (Element) p.item(0);

    final Some<String> pub_ns =
      (Some<String>) OPDSXML.getNodeNamespace(pub_actual);
    Assertions.assertEquals(pub_ns.get(), "http://purl.org/dc/terms/");

    Assertions.assertTrue(OPDSXML.nodeHasName(
      pub_actual,
      URI.create("http://purl.org/dc/terms/"),
      "publisher"));

    OPDSXML.getFirstChildElementWithName(
      r,
      URI.create("http://purl.org/dc/terms/"),
      "publisher");
  }
}
