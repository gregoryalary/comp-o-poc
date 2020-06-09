package io.github.gregoryalary;

import java.util.HashMap;

public class ServiceBindings {

    private ComposableService service;

    private HashMap<ComposablePerform, ComposablePerformBindings> bindings;

    public ServiceBindings(ComposableService service, HashMap<ComposablePerform, ComposablePerformBindings> bindings) {
        this.service = service;
        this.bindings = bindings;
    }

    public ComposableService getService() {
        return service;
    }

    public HashMap<ComposablePerform, ComposablePerformBindings> getBindings() {
        return bindings;
    }

}
