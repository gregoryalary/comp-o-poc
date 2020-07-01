package io.github.gregoryalary;

import java.util.HashMap;

public class ServiceBindings {

    private ComponentBasedService service;

    private HashMap<ComposablePerform, ComposablePerformBindings> bindings;

    public ServiceBindings(ComponentBasedService service, HashMap<ComposablePerform, ComposablePerformBindings> bindings) {
        this.service = service;
        this.bindings = bindings;
    }

    public ComponentBasedService getService() {
        return service;
    }

    public HashMap<ComposablePerform, ComposablePerformBindings> getBindings() {
        return bindings;
    }

}
