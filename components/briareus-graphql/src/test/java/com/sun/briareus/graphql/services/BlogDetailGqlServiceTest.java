package com.sun.briareus.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sun.briareus.codegen.types.BlogDetail;
import com.sun.briareus.codegen.types.BlogWithPropertiesInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.codegen.types.QuerySuccess;
import com.sun.briareus.codegen.types.StandardError;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.graphql.mappers.AttachedTextMapper;
import com.sun.briareus.graphql.mappers.BlogDetailMapper;
import com.sun.briareus.graphql.mappers.BlogGalleryItemMapper;
import com.sun.briareus.graphql.mappers.BlogPostMapper;
import com.sun.briareus.graphql.mappers.BlogPropertySetMapper;
import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.repository.PostRepository;
import com.sun.briareus.service.BlogPostTypeService;
import com.sun.briareus.service.BriareusService;
import com.sun.base.permify.PermifyClient;
import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.cerberus.repository.GalleryItemRepository;
import com.sun.cerberus.service.CerberusService;
import com.sun.gaia.model.PropertySetSchemaEntity;
import com.sun.gaia.repository.ObjectShareRepository;
import com.sun.gaia.service.PropertySetService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.repository.ReaderTextRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlogDetailGqlServiceTest {

  @Mock
  private BriareusService briareusService;

  @Mock
  private BlogPostTypeService blogPostTypeService;

  @Mock
  private BlogPostMapper blogPostMapper;

  @Mock
  private BlogDetailMapper blogDetailMapper;

  @Mock
  private BlogGalleryItemMapper blogGalleryItemMapper;

  @Mock
  private BlogPropertySetMapper blogPropertySetMapper;

  @Mock
  private AttachedTextMapper attachedTextMapper;

  @Mock
  private PropertySetService propertySetService;

  @Mock
  private GalleryItemRepository galleryItemRepository;

  @Mock
  private CerberusService cerberusService;

  @Mock
  private ReaderTextRepository readerTextRepository;

  @Mock
  private PostRepository postRepository;

  @Mock
  private PermifyClient permifyClient;

  @Mock
  private ObjectShareRepository objectShareRepository;

  @InjectMocks
  private BlogDetailGqlService service;

  @AfterEach
  void clear() {
    UserContextHolder.clear();
  }

  @Test
  void blogDetail_shouldReturnDetailWhenCanView() {
    UUID id = UUID.randomUUID();
    PostEntity post = new PostEntity();
    post.setId(id);
    post.setTitle("t");
    post.setContent("c");
    BlogPostTypeEntity type = new BlogPostTypeEntity();
    type.setName("BOT_FAQ");
    post.setType(type);
    post.setRemoteObject(List.of());
    BlogPost mapped = BlogPost.newBuilder().id(id.toString()).title("t").build();
    when(briareusService.locatePost(id)).thenReturn(Optional.of(post));
    when(blogPostMapper.map(post)).thenReturn(mapped);
    when(propertySetService.getSchemaEntity(any(), any())).thenReturn(Optional.empty());
    when(galleryItemRepository.findByRemoteObjectsIn(any(String[].class))).thenReturn(List.of());
    BlogDetail detail = BlogDetail.newBuilder().post(mapped).galleryItems(List.of()).attachedTexts(List.of()).linkedPosts(List.of()).build();
    when(blogDetailMapper.map(any(), any(), any(), any(), any())).thenReturn(detail);

    BlogDetail result = service.blogDetail(id.toString());

    assertThat(result.getPost().getTitle()).isEqualTo("t");
  }

  @Test
  void createBlogWithProperties_shouldReturnSuccess() {
    UUID typeId = UUID.randomUUID();
    BlogPostTypeEntity type = new BlogPostTypeEntity();
    type.setId(typeId);
    type.setName("REVIEW");
    when(blogPostTypeService.findById(typeId)).thenReturn(Optional.of(type));
    BlogWithPropertiesInput input = BlogWithPropertiesInput.newBuilder()
        .title("title")
        .content("content")
        .typeId(typeId.toString())
        .build();
    com.sun.briareus.codegen.types.BlogPostInput postInput = com.sun.briareus.codegen.types.BlogPostInput.newBuilder().content("content").typeId(typeId.toString()).build();
    when(blogPostMapper.toPostInput(input)).thenReturn(postInput);
    PostEntity mapped = new PostEntity();
    mapped.setTitle("title");
    mapped.setContent("content");
    when(blogPostMapper.mapInput(eq("title"), any())).thenReturn(mapped);
    PostEntity saved = new PostEntity();
    saved.setId(UUID.randomUUID());
    saved.setType(type);
    saved.setTitle("title");
    when(briareusService.save(any(PostEntity.class))).thenReturn(saved);

    QueryResult result = service.createBlogWithProperties(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
  }

  @Test
  void updateBlogWithProperties_shouldRejectTypeChange() {
    UUID postId = UUID.randomUUID();
    UUID oldTypeId = UUID.randomUUID();
    UUID newTypeId = UUID.randomUUID();
    BlogPostTypeEntity oldType = new BlogPostTypeEntity();
    oldType.setId(oldTypeId);
    oldType.setName("REVIEW");
    PostEntity post = new PostEntity();
    post.setId(postId);
    post.setTitle("t");
    post.setType(oldType);
    UUID viewer = UUID.randomUUID();
    post.setCreatedBy(viewer);
    UserContextHolder.setUserId(viewer);
    when(briareusService.locatePost(postId)).thenReturn(Optional.of(post));
    BlogWithPropertiesInput input = BlogWithPropertiesInput.newBuilder()
        .title("t2")
        .content("c2")
        .typeId(newTypeId.toString())
        .build();

    QueryResult result = service.updateBlogWithProperties(postId.toString(), input);

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).contains("type immutable");
  }
}
