package io.github.gregoryalary;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;

import java.util.*;

public class ComposablePerform extends ManagedResource {

    private final ComposableService parentService;

    public ComposablePerform(Resource resource, ComposableService parentService) {
        super(resource);
        this.parentService = parentService;
    }

    public boolean isComposableWith(Service service) {
        return bindWith(service).isPresent();
    }

    public Optional<ComposablePerformBindings> bindWith(Service service) {
        if (service.equals(parentService)) return Optional.empty();
        ComposablePerformBindings binding = new ComposablePerformBindings(getRequiredInterface(), service);
        Query query = QueryFactory.create(String.format(
                "PREFIX process:          <http://www.daml.org/services/owl-s/1.2/Process.owl#>\n" +
                        "PREFIX profile:          <http://www.daml.org/services/owl-s/1.2/Profile.owl#>\n" +
                        "PREFIX service:          <http://www.daml.org/services/owl-s/1.2/Service.owl#>\n" +
                        "PREFIX comp-o: <https://gregoryalary.github.io/comp-o#>\n" +
                        "PREFIX owl-list:         <http://www.daml.org/services/owl-s/1.2/generic/ObjectList.owl#>\n" +
                        "\n" +
                        "SELECT ?interfaceInputUri ?interfaceOutputUri ?serviceInputUri ?serviceOutputUri ?requiredInterface" +
                        "WHERE {" +
                        "  { <%s> comp-o:requiredInterface/profile:hasInput ?interfaceInputUri . }" +
                        "  UNION" +
                        "  { <%s> comp-o:requiredInterface/profile:hasOutput ?interfaceOutputUri . }" +
                        "  UNION" +
                        "  { <%s> service:presents/profile:hasInput ?serviceInputUri . }" +
                        "  UNION" +
                        "  { <%s> service:presents/profile:hasOutput ?serviceOutputUri . }" +
                        "}", getURI(), getURI(), service.getURI(), service.getURI()));
        ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect();
        HashMap<String, LinkedList<Parameter>> parameters = new HashMap<String, LinkedList<Parameter>>() {{
            put("interfaceInputUri", new LinkedList());
            put("interfaceOutputUri", new LinkedList());
            put("serviceInputUri", new LinkedList());
            put("serviceOutputUri", new LinkedList());
        }};
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            for (Iterator<String> varNames = solution.varNames(); varNames.hasNext(); ) {
                String varName = varNames.next();
                if (solution.contains(varName)) {
                    if (varName.equals("interfaceInputUri") || varName.equals("serviceInputUri")) {
                        parameters.get(varName).add(new Input(solution.getResource(varName)));
                    } else {
                        parameters.get(varName).add(new Output(solution.getResource(varName)));
                    }
                }
            }
        }
        if (parameters.get("interfaceInputUri").size() != parameters.get("serviceInputUri").size() || parameters.get("interfaceOutputUri").size() != parameters.get("serviceOutputUri").size())
            return Optional.empty();
        for (Parameter interfaceInput : parameters.get("interfaceInputUri")) {
            for (Parameter serviceInput : parameters.get("serviceInputUri")) {
                if (!serviceInput.isUsed() && interfaceInput.isEquivalentOrSubsumed(serviceInput)) {
                    binding.bindInput(interfaceInput, serviceInput.use());
                }
            }
        }
        for (Parameter interfaceOutput : parameters.get("interfaceOutputUri")) {
            for (Parameter serviceOutput : parameters.get("serviceOutputUri")) {
                if (!serviceOutput.isUsed() && interfaceOutput.isEquivalentOrSubsumed(serviceOutput)) {
                    binding.bindOutput(interfaceOutput, serviceOutput.use());
                }
            }
        }
        if (binding.getInputBinding().size() == parameters.get("interfaceInputUri").size() && binding.getOutputBinding().size() == parameters.get("interfaceOutputUri").size()) {
            return preconditionsMatch(service, binding) ? Optional.of(binding) : Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private boolean preconditionsMatch(Service service, ComposablePerformBindings binding) {
        Collection<ServicePrecondition> interfacesPreconditions = new LinkedList();
        Query query = QueryFactory.create(String.format(
                "PREFIX profile:          <http://www.daml.org/services/owl-s/1.2/Profile.owl#>\n" +
                        "PREFIX comp-o: <https://gregoryalary.github.io/comp-o#>\n" +
                        "\n" +
                        "SELECT ?precondition\n" +
                        "WHERE {\n" +
                        "   <%s> comp-o:requiredInterface/profile:hasPrecondition ?precondition .\n" +
                        "}"
                , getURI()));
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            interfacesPreconditions.add(new ServicePrecondition(solution.getResource("?precondition"), null));
        }
        for (ServicePrecondition interfacePrecondition : interfacesPreconditions) {
            for (ServicePrecondition servicePrecondition : service.getPreconditions()) {
                System.out.printf("%s VS %s\n", interfacePrecondition, servicePrecondition);
                if (!interfacePrecondition.isCompatibleWith(servicePrecondition, binding)) return false;
            }
        }
        return true;
    }

    private Service getRequiredInterface() {
        Query query = QueryFactory.create(String.format(
                "PREFIX comp-o: <https://gregoryalary.github.io/comp-o#>\n" +
                        "SELECT ?interface\n" +
                        "WHERE {\n" +
                        "  <%s> comp-o:requiredInterface ?interface .\n" +
                        "}", getURI()));
        Optional<Service> requiredInterface = Optional.empty();
        ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect();
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            if (solution.contains("interface")) {
                requiredInterface = Optional.of(new ServiceInterface(solution.getResource("interface")));
            }
        }
        return requiredInterface.orElseThrow(() -> new RuntimeException("No required interface"));
    }

}
