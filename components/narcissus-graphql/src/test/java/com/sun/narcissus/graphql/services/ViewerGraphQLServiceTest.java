package com.sun.narcissus.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sun.narcissus.codegen.types.VncCredentials;
import com.sun.narcissus.service.VncCredentialsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ViewerGraphQLServiceTest {

  @Mock private VncCredentialsService credentialsService;
  @InjectMocks private ViewerGraphQLService service;

  @Test
  void vncCredentials_shouldReturnCredentials() throws Exception {
    when(credentialsService.generateIframeSrc()).thenReturn("/novnc/vnc.html?token=abc");

    VncCredentials result = service.vncCredentials();

    assertThat(result).isNotNull();
    assertThat(result.getIframeSrc()).isEqualTo("/novnc/vnc.html?token=abc");
  }

  @Test
  void vncCredentials_whenServiceThrows_shouldWrapException() throws Exception {
    when(credentialsService.generateIframeSrc()).thenThrow(new RuntimeException("io error"));

    assertThatThrownBy(() -> service.vncCredentials())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to generate VNC credentials");
  }
}
