package biophysics;

import potts.Cell;
import potts.CellularPotts;
import ui.UIValue;
import utils.MyUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luke on 08/03/17.
 */
public class BasicBiophysicalModel implements BiophysicalModel {

    @UIValue
    public double adh_mult = 1.0;

    public Map<String, Double> AdhesionRules = new HashMap<>();
    private CellularPotts cp;

    @Override
    public Map<String, Double> GetAdhesionRules(){
        return AdhesionRules;
    }

    @Override
    public String GetName(){
        return "Basic";
    }

    @Override
    public double GetAdhesion(Cell c1, int[] p1, Cell c2, int[] p2){

        double dAdh = 0;
        if(c1==c2) return SelfAdhesion();    // Special case: this is the EXACT same cell,
        // not another cell of the same type.

        String cellType = c2.cdt.TypeName;
        dAdh += AdhesionRules.containsKey(cellType) ? AdhesionRules.get(cellType) : 0;

        // Assumes that c1 IS a biophysical model, as c1 is THIS model's cell.
        return dAdh;
    }

    @Override
    public void Initialise(CellularPotts cp) {
         this.cp = cp;
    }



    public double SelfAdhesion(){
        return 0;
    };

    /*public double GetDefendBias(int[] p){
        return GetAdhesionAtPoint(p);
    } */

    public double GetAdhesionAtPoint(int[] p){
        if(cp==null) return 0;
        int[][] ngh = MyUtils.neighbours(p);

        double dAdh = 0;

        Cell c1 = cp.cells.get(cp.GetID(p) - 1);

        for(int[] ng : ngh){
            int cid2 = (cp.GetID(ng));
            if( cid2<=0) continue;
            Cell c2 = cp.cells.get(cid2-1);
            dAdh += ((BiophysicalModel) (c1.model)).GetAdhesion(c1, p, c2, ng);
            if(c2.model instanceof BiophysicalModel) dAdh += ((BiophysicalModel) (c2.model)).GetAdhesion(c2, ng, c1, p);  //Get reciprocal if applicable.
        }
        return dAdh;
    }
}
