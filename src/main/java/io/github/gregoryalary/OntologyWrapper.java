package io.github.gregoryalary;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

import java.util.*;

public class OntologyWrapper {

    private static Optional<OntModel> model = Optional.empty();

    private static Collection<String> ONTOLOGY_URIS = new LinkedList(Arrays.asList(
            "http://www.w3.org/2002/07/owl",
            "http://ontology.tno.nl/saref.ttl"
    ));

    private static Collection<String> ONTOLOGY_FILES = new LinkedList(Arrays.asList(
            "resources/in/ontology.ttl"
    ));

    public static OntModel getOntology() {
        if (!model.isPresent()) {
            init();
        }
        return model.get();
    }

    public static void init() {
        if (!model.isPresent()) {
            OntModel ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_TRANS_INF);
            ONTOLOGY_URIS.stream().forEach((String uri) -> {
                System.out.printf("Loading [%s]...\n", uri);
                ontologyModel.read(uri);
            });
            ONTOLOGY_FILES.stream().forEach((String path) -> {
                System.out.printf("Loading file : %s...\n", path);
                ontologyModel.read(FileManager.get().open(path), null, "TTL");
            });
            model = Optional.of(ontologyModel);
            System.out.print("\n");
        }
    }

}
