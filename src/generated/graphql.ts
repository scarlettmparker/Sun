/* eslint-disable */
import { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';
export type Maybe<T> = T | null;
export type InputMaybe<T> = T | null | undefined;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  Date: { input: any; output: any; }
};

export type BlogMutations = {
  __typename?: 'BlogMutations';
  createBlogPost?: Maybe<QueryResult>;
};


export type BlogMutationsCreateBlogPostArgs = {
  input?: InputMaybe<BlogPostInput>;
  title: Scalars['String']['input'];
};

export type BlogPost = {
  __typename?: 'BlogPost';
  content?: Maybe<Scalars['String']['output']>;
  createdAt?: Maybe<Scalars['Date']['output']>;
  id: Scalars['String']['output'];
  tags?: Maybe<Array<Scalars['String']['output']>>;
  title: Scalars['String']['output'];
  updatedAt?: Maybe<Scalars['Date']['output']>;
};

export type BlogPostInput = {
  content?: InputMaybe<Scalars['String']['input']>;
  tags?: InputMaybe<Array<Scalars['String']['input']>>;
};

export type BlogQueries = {
  __typename?: 'BlogQueries';
  listBlogPosts?: Maybe<Array<Maybe<BlogPost>>>;
  locateBlogPost?: Maybe<BlogPost>;
};


export type BlogQueriesLocateBlogPostArgs = {
  id: Scalars['ID']['input'];
};

export type GalleryItem = {
  __typename?: 'GalleryItem';
  content?: Maybe<Scalars['String']['output']>;
  createdAt?: Maybe<Scalars['Date']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  foreignObject?: Maybe<Array<Scalars['String']['output']>>;
  id: Scalars['String']['output'];
  imagePath?: Maybe<Scalars['String']['output']>;
  title: Scalars['String']['output'];
  updatedAt?: Maybe<Scalars['Date']['output']>;
};

export type GalleryItemInput = {
  content?: InputMaybe<Scalars['String']['input']>;
  description?: InputMaybe<Scalars['String']['input']>;
  foreignObject?: InputMaybe<Array<Scalars['String']['input']>>;
  imagePath?: InputMaybe<Scalars['String']['input']>;
  title: Scalars['String']['input'];
};

export type GalleryMutations = {
  __typename?: 'GalleryMutations';
  create?: Maybe<QueryResult>;
};


export type GalleryMutationsCreateArgs = {
  input?: InputMaybe<GalleryItemInput>;
};

export type GalleryQueries = {
  __typename?: 'GalleryQueries';
  list?: Maybe<Array<Maybe<GalleryItem>>>;
  listByForeignObjects?: Maybe<Array<Maybe<GalleryItem>>>;
  locate?: Maybe<GalleryItem>;
};


export type GalleryQueriesListByForeignObjectsArgs = {
  ids: Array<Scalars['String']['input']>;
};


export type GalleryQueriesLocateArgs = {
  id: Scalars['ID']['input'];
};

export type Mutation = {
  __typename?: 'Mutation';
  blogMutations: BlogMutations;
  galleryMutations: GalleryMutations;
};

export type Query = {
  __typename?: 'Query';
  blogQueries: BlogQueries;
  galleryQueries: GalleryQueries;
  stemPlayerQueries: StemPlayerQueries;
};

export type QueryResult = QuerySuccess | StandardError;

export type QuerySuccess = {
  __typename?: 'QuerySuccess';
  id?: Maybe<Scalars['ID']['output']>;
  message: Scalars['String']['output'];
};

export type Song = {
  __typename?: 'Song';
  id: Scalars['String']['output'];
  name?: Maybe<Scalars['String']['output']>;
  path: Scalars['String']['output'];
  stems?: Maybe<Array<Maybe<Stem>>>;
};

export type StandardError = {
  __typename?: 'StandardError';
  message: Scalars['String']['output'];
};

export type Stem = {
  __typename?: 'Stem';
  name?: Maybe<Scalars['String']['output']>;
  path: Scalars['String']['output'];
};

export type StemPlayerQueries = {
  __typename?: 'StemPlayerQueries';
  list?: Maybe<Array<Maybe<Song>>>;
  locate?: Maybe<Song>;
};


