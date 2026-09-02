package com.sun.briareus.graphql.mappers;

import com.sun.briareus.codegen.types.AttachedText;
import com.sun.briareus.codegen.types.BlogDetail;
import com.sun.briareus.codegen.types.BlogGalleryItem;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPropertySet;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps aggregated blog detail fields.
 */
@Component
public class BlogDetailMapper {

  /**
   * Maps aggregated fields to a detail.
   *
   * @param post the post
   * @param propertySet the property set, or null
   * @param galleryItems the gallery items
   * @param attachedTexts the attached texts
   * @param linkedPosts the linked posts
   * @return the detail
   */
  public BlogDetail map(BlogPost post, BlogPropertySet propertySet,
      List<BlogGalleryItem> galleryItems, List<AttachedText> attachedTexts,
      List<BlogPost> linkedPosts) {
    return BlogDetail.newBuilder()
        .post(post)
        .propertySet(propertySet)
        .galleryItems(galleryItems)
        .attachedTexts(attachedTexts)
        .linkedPosts(linkedPosts)
        .build();
  }
}
