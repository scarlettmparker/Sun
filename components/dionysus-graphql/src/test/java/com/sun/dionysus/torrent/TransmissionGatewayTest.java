package com.sun.dionysus.torrent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TransmissionGateway.
 */
@ExtendWith(MockitoExtension.class)
class TransmissionGatewayTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Mock private TransmissionRpcClient rpc;

  @InjectMocks private TransmissionGateway gateway;

  @Test
  void removeTorrentsByInfoHash_removesOnlyMatchingHashes() {
    ObjectNode response = mapper.createObjectNode();
    ArrayNode torrents = response.putArray("torrents");
    torrents.addObject().put("id", 1).put("hashString", "abc");
    torrents.addObject().put("id", 2).put("hashString", "def");
    when(rpc.call(eq("torrent-get"), any())).thenReturn(response);
    when(rpc.call(eq("torrent-remove"), any())).thenReturn(mapper.createObjectNode());

    gateway.removeTorrentsByInfoHash(List.of("ABC"));

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(rpc).call(eq("torrent-remove"), captor.capture());
    ArrayNode ids = (ArrayNode) captor.getValue().get("ids");
    assertThat(ids).hasSize(1);
    assertThat(ids.get(0).asInt()).isEqualTo(1);
  }

  @Test
  void removeTorrentsByInfoHash_skipsUnknownHashes() {
    ObjectNode response = mapper.createObjectNode();
    response.putArray("torrents").addObject().put("id", 2).put("hashString", "def");
    when(rpc.call(eq("torrent-get"), any())).thenReturn(response);

    gateway.removeTorrentsByInfoHash(List.of("abc"));

    verify(rpc, never()).call(eq("torrent-remove"), any());
  }

  @Test
  void removeTorrentsByInfoHash_ignoresEmptyInput() {
    gateway.removeTorrentsByInfoHash(List.of());

    verify(rpc, never()).call(any(), any());
  }
}
