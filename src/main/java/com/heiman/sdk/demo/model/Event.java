package com.heiman.sdk.demo.model;

/**
 * Event Model Class // 事件模型类
 */
public class Event {
    private String id;
    private String name;
    private Boolean async;
    private ValueType valueType;
    private Expands expands;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public Expands getExpands() {
        return expands;
    }

    public void setExpands(Expands expands) {
        this.expands = expands;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", async=" + async +
                ", valueType=" + valueType +
                ", expands=" + expands +
                ", description='" + description + '\'' +
                '}';
    }
}