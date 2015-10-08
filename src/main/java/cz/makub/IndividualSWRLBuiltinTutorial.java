package cz.makub;

import cz.makub.swrl.CustomSWRLBuiltin;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.clarkparsia.pellet.rules.builtins.BuiltInRegistry;
import org.mindswap.pellet.ABox;
import org.mindswap.pellet.Node;
import org.mindswap.pellet.utils.ATermUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import java.util.Map;
import java.util.Set;

import static org.mindswap.pellet.utils.Namespaces.XSD;

/**
 * Example of a Pellet SWRL built-in that works with both Individuals and data literals.
 *
 * Run in Maven with <code>mvn exec:java -Dexec.mainClass=cz.makub.IndividualSWRLBuiltinTutorial</code>
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class IndividualSWRLBuiltinTutorial {

    /**
     * The built-in implementation.
     */
    public static class IRIparts implements CustomSWRLBuiltin.CustomSWRLFunction {

        @Override
        public boolean isApplicable(boolean[] boundPositions) {
            //applicable only to 4 arguments, two bound and two unbound
            return boundPositions.length == 4 && boundPositions[0] && boundPositions[1] && !boundPositions[2] && !boundPositions[3];
        }

        @Override
        public boolean apply(ABox abox, Node[] args) {
            //accepts IRIparts(individual,separator string,unbound variable,unbound variable)
            if (!args[0].isIndividual() || !args[1].isLiteral() || args[2] != null || args[3] != null) return false;
            //get the IRI of the individual in the first argument
            String iri = args[0].getNameStr();
            //get the string value of the second argument
            String separator = ATermUtils.getLiteralValue(args[1].getTerm());
            //split the IRI at the separator
            int idx = iri.indexOf(separator);
            if (idx == -1) return false;
            String prefix = iri.substring(0, idx);
            String id = iri.substring(idx + separator.length());
            //bind the third and fourth arguments to the IRI parts
            args[2] = abox.addLiteral(ATermUtils.makeTypedLiteral(prefix, XSD + "string"));
            args[3] = abox.addLiteral(ATermUtils.makeTypedLiteral(id, XSD + "string"));
            return true;
        }
    }

    //a simple example ontology
    private static final String DOC_URL = "http://acrab.ics.muni.cz/ontologies/swrl_tutorial_ind.owl";

    public static void main(String[] args) throws OWLOntologyCreationException {
        //register my built-in implementation
        BuiltInRegistry.instance.registerBuiltIn("urn:makub:builtIn#IRIparts", new CustomSWRLBuiltin(new IRIparts()));
        //initialize ontology and reasoner
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(DOC_URL));
        OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        OWLDataFactory factory = manager.getOWLDataFactory();
        PrefixOWLOntologyFormat pm = (PrefixOWLOntologyFormat) manager.getOntologyFormat(ontology);
        //print the SWRL rule
        listSWRLRules(ontology, pm);
        //use the rule with the built-in to infer property values
        OWLNamedIndividual martin = factory.getOWLNamedIndividual(":Martin", pm);
        listAllDataPropertyValues(martin, ontology, reasoner);
    }

    public static void listSWRLRules(OWLOntology ontology, PrefixOWLOntologyFormat pm) {
        OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();
        for (SWRLRule rule : ontology.getAxioms(AxiomType.SWRL_RULE)) {
            System.out.println(renderer.render(rule));
        }
    }

    public static void listAllDataPropertyValues(OWLNamedIndividual individual, OWLOntology ontology, OWLReasoner reasoner) {
        OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();
        Map<OWLDataPropertyExpression, Set<OWLLiteral>> assertedValues = individual.getDataPropertyValues(ontology);
        for (OWLDataProperty dataProp : ontology.getDataPropertiesInSignature(true)) {
            for (OWLLiteral literal : reasoner.getDataPropertyValues(individual, dataProp)) {
                Set<OWLLiteral> literalSet = assertedValues.get(dataProp);
                boolean asserted = (literalSet != null && literalSet.contains(literal));
                System.out.println((asserted ? "asserted" : "inferred") + " data property for " + renderer.render(individual) + " : "
                        + renderer.render(dataProp) + " -> " + renderer.render(literal));
            }
        }
    }
}