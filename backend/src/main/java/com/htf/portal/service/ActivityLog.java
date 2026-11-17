package com.htf.portal.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * The "last 20 actions" list on the admin Teams page, kept only in memory (it is empty after a restart).
 * A LinkedList fits a fixed-size queue: addLast() puts the newest at the end, removeFirst() drops the oldest,
 * and both are cheap.
 */
@Component
public class ActivityLog {

    private static final int MAX_ENTRIES = 20;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    private final LinkedList<String> entries = new LinkedList<>();

    // synchronized: two requests run on two threads at the same time, and a LinkedList
    // is not safe if both change it at once. Only one thread may be inside these methods.
    public synchronized void record(String action) {
        entries.addLast(LocalDateTime.now().format(TIME_FORMAT) + " - " + action);
        if (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    /** Newest first. Returns a copy, so callers can never change our list. */
    public synchronized List<String> recent() {
        List<String> newestFirst = new ArrayList<>(entries);
        Collections.reverse(newestFirst);
        return newestFirst;
    }
}
