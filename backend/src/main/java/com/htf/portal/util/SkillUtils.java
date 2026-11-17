package com.htf.portal.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Helpers for skill lists, which we store as comma-separated text (e.g. "java, react, sql").
 * Static methods only, like UserCodeGenerator.
 */
public final class SkillUtils {

    private SkillUtils() {
    }

    /**
     * Cleans what the user typed: trims spaces, lower-cases, drops empty entries and duplicates.
     * "Java,  REACT ,,java" becomes "java, react". Returns null when nothing is left.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        List<String> cleaned = new ArrayList<>();
        for (String part : raw.split(",")) {
            String skill = part.trim().toLowerCase(Locale.ROOT);
            if (!skill.isEmpty() && !cleaned.contains(skill)) {
                cleaned.add(skill);
            }
        }
        if (cleaned.isEmpty()) {
            return null;
        }
        return String.join(", ", cleaned);
    }

    /** "java, react" -> a HashSet {"java", "react"}. A set makes "does the student have this skill?" a quick lookup. */
    public static Set<String> toSet(String skills) {
        Set<String> set = new HashSet<>();
        String cleaned = normalize(skills);
        if (cleaned == null) {
            return set;
        }
        for (String skill : cleaned.split(", ")) {
            set.add(skill);
        }
        return set;
    }

    /**
     * Match % = how many of the skills a team is looking for the student has, out of all the skills it wants.
     * Team wants {java, react, sql}, student has {react, sql, python}: 2 of 3 -> 67.
     * A team that lists no skills gives 0.
     */
    public static int matchPercent(String teamLookingFor, Set<String> studentSkills) {
        Set<String> wanted = toSet(teamLookingFor);
        if (wanted.isEmpty()) {
            return 0;
        }
        // retainAll keeps only the skills that are in BOTH sets: the intersection.
        Set<String> common = new HashSet<>(wanted);
        common.retainAll(studentSkills);
        return (int) Math.round(100.0 * common.size() / wanted.size());
    }
}
