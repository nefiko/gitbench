package enums;

public enum HotspotReason {
    SERVICE_ANNOTATION,          // @Service annotated class
    REPOSITORY_ANNOTATION,       // @Repository annotated class
    CONTROLLER_ENDPOINT,         // @GetMapping, @PostMapping, etc.
    CONTAINS_LOOP,               // Contains for/while loops
    CONTAINS_SQL_QUERY,          // Contains SQL operations
    RECURSIVE_METHOD,            // Recursive method calls
    HIGH_CYCLOMATIC_COMPLEXITY,  // Complex control flow
    COLLECTION_OPERATIONS,       // Heavy collection processing
    IO_OPERATIONS,               // File or network I/O
    DATABASE_ACCESS,             // JPA/JDBC operations
    CACHING_OPERATIONS,          // Cache-related operations
    MANUALLY_SPECIFIED,          // User added to whitelist
    SPRING_BEAN_METHOD,          // Method from a Spring bean
    TRANSACTIONAL_METHOD,        // @Transactional annotated
    ASYNC_METHOD,                // @Async annotated
    SCHEDULED_METHOD             // @Scheduled annotated
}
