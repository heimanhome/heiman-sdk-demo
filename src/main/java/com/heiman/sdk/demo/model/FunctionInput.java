package com.heiman.sdk.demo.model;

/**
 * Function Input Parameter Model Class // 功能输入参数模型类
 */
public class FunctionInput {
    private String id;
    private String name;
    private ValueType valueType;
    private Expands expands;

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

    @Override
    public String toString() {
        return "FunctionInput{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", valueType=" + valueType +
                ", expands=" + expands +
                '}';
    }
}