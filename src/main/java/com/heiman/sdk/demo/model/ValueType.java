package com.heiman.sdk.demo.model;

import java.util.List;

/**
 * Value Type Model Class // 值类型模型类
 */
public class ValueType {
    private String type;
    private Integer maxLength;
    private Integer scale;
    private String unit;
    private List<Property> properties;
    private List<Element> elements;
    private ValueType elementType;
    private String trueText;
    private String trueValue;
    private String falseText;
    private String falseValue;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    public ValueType getElementType() {
        return elementType;
    }

    public void setElementType(ValueType elementType) {
        this.elementType = elementType;
    }

    public String getTrueText() {
        return trueText;
    }

    public void setTrueText(String trueText) {
        this.trueText = trueText;
    }

    public String getTrueValue() {
        return trueValue;
    }

    public void setTrueValue(String trueValue) {
        this.trueValue = trueValue;
    }

    public String getFalseText() {
        return falseText;
    }

    public void setFalseText(String falseText) {
        this.falseText = falseText;
    }

    public String getFalseValue() {
        return falseValue;
    }

    public void setFalseValue(String falseValue) {
        this.falseValue = falseValue;
    }

    @Override
    public String toString() {
        return "ValueType{" +
                "type='" + type + '\'' +
                ", maxLength=" + maxLength +
                ", scale=" + scale +
                ", unit='" + unit + '\'' +
                ", properties=" + properties +
                ", elements=" + elements +
                ", elementType=" + elementType +
                ", trueText='" + trueText + '\'' +
                ", trueValue='" + trueValue + '\'' +
                ", falseText='" + falseText + '\'' +
                ", falseValue='" + falseValue + '\'' +
                '}';
    }
}