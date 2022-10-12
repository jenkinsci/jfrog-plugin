package io.jenkins.plugins.jfrog.artifactoryclient;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author yahavi
 **/
public class Utils {

    /**
     * Create the request body for the set-statistics request.
     *
     * @param downloadCount    - Total number of downloads
     * @param lastDownloaded   - Time passed in nanoseconds from the Epoch since the last download
     * @param lastDownloadedBy - The last user that downloaded the Artifact
     * @return the request body for the set-statistics request.
     * @throws ParserConfigurationException in case of unexpected error during the creation of the request body.
     * @throws TransformerException         in case of unexpected error during the creation of the request body.
     * @throws IOException                  in case of any I/O error.
     */
    static String createStatsXml(long downloadCount, long lastDownloaded, String lastDownloadedBy) throws ParserConfigurationException, TransformerException, IOException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement = doc.createElement("artifactory.stats");
        doc.appendChild(rootElement);

        addXmlElement(doc, rootElement, "downloadCount", String.valueOf(downloadCount));
        addXmlElement(doc, rootElement, "lastDownloaded", String.valueOf(lastDownloaded));
        addXmlElement(doc, rootElement, "lastDownloadedBy", lastDownloadedBy);

        try (StringWriter writer = new StringWriter()) {
            StreamResult result = new StreamResult(writer);
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), result);
            return writer.getBuffer().toString();
        }
    }

    private static void addXmlElement(Document doc, Element rootElement, String name, String data) {
        Element lastDownloadedByElement = doc.createElement(name);
        lastDownloadedByElement.appendChild(doc.createTextNode(data));
        rootElement.appendChild(lastDownloadedByElement);
    }

    /**
     * Return true if a string is null or empty.
     *
     * @param str - The string
     * @return true if a string is null or empty
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Encode URL.
     *
     * @param url - The URL to encode
     * @return the encoded URL.
     */
    public static String encodeUrl(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }
}
