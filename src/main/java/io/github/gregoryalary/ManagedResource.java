package io.github.gregoryalary;

import org.apache.jena.rdf.model.Resource;

public abstract class ManagedResource {

    private Resource resource;

    public ManagedResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && (o == this || (o instanceof Service && ((Service) o).getURI().equals(getURI())));
    }

    public Resource getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), getURI());
    }

    public String getURI() {
        return resource.getURI();
    }

}
