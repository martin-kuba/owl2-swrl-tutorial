package cz.makub;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.Version;

public class Pellet230Error {

    private static final String BASE_URL = "http://acrab.ics.muni.cz/ontologies/pellet230err.owl";

    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(BASE_URL));
        OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();

        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        Version v = reasoner.getReasonerVersion();
        System.out.println("reasoner "+reasoner.getReasonerName()+ " " + v.getMajor()+"."+v.getMinor()+"."+v.getPatch()+" build "+v.getBuild());

        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass aClass = factory.getOWLClass(IRI.create(BASE_URL+"#A"));
        System.out.println("it will hang on the next line for Pellet 2.3 ...");
        NodeSet<OWLNamedIndividual> nodeSet = reasoner.getInstances(aClass, false);
        System.out.println("this is never printed");
    }
}