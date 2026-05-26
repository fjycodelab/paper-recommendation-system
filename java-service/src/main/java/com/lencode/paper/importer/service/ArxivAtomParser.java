package com.lencode.paper.importer.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.lencode.paper.common.exception.BadRequestException;
import com.lencode.paper.importer.dto.ArxivPaperEntry;

@Component
public class ArxivAtomParser {

    private static final Pattern VERSION_SUFFIX = Pattern.compile("v\\d+$");

    public List<ArxivPaperEntry> parse(String atomXml) {
        if (atomXml == null || atomXml.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Document document = parseDocument(atomXml);
        NodeList entries = document.getElementsByTagNameNS("*", "entry");
        List<ArxivPaperEntry> results = new ArrayList<>();
        for (int i = 0; i < entries.getLength(); i++) {
            results.add(parseEntry((Element) entries.item(i)));
        }
        return results;
    }

    private ArxivPaperEntry parseEntry(Element entry) {
        String sourceUrl = normalize(text(entry, "id"));
        String arxivId = extractArxivId(sourceUrl);
        List<String> categories = categories(entry);
        return new ArxivPaperEntry(
                arxivId,
                normalize(text(entry, "title")),
                authors(entry),
                normalize(text(entry, "summary")),
                parseDate(text(entry, "published")),
                sourceUrl,
                pdfUrl(entry),
                normalize(text(entry, "doi")),
                categories
        );
    }

    private static Document parseDocument(String atomXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            disableExternalEntities(factory);
            return factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(atomXml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BadRequestException("arXiv Atom 解析失败");
        }
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) throws ParserConfigurationException {
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
    }

    private static List<String> authors(Element entry) {
        NodeList authorNodes = entry.getElementsByTagNameNS("*", "author");
        List<String> authors = new ArrayList<>();
        for (int i = 0; i < authorNodes.getLength(); i++) {
            String name = normalize(text((Element) authorNodes.item(i), "name"));
            if (name != null) {
                authors.add(name);
            }
        }
        return authors;
    }

    private static List<String> categories(Element entry) {
        NodeList nodes = entry.getElementsByTagNameNS("*", "category");
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String term = normalize(((Element) nodes.item(i)).getAttribute("term"));
            if (term != null && !categories.contains(term)) {
                categories.add(term);
            }
        }
        return categories;
    }

    private static String pdfUrl(Element entry) {
        NodeList links = entry.getElementsByTagNameNS("*", "link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String title = link.getAttribute("title");
            String type = link.getAttribute("type");
            if ("pdf".equalsIgnoreCase(title) || "application/pdf".equalsIgnoreCase(type)) {
                return normalize(link.getAttribute("href"));
            }
        }
        return null;
    }

    private static String text(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node == null ? null : node.getTextContent();
    }

    private static String extractArxivId(String sourceUrl) {
        if (sourceUrl == null) {
            return null;
        }
        int slash = sourceUrl.lastIndexOf('/');
        String value = slash >= 0 ? sourceUrl.substring(slash + 1) : sourceUrl;
        return VERSION_SUFFIX.matcher(value).replaceFirst("");
    }

    private static LocalDateTime parseDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
