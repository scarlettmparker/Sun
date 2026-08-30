package com.sun.briareus.graphql.resolvers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sun.briareus.graphql.services.BlogGraphQLService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostType;
import com.sun.briareus.codegen.types.BlogQueries;
import com.sun.briareus.codegen.types.BlogMutations;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.codegen.types.IngestBlogInput;
import com.sun.briareus.codegen.types.PagedBlogPosts;
import com.sun.briareus.codegen.types.PaginationInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.codegen.types.ReaderText;

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
   * Retrieves a page of blog posts for the blogsite.
   *
   * @param pagination the pagination and filter input
   * @return the matching page of BlogPost objects
   */
  @DgsData(parentType = "BlogQueries", field = "listBlogPosts")
  @PreAuthorize("@permissions.has('graphql.briareus.listBlogPosts')")
  public PagedBlogPosts listBlogPosts(PaginationInput pagination) {
    return blogGraphQLService.listBlogPosts(pagination);
  }

  /**
   * Retrieves a specific blog post by ID.
   *
   * @param id the blog post ID
   * @return the BlogPost object
   */
  @DgsData(parentType = "BlogQueries", field = "locateBlogPost")
  @PreAuthorize("@permissions.has('graphql.briareus.locateBlogPost')")
  public BlogPost locateBlogPost(String id) {
    return blogGraphQLService.locateBlogPost(id);
  }

  /**
   * Retrieves blog posts that reference any of the given remote-object ids.
   *
   * @param ids the remote-object ids to match
   * @return a list of BlogPost objects
   */
  @DgsData(parentType = "BlogQueries", field = "listByRemoteObjects")
  @PreAuthorize("@permissions.has('graphql.briareus.listByRemoteObjects')")
  public List<BlogPost> listByRemoteObjects(List<String> ids) {
    return blogGraphQLService.listByRemoteObjects(ids);
  }

  /**
   * Lists every blog post type.
   *
   * @return the post types
   */
  @DgsData(parentType = "BlogQueries", field = "blogPostTypes")
  @PreAuthorize("@permissions.has('graphql.briareus.blogPostTypes')")
  public List<BlogPostType> blogPostTypes() {
    return blogGraphQLService.blogPostTypes();
  }

  /**
   * Provides the blog mutations object.
   *
   * @return a new BlogMutations instance
   */
  @DgsData(parentType = "Mutation", field = "blogMutations")
  public BlogMutations getBlogMutations() {
    return BlogMutations.newBuilder().build();
  }

  /**
   * Creates a new blog post.
   *
   * @param title the title of the blog post
   * @param input the input data for the blog post
   * @return QueryResult indicating success or error
   */
  @DgsData(parentType = "BlogMutations", field = "createBlogPost")
  @PreAuthorize("@permissions.has('graphql.briareus.createBlogPost')")
  public QueryResult createBlogPost(String title, BlogPostInput input) {
    return blogGraphQLService.createBlogPost(title, input);
  }

  /**
   * Creates a blog post type with a unique name.
   *
   * @param name the type name
   * @param description an optional description
   * @return QueryResult indicating success or error
   */
  @DgsData(parentType = "BlogMutations", field = "createBlogPostType")
  @PreAuthorize("@permissions.has('graphql.briareus.createBlogPostType')")
  public QueryResult createBlogPostType(String name, String description) {
    return blogGraphQLService.createBlogPostType(name, description);
  }

  /**
   * Lists children of a parent post with pagination.
   *
   * @param parentId the parent post id
   * @param pagination the pagination input
   * @return the matching page
   */
  @DgsData(parentType = "BlogQueries", field = "children")
  @PreAuthorize("@permissions.has('graphql.briareus.children')")
  public PagedBlogPosts children(String parentId, PaginationInput pagination) {
    return blogGraphQLService.children(parentId, pagination);
  }

  /**
   * Resolves the parent of a blog post.
   *
   * @param env the data-fetching environment
   * @return the parent post or null
   */
  @DgsData(parentType = "BlogPost", field = "parent")
  public BlogPost parent(DgsDataFetchingEnvironment env) {
    BlogPost source = env.getSource();
    if (source == null || source.getParentId() == null) {
      return null;
    }
    try {
      return blogGraphQLService.locateBlogPost(source.getParentId());
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Resolves attached reader texts for a blog post.
   *
   * @param env the data-fetching environment
   * @return the attached texts
   */
  @DgsData(parentType = "BlogPost", field = "attachedTexts")
  @PreAuthorize("@permissions.has('graphql.briareus.attachedTexts')")
  public List<ReaderText> attachedTexts(DgsDataFetchingEnvironment env) {
    BlogPost source = env.getSource();
    if (source == null || source.getId() == null) {
      return List.of();
    }
    return blogGraphQLService.attachedTexts(source.getId());
  }

  /**
   * Appends a remote-object edge to a post.
   *
   * @param postId the post id
   * @param target the remote-object string
   * @return the outcome
   */
  @DgsData(parentType = "BlogMutations", field = "addRemoteObject")
  @PreAuthorize("@permissions.has('graphql.briareus.addRemoteObject')")
  public QueryResult addRemoteObject(String postId, String target) {
    return blogGraphQLService.addRemoteObject(postId, target);
  }

  /**
   * Removes a remote-object edge from a post.
   *
   * @param postId the post id
   * @param target the remote-object string
   * @return the outcome
   */
  @DgsData(parentType = "BlogMutations", field = "removeRemoteObject")
  @PreAuthorize("@permissions.has('graphql.briareus.removeRemoteObject')")
  public QueryResult removeRemoteObject(String postId, String target) {
    return blogGraphQLService.removeRemoteObject(postId, target);
  }

  /**
   * Ingests a blog from a source.
   *
   * @param input the ingest input
   * @return the outcome
   */
  @DgsData(parentType = "BlogMutations", field = "ingestBlogFromSource")
  @PreAuthorize("@permissions.has('graphql.briareus.ingestBlogFromSource')")
  public QueryResult ingestBlogFromSource(IngestBlogInput input) {
    return blogGraphQLService.ingestBlogFromSource(input);
  }
}
