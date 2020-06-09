package io.github.gregoryalary;

import org.apache.jena.rdf.model.Resource;

public class ServicePrecondition extends ManagedResource {

    private final Service parentService;

    private final PreconditionExpression preconditionExpression;

    public ServicePrecondition(Resource resource, Service parentService) {
        super(resource);
        this.parentService = parentService;
        preconditionExpression = new PreconditionExpression(this);
    }

    public boolean isCompatibleWith(ServicePrecondition other, ComposablePerformBindings binding) {
        return other != null && (other == this || preconditionExpression.isCompatibleWith(other.getExpression(), binding));
    }

    private PreconditionExpression getExpression() {
        return preconditionExpression;
    }

}
