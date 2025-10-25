# Java Components Coding Style Guide

## Java Code Style

### Indentation

- Use **2 spaces** for indentation (not tabs)
- Never mix spaces and tabs

### Naming Conventions

- **Classes**: PascalCase (e.g., `SongMapper`, `ApolloService`)
- **Methods**: camelCase (e.g., `map()`, `findAll()`)
- **Variables**: camelCase (e.g., `domainSong`, `graphQLSong`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_SIZE`)
- **Packages**: lowercase with dots (e.g., `com.sun.apollo.graphql`)

### File Structure

```
src/main/java/com/sun/
├── base/                    # Shared infrastructure
│   ├── model/BaseEntity.java
│   ├── repository/BaseRepository.java
│   └── service/BaseService.java
├── apollo/                  # Apollo-specific domain
│   ├── model/               # JPA entities
│   ├── repository/          # Spring Data repositories
│   ├── service/             # Business logic services
│   └── graphql/             # GraphQL layer
│       ├── resolvers/       # GraphQL resolvers
│       └── mappers/         # Domain to GraphQL mappers
└── apollo/graphql/          # GraphQL application
```

### Class Organization

- **Fields** at the top
- **Constructors** next
- **Public methods** (business logic first)
- **Private methods** (helpers last)
- **Getters/Setters** at the end

### Method Signatures

- Use descriptive names: `map()` instead of `convert()`
- Single responsibility principle
- Keep methods small and focused

### Annotations

- Place annotations on separate lines above the element they annotate
- Group related annotations together

### Imports

- Organize imports alphabetically
- Separate groups with blank lines:
  - Java standard library
  - Third-party libraries
  - Project imports

## GraphQL Schema Design

### Type Naming

- **Types**: PascalCase (e.g., `Song`, `StemPlayerQueries`)
- **Fields**: camelCase (e.g., `filePath`, `listSongs`)
- **Enums**: PascalCase

### Schema Structure

- Use extension pattern for queries: `stemPlayerQueries: StemPlayerQueries!`
- Keep field names consistent with domain terminology
- Use descriptive field names over abbreviations

## Database Design

### Table Naming

- **Tables**: snake_case (e.g., `songs`, `stems`)
- **Columns**: snake_case (e.g., `file_path`, `song_id`)

### Relationships

- Use explicit foreign key names: `song_id` references `songs.id`
- Consider indexing foreign keys for performance

## Dependency Injection

### Service Layer

- Use constructor injection when possible
- Use `@Autowired` on fields only when necessary
- Prefer interface injection over concrete class injection

### Component Naming

- Services: `@Service`
- Repositories: `@Repository`
- Components: `@Component`
- Controllers: `@RestController` or `@Controller`

## Error Handling

### Exceptions

- Use descriptive exception messages
- Prefer checked exceptions for recoverable errors
- Use runtime exceptions for programming errors

### Validation

- Validate input parameters
- Use Bean Validation (`@Valid`, `@NotNull`, etc.) where appropriate
- Provide meaningful error messages

## Testing

### Unit Tests

- Test public methods only
- Use descriptive test method names: `shouldReturnMappedSong()`
- Follow AAA pattern: Arrange, Act, Assert

### Test Naming

- `ClassNameTest.java` for unit tests
- `ClassNameIT.java` for integration tests

## Documentation

### Code Comments

- Use Javadoc for public APIs
- Explain why, not just what
- Keep comments up-to-date

### Commit Messages

- Use imperative mood: "Add user authentication" not "Added user authentication"
- Keep first line under 50 characters
- Provide detailed description when needed

## Build and Deployment

### Gradle Configuration

- Keep build files clean and well-organized
- Use consistent dependency versions
- Document any custom build logic

### Environment Configuration

- Use profiles for different environments
- Externalize configuration properties
- Never commit sensitive data

## GraphQL Architecture

### Data Fetchers

- Use `DataFetcher` suffix for GraphQL resolver classes
- Keep data fetchers thin - delegate business logic to services
- Data fetchers should only handle GraphQL-specific concerns

### GraphQL Services

- Create dedicated service classes for GraphQL business logic
- Services handle mapping between domain and GraphQL types
- Use `@Service` annotation for GraphQL-specific services

### Mappers

- Each domain entity should have a corresponding mapper
- Mappers should have a single `map()` method
- Use separate private methods for complex mapping logic
- Mappers are injected into services, not data fetchers

## Code Review Checklist

- [ ] Code follows style guide
- [ ] No hardcoded values
- [ ] Proper error handling
- [ ] Unit tests included
- [ ] Documentation updated
- [ ] No performance regressions
- [ ] Security considerations addressed
