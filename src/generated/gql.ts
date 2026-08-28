/* eslint-disable */
import * as types from './graphql';
import { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';

/**
 * Map of all GraphQL operations in the project.
 *
 * This map has several performance disadvantages:
 * 1. It is not tree-shakeable, so it will include all operations in the project.
 * 2. It is not minifiable, so the string of a GraphQL query will be multiple times inside the bundle.
 * 3. It does not support dead code elimination, so it will add unused operations.
 *
 * Therefore it is highly recommended to use the babel or swc plugin for production.
 * Learn more about it here: https://the-guild.dev/graphql/codegen/plugins/presets/preset-client#reducing-bundle-size
 */
type Documents = {
    "query blogPostTypes {\n  blogQueries {\n    blogPostTypes {\n      id\n      name\n      description\n    }\n  }\n}": typeof types.BlogPostTypesDocument,
    "query children($parentId: ID!, $pagination: PaginationInput) {\n  blogQueries {\n    children(parentId: $parentId, pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}": typeof types.ChildrenDocument,
    "mutation createBlogPost($title: String!, $input: BlogPostInput!) {\n  blogMutations {\n    createBlogPost(title: $title, input: $input) {\n      ... on QuerySuccess {\n        __typename\n        message\n        id\n      }\n      ... on StandardError {\n        __typename\n        message\n      }\n    }\n  }\n}": typeof types.CreateBlogPostDocument,
    "query listBlogPosts($pagination: PaginationInput) {\n  blogQueries {\n    listBlogPosts(pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}": typeof types.ListBlogPostsDocument,
    "query locateBlogPost($id: ID!) {\n  blogQueries {\n    locateBlogPost(id: $id) {\n      id\n      title\n      content\n      tags\n      remoteObject\n      language\n      parentId\n      parent {\n        id\n        title\n      }\n      type {\n        id\n        name\n      }\n      createdAt\n      updatedAt\n    }\n  }\n}": typeof types.LocateBlogPostDocument,
    "mutation Login($input: LoginInput!) {\n  gaiaMutations {\n    login(input: $input) {\n      token\n    }\n  }\n}": typeof types.LoginDocument,
    "mutation logout {\n  gaiaMutations {\n    logout {\n      __typename\n      ... on QuerySuccess {\n        message\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}": typeof types.LogoutDocument,
    "query me {\n  gaiaQueries {\n    me {\n      id\n      username\n      personId\n      status\n      createdAt\n      updatedAt\n    }\n  }\n}": typeof types.MeDocument,
    "query myRoles {\n  gaiaQueries {\n    myRoles\n  }\n}": typeof types.MyRolesDocument,
    "mutation createGalleryItem($input: GalleryItemInput!) {\n  galleryMutations {\n    create(input: $input) {\n      ... on QuerySuccess {\n        message\n        id\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}": typeof types.CreateGalleryItemDocument,
    "query listGalleryItems {\n  galleryQueries {\n    list {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}": typeof types.ListGalleryItemsDocument,
    "query listGalleryItemsByRemoteObjects($ids: [String!]!) {\n  galleryQueries {\n    listByRemoteObjects(ids: $ids) {\n      id\n      title\n      imagePath\n    }\n  }\n}": typeof types.ListGalleryItemsByRemoteObjectsDocument,
    "query locateGalleryItem($id: ID!) {\n  galleryQueries {\n    locate(id: $id) {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}": typeof types.LocateGalleryItemDocument,
    "query hubRegistry {\n  gaiaQueries {\n    hubRegistry {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}": typeof types.HubRegistryDocument,
    "mutation saveRegistry($input: HubRegistryInput!) {\n  gaiaMutations {\n    saveRegistry(input: $input) {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}": typeof types.SaveRegistryDocument,
    "query listSongs {\n  stemPlayerQueries {\n    list {\n      id\n      name\n    }\n  }\n}": typeof types.ListSongsDocument,
    "query locateSong($id: ID!) {\n  stemPlayerQueries {\n    locate(id: $id) {\n      name\n      path\n      stems {\n        path\n        name\n      }\n    }\n  }\n}": typeof types.LocateSongDocument,
};
const documents: Documents = {
    "query blogPostTypes {\n  blogQueries {\n    blogPostTypes {\n      id\n      name\n      description\n    }\n  }\n}": types.BlogPostTypesDocument,
    "query children($parentId: ID!, $pagination: PaginationInput) {\n  blogQueries {\n    children(parentId: $parentId, pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}": types.ChildrenDocument,
    "mutation createBlogPost($title: String!, $input: BlogPostInput!) {\n  blogMutations {\n    createBlogPost(title: $title, input: $input) {\n      ... on QuerySuccess {\n        __typename\n        message\n        id\n      }\n      ... on StandardError {\n        __typename\n        message\n      }\n    }\n  }\n}": types.CreateBlogPostDocument,
    "query listBlogPosts($pagination: PaginationInput) {\n  blogQueries {\n    listBlogPosts(pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}": types.ListBlogPostsDocument,
    "query locateBlogPost($id: ID!) {\n  blogQueries {\n    locateBlogPost(id: $id) {\n      id\n      title\n      content\n      tags\n      remoteObject\n      language\n      parentId\n      parent {\n        id\n        title\n      }\n      type {\n        id\n        name\n      }\n      createdAt\n      updatedAt\n    }\n  }\n}": types.LocateBlogPostDocument,
    "mutation Login($input: LoginInput!) {\n  gaiaMutations {\n    login(input: $input) {\n      token\n    }\n  }\n}": types.LoginDocument,
    "mutation logout {\n  gaiaMutations {\n    logout {\n      __typename\n      ... on QuerySuccess {\n        message\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}": types.LogoutDocument,
    "query me {\n  gaiaQueries {\n    me {\n      id\n      username\n      personId\n      status\n      createdAt\n      updatedAt\n    }\n  }\n}": types.MeDocument,
    "query myRoles {\n  gaiaQueries {\n    myRoles\n  }\n}": types.MyRolesDocument,
    "mutation createGalleryItem($input: GalleryItemInput!) {\n  galleryMutations {\n    create(input: $input) {\n      ... on QuerySuccess {\n        message\n        id\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}": types.CreateGalleryItemDocument,
    "query listGalleryItems {\n  galleryQueries {\n    list {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}": types.ListGalleryItemsDocument,
    "query listGalleryItemsByRemoteObjects($ids: [String!]!) {\n  galleryQueries {\n    listByRemoteObjects(ids: $ids) {\n      id\n      title\n      imagePath\n    }\n  }\n}": types.ListGalleryItemsByRemoteObjectsDocument,
    "query locateGalleryItem($id: ID!) {\n  galleryQueries {\n    locate(id: $id) {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}": types.LocateGalleryItemDocument,
    "query hubRegistry {\n  gaiaQueries {\n    hubRegistry {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}": types.HubRegistryDocument,
    "mutation saveRegistry($input: HubRegistryInput!) {\n  gaiaMutations {\n    saveRegistry(input: $input) {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}": types.SaveRegistryDocument,
    "query listSongs {\n  stemPlayerQueries {\n    list {\n      id\n      name\n    }\n  }\n}": types.ListSongsDocument,
    "query locateSong($id: ID!) {\n  stemPlayerQueries {\n    locate(id: $id) {\n      name\n      path\n      stems {\n        path\n        name\n      }\n    }\n  }\n}": types.LocateSongDocument,
};

/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 *
 *
 * @example
 * ```ts
 * const query = graphql(`query GetUser($id: ID!) { user(id: $id) { name } }`);
 * ```
 *
 * The query argument is unknown!
 * Please regenerate the types.
 */
export function graphql(source: string): unknown;

/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query blogPostTypes {\n  blogQueries {\n    blogPostTypes {\n      id\n      name\n      description\n    }\n  }\n}"): (typeof documents)["query blogPostTypes {\n  blogQueries {\n    blogPostTypes {\n      id\n      name\n      description\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query children($parentId: ID!, $pagination: PaginationInput) {\n  blogQueries {\n    children(parentId: $parentId, pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}"): (typeof documents)["query children($parentId: ID!, $pagination: PaginationInput) {\n  blogQueries {\n    children(parentId: $parentId, pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "mutation createBlogPost($title: String!, $input: BlogPostInput!) {\n  blogMutations {\n    createBlogPost(title: $title, input: $input) {\n      ... on QuerySuccess {\n        __typename\n        message\n        id\n      }\n      ... on StandardError {\n        __typename\n        message\n      }\n    }\n  }\n}"): (typeof documents)["mutation createBlogPost($title: String!, $input: BlogPostInput!) {\n  blogMutations {\n    createBlogPost(title: $title, input: $input) {\n      ... on QuerySuccess {\n        __typename\n        message\n        id\n      }\n      ... on StandardError {\n        __typename\n        message\n      }\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query listBlogPosts($pagination: PaginationInput) {\n  blogQueries {\n    listBlogPosts(pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}"): (typeof documents)["query listBlogPosts($pagination: PaginationInput) {\n  blogQueries {\n    listBlogPosts(pagination: $pagination) {\n      items {\n        id\n        title\n        content\n        tags\n        remoteObject\n        language\n        parentId\n        type {\n          id\n          name\n        }\n        createdAt\n        updatedAt\n      }\n      pageInfo {\n        page\n        size\n        totalPages\n        totalCount\n        hasNextPage\n        hasPreviousPage\n      }\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query locateBlogPost($id: ID!) {\n  blogQueries {\n    locateBlogPost(id: $id) {\n      id\n      title\n      content\n      tags\n      remoteObject\n      language\n      parentId\n      parent {\n        id\n        title\n      }\n      type {\n        id\n        name\n      }\n      createdAt\n      updatedAt\n    }\n  }\n}"): (typeof documents)["query locateBlogPost($id: ID!) {\n  blogQueries {\n    locateBlogPost(id: $id) {\n      id\n      title\n      content\n      tags\n      remoteObject\n      language\n      parentId\n      parent {\n        id\n        title\n      }\n      type {\n        id\n        name\n      }\n      createdAt\n      updatedAt\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "mutation Login($input: LoginInput!) {\n  gaiaMutations {\n    login(input: $input) {\n      token\n    }\n  }\n}"): (typeof documents)["mutation Login($input: LoginInput!) {\n  gaiaMutations {\n    login(input: $input) {\n      token\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "mutation logout {\n  gaiaMutations {\n    logout {\n      __typename\n      ... on QuerySuccess {\n        message\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}"): (typeof documents)["mutation logout {\n  gaiaMutations {\n    logout {\n      __typename\n      ... on QuerySuccess {\n        message\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query me {\n  gaiaQueries {\n    me {\n      id\n      username\n      personId\n      status\n      createdAt\n      updatedAt\n    }\n  }\n}"): (typeof documents)["query me {\n  gaiaQueries {\n    me {\n      id\n      username\n      personId\n      status\n      createdAt\n      updatedAt\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query myRoles {\n  gaiaQueries {\n    myRoles\n  }\n}"): (typeof documents)["query myRoles {\n  gaiaQueries {\n    myRoles\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "mutation createGalleryItem($input: GalleryItemInput!) {\n  galleryMutations {\n    create(input: $input) {\n      ... on QuerySuccess {\n        message\n        id\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}"): (typeof documents)["mutation createGalleryItem($input: GalleryItemInput!) {\n  galleryMutations {\n    create(input: $input) {\n      ... on QuerySuccess {\n        message\n        id\n      }\n      ... on StandardError {\n        message\n      }\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query listGalleryItems {\n  galleryQueries {\n    list {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}"): (typeof documents)["query listGalleryItems {\n  galleryQueries {\n    list {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query listGalleryItemsByRemoteObjects($ids: [String!]!) {\n  galleryQueries {\n    listByRemoteObjects(ids: $ids) {\n      id\n      title\n      imagePath\n    }\n  }\n}"): (typeof documents)["query listGalleryItemsByRemoteObjects($ids: [String!]!) {\n  galleryQueries {\n    listByRemoteObjects(ids: $ids) {\n      id\n      title\n      imagePath\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query locateGalleryItem($id: ID!) {\n  galleryQueries {\n    locate(id: $id) {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}"): (typeof documents)["query locateGalleryItem($id: ID!) {\n  galleryQueries {\n    locate(id: $id) {\n      id\n      title\n      description\n      content\n      imagePath\n      remoteObject\n      createdAt\n      updatedAt\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query hubRegistry {\n  gaiaQueries {\n    hubRegistry {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}"): (typeof documents)["query hubRegistry {\n  gaiaQueries {\n    hubRegistry {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "mutation saveRegistry($input: HubRegistryInput!) {\n  gaiaMutations {\n    saveRegistry(input: $input) {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}"): (typeof documents)["mutation saveRegistry($input: HubRegistryInput!) {\n  gaiaMutations {\n    saveRegistry(input: $input) {\n      mode\n      apps {\n        key\n        name\n        dir\n        devPort\n        prodPort\n        url\n        description\n        enabled\n        self\n      }\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query listSongs {\n  stemPlayerQueries {\n    list {\n      id\n      name\n    }\n  }\n}"): (typeof documents)["query listSongs {\n  stemPlayerQueries {\n    list {\n      id\n      name\n    }\n  }\n}"];
/**
 * The graphql function is used to parse GraphQL queries into a document that can be used by GraphQL clients.
 */
export function graphql(source: "query locateSong($id: ID!) {\n  stemPlayerQueries {\n    locate(id: $id) {\n      name\n      path\n      stems {\n        path\n        name\n      }\n    }\n  }\n}"): (typeof documents)["query locateSong($id: ID!) {\n  stemPlayerQueries {\n    locate(id: $id) {\n      name\n      path\n      stems {\n        path\n        name\n      }\n    }\n  }\n}"];

export function graphql(source: string) {
  return (documents as any)[source] ?? {};
}

export type DocumentType<TDocumentNode extends DocumentNode<any, any>> = TDocumentNode extends DocumentNode<  infer TType,  any>  ? TType  : never;