package potts;

import biophysics.BasicBiophysicalModel;
import biophysics.BiophysicalModel;
import com.sun.xml.internal.bind.v2.schemagen.xmlschema.LocalAttribute;
import ecl.DuFortDiffusion;
import ecl.ExtracellularLattice;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import models.*;
import ui.Updatable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 08/03/17.
 */
public class Database implements IDatabase {

    Updatable UI;

    ObservableList<Model> models = FXCollections.observableArrayList();
    ObservableList<BiophysicalModel> biophys = FXCollections.observableArrayList();
    ObservableList<CellDataTable> cellTypes = FXCollections.observableArrayList();
    ObservableList<ExtracellularLattice> ecms = FXCollections.observableArrayList();

    @Override
    public List<Model> models() {
        return models;
    }

    @Override
    public List<BiophysicalModel> biophysicalModels(){
        return biophys;
    }

    @Override
    public List<CellDataTable> cells(){
        return  cellTypes;
    }

    @Override
    public List<ExtracellularLattice> ECMs() {
        return ecms;
    }

    @Override
    public CellDataTable GetCellType(String sLabel){

        for(CellDataTable cdt : cellTypes){
            if(cdt.TypeName == sLabel) return cdt;
        }
        return null;
    }

    @Override
    public Model GetModel(String sLabel){
        for(Model m : models){
            if(m.GetName() == sLabel) return m;
        }
        return null;
    }

    @Override
    public void SetModel(String sLabel, Model m){
        List<Model> remove = new ArrayList<>();
        for(Model m2 : models){
            if(m2.GetName() == sLabel) remove.add(m2);
        }
        models.removeAll(remove);
        models.add(m);
    }

    @Override
    public ExtracellularLattice GetECM(String sLabel){
        for(ExtracellularLattice m : ecms){
            if(m.GetName() == sLabel) return m;
        }
        return null;
    }

    @Override
    public BiophysicalModel GetBiophysicalModel(String sLabel){
        for(BiophysicalModel b : biophys){
            if(b.GetName() == sLabel) return b;
        }
        return null;
    }

    @Override
    public void Init(Updatable UI){
        this.UI = UI;

        models.addListener(new ListChangeListener<Model>() {
            @Override
            public void onChanged(Change<? extends Model> change) {
                Changed();
            }
        });
        biophys.addListener(new ListChangeListener<BiophysicalModel>() {
            @Override
            public void onChanged(Change<? extends BiophysicalModel> change) {
                Changed();
            }
        });
        cellTypes.addListener(new ListChangeListener<CellDataTable>() {
            @Override
            public void onChanged(Change<? extends CellDataTable> change) {
                Changed();
            }
        });
        ecms.addListener(new ListChangeListener<ExtracellularLattice>() {
            @Override
            public void onChanged(Change<? extends ExtracellularLattice> change) {
                Changed();
            }
        });


        Reset();
    }


        @Override
    public void Reset(){
            models.clear();
            biophys.clear();

        FillDatabase();

    }

    private void FillDatabase(){

        models.add(new MaxActModel());
        models.add(new SARModel());
        models.add(new SAR_FAM());
        models.add(new ScarModelGrant());
        models.add(new ScarModelGrantRacPlaced());
        models.add(new SAR_Curvature());

        biophys.add(new BasicBiophysicalModel());

        ecms.add(new DuFortDiffusion());
    }

    @Override
    public void Changed(){
        UI.UpdateUI(true);
    }
}