export type StemPlayerQueriesLocateArgs = {
  id: Scalars['ID']['input'];
};

export type CreateBlogPostMutationVariables = Exact<{
  title: Scalars['String']['input'];
  input: BlogPostInput;
}>;


export type CreateBlogPostMutation = { __typename?: 'Mutation', blogMutations: { __typename?: 'BlogMutations', createBlogPost?:
      | { __typename: 'QuerySuccess', message: string, id?: string | null }
      | { __typename: 'StandardError', message: string }
     | null } };

export type ListBlogPostsQueryVariables = Exact<{ [key: string]: never; }>;


export type ListBlogPostsQuery = { __typename?: 'Query', blogQueries: { __typename?: 'BlogQueries', listBlogPosts?: Array<{ __typename?: 'BlogPost', id: string, title: string, createdAt?: any | null, tags?: Array<string> | null } | null> | null } };

export type LocateBlogPostQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type LocateBlogPostQuery = { __typename?: 'Query', blogQueries: { __typename?: 'BlogQueries', locateBlogPost?: { __typename?: 'BlogPost', title: string, content?: string | null, tags?: Array<string> | null, createdAt?: any | null, updatedAt?: any | null } | null } };

export type CreateGalleryItemMutationVariables = Exact<{
  input: GalleryItemInput;
}>;


export type CreateGalleryItemMutation = { __typename?: 'Mutation', galleryMutations: { __typename?: 'GalleryMutations', create?:
      | { __typename?: 'QuerySuccess', message: string, id?: string | null }
      | { __typename?: 'StandardError', message: string }
     | null } };

export type ListGalleryItemsQueryVariables = Exact<{ [key: string]: never; }>;


export type ListGalleryItemsQuery = { __typename?: 'Query', galleryQueries: { __typename?: 'GalleryQueries', list?: Array<{ __typename?: 'GalleryItem', id: string, title: string, description?: string | null, content?: string | null, imagePath?: string | null, foreignObject?: Array<string> | null, createdAt?: any | null, updatedAt?: any | null } | null> | null } };

export type ListGalleryItemsByForeignObjectsQueryVariables = Exact<{
  ids: Array<Scalars['String']['input']> | Scalars['String']['input'];
}>;


export type ListGalleryItemsByForeignObjectsQuery = { __typename?: 'Query', galleryQueries: { __typename?: 'GalleryQueries', listByForeignObjects?: Array<{ __typename?: 'GalleryItem', id: string, title: string, description?: string | null, content?: string | null, imagePath?: string | null, foreignObject?: Array<string> | null, createdAt?: any | null, updatedAt?: any | null } | null> | null } };

export type LocateGalleryItemQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type LocateGalleryItemQuery = { __typename?: 'Query', galleryQueries: { __typename?: 'GalleryQueries', locate?: { __typename?: 'GalleryItem', id: string, title: string, description?: string | null, content?: string | null, imagePath?: string | null, foreignObject?: Array<string> | null, createdAt?: any | null, updatedAt?: any | null } | null } };

export type ListSongsQueryVariables = Exact<{ [key: string]: never; }>;


export type ListSongsQuery = { __typename?: 'Query', stemPlayerQueries: { __typename?: 'StemPlayerQueries', list?: Array<{ __typename?: 'Song', id: string, name?: string | null } | null> | null } };

export type LocateSongQueryVariables = Exact<{
  id: Scalars['ID']['input'];
}>;


export type LocateSongQuery = { __typename?: 'Query', stemPlayerQueries: { __typename?: 'StemPlayerQueries', locate?: { __typename?: 'Song', name?: string | null, path: string, stems?: Array<{ __typename?: 'Stem', path: string, name?: string | null } | null> | null } | null } };


