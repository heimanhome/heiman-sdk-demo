package com.heiman.sdk.demo.model;

import java.util.List;

/**
 * Extended Properties Model Class // 扩展属性模型类
 */
public class Expands {
    private String source;
    private List<String> type;
    private String level;
    private Boolean required;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public String toString() {
        return "Expands{" +
                "source='" + source + '\'' +
                ", type=" + type +
                ", level='" + level + '\'' +
                ", required=" + required +
                '}';
    }
}