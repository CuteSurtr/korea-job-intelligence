package com.kji.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ScoreBreakdown {

    private final List<Component> components = new ArrayList<>();

    public void add(String key, String label, double points, String evidence) {
        components.add(new Component(key, label, points, evidence));
    }

    public List<Component> components() {
        return List.copyOf(components);
    }

    public double total() {
        return components.stream().mapToDouble(Component::points).sum();
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder("{\"components\":[");
        for (int index = 0; index < components.size(); index++) {
            Component component = components.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"key\":\"").append(escape(component.key()))
                    .append("\",\"label\":\"").append(escape(component.label()))
                    .append("\",\"points\":").append(String.format(Locale.ROOT, "%.2f", component.points()))
                    .append(",\"evidence\":\"").append(escape(component.evidence()))
                    .append("\"}");
        }
        return builder.append("]}").toString();
    }

    public String toExplanation() {
        if (components.isEmpty()) {
            return "No scoring component matched, so nothing supports a score above the floor.";
        }
        List<Component> ordered = new ArrayList<>(components);
        ordered.sort((left, right) -> Double.compare(Math.abs(right.points()), Math.abs(left.points())));

        StringBuilder builder = new StringBuilder();
        for (Component component : ordered) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(component.points() >= 0 ? "+" : "")
                    .append(String.format(Locale.ROOT, "%.1f", component.points()))
                    .append("  ")
                    .append(component.label());
            if (component.evidence() != null && !component.evidence().isBlank()) {
                builder.append(" (").append(component.evidence()).append(')');
            }
        }
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    public record Component(String key, String label, double points, String evidence) {
    }
}
