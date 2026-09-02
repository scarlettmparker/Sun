package com.sun.briareus.graphql.resolvers;

import com.sun.briareus.codegen.types.BlogDetail;
import com.sun.briareus.codegen.types.BlogWithPropertiesInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.graphql.services.BlogDetailGqlService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@DgsComponent
public class BlogDetailDataFetcher {

  @Autowired
  private BlogDetailGqlService blogDetailGqlService;

  /**
   * Returns aggregated detail for a post.
   *
   * @param id the post id
   * @return the detail
   */
  @DgsData(parentType = "BlogQueries", field = "blogDetail")
  @PreAuthorize("@permissions.has('graphql.briareus.blogDetail')")
  public BlogDetail blogDetail(String id) {
    return blogDetailGqlService.blogDetail(id);
  }

  /**
   * Creates a blog post with property set values.
   *
   * @param input the input
   * @return the result
   */
  @DgsData(parentType = "BlogMutations", field = "createBlogWithProperties")
  @PreAuthorize("@permissions.has('graphql.briareus.createBlogWithProperties')")
  public QueryResult createBlogWithProperties(BlogWithPropertiesInput input) {
    return blogDetailGqlService.createBlogWithProperties(input);
  }

  /**
   * Updates a blog post and its property set.
   *
   * @param id the post id
   * @param input the input
   * @return the result
   */
  @DgsData(parentType = "BlogMutations", field = "updateBlogWithProperties")
  @PreAuthorize("@permissions.has('graphql.briareus.updateBlogWithProperties')")
  public QueryResult updateBlogWithProperties(String id, BlogWithPropertiesInput input) {
    return blogDetailGqlService.updateBlogWithProperties(id, input);
  }
}
