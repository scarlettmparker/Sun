package com.sun.echo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistEntryItem;
import com.sun.echo.model.ChecklistEntryItemEntity;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.model.enums.ItemStatus;
import com.sun.echo.repository.ChecklistItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistEntryItemMapperTest {

  @Mock
  private ChecklistItemRepository itemRepository;

  @Test
  void map_entityResolvesNameAndIcon() {
    UUID id = UUID.randomUUID();
    UUID entryId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    ChecklistEntryItemEntity entity = new ChecklistEntryItemEntity();
    entity.setId(id);
    entity.setEntryId(entryId);
    entity.setItemId(itemId);
    entity.setStatus(ItemStatus.NOT_STARTED);
    entity.setPosition(1);

    ChecklistItemEntity item = new ChecklistItemEntity();
    item.setId(itemId);
    item.setName("Buy milk");
    item.setIcon("milk");

    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

    ChecklistEntryItemMapper mapper = new ChecklistEntryItemMapper(itemRepository);
    ChecklistEntryItem result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getEntryId()).isEqualTo(entryId.toString());
    assertThat(result.getItemId()).isEqualTo(itemId.toString());
    assertThat(result.getName()).isEqualTo("Buy milk");
    assertThat(result.getIcon()).isEqualTo("milk");
    assertThat(result.getStatus()).isEqualTo(ItemStatus.NOT_STARTED);
    assertThat(result.getPosition()).isEqualTo(1);
  }

  @Test
  void map_entityWithMissingItemReturnsNullNameAndIcon() {
    UUID id = UUID.randomUUID();
    UUID entryId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    ChecklistEntryItemEntity entity = new ChecklistEntryItemEntity();
    entity.setId(id);
    entity.setEntryId(entryId);
    entity.setItemId(itemId);
    entity.setStatus(ItemStatus.COMPLETE);
    entity.setPosition(2);

    when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

    ChecklistEntryItemMapper mapper = new ChecklistEntryItemMapper(itemRepository);
    ChecklistEntryItem result = mapper.map(entity);

    assertThat(result.getName()).isNull();
    assertThat(result.getIcon()).isNull();
    assertThat(result.getStatus()).isEqualTo(ItemStatus.COMPLETE);
  }

}
