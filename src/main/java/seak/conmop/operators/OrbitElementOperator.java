/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seak.conmop.operators;

import java.util.ArrayList;
import java.util.HashSet;
import org.hipparchus.util.FastMath;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.RealVariable;
import seak.conmop.variable.ConstellationVariable;
import seak.conmop.variable.SatelliteVariable;

/**
 *
 * @author nhitomi
 */
public class OrbitElementOperator implements Variation {

    private final Variation operator;

    public OrbitElementOperator(Variation operator) {
        this.operator = operator;
    }

    @Override
    public int getArity() {
        return operator.getArity();
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        //copy the solutions
        Solution[] children = new Solution[parents.length];
        for (int i = 0; i < parents.length; i++) {
            children[i] = parents[i].copy();
        }

        for (int i = 0; i < children[0].getNumberOfVariables(); i++) {
            //if the solution is composed of constellation variables
            boolean constelVariables = true;
            for (int j = 0; j < parents.length; j++) {
                if (!(children[j].getVariable(i) instanceof ConstellationVariable)) {
                    constelVariables = false;
                    break;
                }
            }
            boolean satVariables = true;
            for (int j = 0; j < parents.length; j++) {
                if (!(children[j].getVariable(i) instanceof SatelliteVariable)) {
                    satVariables = false;
                    break;
                }
            }
            if (constelVariables) {
                ConstellationVariable[] input = new ConstellationVariable[children.length];
                for (int j = 0; j < children.length; j++) {
                    input[j] = (ConstellationVariable) children[j].getVariable(i);
                }
                ConstellationVariable[] output = evolve(input);
                for (int j = 0; j < children.length; j++) {
                    children[j].setVariable(i, output[j]);
                }
            } else if (satVariables) {
                //if the solution is composed of satellite variables
                SatelliteVariable[] input = new SatelliteVariable[children.length];
                for (int j = 0; j < children.length; j++) {
                    input[j] = (SatelliteVariable) children[j].getVariable(i);
                }
                SatelliteVariable[] output = evolve(input);
                for (int j = 0; j < children.length; j++) {
                    children[j].setVariable(i, output[j]);
                }
            }
        }
        return children;
    }

    /**
     * Constellations can have the same number of satellites. If they do not,
     * then a number of satellites equal to the number of satellites in the
     * smallest constellations will be grouped randomly with satellites from the
     * other constellations and crossed using the given operation
     *
     * @param constellations constellations to recombine
     * @return recombined constellation variables
     */
    private ConstellationVariable[] evolve(ConstellationVariable[] constellations) {
        //find the minimum number of satellites contained in any of the constellations
        int minNSats = Integer.MAX_VALUE;
        for (int i = 0; i < constellations.length; i++) {
            minNSats = FastMath.min(minNSats, constellations[i].getNumberOfSatellites());
        }

        //create a 2-D array of all parent satellite variables involved
        SatelliteVariable[][] parentVars = new SatelliteVariable[minNSats][constellations.length];
        for (int i = 0; i < constellations.length; i++) {
            HashSet<Integer> parentsToCross = new HashSet<>();
            while(parentsToCross.size() < minNSats){
                parentsToCross.add(PRNG.nextInt(constellations[i].getNumberOfSatellites()));
            }
            
            int j = 0;
            int count = -1;
            for (SatelliteVariable var : constellations[i].getSatelliteVariables()) {
                count++;
                if(!parentsToCross.contains(count)){
                    continue;
                }
                parentVars[j][i] = var;
                j++;
            }
        }

        //create a 2-D array of all children satellite variables
        SatelliteVariable[][] childrenVars = new SatelliteVariable[minNSats][constellations.length];
        for (int i = 0; i < minNSats; i++) {
            childrenVars[i] = evolve(parentVars[i]);
        }

        ConstellationVariable[] out = new ConstellationVariable[constellations.length];
        for (int i = 0; i < constellations.length; i++) {
            out[i] = (ConstellationVariable) constellations[i].copy();
            ArrayList<SatelliteVariable> satellites = new ArrayList<>(minNSats);
            for (int j = 0; j < minNSats; j++) {
                satellites.add(childrenVars[j][i]);
            }
            out[i].setSatelliteVariables(satellites);
        }
        return out;
    }

    /**
     * Operates on the real-valued orbital elements with the given operator
     *
     * @param satellites The satellite variables to operate on
     * @return the modified satellite variables. They are new instances
     */
    private SatelliteVariable[] evolve(SatelliteVariable[] satellites) {
        Solution[] parents = new Solution[satellites.length];
        for (int i = 0; i < satellites.length; i++) {
            Solution parent = new Solution(6, 0);
            SatelliteVariable sat = satellites[i];
            parent.setVariable(0, new RealVariable(sat.getSma(), sat.getSmaBound().getLowerBound(), sat.getSmaBound().getUpperBound()));
            parent.setVariable(1, new RealVariable(sat.getEcc(), sat.getEccBound().getLowerBound(), sat.getEccBound().getUpperBound()));
            parent.setVariable(2, new RealVariable(sat.getInc(), sat.getIncBound().getLowerBound(), sat.getIncBound().getUpperBound()));
            parent.setVariable(3, new RealVariable(sat.getArgPer(), sat.getArgPerBound().getLowerBound(), sat.getArgPerBound().getUpperBound()));
            parent.setVariable(4, new RealVariable(sat.getRaan(), sat.getRaanBound().getLowerBound(), sat.getRaanBound().getUpperBound()));
            parent.setVariable(5, new RealVariable(sat.getTrueAnomaly(), sat.getAnomBound().getLowerBound(), sat.getAnomBound().getUpperBound()));
            parents[i] = parent;
        }

        Solution[] offspring = operator.evolve(parents);

        SatelliteVariable[] out = new SatelliteVariable[satellites.length];
        for (int i = 0; i < satellites.length; i++) {
            Solution child = offspring[i];
            SatelliteVariable sat = (SatelliteVariable) satellites[i].copy();
            sat.setSma(((RealVariable) child.getVariable(0)).getValue());
            sat.setEcc(((RealVariable) child.getVariable(1)).getValue());
            sat.setInc(((RealVariable) child.getVariable(2)).getValue());
            sat.setArgPer(((RealVariable) child.getVariable(3)).getValue());
            sat.setRaan(((RealVariable) child.getVariable(4)).getValue());
            sat.setTrueAnomaly(((RealVariable) child.getVariable(5)).getValue());
            out[i] = sat;
        }
        return out;
    }

}
