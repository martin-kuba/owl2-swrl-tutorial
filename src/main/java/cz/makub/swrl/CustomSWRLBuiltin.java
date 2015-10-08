package cz.makub.swrl;

import com.clarkparsia.pellet.rules.BindingHelper;
import com.clarkparsia.pellet.rules.VariableBinding;
import com.clarkparsia.pellet.rules.VariableUtils;
import com.clarkparsia.pellet.rules.builtins.BuiltIn;
import com.clarkparsia.pellet.rules.model.*;
import org.mindswap.pellet.ABox;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.Literal;
import org.mindswap.pellet.Node;
import org.mindswap.pellet.exceptions.InternalReasonerException;

import java.util.*;

/**
 * General custom SWRL built-in. Modelled after {com.clarkparsia.pellet.rules.builtins.GeneralFunctionBuiltIn}
 * but supports also Individuals, not only Literals..
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class CustomSWRLBuiltin implements BuiltIn {

    /**
     * Interface for a SWRL built-in implementation.
     */
    public static interface CustomSWRLFunction {

        /**
         * Implements a SWRL built-in function on Literals or Individuals.
         *
         * @param abox ABox
         * @param args array of Individual or Literal
         * @return true on success, false otherwise
         */
        public boolean apply(ABox abox, Node[] args);

        public boolean isApplicable(boolean[] boundPositions);

    }

    private final CustomSWRLFunction function;

    public CustomSWRLBuiltin(CustomSWRLFunction function) {
        this.function = function;
    }

    @Override
    public BindingHelper createHelper(BuiltInAtom atom) {
        return new CustomSWRLFunctionHelper(atom);
    }

    public class CustomSWRLFunctionHelper implements BindingHelper {

        private final BuiltInAtom atom;
        private VariableBinding partial;
        private boolean used;

        /**
         * Constructor. BuiltInAtom describes the SWRL atom as predicate and its arguments, which are variables and constants.
         * A problem is that all variables are of type AtomDVariable i.e. variables for data, no variables for individuals.
         *
         * @param atom the SWRL atom as written in the rule, like my:func(?x,?y,"1"^^xsd:integer)
         * @see AtomDVariable
         */
        private CustomSWRLFunctionHelper(BuiltInAtom atom) {
            this.atom = atom;
        }

        /**
         * Returns a set of variables which this binding helper can additionally bind if the variables in the argument are already bound.
         *
         * @param bound variables that are bound to values
         * @return variables from the atom that the build would additionally bind
         */
        @Override
        public Collection<? extends AtomVariable> getBindableVars(Collection<AtomVariable> bound) {
            if (!isApplicable(bound)) return Collections.emptySet();
            Collection<AtomVariable> vars = VariableUtils.getVars(atom);
            vars.removeAll(bound);
            return vars;
        }

        /**
         * Returns a set of variables which must be bound before this helper can generate bindings.
         * Please note that constants are taken as bound arguments so they are not reported.
         * @param bound variables that are bound to values
         * @return variables that must be also bound before the builtin can be invoked
         */
        @Override
        public Collection<? extends AtomVariable> getPrerequisiteVars(Collection<AtomVariable> bound) {
            Collection<AtomVariable> vars = VariableUtils.getVars(atom);
            vars.removeAll(getBindableVars(bound));
            return vars;
        }

        private AtomIVariable d2i(AtomDVariable atomDVariable) {
            return  new AtomIVariable((atomDVariable.getName()));
        }

        /**
         * Decides for all atom arguments whether they are bound to values, and asks the built-in implementation
         * whether this is an acceptable input. Adds constants to the list of bound arguments.
         * @param bound binding of variables to values, may contain both AtomDVariable and AtomIVariable
         * @return boolean value indicating whether the built-in can be invoked
         */
        private boolean isApplicable(Collection<AtomVariable> bound) {
            boolean[] boundPositions = new boolean[atom.getAllArguments().size()];
            for (int i = 0; i < boundPositions.length; i++) {
                AtomDObject atomDObject = atom.getAllArguments().get(i);
                if (atomDObject instanceof AtomDVariable) {
                    AtomDVariable atomDVariable = (AtomDVariable) atomDObject;
                    boolean b = bound.contains(atomDVariable);
                    if (!b) b = bound.contains(d2i(atomDVariable));
                    boundPositions[i] = b;
                } else if (atomDObject instanceof AtomDConstant) {
                    boundPositions[i] = true;
                }
            }
            return function.isApplicable(boundPositions);
        }

        @Override
        public void rebind(VariableBinding newBinding) {

            List<AtomDObject> atomArguments = atom.getAllArguments();
            Node[] arguments = new Node[atomArguments.size()];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = getValueFromVariableBinding(atomArguments.get(i), newBinding);
            }
            if (function.apply(newBinding.getABox(), arguments)) {
                VariableBinding newPartial = new VariableBinding(newBinding.getABox());
                for (int i = 0; i < arguments.length; i++) {
                    AtomDObject arg = atomArguments.get(i);
                    Node result = arguments[i];
                    Node current = getValueFromVariableBinding(arg, newBinding);
                    if (current != null && !current.equals(result)) {
                        // Oops, we overwrote an argument.
                        throw new InternalReasonerException("Function implementation overwrote argument " + i);
                    }
                    if (current == null) {
                        if (result.isLiteral()) {
                            newBinding.set(arg, (Literal) result);
                        } else if (result.isIndividual()) {
                            newBinding.set(d2i((AtomDVariable)arg), (Individual) result);
                        } else {
                            throw new InternalReasonerException("unknown result node type :" + result);
                        }
                    }
                }
                used = false;
                partial = newPartial;
            } else {
                System.out.println("Function failure: " + atom);
                System.out.println("Arguments: " + Arrays.toString(arguments));
            }
        }

        private Node getValueFromVariableBinding(AtomDObject key, VariableBinding binding) {
            if (key instanceof AtomDVariable) {
                AtomDVariable atomDVariable = (AtomDVariable) key;
                Literal literal = binding.get(atomDVariable);
                return (literal != null) ? literal : binding.get(d2i(atomDVariable));
            } else if (key instanceof AtomDConstant) {
                return binding.get(key);
            } else {
                throw new InternalReasonerException("The argument " + key+" of SWRL atom "+atom+" is neither a constant nor a variable.");
            }
        }

        @Override
        public boolean selectNextBinding() {
            if (partial != null && !used) {
                used = true;
                return true;
            }
            return false;
        }


        @Override
        public void setCurrentBinding(VariableBinding currentBinding) {
            for (Map.Entry<? extends AtomVariable, ? extends Node> entry : partial.entrySet()) {
                AtomVariable atomVariable = entry.getKey();
                Node node = entry.getValue();
                if (atomVariable instanceof AtomIObject && node.isIndividual()) {
                    currentBinding.set((AtomIObject) atomVariable, (Individual) node);
                } else if (atomVariable instanceof AtomDObject && node.isLiteral()) {
                    currentBinding.set((AtomDObject) atomVariable, (Literal) node);
                } else {
                    throw new InternalReasonerException("Unknown atomVariable=" + atomVariable + " node=" + node);
                }
            }
        }
    }



}
