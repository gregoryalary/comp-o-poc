package io.github.gregoryalary;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class ServiceComposer {

    public static String OUT_BASE = "https://www.irit.fr/ambiantcompo/environment/composed#";

    private static final Resource COMPOSABLE_PERFORM = ResourceFactory.createResource("https://gregoryalary.github.io/comp-o#ComposablePerform");

    private static final Property PROCESS_FROM_PROCESS = ResourceFactory.createProperty("http://www.daml.org/services/owl-s/1.2/Process.owl#fromProcess");

    public static final Property PROCESS_HAS_DATA_FROM = ResourceFactory.createProperty("http://www.daml.org/services/owl-s/1.2/Process.owl#hasDataFrom");

    public static final Resource PROCESS_INPUT_BINDING = ResourceFactory.createResource("http://www.daml.org/services/owl-s/1.2/Process.owl#InputBinding");

    public static final Resource PROCESS_OUTPUT_BINDING = ResourceFactory.createResource("http://www.daml.org/services/owl-s/1.2/Process.owl#OutputBinding");

    public static final Property TYPE = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    public static final Property PROCESS_THE_VAR = ResourceFactory.createProperty("http://www.daml.org/services/owl-s/1.2/Process.owl#theVar");

    public static final Property PROCESS_TO_PARAM = ResourceFactory.createProperty("http://www.daml.org/services/owl-s/1.2/Process.owl#toParam");

    public static String toOpeResourceUri(Resource resource) {
        if (resource.isAnon()) {
            return String.format("%sanon-%s", OUT_BASE, resource.getId());
        } else {
            String[] splitted = resource.getURI().split("#");
            return String.format("%s%s", OUT_BASE, splitted[splitted.length - 1]);
        }
    }

    public static boolean keepStatement(Statement statement) {
        return isNotTypeComposableOwls(statement) && isNotComposableOwlsPredicate(statement);
    }

    private static boolean isNotComposableOwlsPredicate(Statement statement) {
        return !statement.getPredicate().getURI().contains("https://gregoryalary.github.io/comp-o");
    }

    private static boolean isNotTypeComposableOwls(Statement statement) {
        return !statement.getPredicate().getURI().equals(TYPE.getURI())
                || (statement.getPredicate().getURI().equals(TYPE.getURI())
                && !statement.getObject().asResource().getURI().contains("https://gregoryalary.github.io/comp-o"));
    }

    public static void recursiveOperationalCopy(Resource resource, Model target) {
        Resource thisOpeResource = ResourceFactory.createResource(toOpeResourceUri(resource));
        resource.listProperties().toList().stream().filter(ServiceComposer::keepStatement).forEach((Statement stmt) -> {
            if (stmt.getObject().isLiteral()) {
                target.add(ResourceFactory.createStatement(thisOpeResource, stmt.getPredicate(), stmt.getLiteral()));
            } else {
                if (target.containsResource(ResourceFactory.createResource(toOpeResourceUri(stmt.getObject().asResource())))) {
                    target.add(ResourceFactory.createStatement(thisOpeResource, stmt.getPredicate(), target.getResource(toOpeResourceUri(stmt.getObject().asResource()))));
                } else {
                    if (stmt.getObject().asResource().listProperties().toList().size() == 0) { // Is not loaded into evt
                        target.add(ResourceFactory.createStatement(thisOpeResource, stmt.getPredicate(), ResourceFactory.createResource(stmt.getObject().asResource().getURI())));
                    } else {
                        target.add(ResourceFactory.createStatement(thisOpeResource, stmt.getPredicate(), ResourceFactory.createResource(toOpeResourceUri(stmt.getObject().asResource()))));
                    }
                    recursiveOperationalCopy(stmt.getObject().asResource(), target);
                }
            }
        });
    }

    public static void composeServices(List<ServiceBindings> allBindings, String filename) {
        Model model = ModelFactory.createDefaultModel();
        allBindings.stream().forEach((binding) -> ServiceComposer.composeService(binding, model));
        writeModel(model, filename);
    }

    private static void composeService(ServiceBindings bindings, Model model) {
        recursiveOperationalCopy(ServiceEnvironment.getModel().getResource(bindings.getService().getURI()), model);
        addMatchingInformation(bindings.getService(), bindings.getBindings(), model);
    }

    private static void addMatchingInformation(ComposableService service, HashMap<ComposablePerform, ComposablePerformBindings> bindings, Model model) {
        addIsService(model, service);
        addBindings(model, bindings);
        updateVarReferences(model, bindings);
    }

    private static void updateVarReferences(Model model, HashMap<ComposablePerform, ComposablePerformBindings> bindings) {
        Query query = QueryFactory.create(String.format(
                "PREFIX process:          <http://www.daml.org/services/owl-s/1.2/Process.owl#>\n" +
                "PREFIX profile:          <http://www.daml.org/services/owl-s/1.2/Profile.owl#>\n" +
                "PREFIX service:          <http://www.daml.org/services/owl-s/1.2/Service.owl#>\n" +
                "PREFIX comp-o: <https://gregoryalary.github.io/comp-o#>\n" +
                "PREFIX owl-list:         <http://www.daml.org/services/owl-s/1.2/generic/ObjectList.owl#>\n" +
                "\n" +
                "SELECT DISTINCT ?valueOf\n" +
                "WHERE {\n" +
                        "?valueOf a process:ValueOf .\n" +
                "}"));
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            Resource valueOf = solution.getResource("valueOf");
            valueOf.listProperties(PROCESS_FROM_PROCESS).toList().stream().filter(
                    statement -> statement.getObject().isResource() && statement.getObject().asResource().hasProperty(TYPE, COMPOSABLE_PERFORM)
            ).forEach(
                    statement -> {
                        Parameter currentParameter = new Output(statement.getSubject().getProperty(PROCESS_THE_VAR).getObject().asResource());
                        Resource targetCPerform = statement.getObject().asResource();
                        bindings.keySet().stream().forEach(cPerform -> {
                            if (cPerform.getURI().equals(targetCPerform.getURI())) {
                                Resource newValueOf = model.getResource(toOpeResourceUri(statement.getSubject()));
                                ComposablePerformBindings binding = bindings.get(cPerform);
                                newValueOf.removeAll(PROCESS_THE_VAR);
                                binding.getOutputBinding().keySet().stream().forEach(parameter -> {
                                    if (parameter.getURI().equals(currentParameter.getURI())) {
                                        model.add(ResourceFactory.createStatement(
                                                newValueOf, PROCESS_THE_VAR, binding.getOutputBinding().get(parameter).getResource()
                                        ));
                                    }
                                });
                            }
                        });
                    }
            );
        }
    }

    private static void addBindings(Model model, HashMap<ComposablePerform, ComposablePerformBindings> bindings) {
        bindings.keySet().stream().forEach((ComposablePerform composablePerform) -> {
            Resource newPerform = model.getResource(toOpeResourceUri(ServiceEnvironment.getModel().getResource(composablePerform.getURI())));
            if (newPerform == null) throw new RuntimeException(String.format("%s does not exist", toOpeResourceUri(ServiceEnvironment.getModel().getResource(composablePerform.getURI()))));
            model.add(
                    newPerform,
                    ResourceFactory.createProperty("http://www.daml.org/services/owl-s/1.2/Process.owl#process"),
                    ResourceFactory.createResource(bindings.get(composablePerform).getService().getURI())
            );
            ComposablePerformBindings binding = bindings.get(composablePerform);
            newPerform.listProperties(PROCESS_HAS_DATA_FROM).toList().stream().forEach(statement -> {
                if (statement.getObject().asResource().hasProperty(TYPE, PROCESS_INPUT_BINDING)) {
                    Statement toParamStatement = statement.getObject().asResource().getProperty(PROCESS_TO_PARAM);
                    if (toParamStatement == null) throw new RuntimeException();
                    binding.getInputBinding().keySet().stream().forEach(parameter -> {
                        if (toParamStatement.getObject().asResource().getURI().equals(toOpeResourceUri(ServiceEnvironment.getModel().getResource(parameter.getURI())))) {
                            model.add(ResourceFactory.createStatement(
                                    statement.getObject().asResource(),
                                    PROCESS_TO_PARAM,
                                    ResourceFactory.createResource(binding.getInputBinding().get(parameter).getURI())
                            ));
                        }
                    });
                    model.remove(toParamStatement);
                } else if (statement.getObject().asResource().hasProperty(TYPE, PROCESS_OUTPUT_BINDING)) {
                    throw new NotImplementedException();
                } else {
                    throw new RuntimeException("Binding is neither input or output");
                }
            });
        });
    }

    private static void addIsService(Model model, ComposableService service) {
        model.add(ResourceFactory.createStatement(
                model.getResource(toOpeResourceUri(ServiceEnvironment.getModel().getResource(service.getURI()))),
                TYPE,
                ResourceFactory.createResource("http://www.daml.org/services/owl-s/1.2/Service.owl#Service")
        ));
    }

    private static void writeModel(Model model, String fileName) {
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("process", "http://www.daml.org/services/owl-s/1.2/Process.owl#");
        model.setNsPrefix("profile", "http://www.daml.org/services/owl-s/1.2/Profile.owl#");
        model.setNsPrefix("service", "http://www.daml.org/services/owl-s/1.2/Service.owl#");
        model.setNsPrefix("comp-o", "https://gregoryalary.github.io/comp-o#");
        model.setNsPrefix("expr", "http://www.daml.org/services/owl-s/1.2/generic/Expression.owl#");
        model.setNsPrefix("owl-list", "http://www.daml.org/services/owl-s/1.2/generic/ObjectList.owl#");
        model.setNsPrefix("", OUT_BASE);
        try {
            File file = new File(String.format("./resources/out/%s", fileName));
            if (file.exists()) file.delete();
            file.createNewFile();
            RDFDataMgr.write(new FileOutputStream(file), model, RDFFormat.TURTLE_PRETTY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
