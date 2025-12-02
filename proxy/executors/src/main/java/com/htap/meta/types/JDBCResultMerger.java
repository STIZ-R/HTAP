package com.htap.meta.types;

import java.util.*;

public class JDBCResultMerger {
    public static List<Map<String, Object>> merge(Object hot, Object cold) {
        List<Map<String, Object>> merged = new ArrayList<>();
        merged.addAll((List<Map<String, Object>>) hot);
        merged.addAll((List<Map<String, Object>>) cold);
        return merged;
    }
}
