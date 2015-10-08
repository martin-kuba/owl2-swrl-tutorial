package cz.makub;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

public class Pellet230Error {

    private static final String BASE_URL = "http://acrab.ics.muni.cz/ontologies/pellet230err.owl";

    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(BASE_URL));
        OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass aClass = factory.getOWLClass(IRI.create(BASE_URL+"#A"));
        System.out.println("it will hang on the next line ...");
        NodeSet<OWLNamedIndividual> nodeSet = reasoner.getInstances(aClass, false);
        System.out.println("this is never printed");
    }
}