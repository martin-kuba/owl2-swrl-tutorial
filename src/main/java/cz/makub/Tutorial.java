package cz.makub;

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import com.clarkparsia.owlapi.explanation.util.SilentExplanationProgressMonitor;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.google.common.collect.Multimap;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrdererImpl;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationTree;
import uk.ac.manchester.cs.owl.explanation.ordering.Tree;

import java.util.*;

/**
 * Example how to use an OWL ontology with a reasoner.
 * <p>
 * Run in Maven with <code>mvn exec:java -Dexec.mainClass=cz.makub.Tutorial</code>
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class Tutorial {

    private static final String BASE_URL = "http://acrab.ics.muni.cz/ontologies/tutorial.owl";
    private static OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();

    public static void main(String[] args) throws OWLOntologyCreationException {

        //prepare ontology and reasoner
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(BASE_URL));
        OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        OWLDataFactory factory = manager.getOWLDataFactory();
        PrefixDocumentFormat pm = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
        pm.setDefaultPrefix(BASE_URL + "#");

        //get class and its individuals
        OWLClass personClass = factory.getOWLClass(":Person", pm);

        for (OWLNamedIndividual person : reasoner.getInstances(personClass, false).getFlattened()) {
            System.out.println("person : " + renderer.render(person));
        }

        //get a given individual
        OWLNamedIndividual martin = factory.getOWLNamedIndividual(":Martin", pm);

        //get values of selected properties on the individual
        OWLDataProperty hasEmailProperty = factory.getOWLDataProperty(":hasEmail", pm);

        OWLObjectProperty isEmployedAtProperty = factory.getOWLObjectProperty(":isEmployedAt", pm);

        for (OWLLiteral email : reasoner.getDataPropertyValues(martin, hasEmailProperty)) {
            System.out.println("Martin has email: " + email.getLiteral());
        }

        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(martin, isEmployedAtProperty).getFlattened()) {
            System.out.println("Martin is employed at: " + renderer.render(ind));
        }

        //get labels
        LocalizedAnnotationSelector as = new LocalizedAnnotationSelector(ontology, factory, "en", "cs");
        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(martin, isEmployedAtProperty).getFlattened()) {
            System.out.println("Martin is employed at: '" + as.getLabel(ind) + "'");
        }

        //get inverse of a property, i.e. which individuals are in relation with a given individual
        OWLNamedIndividual university = factory.getOWLNamedIndividual(":MU", pm);
        OWLObjectPropertyExpression inverse = factory.getOWLObjectInverseOf(isEmployedAtProperty);
        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(university, inverse).getFlattened()) {
            System.out.println("MU inverseOf(isEmployedAt) -> " + renderer.render(ind));
        }

        //find to which classes the individual belongs
        Collection<OWLClassExpression> assertedClasses = EntitySearcher.getTypes(martin, ontology);
        for (OWLClass c : reasoner.getTypes(martin, false).getFlattened()) {
            boolean asserted = assertedClasses.contains(c);
            System.out.println((asserted ? "asserted" : "inferred") + " class for Martin: " + renderer.render(c));
        }

        //list all object property values for the individual
        Multimap<OWLObjectPropertyExpression, OWLIndividual> assertedValues = EntitySearcher.getObjectPropertyValues(martin, ontology);
        for (OWLObjectProperty objProp : ontology.getObjectPropertiesInSignature(Imports.INCLUDED)) {
            for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(martin, objProp).getFlattened()) {
                boolean asserted = assertedValues.get(objProp).contains(ind);
                System.out.println((asserted ? "asserted" : "inferred") + " object property for Martin: "
                        + renderer.render(objProp) + " -> " + renderer.render(ind));
            }
        }

        //list all same individuals
        for (OWLNamedIndividual ind : reasoner.getSameIndividuals(martin)) {
            System.out.println("same as Martin: " + renderer.render(ind));
        }

        //ask reasoner whether Martin is employed at MU
        boolean result = reasoner.isEntailed(factory.getOWLObjectPropertyAssertionAxiom(isEmployedAtProperty, martin, university));
        System.out.println("Is Martin employed at MU ? : " + result);


        //check whether the SWRL rule is used
        OWLNamedIndividual ivan = factory.getOWLNamedIndividual(":Ivan", pm);
        OWLClass chOMPClass = factory.getOWLClass(":ChildOfMarriedParents", pm);
        OWLClassAssertionAxiom axiomToExplain = factory.getOWLClassAssertionAxiom(chOMPClass, ivan);
        System.out.println("Is Ivan child of married parents ? : " + reasoner.isEntailed(axiomToExplain));


        //explain why Ivan is child of married parents
        DefaultExplanationGenerator explanationGenerator =
                new DefaultExplanationGenerator(
                        manager, reasonerFactory, ontology, reasoner, new SilentExplanationProgressMonitor());
        Set<OWLAxiom> explanation = explanationGenerator.getExplanation(axiomToExplain);
        ExplanationOrderer deo = new ExplanationOrdererImpl(manager);
        ExplanationTree explanationTree = deo.getOrderedExplanation(axiomToExplain, explanation);
        System.out.println();
        System.out.println("-- explanation why Ivan is in class ChildOfMarriedParents --");
        printIndented(explanationTree, "");
    }

    private static void printIndented(Tree<OWLAxiom> node, String indent) {
        OWLAxiom axiom = node.getUserObject();
        System.out.println(indent + renderer.render(axiom));
        if (!node.isLeaf()) {
            for (Tree<OWLAxiom> child : node.getChildren()) {
                printIndented(child, indent + "    ");
            }
        }
    }

    /**
     * Helper class for extracting labels, comments and other anotations in preffered languages.
     * Selects the first literal annotation matching the given languages in the given order.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LocalizedAnnotationSelector {
        private final List<String> langs;
        private final OWLOntology ontology;
        private final OWLDataFactory factory;

        /**
         * Constructor.
         *
         * @param ontology ontology
         * @param factory  data factory
         * @param langs    list of prefered languages; if none is provided the Locale.getDefault() is used
         */
        public LocalizedAnnotationSelector(OWLOntology ontology, OWLDataFactory factory, String... langs) {
            this.langs = (langs == null) ? Collections.singletonList(Locale.getDefault().toString()) : Arrays.asList(langs);
            this.ontology = ontology;
            this.factory = factory;
        }

        /**
         * Provides the first label in the first matching language.
         *
         * @param ind individual
         * @return label in one of preferred languages or null if not available
         */
        public String getLabel(OWLNamedIndividual ind) {
            return getAnnotationString(ind, OWLRDFVocabulary.RDFS_LABEL.getIRI());
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getComment(OWLNamedIndividual ind) {
            return getAnnotationString(ind, OWLRDFVocabulary.RDFS_COMMENT.getIRI());
        }

        public String getAnnotationString(OWLNamedIndividual ind, IRI annotationIRI) {
            return getLocalizedString(EntitySearcher.getAnnotations(ind, ontology, factory.getOWLAnnotationProperty(annotationIRI)));
        }

        private String getLocalizedString(Collection<OWLAnnotation> annotations) {
            List<OWLLiteral> literalLabels = new ArrayList<>(annotations.size());
            for (OWLAnnotation label : annotations) {
                if (label.getValue() instanceof OWLLiteral) {
                    literalLabels.add((OWLLiteral) label.getValue());
                }
            }
            for (String lang : langs) {
                for (OWLLiteral literal : literalLabels) {
                    if (literal.hasLang(lang)) return literal.getLiteral();
                }
            }
            for (OWLLiteral literal : literalLabels) {
                if (!literal.hasLang()) return literal.getLiteral();
            }
            return null;
        }
    }
}