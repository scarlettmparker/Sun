package com.sun.echo.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.base.error.MutationException;
import com.sun.echo.codegen.types.AddTemplateItemResponse;
import com.sun.echo.codegen.types.ArchiveTemplateResponse;
import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.CreateTemplateResponse;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateItemMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateMapper;
import com.sun.echo.model.ChecklistTemplateEntity;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistTemplateItemService;
import com.sun.echo.service.ChecklistTemplateService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistTemplateGraphQLServiceTest {

  @Mock private ChecklistTemplateService templateService;
  @Mock private ChecklistTemplateItemService templateItemService;
  @Mock private ChecklistDetailService detailService;
  @Mock private ChecklistTemplateMapper templateMapper;
  @Mock private ChecklistTemplateItemMapper templateItemMapper;
  @Mock private ChecklistDetailMapper detailMapper;

  @InjectMocks private ChecklistTemplateGraphQLService service;

  @Test
  void template_delegatesToLocateAndMap() {
    UUID id = UUID.randomUUID();
    ChecklistTemplateEntity entity = new ChecklistTemplateEntity();
    entity.setId(id);
    when(templateService.locate(id)).thenReturn(Optional.of(entity));
    ChecklistTemplate mapped = ChecklistTemplate.newBuilder().id(id.toString()).name("t").build();
    when(templateMapper.map(entity)).thenReturn(mapped);

    ChecklistTemplate result = service.template(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(templateService).locate(id);
  }

  @Test
  void createTemplate_savesAndReturnsTemplate() {
    ChecklistTemplateEntity saved = new ChecklistTemplateEntity();
    saved.setId(UUID.randomUUID());
    when(templateService.save(any(ChecklistTemplateEntity.class))).thenReturn(saved);
    ChecklistTemplate mapped = ChecklistTemplate.newBuilder().id(saved.getId().toString()).name("Name").build();
    when(templateMapper.map(saved)).thenReturn(mapped);

    CreateTemplateResponse result = service.createTemplate("Name", "desc", null);

    assertThat(result.getTemplate()).isEqualTo(mapped);
    assertThat(result.getMessage()).contains("created");
    verify(templateService).save(any(ChecklistTemplateEntity.class));
  }

  @Test
  void createTemplate_throwsMutationExceptionWhenSaveFails() {
    when(templateService.save(any(ChecklistTemplateEntity.class))).thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> service.createTemplate("Name", "desc", null))
        .isInstanceOf(MutationException.class)
        .hasMessageContaining("createTemplate failed: Database error");
  }

  @Test
  void archiveTemplate_delegatesToService() {
    UUID id = UUID.randomUUID();
    ChecklistTemplateEntity archived = new ChecklistTemplateEntity();
    archived.setId(id);
    when(templateService.archive(id)).thenReturn(archived);
    ChecklistTemplate mapped = ChecklistTemplate.newBuilder().id(id.toString()).name("t").build();
    when(templateMapper.map(archived)).thenReturn(mapped);

    ArchiveTemplateResponse result = service.archiveTemplate(id.toString());

    assertThat(result.getTemplate()).isEqualTo(mapped);
    verify(templateService).archive(id);
  }

  @Test
  void listTemplates_delegatesToService() {
    ChecklistTemplateEntity entity = new ChecklistTemplateEntity();
    entity.setId(UUID.randomUUID());
    when(templateService.findAll()).thenReturn(List.of(entity));
    ChecklistTemplate mapped = ChecklistTemplate.newBuilder().id(entity.getId().toString()).name("t").build();
    when(templateMapper.map(entity)).thenReturn(mapped);

    List<ChecklistTemplate> result = service.listTemplates();

    assertThat(result).containsExactly(mapped);
    verify(templateService).findAll();
  }

  @Test
  void addTemplateItem_delegatesToService() {
    UUID templateId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    ChecklistTemplateItemEntity itemEntity = new ChecklistTemplateItemEntity();
    itemEntity.setId(UUID.randomUUID());
    when(templateItemService.addTemplateItem(templateId, itemId, null)).thenReturn(itemEntity);
    ChecklistTemplateEntity template = new ChecklistTemplateEntity();
    template.setId(templateId);
    when(templateService.locate(templateId)).thenReturn(Optional.of(template));
    ChecklistTemplate mapped = ChecklistTemplate.newBuilder().id(templateId.toString()).name("t").build();
    when(templateMapper.map(template)).thenReturn(mapped);

    AddTemplateItemResponse result = service.addTemplateItem(templateId.toString(), itemId.toString(), null);

    assertThat(result.getTemplate()).isEqualTo(mapped);
    verify(templateItemService).addTemplateItem(templateId, itemId, null);
  }
}
