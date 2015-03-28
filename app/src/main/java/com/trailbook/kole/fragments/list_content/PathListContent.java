package com.trailbook.kole.fragments.list_content;

import java.util.ArrayList;
import java.util.Collections;
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

    public static class PathSummaryItem implements Comparable<PathSummaryItem> {
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

        /**
         * Compares this object to the specified object to determine their relative
         * order.
         *
         * @param another the object to compare to this instance.
         * @return a negative integer if this instance is less than {@code another};
         * a positive integer if this instance is greater than
         * {@code another}; 0 if this instance has the same order as
         * {@code another}.
         * @throws ClassCastException if {@code another} cannot be converted into something
         *                            comparable to {@code this} instance.
         */
        @Override
        public int compareTo(PathSummaryItem another) {
            int result = this.pathName.compareToIgnoreCase(another.pathName);
            return result;
        }
    }

    public static void sort(){
        Collections.sort(ITEMS);
    }
}
