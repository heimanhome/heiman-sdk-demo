package com.heiman.sdk.demo.model;

import java.util.List;

/**
 * Function Model Class // 功能模型类
 */
public class Function {
    private String id;
    private String name;
    private Boolean async;
    private List<FunctionInput> inputs;
    private ValueType output;
    private String description;
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

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public List<FunctionInput> getInputs() {
        return inputs;
    }

    public void setInputs(List<FunctionInput> inputs) {
        this.inputs = inputs;
    }

    public ValueType getOutput() {
        return output;
    }

    public void setOutput(ValueType output) {
        this.output = output;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Expands getExpands() {
        return expands;
    }

    public void setExpands(Expands expands) {
        this.expands = expands;
    }

    @Override
    public String toString() {
        return "Function{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", async=" + async +
                ", inputs=" + inputs +
                ", output=" + output +
                ", description='" + description + '\'' +
                ", expands=" + expands +
                '}';
    }
}