package io.github.gregoryalary;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;

import java.util.Collection;
import java.util.LinkedList;

public class ComponentBasedService extends Service {

    private Collection<ComposablePerform> composablePerforms;

    private boolean isComposed = false;

    private static String FIND_ALL_COMPOSABLE_PERFORMS_QUERY =
            "PREFIX process:          <http://www.daml.org/services/owl-s/1.2/Process.owl#>\n" +
            "PREFIX profile:          <http://www.daml.org/services/owl-s/1.2/Profile.owl#>\n" +
            "PREFIX service:          <http://www.daml.org/services/owl-s/1.2/Service.owl#>\n" +
            "PREFIX comp-o: <https://gregoryalary.github.io/comp-o#>\n" +
            "PREFIX owl-list:         <http://www.daml.org/services/owl-s/1.2/generic/ObjectList.owl#>\n" +
            "\n" +
            "SELECT ?instruction\n" +
            "WHERE {\n" +
                    "   <%s> service:presents/profile:has_process/process:composedOf/(process:then|process:else|process:whileProcess|process:untilProcess|process:components)*/(owl-list:rest*)/owl-list:first+ ?instruction .\n" +
                    "  ?instruction a comp-o:RequiredPerform\n" +
            "}";

    public ComponentBasedService(Resource resource) {
        super(resource);
        this.reloadComposablePerforms();
    }

    public boolean isComposed() {
        return isComposed;
    }

    public void setComposed(boolean composed) {
        isComposed = composed;
    }

    private void reloadComposablePerforms() {
        composablePerforms = new LinkedList();
        Query query = QueryFactory.create(String.format(FIND_ALL_COMPOSABLE_PERFORMS_QUERY, getURI()));
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            composablePerforms.add(new ComposablePerform(solution.getResource("?instruction"), this));
        }
    }

    public Collection<ComposablePerform> getComposablePerforms() {
        return composablePerforms;
    }

}
