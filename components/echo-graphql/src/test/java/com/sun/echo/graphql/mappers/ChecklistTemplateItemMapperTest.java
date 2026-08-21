package com.sun.echo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistTemplateItem;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.repository.ChecklistItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistTemplateItemMapperTest {

  @Mock
  private ChecklistItemRepository itemRepository;

  @Test
  void map_entityResolvesNameAndIcon() {
    UUID id = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    ChecklistTemplateItemEntity entity = new ChecklistTemplateItemEntity();
    entity.setId(id);
    entity.setTemplateId(templateId);
    entity.setItemId(itemId);
    entity.setPosition(3);

    ChecklistItemEntity item = new ChecklistItemEntity();
    item.setId(itemId);
    item.setName("Water plants");
    item.setIcon("leaf");

    when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

    ChecklistTemplateItemMapper mapper = new ChecklistTemplateItemMapper(itemRepository);
    ChecklistTemplateItem result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getTemplateId()).isEqualTo(templateId.toString());
    assertThat(result.getItemId()).isEqualTo(itemId.toString());
    assertThat(result.getName()).isEqualTo("Water plants");
    assertThat(result.getIcon()).isEqualTo("leaf");
    assertThat(result.getPosition()).isEqualTo(3);
  }

  @Test
  void map_entityWithMissingItemReturnsNullNameAndIcon() {
    UUID id = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    ChecklistTemplateItemEntity entity = new ChecklistTemplateItemEntity();
    entity.setId(id);
    entity.setTemplateId(templateId);
    entity.setItemId(itemId);
    entity.setPosition(0);

    when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

    ChecklistTemplateItemMapper mapper = new ChecklistTemplateItemMapper(itemRepository);
    ChecklistTemplateItem result = mapper.map(entity);

    assertThat(result.getName()).isNull();
    assertThat(result.getIcon()).isNull();
  }

}
