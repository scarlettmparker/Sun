package com.sun.hades.graphql.services;

import com.sun.base.cache.CaffeineSpec;
import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordDictionary;
import com.sun.hades.codegen.types.WordScope;
import com.sun.hades.graphql.domain.WordDefinition;
import com.sun.hades.graphql.domain.WordDefinitionEntry;
import com.sun.hades.graphql.domain.WordDefinitionTranslation;
import com.sun.hades.graphql.mappers.WordMapper;
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
  private static final String ENGLISH_URL = "https://www.wordreference.com/definition/%s";
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
          + " (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
  private static final int DEFAULT_ENTRY_LIMIT = 2;
  private static final String MAIN_TABLE_ID = "regular";
  private static final String REVERSE_TABLE_ID = "othersideregular";
  private static final String COMPOUND_TABLE_ID = "compounds";
  private static final String REVERSE_COMPOUND_TABLE_ID = "othersidecompound";

  private final RestClient restClient;
  private final WordMapper wordMapper;

  public WordReferenceService(WordMapper wordMapper) {
    this.wordMapper = wordMapper;
    this.restClient = RestClient.builder()
        .baseUrl("https://www.wordreference.com")
        .defaultHeader("User-Agent", USER_AGENT)
        .defaultHeader("Cookie", "nginx_wr_human=1")
        .build();
  }

  public WordReferenceService() {
    this(new WordMapper());
  }

  /**
   * Defines a word, mapping the requested scopes onto the scraped sections.
   *
   * @param word the headword to look up
   * @param scope the parts of the page to include
   * @param dictionary the dictionary to query, defaulting to Greek-English
   * @return the word, or null when the entry does not exist
   */
  @Cacheable(value = "defineWord", key = "#a0 == null ? '' : #a0.toString().toLowerCase() + ':' + #a1 + ':' + #a2", unless = "#result == null")
  @CaffeineSpec(expireAfterWrite = "24h", maximumSize = 2000)
  public Word defineWord(String word, List<WordScope> scope, WordDictionary dictionary) {
    WordDictionary dict = dictionary == null ? WordDictionary.GREEK_ENGLISH : dictionary;
    Document doc = fetch(word, dict);
    if (doc == null) {
      return null;
    }
    WordDefinition definition = parseDocument(doc, word, scope, dict);
    return wordMapper.map(definition);
  }

  /**
   * Defines a word from the Greek-English dictionary.
   *
   * @param word the headword to look up
   * @param scope the parts of the page to include
   * @return the word, or null when the entry does not exist
   */
  public Word defineWord(String word, List<WordScope> scope) {
    return defineWord(word, scope, WordDictionary.GREEK_ENGLISH);
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
    return parseWord(html, word, scope, WordDictionary.GREEK_ENGLISH);
  }

  /**
   * Maps a fetched page into a word for the given dictionary.
   *
   * @param html the page HTML
   * @param word the headword
   * @param scope the parts of the page to include
   * @param dictionary the dictionary to query
   * @return the word, or null when the page has no dictionary content
   */
  Word parseWord(String html, String word, List<WordScope> scope, WordDictionary dictionary) {
    WordDefinition definition = parseDocument(Jsoup.parse(html), word, scope, dictionary);
    return wordMapper.map(definition);
  }

  /**
   * Parses a document into a domain definition.
   *
   * @param doc the parsed page
   * @param word the headword
   * @param scope the parts of the page to include
   * @param dictionary the dictionary queried
   * @return the domain definition or null
   */
  private WordDefinition parseDocument(Document doc, String word, List<WordScope> scope, WordDictionary dictionary) {
    if (dictionary == WordDictionary.ENGLISH) {
      return parseEnglishDocument(doc, word, scope);
    }
    if (doc.selectFirst("table.WRD") == null) {
      return null;
    }
    Set<WordScope> scopes = scope == null || scope.isEmpty() ? Set.of() : Set.copyOf(scope);
    boolean allTranslations = scopes.contains(WordScope.ALL_TRANSLATIONS);
    boolean includeExamples = scopes.contains(WordScope.EXAMPLES);
    boolean includeCompounds = scopes.contains(WordScope.COMPOUNDS);
    boolean includeRelated = scopes.contains(WordScope.RELATED_WORDS);

    int limit = allTranslations ? -1 : DEFAULT_ENTRY_LIMIT;
    List<WordDefinitionEntry> entries = parseTable(section(doc, MAIN_TABLE_ID), limit, includeExamples);
    if (entries.isEmpty()) {
      entries = parseTable(section(doc, REVERSE_TABLE_ID), limit, includeExamples);
    }
    List<WordDefinitionEntry> compounds = List.of();
    if (includeCompounds) {
      compounds = parseTable(section(doc, COMPOUND_TABLE_ID), -1, includeExamples);
      if (compounds.isEmpty()) {
        compounds = parseTable(section(doc, REVERSE_COMPOUND_TABLE_ID), -1, includeExamples);
      }
    }
    List<WordDefinition.RelatedWord> relatedWords = includeRelated ? relatedWords(doc) : List.of();
    return new WordDefinition(word, word, entries, compounds, relatedWords, String.format(WORD_URL, encode(word)));
  }

  /**
   * Maps an English definition page into a domain definition.
   *
   * @param doc the parsed page
   * @param word the headword
   * @param scope the parts of the page to include
   * @return the domain definition or null
   */
  private WordDefinition parseEnglishDocument(Document doc, String word, List<WordScope> scope) {
    Set<WordScope> scopes = scope == null || scope.isEmpty() ? Set.of() : Set.copyOf(scope);
    boolean allTranslations = scopes.contains(WordScope.ALL_TRANSLATIONS);
    boolean includeExamples = scopes.contains(WordScope.EXAMPLES);
    int limit = allTranslations ? -1 : DEFAULT_ENTRY_LIMIT;
    List<WordDefinitionEntry> entries = parseEnglishEntries(doc, limit, includeExamples);
    if (entries.isEmpty()) {
      return null;
    }
    return new WordDefinition(word, word, entries, List.of(), List.of(), String.format(ENGLISH_URL, encode(word)));
  }

  /**
   * Parses English monolingual entries from Random House and Collins sections.
   *
   * @param doc the parsed page
   * @param limit the entry limit, or -1 for all
   * @param includeExamples whether to keep example spans
   * @return the parsed entries
   */
  private List<WordDefinitionEntry> parseEnglishEntries(Document doc, int limit, boolean includeExamples) {
    List<WordDefinitionEntry> entries = new ArrayList<>();
    List<Element> sections = doc.select("div.entryRH, div.superentry");
    for (Element section : sections) {
      String headword = englishHeadword(section);
      if (section.hasClass("entryRH")) {
        entries.addAll(parseRhSection(section, headword, includeExamples));
      } else {
        entries.addAll(parseCollinsSection(section, headword));
      }
      if (limit >= 0 && entries.size() >= limit) {
        break;
      }
    }
    if (limit >= 0 && entries.size() > limit) {
      return entries.subList(0, limit);
    }
    return entries;
  }

  /**
   * Parses a Random House entry block.
   *
   * @param section the section element
   * @param headword the headword
   * @param includeExamples whether to keep examples
   * @return the entries
   */
  private List<WordDefinitionEntry> parseRhSection(Element section, String headword, boolean includeExamples) {
    List<WordDefinitionEntry> entries = new ArrayList<>();
    Element posHeader = section.selectFirst("span.rh_pdef");
    String fallbackPos = posHeader == null ? null : posHeader.text().trim();
    int index = 0;
    for (Element empos : section.select("span.rh_empos")) {
      String pos = empos.text().trim().replaceAll("[.,]$", "");
      if (pos.isEmpty()) {
        pos = fallbackPos;
      }
      Element ol = empos.nextElementSibling();
      while (ol != null && !ol.tagName().equals("ol")) {
        ol = ol.nextElementSibling();
      }
      if (ol == null) {
        continue;
      }
      for (Element li : ol.select("li")) {
        Element def = li.selectFirst("span.rh_def");
        if (def == null) {
          continue;
        }
        String definition = rhDefinition(def, includeExamples);
        if (definition.isEmpty()) {
          continue;
        }
        List<String> examples = rhExamples(li, includeExamples);
        String id = "en:rh:" + headword + ":" + index++;
        entries.add(new WordDefinitionEntry(
            id, headword, pos == null ? "" : pos, null,
            List.of(new WordDefinitionTranslation(definition, null, List.of())),
            examples, null));
      }
    }
    return entries;
  }

  /**
   * Parses a Collins entry block.
   *
   * @param section the section element
   * @param headword the headword
   * @return the entries
   */
  private List<WordDefinitionEntry> parseCollinsSection(Element section, String headword) {
    List<WordDefinitionEntry> entries = new ArrayList<>();
    int index = 0;
    for (Element entry : section.select("div.entry")) {
      Element gramcat = entry.selectFirst("span.gramcat");
      String pos = null;
      if (gramcat != null) {
        Element subjarea = gramcat.selectFirst("span.subjarea");
        pos = subjarea == null ? gramcat.text().trim() : subjarea.text().trim();
      }
      for (Element sense : entry.select("ol.senses > li.sense")) {
        Element def = sense.selectFirst("span.definition");
        if (def == null) {
          continue;
        }
        String definition = def.text().trim();
        if (definition.isEmpty()) {
          continue;
        }
        String id = "en:collins:" + headword + ":" + index++;
        entries.add(new WordDefinitionEntry(
            id, headword, pos == null ? "" : pos, null,
            List.of(new WordDefinitionTranslation(definition, null, List.of())),
            List.of(), null));
      }
    }
    return entries;
  }

  /**
   * Returns the English headword for a section.
   *
   * @param section the section element
   * @return the headword
   */
  private String englishHeadword(Element section) {
    Element me = section.selectFirst("span.rh_me");
    if (me != null) {
      return me.text().trim().split("\\s+")[0];
    }
    Element head = section.selectFirst("span.hw, h3");
    if (head != null) {
      return head.text().trim().split("\\s+")[0];
    }
    return "";
  }

  /**
   * Extracts a Random House definition, optionally stripping examples.
   *
   * @param def the definition span
   * @param includeExamples whether to keep examples inline
   * @return the definition text
   */
  private String rhDefinition(Element def, boolean includeExamples) {
    if (includeExamples) {
      return def.text().trim();
    }
    Element clone = def.clone();
    clone.select("span.rh_ex").remove();
    return clone.text().trim();
  }

  /**
   * Extracts Random House examples from a list item.
   *
   * @param li the list item
   * @param includeExamples whether to keep examples
   * @return the examples
   */
  private List<String> rhExamples(Element li, boolean includeExamples) {
    if (!includeExamples) {
      return List.of();
    }
    List<String> examples = new ArrayList<>();
    for (Element ex : li.select("span.rh_ex")) {
      String text = ex.text().trim();
      if (!text.isEmpty()) {
        examples.add(text);
      }
    }
    return examples;
  }

  /**
   * Fetches and parses a word page, or null when the entry does not exist.
   *
   * @param word the headword
   * @param dictionary the dictionary to query
   * @return the parsed document, or null
   */
  private Document fetch(String word, WordDictionary dictionary) {
    String path = dictionary == WordDictionary.ENGLISH ? "/definition/{enc}" : "/gren/{enc}";
    try {
      String html = restClient.get()
          .uri(path, encode(word))
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
  private Element section(Document doc, String id) {
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
  private List<WordDefinitionEntry> parseTable(Element table, int limit, boolean includeExamples) {
    List<WordDefinitionEntry> entries = new ArrayList<>();
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
  private void parseEntryRow(EntryBuilder entry, Element row) {
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
  private void parseContinuationRow(EntryBuilder entry, Element row, boolean includeExamples) {
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
  private WordDefinitionTranslation translation(Element middle) {
    Element toWrd = middle.parent().selectFirst("td.ToWrd");
    String term = toWrd.text();
    Element pos = toWrd.selectFirst("em.POS2");
    String wordType = pos == null ? null : pos.text();
    if (wordType != null) {
      term = term.replace(wordType, "").trim();
    }
    return new WordDefinitionTranslation(term, wordType, usageNotes(middle));
  }

  /**
   * Extracts the base sense text from a sense cell, minus usage notes.
   *
   * @param middle the sense cell
   * @return the base sense, or null
   */
  private String baseSense(Element middle) {
    String sense = middle.ownText().trim().replaceAll("^\\s*\\(|\\)\\s*$", "");
    return sense.isEmpty() ? null : sense;
  }

  /**
   * Extracts the usage notes from a sense cell.
   *
   * @param middle the sense cell
   * @return the usage notes
   */
  private List<String> usageNotes(Element middle) {
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
  private String headword(Element frWrd) {
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
  private String pos(Element cell) {
    Element pos = cell.selectFirst("em.POS2");
    return pos == null ? null : pos.text();
  }

  /**
   * Strips the "Σχόλιο" label from a note row.
   *
   * @param note the note cell
   * @return the note text
   */
  private String noteText(Element note) {
    return note.text().trim().replaceFirst("^\\s*Σχόλιο\\s*:\\s*", "");
  }

  /**
   * Collects the See-Also sidebar links as shallow related words.
   *
   * @param doc the parsed page
   * @return the related words
   */
  private List<WordDefinition.RelatedWord> relatedWords(Document doc) {
    Element title = doc.selectFirst("li.title1[title='See Also:']");
    Element list = title == null ? null : title.parent().nextElementSibling();
    if (list == null) {
      return List.of();
    }
    List<WordDefinition.RelatedWord> related = new ArrayList<>();
    for (Element link : list.select("a")) {
      String href = link.attr("href");
      String term = link.text().trim();
      related.add(new WordDefinition.RelatedWord(term, "https://www.wordreference.com" + href));
    }
    return related;
  }

  /**
   * URL-encodes a word for a WordReference path.
   *
   * @param word the headword
   * @return the encoded path segment
   */
  private String encode(String word) {
    return URLEncoder.encode(word, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * Accumulates an entry while its continuation rows are parsed.
   */
  private static final class EntryBuilder {

    private final String id;
    private final String term;
    private final String wordType;
    private final List<WordDefinitionTranslation> translations = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private String sense;
    private String note;

    private EntryBuilder(String id, String term, String wordType) {
      this.id = id;
      this.term = term;
      this.wordType = wordType == null ? "" : wordType;
    }

    private WordDefinitionEntry build() {
      return new WordDefinitionEntry(id, term, wordType, sense, List.copyOf(translations), List.copyOf(examples), note);
    }
  }
}
