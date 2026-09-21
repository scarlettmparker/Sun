package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.ApplyConfigurationResponse;
import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.codegen.types.ConfigurationInput;
import com.sun.gaia.codegen.types.CreateConfigurationResponse;
import com.sun.gaia.codegen.types.DeleteConfigurationResponse;
import com.sun.gaia.codegen.types.UpdateConfigurationResponse;
import com.sun.gaia.graphql.services.ConfigurationGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationDataFetcherTest {

  @Mock private ConfigurationGraphQLService service;

  @InjectMocks private ConfigurationDataFetcher fetcher;

  @Test
  void configurations_shouldDelegate() {
    Configuration c = Configuration.newBuilder().id("id1").name("cfg").build();
    when(service.configurations()).thenReturn(List.of(c));

    List<Configuration> result = fetcher.configurations();

    assertThat(result).containsExactly(c);
    verify(service).configurations();
  }

  @Test
  void configuration_shouldDelegate() {
    Configuration c = Configuration.newBuilder().id("id1").name("cfg").build();
    when(service.configuration("id1")).thenReturn(c);

    Configuration result = fetcher.configuration("id1");

    assertThat(result).isEqualTo(c);
    verify(service).configuration("id1");
  }

  @Test
  void createConfiguration_shouldDelegate() {
    ConfigurationInput input = ConfigurationInput.newBuilder().name("cfg").build();
    Configuration c = Configuration.newBuilder().id("id1").name("cfg").build();
    CreateConfigurationResponse mock = CreateConfigurationResponse.newBuilder()
        .message("Configuration created").configuration(c).build();
    when(service.createConfiguration(input)).thenReturn(mock);

    CreateConfigurationResponse result = fetcher.createConfiguration(input);

    assertThat(result).isEqualTo(mock);
    verify(service).createConfiguration(input);
  }

  @Test
  void updateConfiguration_shouldDelegate() {
    ConfigurationInput input = ConfigurationInput.newBuilder().name("cfg2").build();
    Configuration c = Configuration.newBuilder().id("id1").name("cfg2").build();
    UpdateConfigurationResponse mock = UpdateConfigurationResponse.newBuilder()
        .message("Configuration updated").configuration(c).build();
    when(service.updateConfiguration("id1", input)).thenReturn(mock);

    UpdateConfigurationResponse result = fetcher.updateConfiguration("id1", input);

    assertThat(result).isEqualTo(mock);
    verify(service).updateConfiguration("id1", input);
  }

  @Test
  void deleteConfiguration_shouldDelegate() {
    DeleteConfigurationResponse mock = DeleteConfigurationResponse.newBuilder()
        .message("Configuration deleted").id("id1").build();
    when(service.deleteConfiguration("id1")).thenReturn(mock);

    DeleteConfigurationResponse result = fetcher.deleteConfiguration("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).deleteConfiguration("id1");
  }

  @Test
  void applyConfiguration_shouldDelegate() {
    Configuration c = Configuration.newBuilder().id("id1").name("cfg").build();
    ApplyConfigurationResponse mock = ApplyConfigurationResponse.newBuilder()
        .message("Configuration applied").configuration(c).build();
    when(service.applyConfiguration("id1")).thenReturn(mock);

    ApplyConfigurationResponse result = fetcher.applyConfiguration("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).applyConfiguration("id1");
  }
}
