package io.github.gregoryalary;

public class AppTest {

    public static void main(String[] args) {
        ServiceEnvironment.getModel(); // force init
        OntologyWrapper.init(); // force init
        ComponentBasedService positiveService = (ComponentBasedService) ServiceEnvironment.getService("https://www.irit.fr/ambiantcompo/environment#positive-service").get();
        Service negativeService = ServiceEnvironment.getService("https://www.irit.fr/ambiantcompo/environment#negative-service").get();
        // ServiceComposer.composeServices(, "out.ttl");
    }

}
