package com.lencode.paper.importer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lencode.paper.importer.dto.ArxivPaperEntry;

class ArxivAtomParserTest {

    private final ArxivAtomParser parser = new ArxivAtomParser();

    @Test
    void parsesArxivAtomEntryIntoPaperMetadata() {
        String atom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:arxiv=\"http://arxiv.org/schemas/atom\">"
                + "<entry>"
                + "<id>http://arxiv.org/abs/2401.00001v2</id>"
                + "<title>  Neural  Search  Systems </title>"
                + "<summary> A paper about neural retrieval. </summary>"
                + "<published>2026-05-20T12:30:00Z</published>"
                + "<author><name>Alice</name></author><author><name>Bob</name></author>"
                + "<category term=\"cs.CL\"/><category term=\"cs.IR\"/>"
                + "<link href=\"http://arxiv.org/abs/2401.00001v2\" rel=\"alternate\" type=\"text/html\"/>"
                + "<link title=\"pdf\" href=\"http://arxiv.org/pdf/2401.00001v2\" rel=\"related\" type=\"application/pdf\"/>"
                + "<arxiv:doi>10.1000/example</arxiv:doi>"
                + "</entry>"
                + "</feed>";

        List<ArxivPaperEntry> entries = parser.parse(atom);

        assertThat(entries).hasSize(1);
        ArxivPaperEntry entry = entries.get(0);
        assertThat(entry.getSourcePaperId()).isEqualTo("2401.00001");
        assertThat(entry.getTitle()).isEqualTo("Neural Search Systems");
        assertThat(entry.getAuthors()).containsExactly("Alice", "Bob");
        assertThat(entry.getSummary()).isEqualTo("A paper about neural retrieval.");
        assertThat(entry.getPublishedAt()).isEqualTo(LocalDateTime.parse("2026-05-20T12:30:00"));
        assertThat(entry.getSourceUrl()).isEqualTo("http://arxiv.org/abs/2401.00001v2");
        assertThat(entry.getDownloadUrl()).isEqualTo("http://arxiv.org/pdf/2401.00001v2");
        assertThat(entry.getDoi()).isEqualTo("10.1000/example");
        assertThat(entry.getCategories()).containsExactly("cs.CL", "cs.IR");
    }
}
