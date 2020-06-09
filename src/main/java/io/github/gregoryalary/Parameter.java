package io.github.gregoryalary;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class Parameter extends ManagedResource {

    private String typeUri;
    private boolean isUsed;

    public Parameter(Resource resource) {
        super(resource);
        isUsed = false;
        this.getType();
    }

    private void getType() {
        if (typeUri == null) {
            Query query = QueryFactory.create(String.format(
                    "PREFIX process: <http://www.daml.org/services/owl-s/1.2/Process.owl#>\n" +
                            "SELECT ?parameterType\n" +
                            "WHERE {\n" +
                            "   <%s> process:parameterType ?parameterType .\n" +
                            "}\n", getURI()));
            ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                typeUri = solution.getLiteral("parameterType").getString();
            }
            if (typeUri == null) {
                throw new RuntimeException("No parameter type for " + this);
            }
        }
    }

    public Parameter use() {
        if (isUsed) {
            throw new RuntimeException("Trying to use and already used IOUri " + this);
        }
        isUsed = true;
        return this;
    }

    public boolean isUsed() {
        return isUsed;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) || (o != null && (o == this || (o instanceof Parameter && ((Parameter) o).getTypeUri().equals(getTypeUri()))));
    }

    public boolean isEquivalentOrSubsumed(Parameter other) {
        if (equals(other) || other.getURI().equals("http://www.w3.org/2002/07/owl#Thing")) return true;
        Query query = QueryFactory.create(String.format(
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                        "ASK {" +
                        "  { <%s> owl:equivalentClass <%s> . }" +
                        "  UNION" +
                        "  { <%s> owl:equivalentClass <%s> . }" +
                        "  UNION" +
                        "  { <%s> rdfs:subClassOf <%s> . }" +
                        "}", getTypeUri(), other.getTypeUri(), other.getTypeUri(), getTypeUri(), getTypeUri(), other.getTypeUri()));
        return QueryExecutionFactory.create(query, OntologyWrapper.getOntology()).execAsk();
    }

    public String getTypeUri() {
        return typeUri;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", super.toString(), typeUri);
    }

}
