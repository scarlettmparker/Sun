package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.codegen.types.ConfigurationInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.graphql.mappers.ConfigurationMapper;
import com.sun.gaia.model.ConfigurationEntity;
import com.sun.gaia.service.ConfigurationReconciler;
import com.sun.gaia.service.ConfigurationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationGraphQLServiceTest {

  @Mock private ConfigurationService configurationService;
  @Mock private ConfigurationReconciler configurationReconciler;
  @Mock private ConfigurationMapper configurationMapper;

  @InjectMocks private ConfigurationGraphQLService service;

  @Test
  void configurations_returnsMappedList() {
    ConfigurationEntity entity = new ConfigurationEntity();
    Configuration mapped = Configuration.newBuilder().name("react-app-themes").build();
    when(configurationService.list()).thenReturn(List.of(entity));
    when(configurationMapper.map(entity)).thenReturn(mapped);

    List<Configuration> result = service.configurations();

    assertThat(result).containsExactly(mapped);
    verify(configurationService).list();
  }

  @Test
  void configuration_returnsNullWhenAbsent() {
    UUID id = UUID.randomUUID();
    when(configurationService.locate(id)).thenReturn(Optional.empty());

    assertThat(service.configuration(id.toString())).isNull();
  }

  @Test
  void configuration_returnsMappedWhenPresent() {
    UUID id = UUID.randomUUID();
    ConfigurationEntity entity = new ConfigurationEntity();
    Configuration mapped = Configuration.newBuilder().id(id.toString()).build();
    when(configurationService.locate(id)).thenReturn(Optional.of(entity));
    when(configurationMapper.map(entity)).thenReturn(mapped);

    assertThat(service.configuration(id.toString())).isEqualTo(mapped);
  }

  @Test
  void createConfiguration_delegatesToService() {
    ConfigurationInput input = ConfigurationInput.newBuilder()
        .name("my-config").description("desc").enabled(true)
        .content(Map.of("key", "value")).build();
    ConfigurationEntity entity = new ConfigurationEntity();
    Configuration mapped = Configuration.newBuilder().name("my-config").build();
    when(configurationService.create(eq("my-config"), eq("desc"), eq(true), any())).thenReturn(entity);
    when(configurationMapper.map(entity)).thenReturn(mapped);

    Configuration result = service.createConfiguration(input);

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void createConfiguration_defaultsEnabledWhenNull() {
    ConfigurationInput input = ConfigurationInput.newBuilder()
        .name("my-config").content(Map.of("k", "v")).build();
    ConfigurationEntity entity = new ConfigurationEntity();
    Configuration mapped = Configuration.newBuilder().name("my-config").build();
    when(configurationService.create(eq("my-config"), any(), eq(true), any())).thenReturn(entity);
    when(configurationMapper.map(entity)).thenReturn(mapped);

    Configuration result = service.createConfiguration(input);

    assertThat(result).isEqualTo(mapped);
    verify(configurationService).create(eq("my-config"), any(), eq(true), any());
  }

  @Test
  void updateConfiguration_delegatesToService() {
    UUID id = UUID.randomUUID();
    ConfigurationInput input = ConfigurationInput.newBuilder()
        .name("updated").description("desc").enabled(false)
        .content(Map.of("k", "v")).build();
    ConfigurationEntity entity = new ConfigurationEntity();
    Configuration mapped = Configuration.newBuilder().name("updated").build();
    when(configurationService.update(eq(id), eq("updated"), eq("desc"), eq(false), any()))
        .thenReturn(entity);
    when(configurationMapper.map(entity)).thenReturn(mapped);

    Configuration result = service.updateConfiguration(id.toString(), input);

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void deleteConfiguration_returnsSuccess() {
    UUID id = UUID.randomUUID();

    QueryResult result = service.deleteConfiguration(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(configurationService).deleteById(id);
  }

  @Test
  void applyConfiguration_delegatesToReconciler() {
    UUID id = UUID.randomUUID();
    ConfigurationEntity entity = new ConfigurationEntity();
    Configuration mapped = Configuration.newBuilder().id(id.toString()).build();
    when(configurationReconciler.reconcileById(id)).thenReturn(entity);
    when(configurationMapper.map(entity)).thenReturn(mapped);

    Configuration result = service.applyConfiguration(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(configurationReconciler).reconcileById(id);
  }
}
