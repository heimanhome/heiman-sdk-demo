package com.heiman.sdk.demo.model;

/**
 * Tag Model Class // 标签模型类
 */
public class Tag {
    private String id;
    private String name;
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
        return "Tag{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", valueType=" + valueType +
                ", expands=" + expands +
                ", description='" + description + '\'' +
                '}';
    }
}