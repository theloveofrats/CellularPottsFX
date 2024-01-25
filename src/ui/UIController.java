package ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.List;

import biophysics.BiophysicalModel;
import com.sun.javafx.stage.StageHelper;
import ecl.DuFortDiffusion;
import ecl.ExtracellularLattice;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import models.*;
import potts.*;
import potts.Cell;
import ui.microscopy.BrightField;
import ui.microscopy.DIC;
import ui.microscopy.DarkField;
import ui.microscopy.PhaseContrast;
import utils.MyUtils;
import utils.Point;

import javax.imageio.ImageIO;
import javax.xml.crypto.Data;


public class UIController implements Initializable, Updatable {

    private FileChooser fc = new FileChooser();

    private List<LightMicroscopy> lm           = new ArrayList<LightMicroscopy>();

    public static UIController current;

    public LightMicroscopy microscope;

    public CellDataTable cdt;
    private CellularPotts sim;
    private IDatabase database;
    public ExtracellularLattice ecl;

    @FXML
    private AnchorPane topPanel;
    @FXML
    private Button btnMake;
    @FXML
    private Button btnFill;
    @FXML
    private Button btnPlay;
    @FXML
    private Button btnNewCell;


    @FXML
    private TextField fldArea;
    @FXML
    private TextField fldAreaStiffness;
    @FXML
    private TextField fldPerimeter;
    @FXML
    private TextField fldPerimeterStiffness;
    @FXML
    private TextField cellNameField;


    @FXML
    TableView<UIEntry> tvModelParams;
    @FXML
    TableView<UIEntry> tvECMParams;
    @FXML
    ListView<RadioButton> valveList;
    @FXML
    TableView<UIEntry> tvAdhesionStrengths;

    @FXML
    private ListView biophysParamsList;
    @FXML
    private ListView biophysValuesList;

    @FXML
    private ListView adhesionStrengthList;
    @FXML
    private TextField fldTemperature;
    @FXML
    private TextField fldChemotax;

    @FXML
    private CheckBox recordBox;

    @FXML
    private TextField pathToRecordFile;

    @FXML
    ListView<String> listCellTypes;
    @FXML
    ListView<String> AdhesionLabels;
    @FXML
    ChoiceBox<String> ddModel;
    @FXML
    ChoiceBox<String> ddPhysical;
    @FXML
    ChoiceBox<String> ddECModel;

    private ObservableList<String> blueprintNames = FXCollections.observableArrayList();

    @FXML
    ChoiceBox<String> ddMicroscopy;
    @FXML
    private Slider LEDBrightness;
    @FXML
    ListView<String> taggableList;
    @FXML
    private Slider laserBrightness;
    @FXML
    ColorPicker tagColour;

    @FXML
    private TextField fldWidth;
    @FXML
    private TextField fldHeight;



    @FXML
    private TextField fldPhotoactivationTarget;
    @FXML
    private Button btnPhotoactivate;



    @FXML
    private MenuItem optSave;

    @FXML
    private MenuItem optLoad;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        assert btnMake    != null : "fx:id=\"btnMake\" was not injected: check your FXML file.";
        assert btnFill    != null : "fx:id=\"btnFill\" was not injected: check your FXML file.";
        assert btnPlay    != null : "fx:id=\"btnPlay\" was not injected: check your FXML file.";
        assert btnNewCell != null : "fx:id=\"btnNewCell\" was not injected: check your FXML file.";

        current = this;

        optSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                SaveConfiguration();
            }
        });
        optLoad.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                LoadConfiguration();
            }
        });

        SetUpTableFactories();
        blueprintNames = listCellTypes.getItems();
        //listCellTypes.setItems(blueprintNames);
        database = new Database();
        database.Init(this);
        //Cell.InitCell();
        database.cells().add(new CellDataTable("Cell", 3000, 10, 550, 0.2));
        WriteCellFields();
        LoadMicroscopes();

        topPanel.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent ke) {
                if (ke.getCode() == KeyCode.ENTER && ke.isControlDown()) {
                    btnPlayHandler();
                }
            }
        });

        listCellTypes.getSelectionModel().select(0);
        listSelectionHandler();


        ddModel.getSelectionModel().selectFirst();

        ddModel.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                modelSelectHandler();
            }
        });

        ddPhysical.getSelectionModel().selectFirst();

        ddPhysical.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                physicalModelSelectHandler();
            }
        });

        ddECModel.getSelectionModel().selectFirst();

        ddECModel.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                ecmSelectHandler();
            }
        });

        ddMicroscopy.getSelectionModel().selectFirst();

        ddMicroscopy.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                microscopeSelectHandler();
            }
        });

        LEDBrightness.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                microscope.SetBrightness(new_val.intValue());
            }
        });

        tagColour.setOnAction(new EventHandler() {
            public void handle(Event t) {
                tagColourSelectedHandler();
            }
        });
        tagColour.getCustomColors().add(Color.TRANSPARENT);
        tagColour.getCustomColors().add(Color.RED);
        tagColour.getCustomColors().add(Color.GREEN);
        tagColour.getCustomColors().add(Color.BLUE);
        tagColour.getCustomColors().add(Color.YELLOW);
        tagColour.getCustomColors().add(Color.CYAN);


        // Set listeners for user input
        //HERE//

        /*paramValuesList.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView listView) {
                TextFieldListCell<Double> cell = new TextFieldListCell<>();
                cell.setConverter(new DoubleStringConverter());
                //if (cell.itemProperty().getValue() != null) {
                cell.itemProperty().addListener(new ChangeListener<Double>() {
                    @Override
                    public void changed(ObservableValue<? extends Double> observableValue, Double oldVal, Double newVal) {
                        //System.out.println("value: "+oldVal+" -> "+newVal);
                        if (newVal == null) return;
                        try {
                            handleValueChanged2(newVal);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ;
                    }
                });
                //}
                return cell;
            }
        });
        paramsList.setEditable(false);
        paramValuesList.setEditable(true);   */

        // Set listeners for user input
        biophysValuesList.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView listView) {
                TextFieldListCell<Double> cell = new TextFieldListCell<>();
                cell.setConverter(new DoubleStringConverter());
                //if (cell.itemProperty().getValue() != null) {
                cell.itemProperty().addListener(new ChangeListener<Double>() {
                    @Override
                    public void changed(ObservableValue<? extends Double> observableValue, Double oldVal, Double newVal) {
                        //System.out.println("value: "+oldVal+" -> "+newVal);
                        if (newVal == null) return;
                        try {
                            handleValueChanged2(newVal);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ;
                    }
                });
                //}
                return cell;
            }
        });
        biophysParamsList.setEditable(false);
        biophysValuesList.setEditable(true);


        adhesionStrengthList.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView listView) {
                TextFieldListCell<Double> cell = new TextFieldListCell<>();
                cell.setConverter(new DoubleStringConverter());
                cell.itemProperty().addListener(new ChangeListener<Double>() {
                    @Override
                    public void changed(ObservableValue<? extends Double> observableValue, Double aDouble, Double t1) {
                        if (t1 == null) return;
                        try {
                            handleValueChanged2(t1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                return cell;
            }
        });
        AdhesionLabels.setEditable(false);
        adhesionStrengthList.setEditable(true);

    };

    public double GetLaserBrightness(){
        return laserBrightness.getValue();
    }

    private void tagColourSelectedHandler(){
        if(cdt==null) return;
        if(listCellTypes.getSelectionModel().getSelectedItems().size()==0) return;
        if(taggableList.getSelectionModel().getSelectedItems().size()==0) return;

        Color clr = tagColour.getValue();
        String sType = taggableList.getSelectionModel().getSelectedItems().get((0));

        if(sType=="Free Expression"){
            cdt.cellTag = clr;
        }
        else if(sType=="Membrane-targeted"){
            cdt.membraneTag = clr;
        }

        else{
            if(cdt.model!=null) {
                if (cdt.model.GetElements().contains(sType)) cdt.model.GetTags().put(sType, clr);
            }
            if(ecl!=null) {
                if (ecl.GetTags().keySet().contains(sType)) {
                    ecl.GetTags().put(sType, clr);
                }
            }
        }
    }

    private void LoadMicroscopes(){

        ObservableList<String> microscopyNames = ddMicroscopy.getItems();
        microscopyNames.clear();

        lm.clear();
        lm.add(new BrightField());
        lm.add(new DarkField());
        lm.add(new DIC());
        lm.add(new PhaseContrast());

        for(LightMicroscopy m : lm){
            microscopyNames.add(m.GetName());
        }
        //ddMicroscopy.setItems(microscopyNames);

        microscope = lm.get(lm.size()-1);
    }

    private LightMicroscopy SelectMicroscope(String sName){
        for(LightMicroscopy m : lm){
            if(m.GetName()==sName) return m;
        }
        return null;
    }

    private void modelSelectHandler(){

        Model m = database.GetModel(ddModel.getValue());

        // If no change, return;
        if(cdt.model==m) return;

        if(cdt!=null) {
            if(m==null) cdt.model=null;
            else{
                if (cdt.model == null) cdt.model = MyUtils.GetNewInstanceOfType(m);
                else if (cdt.model.getClass() != m.getClass()) cdt.model = MyUtils.GetNewInstanceOfType(m);
            }
            if(cdt.model!=null && sim!=null){
                cdt.model.Initialise(sim);
            }
        }
        UpdateTaggableList();
        GetModelParams();
        if(sim!=null) {
            sim.UpdateCells(cdt.TypeName);
        }
    }

    private void ecmSelectHandler(){

        ExtracellularLattice m = database.GetECM(ddECModel.getValue());

        boolean newInstance = false;
        if(m==null) {
            ecl = null;
            GetECMParams();
        }
        else{
            if (ecl == null || m.getClass() != ecl.getClass()) newInstance = true;
            GetECMParams();
            if (newInstance) {

                ecl = MyUtils.GetNewInstanceOfType(m);

                if (sim != null) {
                    ecl.Initialise(sim);
                    sim.viewer.ec = ecl;
                }
                UpdateTaggableList();
                try {
                    ImportCurrentModelFields();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void physicalModelSelectHandler(){

        BiophysicalModel m = database.GetBiophysicalModel(ddPhysical.getValue());

        if(cdt!=null) {
            if(m!=null) {
                if (cdt.biophysicalModel == null) cdt.biophysicalModel = MyUtils.GetNewInstanceOfType(m);
                else if (cdt.biophysicalModel.getClass() != m.getClass()) cdt.biophysicalModel = MyUtils.GetNewInstanceOfType(m);
            }
            if(cdt.model!=null&&sim!=null){
                cdt.model.Initialise(sim);
            }
        }
        UpdateTaggableList();
        try{
            ImportCurrentModelFields();
        }
        catch (IllegalAccessException e){
            e.printStackTrace();
        }
    }

    private void microscopeSelectHandler(){
        microscope = SelectMicroscope(ddMicroscopy.getValue());
    }

    @FXML
    private void btnNewCellHandler(){

        String sName = cellNameField.getText();
        if(sName==null || sName.length()==0 || listCellTypes.getItems().contains(sName)) {
            sName = "Cell Type " + Integer.toString(listCellTypes.getItems().size() + 1);
        }

        CellDataTable newTable = new CellDataTable(sName);
        newTable.UpdateInformation(sName, cdt.a0, cdt.aK, cdt.p0, cdt.pK);
        cdt = newTable;
        database.cells().add(newTable);

        cellNameField.setText(sName);
        UpdateCellTypeList();
        listCellTypes.getSelectionModel().select(cdt.TypeName);
    }

    @FXML
    public void onClickRecordBox(){
        if(this.sim ==null) {
            recordBox.setSelected(false);
            return;
        }

        this.sim.recording = recordBox.isSelected();

    }

    @FXML
    private void OnPhotoactivate(){
        if(sim!=null) {
            if(taggableList.getSelectionModel().getSelectedItem()!=null) {
                String target = taggableList.getSelectionModel().getSelectedItem();
                double value = 0;
                try {
                    value = Double.parseDouble(fldPhotoactivationTarget.getText());
                } catch (NumberFormatException e) {
                    return;
                }
                sim.TryPhotoactivate(target, value);
            }
        }
    }

    @FXML
    public void pathToRecordFileChanged(){
        if(sim==null) return;
        sim.recordPath = pathToRecordFile.getText();

        System.out.println("Changed record path to "+sim.recordPath);

        File f = new File(sim.recordPath);
        if(!f.exists()){
            if(f.mkdirs()) System.out.println("Made output directory");
            else System.out.println("Failed to make output directory");
        }
    }

    @FXML
    private void listSelectionHandler(){
        String sType = listCellTypes.getSelectionModel().getSelectedItems().get((0));
        cdt = database.GetCellType(sType);
        if(cdt==null) return;

        WriteCellFields();
        UpdateTaggableList();
        try{
            ImportCurrentModelFields();
        }
        catch(Exception e){ e.printStackTrace(); }

    }


    @FXML
    private void TaggableSelectionHandler(){
        if(cdt==null) return;
        if(listCellTypes.getSelectionModel().getSelectedItems().size()==0) return;
        if(taggableList.getSelectionModel().getSelectedItems().size()==0) return;

        String sType = taggableList.getSelectionModel().getSelectedItems().get((0));
        Color tagC = Color.TRANSPARENT;


        if(sType=="Free Expression"){
            tagC = cdt.cellTag;
        }
        else if(sType=="Membrane-targeted"){
            tagC = cdt.membraneTag;
        }

        else{
            if(cdt.model!=null) {
                if (cdt.model.GetTags().containsKey(sType)) tagC = cdt.model.GetTags().get(sType);
            }
            if(ecl!=null){
                if(ecl.GetTags().containsKey(sType)){
                    tagC = ecl.GetTags().get(sType);
                }
            }
        }
        tagColour.setValue(tagC);
    }

    private void UpdateTaggableList(){

        ObservableList<String> taggables = taggableList.getItems();
        taggables.clear();

        if(cdt!=null) {

            taggables.add("Free Expression");
            taggables.add("Membrane-targeted");

            if (cdt.model != null) {
                for (String s : cdt.model.GetElements()) {
                    taggables.add(s);
                }
            }
            if(ecl!=null){
                for(String s : ecl.GetTags().keySet()){
                    taggables.add(s);
                }
            }
        }
        //taggableList.setItems(taggables);
    }

    @FXML private void btnTemperatureHandler(){

        boolean fault = false;
        double dTemp = sim!=null ? sim.taxis : 5;

        try{
            dTemp = Double.parseDouble(fldTemperature.getText());
        }
        catch(NumberFormatException e){

            fault = true;
        }
        if(fault||dTemp<5){
            dTemp = 5;
        }

        else if(sim!=null) sim.BoltzT = dTemp;
        fldTemperature.setText(Double.toString(dTemp));
    }

    @FXML private void btnTaxisHandler(){

        boolean fault = false;
        double dTax = sim!=null ? sim.taxis : 0;

        try{
            dTax = Double.parseDouble(fldChemotax.getText());
        }
        catch(NumberFormatException e) {

            fault = true;
        }

        if(sim!=null) sim.taxis = dTax;
        fldChemotax.setText(Double.toString(dTax));
    }

    @FXML
    private void btnMakeHandler(){

        int width  = 500;
        int height = 500;

        try {
            width = Integer.parseInt(fldHeight.getText());
        }
        catch(NumberFormatException e){}
        try{
            height = Integer.parseInt(fldWidth.getText());
        }
        catch(NumberFormatException e){}


        if(width<0)  width  = -width;
        if(height<0) height = -height;

        MakeNewSim(width, height, true);
    }


    private void MakeNewSim(int w, int h, boolean init){

        this.sim = new CellularPotts(w, h);
        this.sim.BoltzT = Double.parseDouble(fldTemperature.getText());

        for(CellDataTable table : database.cells()) {
            if(table.model!=null) table.model.Initialise(sim);
        }

        if(init){
            if(ecl!=null) ecl.Initialise(this.sim);
            this.sim.viewer.ec = ecl;
        }

        sim.Run();
    }

    private void StartLoadedSim(){
        sim.Run();
    }

    @FXML
    public void openImageAsEnvironment(){

        FileChooser fc = new FileChooser();
        fc.setTitle("Open Environment File");
        File f = fc.showOpenDialog(this.topPanel.getScene().getWindow());
        if(f!=null){

            try{
                MakeSimFromImageFile(f);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void MakeSimFromImageFile(File f) throws IOException {

        if(f==null || !f.exists()) return;
        if(!f.isFile()) return;

        if(this.sim!=null) {
            this.sim.paused = true;
            this.sim.terminated = true;
            this.sim = null;
        }
        BufferedImage bi = ImageIO.read(f);

        MakeNewSim(bi.getWidth(), bi.getHeight(), false);

        for(int i=0; i<bi.getWidth(); i++) {
            for (int j = 0; j < bi.getHeight(); j++) {

                int rgb = bi.getRGB(i, j);
                int r = (rgb >> 16) & 0x000000FF;
                int g = (rgb >>8 ) & 0x000000FF;
                int b = (rgb) & 0x000000FF;

                int val = ((r<50) && (g<50) && (b<50) ? -1 : 0);

                if(r > 200 && g<= 200 && b <= 200) val-=5;
                if(r <=200 && g>  200 && b <= 200) val-=10;
                if(r <=200 && g<= 200 && b >  200) val-=15;


                if(r <=200 && g > 200 && b > 200) val-=20;
                if(r > 200 && g<= 200 && b > 200) val-=25;
                if(r > 200 && g > 200 && b<= 200) val-=30;

                sim.SetID(i,j,val);
            }
        }

        for(CellDataTable table : database.cells()){
            if(table.model!=null) table.model.Initialise(sim);
        }
        if(ecl!=null){
            ecl.Initialise(this.sim);
            this.sim.viewer.ec = ecl;
        }
    }

    @FXML
    public void btnPlayHandler(){

        if(this.sim!=null) {
            this.sim.paused = !this.sim.paused;
            System.out.println("Steps: "+ this.sim.mcsCount);
        }



    }
    @FXML
    private void ReadCellFields(){

        cdt.UpdateInformation(
                cellNameField.getText(),
                Double.parseDouble(fldArea.getText()),
                Double.parseDouble(fldAreaStiffness.getText()),
                Double.parseDouble(fldPerimeter.getText()),
                Double.parseDouble(fldPerimeterStiffness.getText())
        );
        if(sim!=null){
            sim.UpdateCells(cdt.TypeName);
        }
        UpdateUI(false);
    }

    private void WriteCellFields(){
        if(cdt==null){
            fldArea.setText(Double.toString(0.));
            fldAreaStiffness.setText(Double.toString(0.));
            fldPerimeter.setText(Double.toString(0.));
            fldPerimeterStiffness.setText(Double.toString(0.));
            cellNameField.setText("");
        }
        else {
            fldArea.setText(Double.toString(cdt.a0));
            fldAreaStiffness.setText(Double.toString(cdt.aK));
            fldPerimeter.setText(Double.toString(cdt.p0));
            fldPerimeterStiffness.setText(Double.toString(cdt.pK));
            cellNameField.setText(cdt.TypeName);

            if(cdt.model!=null){
                ddModel.getSelectionModel().select(cdt.model.GetName());
            }
            else                ddModel.getSelectionModel().select("None");

            if(cdt.biophysicalModel!=null){
                ddPhysical.getSelectionModel().select(cdt.biophysicalModel.GetName());
            }
            else                ddPhysical.getSelectionModel().select("None");
        }
    }

    private void GetAdhesionStrengths(){

        /*ObservableList<String> cells = listCellTypes.getItems();
        Map<String,Double> adhk      = cdt.biophysicalModel.GetAdhesionRules();

        for(String label : cells){
            adhc.add(label);
            if(adhk.keySet().contains(label)){
                adhv.add(adhk.get(label));
            }
            else{
                adhv.add(0.0);
                adhk.put(label, 0.0);
            }
        }  */
    }

    private void GetModelParams(){

        tvModelParams.getItems().clear();
        if(cdt==null || cdt.model==null) return;

        List<UIEntry> uiEntries = new ArrayList<UIEntry>();
        Class c = cdt.model.getClass();
        while (c != null) {
            uiEntries.addAll(UIEntry.GetUILinkedFields(c, cdt.model));
            c = c.getSuperclass();
        }

        for(UIEntry entry  : uiEntries){
            tvModelParams.getItems().add(entry);
        }
    }

    private void GetECMParams(){

        tvECMParams.getItems().clear();
        if(ecl==null) return;

        List<UIEntry> uiEntries = new ArrayList<UIEntry>();
        Class c = ecl.getClass();
        while (c != null) {
            uiEntries.addAll(UIEntry.GetUILinkedFields(c, ecl));
            c = c.getSuperclass();
        }

        for(UIEntry entry  : uiEntries){
            tvECMParams.getItems().add(entry);
        }
        for(int i=0; i<ecl.GetNValves(); i++){

        }
    }

    private void SetUpTableFactories(){

        TableColumn<UIEntry, String> labels;
        TableColumn<UIEntry, String> values;

        //Model
        labels = (TableColumn) tvModelParams.getColumns().get(0);
        values = (TableColumn) tvModelParams.getColumns().get(1);
        values.setCellFactory(UIEntry.GetUIEntryCallback());

        labels.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue().Label().getValue()));
        values.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue().Value().getValue()));


        //ECM
        labels = (TableColumn) tvECMParams.getColumns().get(0);
        values = (TableColumn) tvECMParams.getColumns().get(1);
        values.setCellFactory(UIEntry.GetUIEntryCallback());

        labels.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue().Label().getValue()));
        values.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue().Value().getValue()));
        /*
        //Adhesions
        labels = (TableColumn) tvAdhesionStrengths.getColumns().get(0);
        values = (TableColumn) tvAdhesionStrengths.getColumns().get(1);
        values.setCellFactory(UIEntry.GetUIEntryCallback());

        labels.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue().Label().getValue()));
        values.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue().Value().getValue())); */
    }




    private void ImportCurrentModelFields() throws IllegalAccessException{

       ;

        GetModelParams();
        GetECMParams();
        GetAdhesionStrengths();

         /*
        ObservableList<String> ph_items  =  biophysParamsList.getItems();
        ObservableList<Double> ph_values =  biophysValuesList.getItems();
        ph_items.clear();
        ph_values.clear();

        ObservableList<String> adhc  = AdhesionLabels.getItems();
        ObservableList<Double> adhv  = adhesionStrengthList.getItems();
        adhc.clear();
        adhv.clear();

        if(ecl!=null){
            //Iterate all tagged fields and import
            for (Field field : ecl.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(UIValue.class)) {
                    if (field.getType() != Double.TYPE) continue;
                    field.setAccessible(true);
                    ec_items.add(field.getName());
                    ec_values.add(field.getDouble(ecl));
                }
            }
        }

        if(cdt.biophysicalModel!=null) {
            //biophysParamsList.setEditable(false);
            //biophysValuesList.setEditable(true);

            //Iterate all tagged fields and import
            for (Field field : cdt.biophysicalModel.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(UIValue.class)) {
                    if (field.getType() != Double.TYPE) continue;
                    field.setAccessible(true);
                    ph_items.add(field.getName());
                    ph_values.add(field.getDouble(cdt.biophysicalModel));
                }
            }

            //Now for the adhesions- first go through cells!
            ObservableList<String> cells = listCellTypes.getItems();
            Map<String,Double> adhk      = cdt.biophysicalModel.GetAdhesionRules();

            for(String label : cells){
                adhc.add(label);
                if(adhk.keySet().contains(label)){
                    adhv.add(adhk.get(label));
                }
                else{
                    adhv.add(0.0);
                    adhk.put(label, 0.0);
                }
            }

        }
        //paramsList.setItems(items);
        //paramValuesList.setItems(values);    */
    }


    private void handleValueChanged2(double newVal) throws NoSuchFieldException, IllegalAccessException{
        if (cdt == null) return;

        ObservableList<String> ph_items  = biophysParamsList.getItems();
        ObservableList<Double> ph_values = biophysValuesList.getItems();

        ObservableList<String> adh_labels = AdhesionLabels.getItems();
        ObservableList<Double> adh_values  = adhesionStrengthList.getItems();

        GetModelParams();
        GetECMParams();

        if(cdt.biophysicalModel !=  null) {
            for (int i = 0; i < ph_items.size(); i++) {
                String label = ph_items.get(i);
                if (label == null) continue;
                Field fld = cdt.biophysicalModel.getClass().getDeclaredField(label);
                fld.setAccessible(true);
                fld.setDouble(cdt.biophysicalModel, ph_values.get(i));
            }


            Map<String,Double> adh = cdt.biophysicalModel.GetAdhesionRules();
            for (int i = 0; i < adh_labels.size(); i++) {
                String label = adh_labels.get(i);
                if (label == null) continue;

                adh.put(adh_labels.get(i), adh_values.get(i));
            }
        }
    }


    @Override
    public void UpdateUI(boolean reinitialise) {
        if(reinitialise) {
            UpdateModelList();
            UpdateBiophysicalModelList();
            UpdateECModelList();
        }
        UpdateCellTypeList();
        UpdateTaggableList();
        UpdateValveList();

        try{
            ImportCurrentModelFields();
        }
        catch(Exception e){e.printStackTrace();}

        if(cdt!=null) {
            listCellTypes.getSelectionModel().select(cdt.TypeName);
            WriteCellFields();

        }
    }


    private void UpdateModelList(){
        ObservableList<String> modelNames = ddModel.getItems();
        modelNames.clear();
        if(!modelNames.contains("None")) modelNames.add("None");
        for(Model m : database.models()) if(!modelNames.contains(m.GetName())) modelNames.add(m.GetName());
    }

    private void UpdateBiophysicalModelList(){
        ObservableList<String> modelNames = ddPhysical.getItems();
        modelNames.clear();
        modelNames.add("None");
        for(BiophysicalModel bpm : database.biophysicalModels()) modelNames.add(bpm.GetName());
    }

    private void UpdateECModelList(){
        ObservableList<String> modelNames = ddECModel.getItems();
        modelNames.clear();
        modelNames.add("None");
        for(ExtracellularLattice ecm : database.ECMs()) modelNames.add(ecm.GetName());
    }


    private void UpdateCellTypeList(){

        blueprintNames.clear();
        for(CellDataTable cdt : database.cells()){
            if(!blueprintNames.contains(cdt.TypeName)) blueprintNames.add(cdt.TypeName);
        }
    }

    private void UpdateValveList(){
        valveList.getItems().clear();
        if(ecl==null) return;
        int numButtons = ecl.GetNValves();
        for(int i=0; i<numButtons; i++){
            RadioButton valveButton = new RadioButton();
            final int valveNum = i;
            valveButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if(valveButton.isSelected()){
                        ecl.OpenValve(valveNum);
                    }
                    else{
                        ecl.CloseValve(valveNum);
                    }
                }
            });
            valveList.getItems().add(valveButton);
        }
    }



    @FXML
    private void btnFillHandler(){

        if(sim!=null) {
            if(ecl==null) return;

            Rectangle rect = sim.GetFocusRect();
            ecl.TakeCommand("FILL", new Object[]{rect});
        }

    }

    private void SaveConfiguration() {

        if (sim != null) {
            fc.setTitle("Save Configuration");
            File f = fc.showSaveDialog(topPanel.getScene().getWindow());
            if(f==null) return;
            boolean isPaused = sim.paused;
            sim.paused = true;
            synchronized (sim) {
                System.out.println("SAVING...");
                try {
                    FileOutputStream fileOut =
                            new FileOutputStream(f);
                    try {
                        ObjectOutputStream out = new ObjectOutputStream(fileOut);
                        out.writeObject(sim);
                        out.close();
                        fileOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                ;
            }
            sim.paused = isPaused;
        }
    }

    private void LoadConfiguration() {
        if (sim != null) {
            sim.terminated = true;
            sim.viewer.Close();
            sim = null;
        }
        database.Init(this);

        try {
            fc.setTitle("Load Configuration");
            File f = fc.showOpenDialog(topPanel.getScene().getWindow());
            if(f==null) return;
            FileInputStream fileIn = new FileInputStream(f);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            sim = (CellularPotts) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
            return;
        }

        database.cells().clear();
        for(Cell c : sim.cells){
            if(!database.cells().contains(c.cdt)) {
                database.cells().add(c.cdt);
                if(c.cdt.model!=null){
                    database.SetModel(c.cdt.model.GetName(), c.cdt.model);
                }
            }
        }

        UpdateTaggableList();
        GetModelParams();

        sim.viewer = new CPViewer(sim);
        StartLoadedSim();
    }
}
