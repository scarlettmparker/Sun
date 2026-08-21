package com.sun.echo.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
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
  void createTemplate_savesAndReturnsSuccess() {
    ChecklistTemplateEntity saved = new ChecklistTemplateEntity();
    saved.setId(UUID.randomUUID());
    when(templateService.save(any(ChecklistTemplateEntity.class))).thenReturn(saved);

    QueryResult result = service.createTemplate("Name", "desc", null);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
    verify(templateService).save(any(ChecklistTemplateEntity.class));
  }

  @Test
  void archiveTemplate_delegatesToService() {
    UUID id = UUID.randomUUID();
    ChecklistTemplateEntity archived = new ChecklistTemplateEntity();
    archived.setId(id);
    when(templateService.archive(id)).thenReturn(archived);

    QueryResult result = service.archiveTemplate(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
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

    QueryResult result = service.addTemplateItem(templateId.toString(), itemId.toString(), null);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(templateItemService).addTemplateItem(templateId, itemId, null);
  }
}
