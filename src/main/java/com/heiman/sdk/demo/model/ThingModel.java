package com.heiman.sdk.demo.model;

import java.util.List;

/**
 * Thing Model Class - Unified Device Model // 物模型类 - 统一的设备模型
 */
public class ThingModel {
    private List<Tag> tags;
    private List<Property> properties;
    private List<Event> events;
    private List<Function> functions;

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public void setFunctions(List<Function> functions) {
        this.functions = functions;
    }

    @Override
    public String toString() {
        return "ThingModel{" +
                "tags=" + tags +
                ", properties=" + properties +
                ", events=" + events +
                ", functions=" + functions +
                '}';
    }
}