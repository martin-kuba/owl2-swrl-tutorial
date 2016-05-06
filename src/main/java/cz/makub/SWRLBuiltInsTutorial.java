package cz.makub;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.clarkparsia.pellet.rules.builtins.BuiltInRegistry;
import com.clarkparsia.pellet.rules.builtins.GeneralFunction;
import com.clarkparsia.pellet.rules.builtins.GeneralFunctionBuiltIn;
import com.google.common.collect.Multimap;
import org.mindswap.pellet.ABox;
import org.mindswap.pellet.Literal;
import org.mindswap.pellet.utils.ATermUtils;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;

import static org.mindswap.pellet.utils.Namespaces.XSD;

/**
 * Example of Pellet custom SWRL built-in.
 * <p>
 * Run in Maven with <code>mvn exec:java -Dexec.mainClass=cz.makub.SWRLBuiltInsTutorial</code>
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class SWRLBuiltInsTutorial {

    /**
     * Implementation of a SWRL custom built-in.
     */
    private static class ThisYear implements GeneralFunction {

        public boolean apply(ABox abox, Literal[] args) {
            Calendar calendar = Calendar.getInstance();
            String year = new SimpleDateFormat("yyyy").format(calendar.getTime());
            if (args[0] == null) {
                //variable not bound, fill it with the current year
                args[0] = abox.addLiteral(ATermUtils.makeTypedLiteral(year, XSD + "integer"));
                return args[0] != null;
            } else {
                //variable is bound, compare its value with the current year
                return year.equals(args[0].getLexicalValue());
            }
        }

        public boolean isApplicable(boolean[] boundPositions) {
            //the built-in is applicable for one argument only
            return boundPositions.length == 1;
        }

    }

    private static final String DOC_URL = "http://acrab.ics.muni.cz/ontologies/swrl_tutorial.owl";

    public static void main(String[] args) throws OWLOntologyCreationException {
        //register my built-in
        BuiltInRegistry.instance.registerBuiltIn("urn:makub:builtIn#thisYear", new GeneralFunctionBuiltIn(new ThisYear()));
        //initialize ontology and reasoner
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(DOC_URL));
        OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        OWLDataFactory factory = manager.getOWLDataFactory();
        PrefixDocumentFormat pm = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
        //use the rule with the built-in to infer data property values
        OWLNamedIndividual martin = factory.getOWLNamedIndividual(":Martin", pm);
        listAllDataPropertyValues(martin, ontology, reasoner);

        OWLNamedIndividual ivan = factory.getOWLNamedIndividual(":Ivan", pm);
        listAllDataPropertyValues(ivan, ontology, reasoner);
    }

    static void listAllDataPropertyValues(OWLNamedIndividual individual, OWLOntology ontology, OWLReasoner reasoner) {
        OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();
        Multimap<OWLDataPropertyExpression, OWLLiteral> assertedValues = EntitySearcher.getDataPropertyValues(individual, ontology);
        for (OWLDataProperty dataProp : ontology.getDataPropertiesInSignature(Imports.INCLUDED)) {
            for (OWLLiteral literal : reasoner.getDataPropertyValues(individual, dataProp)) {
                Collection<OWLLiteral> literalSet = assertedValues.get(dataProp);
                boolean asserted = (literalSet != null && literalSet.contains(literal));
                System.out.println((asserted ? "asserted" : "inferred") + " data property for " + renderer.render(individual) + " : "
                        + renderer.render(dataProp) + " -> " + renderer.render(literal));
            }
        }
    }
}