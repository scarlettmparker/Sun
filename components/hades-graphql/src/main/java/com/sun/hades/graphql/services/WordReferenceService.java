package com.sun.hades.graphql.services;

import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordEntry;
import com.sun.hades.codegen.types.WordScope;
import com.sun.hades.codegen.types.WordTranslation;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Scrapes WordReference dictionary pages into GraphQL word types.
 */
@Component
public class WordReferenceService {

  private static final String WORD_URL = "https://www.wordreference.com/gren/%s";
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
          + " (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
  private static final int DEFAULT_ENTRY_LIMIT = 2;
  private static final String MAIN_TABLE_ID = "regular";
  private static final String REVERSE_TABLE_ID = "othersideregular";
  private static final String COMPOUND_TABLE_ID = "compounds";
  private static final String REVERSE_COMPOUND_TABLE_ID = "othersidecompound";

  private final RestClient restClient;

  public WordReferenceService() {
    this.restClient = RestClient.builder()
        .baseUrl("https://www.wordreference.com")
        .defaultHeader("User-Agent", USER_AGENT)
        .defaultHeader("Cookie", "nginx_wr_human=1")
        .build();
  }

  /**
   * Defines a word, mapping the requested scopes onto the scraped sections.
   *
   * @param word the headword to look up
   * @param scope the parts of the page to include
   * @return the word, or null when the entry does not exist
   */
  @Cacheable(value = "defineWord", key = "#word.toLowerCase() + ':' + #scope")
  public Word defineWord(String word, List<WordScope> scope) {
    Document doc = fetch(word);
    return doc == null ? null : map(doc, word, scope);
  }

  /**
   * Maps a fetched page into a word honoring the scopes.
   *
   * @param html the page HTML
   * @param word the headword
   * @param scope the parts of the page to include
   * @return the word, or null when the page has no dictionary content
   */
  Word parseWord(String html, String word, List<WordScope> scope) {
    return map(Jsoup.parse(html), word, scope);
  }

  /**
   * Maps a parsed page into a word honoring the scopes.
   *
   * @param doc the parsed page
   * @param word the headword
   * @param scope the parts of the page to include
   * @return the word, or null when the page has no dictionary content
   */
  private static Word map(Document doc, String word, List<WordScope> scope) {
    if (doc.selectFirst("table.WRD") == null) {
      return null;
    }
    Set<WordScope> scopes = scope == null || scope.isEmpty() ? Set.of() : Set.copyOf(scope);
    boolean allTranslations = scopes.contains(WordScope.ALL_TRANSLATIONS);
    boolean includeExamples = scopes.contains(WordScope.EXAMPLES);
    boolean includeCompounds = scopes.contains(WordScope.COMPOUNDS);
    boolean includeRelated = scopes.contains(WordScope.RELATED_WORDS);

    int limit = allTranslations ? -1 : DEFAULT_ENTRY_LIMIT;
    List<WordEntry> entries = parseTable(section(doc, MAIN_TABLE_ID), limit, includeExamples);
    if (entries.isEmpty()) {
      entries = parseTable(section(doc, REVERSE_TABLE_ID), limit, includeExamples);
    }
    List<WordEntry> compounds = List.of();
    if (includeCompounds) {
      compounds = parseTable(section(doc, COMPOUND_TABLE_ID), -1, includeExamples);
      if (compounds.isEmpty()) {
        compounds = parseTable(section(doc, REVERSE_COMPOUND_TABLE_ID), -1, includeExamples);
      }
    }
    return Word.newBuilder()
        .id(word)
        .term(word)
        .entries(entries)
        .compounds(compounds)
        .relatedWords(includeRelated ? relatedWords(doc) : List.of())
        .sourceUrl(String.format(WORD_URL, encode(word)))
        .build();
  }

