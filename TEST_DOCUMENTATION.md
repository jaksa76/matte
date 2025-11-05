# Matte Framework Test Suite

This document describes the comprehensive test suite for the Matte Framework.

## Test Coverage Summary

**Total Tests: 95**
- Unit Tests: 65
- Integration Tests: 15
- End-to-End Tests: 15

All tests are passing ✅

## Test Structure

```
src/test/java/io/matte/
├── FieldTest.java              (9 tests)
├── EntityTest.java             (10 tests)
├── JsonSerializerTest.java     (13 tests)
├── RepositoryTest.java         (14 tests)
├── EntityControllerTest.java   (19 tests)
├── MatteTest.java              (15 tests)
└── MatteEndToEndTest.java      (15 tests)
```

## Unit Tests

### FieldTest (9 tests)
Tests the core `Field` class functionality:
- Field creation with name and type
- Initial null value state
- Setting and getting values for String, Integer, Long, Boolean types
- Value updates and null handling
- Support for different field name types

### EntityTest (10 tests)
Tests the `Entity` base class:
- Default id field creation
- Field registration in data map
- Custom field support
- Multiple field types (String, Integer, Boolean, Long)
- Entity isolation (multiple instances)

### JsonSerializerTest (13 tests)
Tests JSON serialization:
- Null value serialization
- String, Integer, Long, Boolean serialization
- Quote escaping in strings
- Valid JSON format generation
- Edge cases (zero values, empty strings, max long values)
- Complete entity serialization

### RepositoryTest (14 tests)
Tests the `Repository` class for data persistence:
- Entity save with auto-generated IDs
- ID auto-increment functionality
- Save with existing ID preservation
- Find by ID (success and not found cases)
- Find all entities
- Delete operations
- Entity count
- Update existing entities
- Multiple repository independence
- Entity reference integrity

### EntityControllerTest (19 tests)
Tests the `EntityController` request handling:
- GET all entities (empty and with data)
- GET entity by ID (success and not found)
- POST create entity
- PUT update entity (success and not found)
- DELETE entity (success and not found)
- Invalid ID format handling
- Route not found handling
- JSON parsing for all field types
- Missing field handling
- Invalid value handling
- Resource name capitalization

## Integration Tests

### MatteTest (15 tests)
Tests the main `Matte` framework integration:
- Default and custom port configuration
- Entity registration
- Repository and controller retrieval
- Method chaining support
- Multiple entity registration
- Data persistence before server start
- Server start/stop lifecycle
- Error handling for non-existent resources
- Server state management

## End-to-End Tests

### MatteEndToEndTest (15 tests)
Tests complete HTTP request/response flows:
1. Get list of registered entities
2. Get all users
3. Get user by ID
4. Handle 404 for non-existent user
5. Create new user
6. Update existing user
7. Verify update persisted
8. Delete user
9. Verify user was deleted
10. Independent product endpoint handling
11. Get all products
12. CRUD operations on multiple entities
13. Handle non-existent routes
14. Serve static HTML files
15. Handle invalid JSON gracefully

All E2E tests use actual HTTP requests via Java's HttpClient to test the complete stack.

## Running the Tests

### Run all tests:
```bash
mvn test
```

### Run specific test class:
```bash
mvn test -Dtest=FieldTest
mvn test -Dtest=MatteEndToEndTest
```

### Run tests with coverage:
```bash
mvn test jacoco:report
```

## Test Dependencies

The following testing frameworks and libraries are used:

- **JUnit 5 (5.10.1)**: Testing framework
- **Mockito (5.7.0)**: Mocking framework
- **AssertJ (3.24.2)**: Fluent assertions
- **Java HttpClient**: For E2E HTTP requests

## Test Quality Practices

1. **Clear Test Names**: All tests use descriptive names with `@DisplayName`
2. **Arrange-Act-Assert Pattern**: Tests follow AAA structure
3. **Test Isolation**: Each test is independent and can run in any order
4. **Edge Cases**: Tests cover null values, empty collections, invalid inputs
5. **Error Scenarios**: Tests verify error handling and error messages
6. **Happy Path & Error Path**: Both success and failure scenarios are tested
7. **Integration Testing**: Tests verify component interactions
8. **End-to-End Testing**: Tests verify complete user workflows

## Coverage Areas

### Core Functionality ✅
- Entity creation and field management
- JSON serialization/deserialization
- Data persistence (in-memory)
- CRUD operations
- ID generation

### HTTP/REST API ✅
- GET, POST, PUT, DELETE operations
- Request routing
- Error responses
- Content-Type headers
- Status codes

### Framework Features ✅
- Entity registration
- Multiple entity support
- Server lifecycle management
- Static file serving
- Method chaining API

### Error Handling ✅
- Invalid ID format
- Entity not found
- Invalid JSON
- Non-existent routes
- Missing fields

## Next Steps

To further improve the test suite, consider:

1. **Code Coverage**: Add JaCoCo plugin for code coverage reports
2. **Performance Tests**: Add tests for concurrent requests and load testing
3. **Security Tests**: Add tests for SQL injection, XSS, etc.
4. **Contract Tests**: Add API contract tests
5. **Mutation Tests**: Use PIT for mutation testing

## Test Maintenance

- Run tests before every commit
- Keep tests updated with code changes
- Add tests for every new feature
- Refactor tests to reduce duplication
- Monitor test execution time
