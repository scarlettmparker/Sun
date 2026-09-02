package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.codegen.types.PropertySetSchemaInput;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.codegen.types.SetPropertyInput;
import com.sun.gaia.codegen.types.UpsertPropertyEntryInput;
import com.sun.gaia.graphql.services.PropertySetGraphQLService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertySetDataFetcherTest {

  @Mock private PropertySetGraphQLService service;

  @InjectMocks private PropertySetDataFetcher fetcher;

  @Test
  void propertySet_shouldDelegate() {
    Map<String, Object> values = Map.of("key", "val");
    when(service.propertySet("owner", "set", "entry")).thenReturn(values);

    Object result = fetcher.propertySet("owner", "set", "entry");

    assertThat(result).isEqualTo(values);
    verify(service).propertySet("owner", "set", "entry");
  }

  @Test
  void propertySets_shouldDelegate() {
    PropertySetEntry entry = PropertySetEntry.newBuilder().id("id1").build();
    when(service.propertySets("owner", "set")).thenReturn(List.of(entry));

    List<PropertySetEntry> result = fetcher.propertySets("owner", "set");

    assertThat(result).containsExactly(entry);
    verify(service).propertySets("owner", "set");
  }

  @Test
  void propertySetSchema_shouldDelegate() {
    PropertySetSchema schema = PropertySetSchema.newBuilder().id("id1").name("set").build();
    when(service.propertySetSchema("owner", "set")).thenReturn(schema);

    PropertySetSchema result = fetcher.propertySetSchema("owner", "set");

    assertThat(result).isEqualTo(schema);
    verify(service).propertySetSchema("owner", "set");
  }

  @Test
  void accessibleCommandIntents_shouldDelegate() {
    PropertySetEntry entry = PropertySetEntry.newBuilder().id("id1").build();
    when(service.accessibleCommandIntents(RemoteUserType.DISCORD, "123", "owner", "set"))
        .thenReturn(List.of(entry));

    List<PropertySetEntry> result =
        fetcher.accessibleCommandIntents(RemoteUserType.DISCORD, "123", "owner", "set");

    assertThat(result).containsExactly(entry);
    verify(service).accessibleCommandIntents(RemoteUserType.DISCORD, "123", "owner", "set");
  }

  @Test
  void upsertPropertyEntry_shouldDelegate() {
    PropertySetEntry entry = PropertySetEntry.newBuilder().id("id1").build();
    Object values = Map.of("k", "v");
    UpsertPropertyEntryInput input =
        UpsertPropertyEntryInput.newBuilder().values(values).build();
    when(service.upsertPropertyEntry("owner", "set", "entry", values)).thenReturn(entry);

    PropertySetEntry result = fetcher.upsertPropertyEntry("owner", "set", "entry", input);

    assertThat(result).isEqualTo(entry);
    verify(service).upsertPropertyEntry("owner", "set", "entry", values);
  }

  @Test
  void setProperty_shouldDelegate() {
    PropertySetEntry entry = PropertySetEntry.newBuilder().id("id1").build();
    SetPropertyInput input = SetPropertyInput.newBuilder().property("prop").value("val").build();
    when(service.setProperty("owner", "set", "entry", "prop", "val")).thenReturn(entry);

    PropertySetEntry result = fetcher.setProperty("owner", "set", "entry", input);

    assertThat(result).isEqualTo(entry);
    verify(service).setProperty("owner", "set", "entry", "prop", "val");
  }

  @Test
  void registerPropertySetSchema_shouldDelegate() {
    PropertySetSchemaInput input = PropertySetSchemaInput.newBuilder().ownerKey("owner").name("set").build();
    PropertySetSchema schema = PropertySetSchema.newBuilder().id("id1").name("set").build();
    when(service.registerPropertySetSchema(input)).thenReturn(schema);

    PropertySetSchema result = fetcher.registerPropertySetSchema(input);

    assertThat(result).isEqualTo(schema);
    verify(service).registerPropertySetSchema(input);
  }

  @Test
  void propertySetSchemas_shouldDelegate() {
    PropertySetSchema schema = PropertySetSchema.newBuilder().id("id1").name("set").build();
    when(service.propertySetSchemas("owner")).thenReturn(List.of(schema));

    List<PropertySetSchema> result = fetcher.propertySetSchemas("owner");

    assertThat(result).containsExactly(schema);
    verify(service).propertySetSchemas("owner");
  }

  @Test
  void propertySetSchemas_shouldDelegateWithNullOwner() {
    PropertySetSchema schema = PropertySetSchema.newBuilder().id("id1").name("set").build();
    when(service.propertySetSchemas(null)).thenReturn(List.of(schema));

    List<PropertySetSchema> result = fetcher.propertySetSchemas(null);

    assertThat(result).containsExactly(schema);
    verify(service).propertySetSchemas(null);
  }

  @Test
  void deletePropertyEntry_shouldDelegate() {
    com.sun.gaia.codegen.types.QuerySuccess success =
        com.sun.gaia.codegen.types.QuerySuccess.newBuilder().message("Entry deleted").id("entry").build();
    when(service.deletePropertyEntry("owner", "set", "entry")).thenReturn(success);

    var result = fetcher.deletePropertyEntry("owner", "set", "entry");

    assertThat(result).isEqualTo(success);
    verify(service).deletePropertyEntry("owner", "set", "entry");
  }
}
