package potts;

import biophysics.BiophysicalModel;
import ecl.ExtracellularLattice;
import models.Model;
import ui.UIController;
import ui.Updatable;

import java.awt.*;
import java.util.List;
import java.util.function.Function;

/**
 * Created by luke on 08/03/17.
 */
public interface IDatabase {

    public List<Model> models();
    public List<ExtracellularLattice> ECMs();
    public List<BiophysicalModel> biophysicalModels();
    public List<CellDataTable> cells();

    public CellDataTable GetCellType(String sLabel);
    public ExtracellularLattice GetECM(String sLabel);
    public Model GetModel(String sLabel);
    public void SetModel(String label, Model model);
    public BiophysicalModel GetBiophysicalModel(String sLabel);

    public void Init(Updatable UI);
    public void Changed();
    public void Reset();
}
