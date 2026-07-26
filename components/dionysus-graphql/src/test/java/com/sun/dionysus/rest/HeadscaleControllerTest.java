package com.sun.dionysus.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.dionysus.headscale.HeadscaleService;
import com.sun.dionysus.headscale.HeadscaleService.HeadscaleNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HeadscaleController.class)
class HeadscaleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private HeadscaleService headscaleService;

  @Test
  void listNodes_returnsNodesFromService() throws Exception {
    when(headscaleService.listNodes()).thenReturn(List.of(
        new HeadscaleNode(1, "node1", "100.64.0.1", true, "2026-07-26T12:00:00Z"),
        new HeadscaleNode(2, "node2", "100.64.0.2", false, "2026-07-26T10:00:00Z")));

    mockMvc.perform(get("/api/headscale/nodes"))
        .andExpect(status().isOk())
        .andExpect(content().json("""
            [{"id":1,"name":"node1","ipv4":"100.64.0.1","online":true},
             {"id":2,"name":"node2","ipv4":"100.64.0.2","online":false}]
            """));
  }

  @Test
  void expireNode_callsService() throws Exception {
    mockMvc.perform(post("/api/headscale/nodes/42/expire"))
        .andExpect(status().isOk());

    verify(headscaleService).expireNode(42);
  }

  @Test
  void preauthKey_returnsPng() throws Exception {
    when(headscaleService.createPreAuthKey(any())).thenReturn("hskey-auth-test-key");

    mockMvc.perform(get("/api/headscale/preauth-key")
            .param("expiry", "1h"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));
  }

  @Test
  void preauthKey_defaultsTo1h() throws Exception {
    when(headscaleService.createPreAuthKey(any())).thenReturn("hskey-auth-test-key");

    mockMvc.perform(get("/api/headscale/preauth-key"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));
  }
}
