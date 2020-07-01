package io.github.gregoryalary;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ServiceEnvironment {

    private static Model model;

    private static Collection<ComponentBasedService> componentBasedServices = new LinkedList<>();
    private static Collection<OperationalService> operationalServices = new LinkedList<>();

    public static Model getModel() {
        if (model == null) {
            ServiceEnvironment.loadModel();
        }
        return model;
    }

    private static void loadModel() {
        model = ModelFactory.createDefaultModel();
        model.read("./resources/in/environment/interfaces.ttl", "TURTLE");
        model.read("./resources/in/environment/converter.ttl", "TURTLE");
        model.read("./resources/in/environment/duplicator.ttl", "TURTLE");
        model.read("./resources/in/environment/lamp.ttl", "TURTLE");
        model.read("./resources/in/environment/slider.ttl", "TURTLE");
        model.read("./resources/in/environment/switch.ttl", "TURTLE");
        model.read("./resources/in/environment/screen.ttl", "TURTLE");
        model.read("./resources/in/environment/pos-service.ttl", "TURTLE");
        model.read("./resources/in/environment/negative-service.ttl", "TURTLE");
        model.read("./resources/in/environment/square.ttl", "TURTLE");
        model.read("./resources/in/environment/btn-always-on.ttl", "TURTLE");
        model.read("./resources/in/environment/plus-one.ttl", "TURTLE");
        model.read("./resources/in/environment/two-times-int-operation.ttl", "TURTLE");
        model.read("./resources/in/environment/degree-to-farenheit.ttl", "TURTLE");
        model.read("./resources/in/environment/ambiant-temperature-computer.ttl", "TURTLE");
        model.read("./resources/in/environment/anenometer-service.ttl", "TURTLE");
        model.read("./resources/in/environment/thermometer-service.ttl", "TURTLE");
        loadComposableServices();
        loadOperationalServices();
    }

    public static Collection<ComponentBasedService> getComponentBasedServices() {
        if (model == null) {
            ServiceEnvironment.loadModel();
        }
        return componentBasedServices;
    }

    public static Collection<OperationalService> getOperationalServices() {
        if (model == null) {
            ServiceEnvironment.loadModel();
        }
        return operationalServices;
    }

    public static Collection<Service> getServices() {
        if (model == null) {
            ServiceEnvironment.loadModel();
        }
        List<Service> union = new LinkedList<>();
        union.addAll(componentBasedServices);
        union.addAll(operationalServices);
        return union;
    }

    private static void loadComposableServices() {
        componentBasedServices = new LinkedList<>();
        Query query = QueryFactory.create("SELECT ?service WHERE { ?service a <https://gregoryalary.github.io/comp-o#ComponentBasedService> }");
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            componentBasedServices.add(new ComponentBasedService(solution.getResource("?service")));
        }
    }

    public static Optional<Service> getService(String URI) {
        return ServiceEnvironment.getServices().stream().filter(
                (Service s) -> s.getURI().equals(URI)
        ).findFirst();
    }

    private static void loadOperationalServices() {
        operationalServices = new LinkedList<>();
        Query query = QueryFactory.create(
                "SELECT ?service\n" +
                        "WHERE {\n" +
                        "   ?service a <http://www.daml.org/services/owl-s/1.2/Service.owl#Service> .\n" +
                         "   FILTER NOT EXISTS { ?service a <https://gregoryalary.github.io/comp-o#ComponentBasedService> }" +
                        "}");
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            operationalServices.add(new OperationalService(solution.getResource("?service")));
        }
    }

}
