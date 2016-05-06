package cz.makub;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Example how to create an ontology in Java code using OWL API.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class CreateOntologyInCodeExample {

    private static final String DOCUMENT_IRI = "http://acrab.ics.muni.cz/ontologies/example.owl";
    private static OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();

    public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        // create new empty ontology
        OWLOntology ontology = manager.createOntology(IRI.create(DOCUMENT_IRI));
        //set up prefixes
        DefaultPrefixManager pm = new DefaultPrefixManager();
        pm.setDefaultPrefix(DOCUMENT_IRI + "#");
        pm.setPrefix("var:", "urn:swrl#");

        //class declarations
        OWLClass personClass = factory.getOWLClass(":Person", pm);
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(personClass));

        OWLClass manClass = factory.getOWLClass(":Man", pm);
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(manClass));

        OWLClass englishProgrammerClass = factory.getOWLClass(":EnglishProgrammer", pm);
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(englishProgrammerClass));

        //named individuals declarations
        OWLNamedIndividual english = createIndividual(ontology, pm, manager, ":English");
        OWLNamedIndividual comp = createIndividual(ontology, pm, manager, ":Computer-Programming");
        OWLNamedIndividual john = createIndividual(ontology, pm, manager, ":John");

        //annotated subclass axiom
        OWLAnnotationProperty annotationProperty = factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());
        OWLAnnotationValue value = factory.getOWLLiteral("States that every man is a person.");
        OWLAnnotation annotation = factory.getOWLAnnotation(annotationProperty, value);
        OWLSubClassOfAxiom subClassOfAxiom = factory.getOWLSubClassOfAxiom(manClass, personClass, Collections.singleton(annotation));
        manager.addAxiom(ontology, subClassOfAxiom);

        //object property declaration
        OWLObjectProperty speaksLanguageProperty = createObjectProperty(ontology, pm, manager, ":speaksLanguage");
        OWLObjectProperty hasKnowledgeOfProperty = createObjectProperty(ontology, pm, manager, ":hasKnowledgeOf");

        //axiom - John is a Person
        manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(personClass, john));
        //axiom - John speaksLanguage English
        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(speaksLanguageProperty, john, english));
        //axiom - John hasKnowledgeOf Computer-Programming
        manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(hasKnowledgeOfProperty, john, comp));

        //axiom - EnglishProgrammers is equivalent to intersection of classes
        OWLObjectHasValue c1 = factory.getOWLObjectHasValue(speaksLanguageProperty, english);
        OWLObjectHasValue c2 = factory.getOWLObjectHasValue(hasKnowledgeOfProperty, comp);
        OWLObjectIntersectionOf andExpr = factory.getOWLObjectIntersectionOf(personClass, c1, c2);
        manager.addAxiom(ontology, factory.getOWLEquivalentClassesAxiom(englishProgrammerClass, andExpr));


        //SWRL rule - Person(?x),speaksLanguage(?x,English),hasKnowledgeOf(?x,Computer-Programming)->englishProgrammer(?x)
        SWRLVariable varX = factory.getSWRLVariable(pm.getIRI("var:x"));
        Set<SWRLAtom> body = new LinkedHashSet<>();
        body.add(factory.getSWRLClassAtom(personClass, varX));
        body.add(factory.getSWRLObjectPropertyAtom(speaksLanguageProperty, varX, factory.getSWRLIndividualArgument(english)));
        body.add(factory.getSWRLObjectPropertyAtom(hasKnowledgeOfProperty, varX, factory.getSWRLIndividualArgument(comp)));
        Set<? extends SWRLAtom> head = Collections.singleton(factory.getSWRLClassAtom(englishProgrammerClass, varX));
        SWRLRule swrlRule = factory.getSWRLRule(body, head);
        manager.addAxiom(ontology, swrlRule);

        //save  to a file
        FunctionalSyntaxDocumentFormat ontologyFormat = new FunctionalSyntaxDocumentFormat();
        ontologyFormat.copyPrefixesFrom(pm);
        manager.saveOntology(ontology, ontologyFormat, IRI.create(new File("example.owl").toURI()));

        //reason
        OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        for (OWLNamedIndividual person : reasoner.getInstances(personClass, false).getFlattened()) {
            System.out.println("person : " + renderer.render(person));
        }
        for (OWLNamedIndividual englishProgrammer : reasoner.getInstances(englishProgrammerClass, false).getFlattened()) {
            System.out.println("englishProgrammer : " + renderer.render(englishProgrammer));
        }
    }

    private static OWLNamedIndividual createIndividual(OWLOntology ontology, DefaultPrefixManager pm, OWLOntologyManager manager, String name) {
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(name, pm);
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(individual));
        return individual;
    }

    private static OWLObjectProperty createObjectProperty(OWLOntology ontology, DefaultPrefixManager pm, OWLOntologyManager manager, String name) {
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLObjectProperty objectProperty = factory.getOWLObjectProperty(name, pm);
        manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(objectProperty));
        return objectProperty;


    }
}
