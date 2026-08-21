package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.HubAppInput;
import com.sun.gaia.codegen.types.HubMode;
import com.sun.gaia.codegen.types.HubRegistry;
import com.sun.gaia.codegen.types.HubRegistryInput;
import com.sun.gaia.model.PropertySetEntryEntity;
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
class HubRegistryGraphQLServiceTest {

  @Mock private PropertySetService propertySetService;

  @InjectMocks private HubRegistryGraphQLService service;

  @Test
  void hubRegistry_returnsStoredRegistryWhenPresent() {
    PropertySetEntryEntity entity = new PropertySetEntryEntity();
    entity.setValues(Map.of(
        "mode", "serve",
        "apps", List.of(Map.of("key", "sun", "name", "Sun", "devPort", 5173, "prodPort", 5173))));
    when(propertySetService.getEntry("hub", "registry", "apps")).thenReturn(Optional.of(entity));

    HubRegistry result = service.hubRegistry();

    assertThat(result.getMode()).isEqualTo(HubMode.serve);
    assertThat(result.getApps()).hasSize(1);
    assertThat(result.getApps().get(0).getKey()).isEqualTo("sun");
  }

  @Test
  void hubRegistry_returnsDefaultWhenAbsent() {
    when(propertySetService.getEntry("hub", "registry", "apps")).thenReturn(Optional.empty());

    HubRegistry result = service.hubRegistry();

    assertThat(result.getMode()).isEqualTo(HubMode.dev);
    assertThat(result.getApps()).isNotEmpty();
  }

  @Test
  void saveRegistry_persistsAndReturnsRegistry() {
    HubRegistryInput input = HubRegistryInput.newBuilder()
        .mode(HubMode.serve)
        .apps(List.of(HubAppInput.newBuilder()
            .key("sun").name("Sun").dir(".").devPort(5173).prodPort(5173)
            .url("https://sun.test").description("desc").enabled(true).self(true).build()))
        .build();
    PropertySetEntryEntity entity = new PropertySetEntryEntity();
    entity.setValues(Map.of("mode", "serve", "apps", List.of()));
    when(propertySetService.upsertEntry(eq("hub"), eq("registry"), eq("apps"), any(), eq(false)))
        .thenReturn(entity);

    HubRegistry result = service.saveRegistry(input);

    assertThat(result.getMode()).isEqualTo(HubMode.serve);
    verify(propertySetService).upsertEntry(eq("hub"), eq("registry"), eq("apps"), any(), eq(false));
  }

  @Test
  void saveRegistry_throwsWhenModeNull() {
    HubRegistryInput input = HubRegistryInput.newBuilder()
        .apps(List.of()).build();

    assertThatThrownBy(() -> service.saveRegistry(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Hub mode is required");
  }

  @Test
  void saveRegistry_throwsWhenAppKeyBlank() {
    HubRegistryInput input = HubRegistryInput.newBuilder()
        .mode(HubMode.dev)
        .apps(List.of(HubAppInput.newBuilder()
            .key(" ").name("Sun").dir(".").devPort(5173).prodPort(5173).build()))
        .build();

    assertThatThrownBy(() -> service.saveRegistry(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Hub app key is required");
  }

  @Test
  void saveRegistry_throwsWhenPortsNotPositive() {
    HubRegistryInput input = HubRegistryInput.newBuilder()
        .mode(HubMode.dev)
        .apps(List.of(HubAppInput.newBuilder()
            .key("sun").name("Sun").dir(".").devPort(0).prodPort(5173).build()))
        .build();

    assertThatThrownBy(() -> service.saveRegistry(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Hub app ports must be positive");
  }
}