export const CreateBlogPostDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createBlogPost"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"title"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"input"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"BlogPostInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"blogMutations"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createBlogPost"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"title"},"value":{"kind":"Variable","name":{"kind":"Name","value":"title"}}},{"kind":"Argument","name":{"kind":"Name","value":"input"},"value":{"kind":"Variable","name":{"kind":"Name","value":"input"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"InlineFragment","typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"QuerySuccess"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"__typename"}},{"kind":"Field","name":{"kind":"Name","value":"message"}},{"kind":"Field","name":{"kind":"Name","value":"id"}}]}},{"kind":"InlineFragment","typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"StandardError"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"__typename"}},{"kind":"Field","name":{"kind":"Name","value":"message"}}]}}]}}]}}]}}]} as unknown as DocumentNode<CreateBlogPostMutation, CreateBlogPostMutationVariables>;
export const ListBlogPostsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"listBlogPosts"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"blogQueries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listBlogPosts"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"createdAt"}},{"kind":"Field","name":{"kind":"Name","value":"tags"}}]}}]}}]}}]} as unknown as DocumentNode<ListBlogPostsQuery, ListBlogPostsQueryVariables>;
export const LocateBlogPostDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"locateBlogPost"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"ID"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"blogQueries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locateBlogPost"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"content"}},{"kind":"Field","name":{"kind":"Name","value":"tags"}},{"kind":"Field","name":{"kind":"Name","value":"createdAt"}},{"kind":"Field","name":{"kind":"Name","value":"updatedAt"}}]}}]}}]}}]} as unknown as DocumentNode<LocateBlogPostQuery, LocateBlogPostQueryVariables>;
export const CreateGalleryItemDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"createGalleryItem"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"input"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"GalleryItemInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"galleryMutations"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"create"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"input"},"value":{"kind":"Variable","name":{"kind":"Name","value":"input"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"InlineFragment","typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"QuerySuccess"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"message"}},{"kind":"Field","name":{"kind":"Name","value":"id"}}]}},{"kind":"InlineFragment","typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"StandardError"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"message"}}]}}]}}]}}]}}]} as unknown as DocumentNode<CreateGalleryItemMutation, CreateGalleryItemMutationVariables>;
export const ListGalleryItemsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"listGalleryItems"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"galleryQueries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"list"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"content"}},{"kind":"Field","name":{"kind":"Name","value":"imagePath"}},{"kind":"Field","name":{"kind":"Name","value":"foreignObject"}},{"kind":"Field","name":{"kind":"Name","value":"createdAt"}},{"kind":"Field","name":{"kind":"Name","value":"updatedAt"}}]}}]}}]}}]} as unknown as DocumentNode<ListGalleryItemsQuery, ListGalleryItemsQueryVariables>;
export const ListGalleryItemsByForeignObjectsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"listGalleryItemsByForeignObjects"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"ids"}},"type":{"kind":"NonNullType","type":{"kind":"ListType","type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"galleryQueries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listByForeignObjects"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"ids"},"value":{"kind":"Variable","name":{"kind":"Name","value":"ids"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"content"}},{"kind":"Field","name":{"kind":"Name","value":"imagePath"}},{"kind":"Field","name":{"kind":"Name","value":"foreignObject"}},{"kind":"Field","name":{"kind":"Name","value":"createdAt"}},{"kind":"Field","name":{"kind":"Name","value":"updatedAt"}}]}}]}}]}}]} as unknown as DocumentNode<ListGalleryItemsByForeignObjectsQuery, ListGalleryItemsByForeignObjectsQueryVariables>;
export const LocateGalleryItemDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"locateGalleryItem"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"ID"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"galleryQueries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locate"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"content"}},{"kind":"Field","name":{"kind":"Name","value":"imagePath"}},{"kind":"Field","name":{"kind":"Name","value":"foreignObject"}},{"kind":"Field","name":{"kind":"Name","value":"createdAt"}},{"kind":"Field","name":{"kind":"Name","value":"updatedAt"}}]}}]}}]}}]} as unknown as DocumentNode<LocateGalleryItemQuery, LocateGalleryItemQueryVariables>;
export const ListSongsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"listSongs"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"stemPlayerQueries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"list"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}}]}}]}}]}}]} as unknown as DocumentNode<ListSongsQuery, ListSongsQueryVariables>;
export const LocateSongDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"locateSong"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"ID"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"stemPlayerQueries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locate"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"path"}},{"kind":"Field","name":{"kind":"Name","value":"stems"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"path"}},{"kind":"Field","name":{"kind":"Name","value":"name"}}]}}]}}]}}]}}]} as unknown as DocumentNode<LocateSongQuery, LocateSongQueryVariables>;