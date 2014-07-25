package com.trailbook.kole.fragments.list_content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class PathListContent {

    /**
     * An array of sample (dummy) items.
     */
    public static List<PathSummaryItem> ITEMS = new ArrayList<PathSummaryItem>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<String, PathSummaryItem> ITEM_MAP = new HashMap<String, PathSummaryItem>();

    public static void addItem(PathSummaryItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    public static void removeAllItems() {
        ITEMS.clear();
        ITEM_MAP.clear();
    }

    /**
     * A dummy item representing a piece of content.
     */
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
