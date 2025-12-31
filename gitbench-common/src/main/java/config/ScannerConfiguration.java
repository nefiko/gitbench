package config;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class ScannerConfiguration {
    private List<String> includePackages = new ArrayList<>();
    private List<String> excludePackages = new ArrayList<>(List.of("java.", "javax.", "sun."));
    private Set<String> whitelist = new HashSet<>();
    private Set<String> blacklist = new HashSet<>();
    private int minComplexity = 5;
    private int maxMethods = 100;
}