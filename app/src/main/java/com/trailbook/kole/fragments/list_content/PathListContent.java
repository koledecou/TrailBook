package com.trailbook.kole.fragments.list_content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathListContent {


    public static List<PathSummaryItem> ITEMS = new ArrayList<PathSummaryItem>();

    public static Map<String, PathSummaryItem> ITEM_MAP = new HashMap<String, PathSummaryItem>();

    public static void addItem(PathSummaryItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    public static void removeAllItems() {
        ITEMS.clear();
        ITEM_MAP.clear();
    }

    public static class PathSummaryItem {
        public String id;
        public String pathName;

        public PathSummaryItem(String id, String pathName) {
            this.id = id;
            this.pathName = pathName;
        }

        @Override
        public String toString() {
            return pathName;
        }
    }
}
