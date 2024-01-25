package ecl;

import javafx.scene.paint.Color;
import potts.CellularPotts;
import utils.Point;

import java.util.List;
import java.util.Map;

/**
 * Created by luke on 05/03/17.
 */
public interface ExtracellularLattice {


    public String GetName();
    public void Update();
    public double GetConcentration(String species, Point point, String type);
    public double GetOccupancy(String species, Point point, String type);
    public void ChangeConcentration(int i, int j, double dC);
    public Map<String, Color> GetTags();
    public void Initialise(CellularPotts cp);
    public void TakeCommand(String sCom, Object[] args);
    public void OpenValve(int i);
    public void CloseValve(int i);
    public int  GetNValves();

}
