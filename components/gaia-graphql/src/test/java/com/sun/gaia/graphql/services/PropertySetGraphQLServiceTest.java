package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.codegen.types.PropertySetSchemaInput;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.graphql.mappers.PropertySetMapper;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import com.sun.gaia.service.PropertySetService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertySetGraphQLServiceTest {

  @Mock private PropertySetService propertySetService;
  @Mock private PropertySetMapper propertySetMapper;

  @InjectMocks private PropertySetGraphQLService service;

  @Test
  void accessibleCommandIntents_returnsAccessibleEntries() {
    PropertySetEntryEntity entry = new PropertySetEntryEntity();
    entry.setEntryName("texts");
    entry.setValues(Map.of("command", "texts", "description", "List reader texts"));
    when(propertySetService.listAccessibleEntries("12345", "NieceScarlett", "command-intents"))
        .thenReturn(List.of(entry));

    PropertySetEntry mapped = PropertySetEntry.newBuilder()
        .entryName("texts")
        .values(Map.of("command", "texts", "description", "List reader texts"))
        .build();
    when(propertySetMapper.map(entry)).thenReturn(mapped);

    List<PropertySetEntry> result = service.accessibleCommandIntents(
        RemoteUserType.DISCORD, "12345", "NieceScarlett", "command-intents");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEntryName()).isEqualTo("texts");
  }

  @Test
  void accessibleCommandIntents_returnsEmptyForUnknownUser() {
    List<PropertySetEntry> result = service.accessibleCommandIntents(
        RemoteUserType.DISCORD, "unknown", "NieceScarlett", "command-intents");

    assertThat(result).isEmpty();
  }

  @Test
  void accessibleCommandIntents_returnsEmptyForBlankUserId() {
    List<PropertySetEntry> result = service.accessibleCommandIntents(
        RemoteUserType.DISCORD, "  ", "NieceScarlett", "command-intents");

    assertThat(result).isEmpty();
  }

  @Test
  void accessibleCommandIntents_returnsEmptyForNonDiscordType() {
    List<PropertySetEntry> result = service.accessibleCommandIntents(
        RemoteUserType.DISCORD, null, "NieceScarlett", "command-intents");

    assertThat(result).isEmpty();
  }

  @Test
  void propertySets_returnsMappedEntries() {
    PropertySetEntryEntity entity = new PropertySetEntryEntity();
    entity.setEntryName("greek");
    when(propertySetService.listActiveEntries("ReactApp", "themes")).thenReturn(List.of(entity));
    PropertySetEntry mapped = PropertySetEntry.newBuilder().entryName("greek").build();
    when(propertySetMapper.map(entity)).thenReturn(mapped);

    List<PropertySetEntry> result = service.propertySets("ReactApp", "themes");

    assertThat(result).containsExactly(mapped);
    verify(propertySetService).listActiveEntries("ReactApp", "themes");
  }

  @Test
  void propertySet_returnsAllEntriesWhenEntryNull() {
    PropertySetEntryEntity e1 = new PropertySetEntryEntity();
    e1.setEntryName("greek");
    e1.setValues(Map.of("primary", "#1d4ed8"));
    PropertySetEntryEntity e2 = new PropertySetEntryEntity();
    e2.setEntryName("dark");
    e2.setValues(Map.of("primary", "#000000"));
    when(propertySetService.listActiveEntries("ReactApp", "themes")).thenReturn(List.of(e1, e2));

    Object result = service.propertySet("ReactApp", "themes", null);

    assertThat(result).isInstanceOf(Map.class);
    Map<?, ?> map = (Map<?, ?>) result;
    assertThat(map.containsKey("greek")).isTrue();
    assertThat(map.containsKey("dark")).isTrue();
  }

  @Test
  void propertySet_returnsValuesWhenEntryFound() {
    PropertySetEntryEntity entity = new PropertySetEntryEntity();
    entity.setValues(Map.of("primary", "#fff"));
    when(propertySetService.getEntry("ReactApp", "themes", "greek")).thenReturn(Optional.of(entity));

    Object result = service.propertySet("ReactApp", "themes", "greek");

    assertThat(result).isEqualTo(Map.of("primary", "#fff"));
  }

  @Test
  void propertySet_returnsNullWhenEntryMissing() {
    when(propertySetService.getEntry("ReactApp", "themes", "missing")).thenReturn(Optional.empty());

    Object result = service.propertySet("ReactApp", "themes", "missing");

    assertThat(result).isNull();
  }

  @Test
  void propertySetSchema_returnsNullWhenAbsent() {
    when(propertySetService.getSchemaEntity("ReactApp", "themes")).thenReturn(Optional.empty());

    assertThat(service.propertySetSchema("ReactApp", "themes")).isNull();
  }

  @Test
  void propertySetSchema_returnsMappedWhenPresent() {
    PropertySetSchemaEntity entity = new PropertySetSchemaEntity();
    PropertySetSchema mapped = PropertySetSchema.newBuilder().name("themes").build();
    when(propertySetService.getSchemaEntity("ReactApp", "themes")).thenReturn(Optional.of(entity));
    when(propertySetMapper.map(entity)).thenReturn(mapped);

    assertThat(service.propertySetSchema("ReactApp", "themes")).isEqualTo(mapped);
  }

  @Test
  void upsertPropertyEntry_delegatesToService() {
    PropertySetEntryEntity entity = new PropertySetEntryEntity();
    PropertySetEntry mapped = PropertySetEntry.newBuilder().entryName("greek").build();
    when(propertySetService.upsertEntry(eq("ReactApp"), eq("themes"), eq("greek"), any(), eq(false)))
        .thenReturn(entity);
    when(propertySetMapper.map(entity)).thenReturn(mapped);

    PropertySetEntry result = service.upsertPropertyEntry("ReactApp", "themes", "greek", Map.of("a", "b"));

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void setProperty_delegatesToService() {
    PropertySetEntryEntity entity = new PropertySetEntryEntity();
    PropertySetEntry mapped = PropertySetEntry.newBuilder().entryName("greek").build();
    when(propertySetService.setProperty("ReactApp", "themes", "greek", "primary", "#fff"))
        .thenReturn(entity);
    when(propertySetMapper.map(entity)).thenReturn(mapped);

    PropertySetEntry result = service.setProperty("ReactApp", "themes", "greek", "primary", "#fff");

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void registerPropertySetSchema_delegatesToService() {
    PropertySetSchemaEntity entity = new PropertySetSchemaEntity();
    PropertySetSchema mapped = PropertySetSchema.newBuilder().name("themes").build();
    PropertySetSchemaInput input = PropertySetSchemaInput.newBuilder()
        .ownerKey("ReactApp").name("themes").configurable(true)
        .properties(Map.of("primary", Map.of("type", "color"))).build();
    when(propertySetService.upsertSchema(eq("ReactApp"), eq("themes"), eq(true), any()))
        .thenReturn(entity);
    when(propertySetMapper.map(entity)).thenReturn(mapped);

    PropertySetSchema result = service.registerPropertySetSchema(input);

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void propertySetSchemas_returnsMapped() {
    PropertySetSchemaEntity entity = new PropertySetSchemaEntity();
    PropertySetSchema mapped = PropertySetSchema.newBuilder().name("review-attributes").build();
    when(propertySetService.listSchemas("Blog")).thenReturn(List.of(entity));
    when(propertySetMapper.map(entity)).thenReturn(mapped);

    List<PropertySetSchema> result = service.propertySetSchemas("Blog");

    assertThat(result).containsExactly(mapped);
    verify(propertySetService).listSchemas("Blog");
  }

  @Test
  void propertySetSchemas_returnsAllWhenOwnerNull() {
    PropertySetSchemaEntity entity = new PropertySetSchemaEntity();
    PropertySetSchema mapped = PropertySetSchema.newBuilder().name("themes").build();
    when(propertySetService.listSchemas(null)).thenReturn(List.of(entity));
    when(propertySetMapper.map(entity)).thenReturn(mapped);

    List<PropertySetSchema> result = service.propertySetSchemas(null);

    assertThat(result).containsExactly(mapped);
  }

  @Test
  void deletePropertyEntry_returnsSuccessWhenDeleted() {
    when(propertySetService.deleteEntry("Blog", "review-attributes", "entry1"))
        .thenReturn(true);

    var result = service.deletePropertyEntry("Blog", "review-attributes", "entry1");

    assertThat(result).isInstanceOf(com.sun.gaia.codegen.types.QuerySuccess.class);
  }

  @Test
  void deletePropertyEntry_returnsErrorWhenNotFound() {
    when(propertySetService.deleteEntry("Blog", "review-attributes", "missing"))
        .thenReturn(false);

    var result = service.deletePropertyEntry("Blog", "review-attributes", "missing");

    assertThat(result).isInstanceOf(com.sun.gaia.codegen.types.StandardError.class);
  }
}
