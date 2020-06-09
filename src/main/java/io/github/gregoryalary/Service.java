package io.github.gregoryalary;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class Service extends ManagedResource {

    private static String FIND_ALL_PRECONDITIONS_QUERY =
            "PREFIX profile:          <http://www.daml.org/services/owl-s/1.2/Profile.owl#>\n" +
                    "PREFIX service:          <http://www.daml.org/services/owl-s/1.2/Service.owl#>\n" +
                    "\n" +
                    "SELECT ?precondition\n" +
                    "WHERE {\n" +
                    "   <%s> service:presents/profile:hasPrecondition ?precondition .\n" +
                    "}";

    private Collection<ServicePrecondition> preconditions;

    public Service(Resource resource) {
        super(resource);
        initPrecondition();
    }

    public Collection<ServicePrecondition> getPreconditions() {
        return preconditions;
    }

    private void initPrecondition() {
        preconditions = new LinkedList();
        Query query = QueryFactory.create(String.format(FIND_ALL_PRECONDITIONS_QUERY, getURI()));
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            preconditions.add(new ServicePrecondition(solution.getResource("?precondition"), this));
        }
    }


}
