package models;

import com.sun.org.apache.xpath.internal.operations.Mod;
import javafx.scene.paint.Color;
import potts.Cell;
import potts.CellularPotts;
import utils.Point;

import javax.swing.text.html.ListView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luke on 23/08/16.
 */
public interface Model {

    public String GetName();

    public List<String> GetElements();

    public Map<String, Color> GetTags();

    //public List<String> GetParams();

    //public double GetParam(String key);

    //public void SetParam(String key, double val);

    public double GetAttackBias(Point point);

    public double GetDefendBias(Point point);

    public double GetLocalConcentration(String s, Point point);

    public void SetLocalConcentration(String s, Point point, double value);

    public void Update(Cell c);

    public void ClearLostPoint(Point point);

    public void   InitialiseConqueredPoint(Point target, Point source);

    public void Initialise(CellularPotts cp);

}
