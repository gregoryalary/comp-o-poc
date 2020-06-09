package io.github.gregoryalary;

import java.util.HashMap;

public class ComposablePerformBindings {

    private Service requiredInterface;

    private Service service;

    private HashMap<Parameter, Parameter> inputBinding = new HashMap();

    private HashMap<Parameter, Parameter> outputBinding = new HashMap();

    public ComposablePerformBindings(Service requiredInterface, Service service) {
        this.requiredInterface = requiredInterface;
        this.service = service;
    }

    public Service getRequiredInterface() {
        return requiredInterface;
    }

    public Service getService() {
        return service;
    }

    public HashMap<Parameter, Parameter> getInputBinding() {
        return inputBinding;
    }

    public HashMap<Parameter, Parameter> getOutputBinding() {
        return outputBinding;
    }

    public void bindInput(Parameter interfaceInput, Parameter serviceInput) {
        inputBinding.put(interfaceInput, serviceInput);
    }

    public void bindOutput(Parameter interfaceOutput, Parameter serviceOutput) {
        outputBinding.put(interfaceOutput, serviceOutput);
    }

    @Override
    public String toString() {
        return String.format("Input:w%s, Output:%s", inputBinding, outputBinding);
    }

}
