# Sun GraphQL Architecture

This document outlines the architecture for the Sun GraphQL backend, a modular Spring Boot application that provides GraphQL API access to stem-separated music tracks stored in PostgreSQL.

## Overview

The Sun GraphQL is built using a clean architecture approach with four main modules:

1. **SunService** – Shared infrastructure and generic data access patterns
2. **ApolloService** – Apollo-specific domain logic and data models
3. **ApolloGraphQL** – GraphQL API layer with resolvers and mappers
4. **SunGraphQL** – Composition layer that combines all GraphQL components

## Architecture Principles

- **Separation of Concerns**: Each module has a single responsibility
- **Dependency Inversion**: Higher-level modules depend on abstractions, not concretions
- **Domain-Driven Design**: Business logic is encapsulated in the service layer
- **Clean Architecture**: Dependencies flow inward toward the domain

## Module Architecture

### 1. SunService Module

**Purpose**: Provides reusable infrastructure for data access and service operations.

**Contents**:

- `BaseEntity`: Abstract base class with UUID primary key
- `BaseRepository<T>`: Generic Spring Data JPA repository interface
- `BaseService<T>`: Generic service class with CRUD operations

**Key Features**:

- UUID-based entity identification
- Generic repository pattern
- Transactional service operations
- Extension points for domain-specific logic

### 2. ApolloService Module

**Purpose**: Implements Apollo-specific business logic and data persistence.

**Contents**:

- `Song` & `Stem` JPA entities with PostgreSQL mappings
- `SongRepository`: Extends BaseRepository for song queries
- `ApolloService`: Extends BaseService with Apollo-specific operations

**Key Features**:

- JPA entity relationships (Song ↔ Stem)
- PostgreSQL-specific mappings and constraints
- Domain validation and business rules
- Repository query methods

### 3. ApolloGraphQL Module

**Purpose**: Implements Apollo-specific GraphQL resolvers and mappers.

**Contents**:

- GraphQL schema definition (`schema.graphqls`)
- `StemPlayerQueryResolver`: DGS data fetchers
- `SongMapper`: Domain-to-GraphQL type conversion

**Key Features**:

- Netflix DGS framework integration
- Type-safe GraphQL schema
- Automatic code generation from schema
- Clean separation of GraphQL and domain concerns

### 4. SunGraphQL Module

**Purpose**: Composes all GraphQL components into a single runnable application.

**Contents**:

- `SunGraphQLApplication`: Main Spring Boot application class

**Key Features**:

- Composition of all GraphQL modules
- Single entry point for the GraphQL server
- No additional logic - purely composes existing components

**Purpose**: Exposes Apollo domain functionality through GraphQL API.

**Contents**:

- GraphQL schema definition (`schema.graphqls`)
- `StemPlayerQueryResolver`: DGS data fetchers
- `SongMapper`: Domain-to-GraphQL type conversion

**Key Features**:

- Netflix DGS framework integration
- Type-safe GraphQL schema
- Automatic code generation from schema
- Clean separation of GraphQL and domain concerns

## Data Flow

```
GraphQL Query → SunGraphQLApplication → ApolloGraphQL → StemPlayerQueryResolver → ApolloService → SongRepository → PostgreSQL
                      ↓
                SongMapper → GraphQL Types
```

## Database Schema

### Songs Table

```sql
CREATE TABLE songs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT
);
```

### Stems Table

```sql
CREATE TABLE stems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    song_id UUID REFERENCES songs(id),
    file_path TEXT NOT NULL,
    name TEXT
);
```

## GraphQL Schema

```graphql
type Song {
  id: String!
  name: String
  stems: [Stem]
}

type Stem {
  filePath: String!
  name: String
}

type StemPlayerQueries {
  listSongs: [Song]
}

type Query {
  stemPlayerQueries: StemPlayerQueries!
}
```

## Project Structure

```
apollo-stem-player/
├── sun-service/                     # Shared infrastructure
│   └── src/main/java/com/sun/base/
│       ├── model/BaseEntity.java
│       ├── repository/BaseRepository.java
│       └── service/BaseService.java
├── apollo-service/                  # Apollo domain logic
│   └── src/main/java/com/sun/apollo/
│       ├── model/
│       │   ├── Song.java
│       │   └── Stem.java
│       ├── repository/SongRepository.java
│       └── service/ApolloService.java
├── apollo-graphql/                  # GraphQL API layer
│   ├── src/main/resources/schema.graphqls
│   └── src/main/java/com/sun/apollo/graphql/
│       ├── resolvers/StemPlayerQueryResolver.java
│       └── mappers/SongMapper.java
├── sun-graphql/                     # GraphQL composition layer
│   └── src/main/java/com/sun/graphql/
│       └── SunGraphQLApplication.java
├── settings.gradle                 # Multi-module configuration
└── CODING_STYLE_GUIDE.md          # Development standards
```

## Dependency Management

- **sun-graphql** depends on **apollo-graphql**
- **apollo-graphql** depends on **apollo-service**
- **apollo-service** depends on **sun-service**
- Dependencies flow inward, never outward
- Spring Boot manages transitive dependencies

## Development Workflow

1. **Database Setup**: Create PostgreSQL database and run schema scripts
2. **Service Development**: Implement business logic in ApolloService
3. **GraphQL Integration**: Add resolvers and update schema as needed
4. **Composition**: Use SunGraphQL to compose all GraphQL components
5. **Testing**: Unit tests for services, integration tests for GraphQL

## Configuration

Application configuration is externalized in `application.properties`:

- Database connection settings
- JPA/Hibernate properties
- GraphQL endpoint configuration

## Deployment

The application is packaged as a Spring Boot executable JAR with embedded Tomcat server, ready for deployment to any servlet container or cloud platform.
