package service;

import service.impl.InMemoryBenchmarkStorageService;

public class StorageServiceFactory {

    public enum StorageType {
        IN_MEMORY,
        DATABASE
    }

    private static BenchmarkStorageService inMemoryInstance;

    public static BenchmarkStorageService create(StorageType type) {
        return switch (type) {
            case IN_MEMORY -> getInMemoryInstance();
            case DATABASE -> throw new UnsupportedOperationException(
                    "Database storage requires Spring context. Use JpaBenchmarkStorageService directly.");
        };
    }

    public static BenchmarkStorageService createDefault() {
        return getInMemoryInstance();
    }

    private static BenchmarkStorageService getInMemoryInstance() {
        if (inMemoryInstance == null) {
            inMemoryInstance = new InMemoryBenchmarkStorageService();
        }
        return inMemoryInstance;
    }
}
