package io.github.gregoryalary;

import java.util.*;

public class App {

    public static String BASE = "https://www.irit.fr/ambiantcompo/environment#";

    private final static Scanner sc = new Scanner(System.in);

    public static List<ServiceBindings> buildWholeComposition(ComponentBasedService service, List<ServiceBindings> bindings) {
        HashMap<ComposablePerform, ComposablePerformBindings> bindingsForThisService = new HashMap();
        // Print the composable performs
        Collection<ComposablePerform> cPerforms = service.getComposablePerforms();
        System.out.printf("\nThe service %s needs to be bound.\n", service.getURI());
        System.out.printf("This service has %d required perform(s) :\n", cPerforms.size());
        cPerforms.parallelStream().forEach(composablePerform -> System.out.printf("\t- %s\n", composablePerform.getURI()));
        // Bind the composable performs
        cPerforms.stream().forEach((ComposablePerform cPerform) -> {
            ComposablePerformBindings bindingsForThisPerform = null;
            System.out.println();
            do {
                System.out.printf("What service do you want to bind with %s : ", cPerform.getURI());
                String targetServiceUri = sc.nextLine();
                Service target = ServiceEnvironment.getServices().stream().filter(
                        (Service s) -> s.getURI().equals(BASE + targetServiceUri)
                ).findFirst().orElse(null);
                if (targetServiceUri.equals("???")) {
                    ServiceEnvironment.getServices().parallelStream().forEach(s -> System.out.printf("\t- %s\n", s));
                } else if (target == null) {
                    System.out.println("\t-> Could not find this service.");
                } else {
                    bindingsForThisPerform = cPerform.bindWith(target).orElse(null);
                    if (bindingsForThisPerform == null) {
                        System.out.printf("\t-> The service %s is not compatible with the perform %s\n", target.getURI(), cPerform.getURI());
                    }
                }
            } while (bindingsForThisPerform == null);
            bindingsForThisService.put(cPerform, bindingsForThisPerform);
        });
        bindings.add(new ServiceBindings(service, bindingsForThisService));
        service.setComposed(true);
        for (ComposablePerformBindings bindingsForThisPerform : bindingsForThisService.values()) {
            if (bindingsForThisPerform.getService() instanceof ComponentBasedService && !((ComponentBasedService) bindingsForThisPerform.getService()).isComposed()) {
                buildWholeComposition((ComponentBasedService) bindingsForThisPerform.getService(), bindings);
            }
        }
        return bindings;
    }

    public static void main(String[] args) {
        ServiceEnvironment.getModel(); // force init
        OntologyWrapper.init(); // force init
        System.out.println("+------------ POC CLI ------------+\n");
        ComponentBasedService rootService = null;
        do {
            System.out.print("Root service URI (component-based) : ");
            String rootServiceUri = sc.nextLine();
            rootService = ServiceEnvironment.getComponentBasedServices().stream().filter(
                    (Service s) -> s.getURI().equals(BASE + rootServiceUri)
            ).findFirst().orElse(null);
            if (rootServiceUri.equals("???")) {
                ServiceEnvironment.getComponentBasedServices().parallelStream().forEach(s -> System.out.printf("\t- %s\n", s));
            } else if (rootService == null) {
                System.out.println("\t-> Could not find this service. Are you sure it is a component-based service ?");
            }
        } while (rootService == null);
        List<ServiceBindings> bindingList = buildWholeComposition(rootService, new LinkedList());
        String outFile = null;
        do {
            System.out.print("\nWhere do you want to save the description of the composite service ? : ");
            outFile = sc.nextLine();
        } while (outFile == null);
        ServiceComposer.composeServices(bindingList, outFile);
        System.out.printf("\t-> Graph saved in ./resources/out/%s\n", outFile);
    }

}