  /**
   * Fetches and parses a word page, or null when the entry does not exist.
   *
   * @param word the headword
   * @return the parsed document, or null
   */
  private Document fetch(String word) {
    try {
      String html = restClient.get()
          .uri("/gren/{enc}", encode(word))
          .retrieve()
          .body(String.class);
      return html == null ? null : Jsoup.parse(html);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Locates a dictionary table by the id on its section header cell.
   *
   * @param doc the parsed page
   * @param id the section cell id
   * @return the table, or null
   */
  private static Element section(Document doc, String id) {
    Element header = doc.selectFirst("td#" + id);
    return header == null ? null : header.closest("table");
  }

  /**
   * Parses the entries of a dictionary table.
   *
   * @param table the table
   * @param limit the entry limit, or -1 for all
   * @param includeExamples whether to keep example rows
   * @return the parsed entries
   */
  private static List<WordEntry> parseTable(Element table, int limit, boolean includeExamples) {
    List<WordEntry> entries = new ArrayList<>();
    if (table == null) {
      return entries;
    }
    EntryBuilder current = null;
    for (Element row : table.select("tr")) {
      Element frWrd = row.selectFirst("td.FrWrd");
      if (frWrd != null && row.hasAttr("id")) {
        if (current != null) {
          entries.add(current.build());
          current = null;
          if (limit >= 0 && entries.size() >= limit) {
            break;
          }
        }
        current = new EntryBuilder(row.id(), headword(frWrd), pos(frWrd));
        parseEntryRow(current, row);
      } else if (current != null) {
        parseContinuationRow(current, row, includeExamples);
      }
    }
    if (current != null) {
      entries.add(current.build());
    }
    return entries;
  }

  /**
   * Parses an entry's first row: base sense, usage notes, and first translation.
   *
   * @param entry the entry being built
   * @param row the entry row
   */
  private static void parseEntryRow(EntryBuilder entry, Element row) {
    List<Element> cells = row.children();
    if (cells.size() >= 2) {
      Element middle = cells.get(1);
      entry.sense = baseSense(middle);
      entry.translations.add(translation(middle));
    }
  }

  /**
   * Parses a continuation row: translation, example, or note.
   *
   * @param entry the entry being built
   * @param row the continuation row
   * @param includeExamples whether to keep example rows
   */
  private static void parseContinuationRow(EntryBuilder entry, Element row, boolean includeExamples) {
    Element note = row.selectFirst("td.notePubl");
    if (note != null) {
      entry.note = noteText(note);
      return;
    }
    Element example = row.selectFirst("td.FrEx, td.ToEx");
    if (example != null) {
      if (includeExamples) {
        entry.examples.add(example.text().trim());
      }
      return;
    }
    Element toWrd = row.selectFirst("td.ToWrd");
    if (toWrd != null) {
      List<Element> cells = row.children();
      Element middle = cells.size() >= 2 ? cells.get(1) : toWrd;
      entry.translations.add(translation(middle));
    }
  }

  /**
   * Builds a translation from its sense cell and the ToWrd cell in the same row.
   *
   * @param middle the sense cell
   * @return the translation
   */
  private static WordTranslation translation(Element middle) {
    Element toWrd = middle.parent().selectFirst("td.ToWrd");
    String term = toWrd.text();
    Element pos = toWrd.selectFirst("em.POS2");
    String wordType = pos == null ? null : pos.text();
    if (wordType != null) {
      term = term.replace(wordType, "").trim();
    }
    return WordTranslation.newBuilder()
        .term(term)
        .wordType(wordType)
        .usageNotes(usageNotes(middle))
        .build();
  }

  /**
   * Extracts the base sense text from a sense cell, minus usage notes.
   *
   * @param middle the sense cell
   * @return the base sense, or null
   */
  private static String baseSense(Element middle) {
    String sense = middle.ownText().trim().replaceAll("^\\s*\\(|\\)\\s*$", "");
    return sense.isEmpty() ? null : sense;
  }

  /**
   * Extracts the usage notes from a sense cell.
   *
   * @param middle the sense cell
   * @return the usage notes
   */
  private static List<String> usageNotes(Element middle) {
    List<String> notes = new ArrayList<>();
    for (Element note : middle.select("span.dsense")) {
      String text = note.text().trim().replaceAll("^\\s*\\(|\\)\\s*$", "");
      if (!text.isEmpty()) {
        notes.add(text);
      }
    }
    return notes;
  }

  /**
   * Returns the headword with variant line breaks joined as " / ".
   *
   * @param frWrd the headword cell
   * @return the headword
   */
  private static String headword(Element frWrd) {
    Element strong = frWrd.selectFirst("strong");
    if (strong == null) {
      return "";
    }
    return strong.html()
        .replaceAll("(?i)<br\\s*/?>", " / ")
        .replaceAll("<[^>]+>", "")
        .trim();
  }

  /**
   * Returns the part-of-speech abbreviation from a cell.
   *
   * @param cell the cell
   * @return the abbreviation, or null
   */
  private static String pos(Element cell) {
    Element pos = cell.selectFirst("em.POS2");
    return pos == null ? null : pos.text();
  }

  /**
   * Strips the "Σχόλιο" label from a note row.
   *
   * @param note the note cell
   * @return the note text
   */
  private static String noteText(Element note) {
    return note.text().trim().replaceFirst("^\\s*Σχόλιο\\s*:\\s*", "");
  }

  /**
   * Collects the See-Also sidebar links as shallow related words.
   *
   * @param doc the parsed page
   * @return the related words
   */
  private static List<Word> relatedWords(Document doc) {
    Element title = doc.selectFirst("li.title1[title='See Also:']");
    Element list = title == null ? null : title.parent().nextElementSibling();
    if (list == null) {
      return List.of();
    }
    List<Word> related = new ArrayList<>();
    for (Element link : list.select("a")) {
      String href = link.attr("href");
      String term = link.text().trim();
      related.add(Word.newBuilder()
          .id(term)
          .term(term)
          .entries(List.of())
          .compounds(List.of())
          .relatedWords(List.of())
          .sourceUrl("https://www.wordreference.com" + href)
          .build());
    }
    return related;
  }

  /**
   * URL-encodes a word for a WordReference path.
   *
   * @param word the headword
   * @return the encoded path segment
   */
  private static String encode(String word) {
    return URLEncoder.encode(word, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * Accumulates a WordEntry while its continuation rows are parsed.
   */
  private static final class EntryBuilder {

    private final String id;
    private final String term;
    private final String wordType;
    private final List<WordTranslation> translations = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private String sense;
    private String note;

    private EntryBuilder(String id, String term, String wordType) {
      this.id = id;
      this.term = term;
      this.wordType = wordType == null ? "" : wordType;
    }

    private WordEntry build() {
      return WordEntry.newBuilder()
          .id(id)
          .term(term)
          .wordType(wordType)
          .sense(sense)
          .translations(translations)
          .examples(examples)
          .note(note)
          .build();
    }
  }
}
