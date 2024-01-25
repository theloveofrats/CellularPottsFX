package biophysics;

import models.BiochemicalModel;
import models.Model;
import potts.Cell;
import potts.CellularPotts;
import utils.MyUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luke on 31/08/16.
 */
public interface BiophysicalModel {

    Map<String, Double> GetAdhesionRules();

    double GetAdhesion(Cell c1, int[] p1, Cell c2, int[] p2);

    public String GetName();

    public void Initialise(CellularPotts cp);
}
