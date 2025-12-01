package com.sun.briareus.graphql.resolvers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.sun.briareus.graphql.services.BlogGraphQLService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogQueries;

@DgsComponent
public class BlogDataFetcher {

  @Autowired
  private BlogGraphQLService blogGraphQLService;

  /**
   * Provides the blog queries object.
   *
   * @return a new BlogQueries instance
   */
  @DgsData(parentType = "Query", field = "blogQueries")
  public BlogQueries getBlogQueries() {
    return BlogQueries.newBuilder().build();
  }

  /**
   * Retrieves all blog posts for the blogsite.
   *
   * @return a list of BlogPost objects
   */
  @DgsData(parentType = "BlogQueries", field = "listBlogPosts")
  public List<BlogPost> listBlogPosts() {
    return blogGraphQLService.listBlogPosts();
  }

  /**
   * Retrieves a specific blog post by ID.
   *
   * @param id the blog post ID
   * @return the BlogPost object
   */
  @DgsData(parentType = "BlogQueries", field = "locateBlogPost")
  public BlogPost locateBlogPost(String id) {
    return blogGraphQLService.locateBlogPost(id);
  }
}
