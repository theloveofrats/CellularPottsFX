package models;

import com.sun.org.apache.xpath.internal.operations.Mod;
import ecl.ExtracellularLattice;
import javafx.scene.paint.Color;
import models.Model;
import potts.Cell;
import potts.CellularPotts;
import utils.Point;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Created by luke on 23/08/16.
 */
public abstract class BiochemicalModel implements Model, Serializable {

    public    ArrayList<String> nodes = new ArrayList<String>();
    public CellularPotts cp;
    protected String name;

    public    Map<String, double[][]> modelGrid;

    public transient Map<String, Color>      modelTags;
    protected Map<String, Double>     modelParams;

    public BiochemicalModel(){
        LoadNodes();
    }

    public ExtracellularLattice ecl;

    @Override
    public void ClearLostPoint(Point point){
        for(String k : modelGrid.keySet()){
            modelGrid.get(k)[point.x][point.y] = 0;
        }
    }

    @Override
    public Map<String, Color> GetTags(){
        return modelTags;
    }

    @Override
    public void Initialise(CellularPotts cp){
        this.cp = cp;
        for(String s : modelGrid.keySet()) modelGrid.put(s, new double[cp.w][cp.h]);
        if(cp.ecl!=null) ecl = cp.ecl;
    }

    @Override
    public List<String> GetElements(){
        return new ArrayList<String>(modelGrid.keySet());
    }

    public double GetLocalConcentration(String chemical, Point point){

        return GetLocalConcentration(chemical, point, "POINT");

    }

    public void SetLocalConcentration(String chemical, Point point, double value){

       double[][] grC = modelGrid.get(chemical);
        if(grC!=null){
            if(point.x<0 || point.x>= grC.length || point.y<0 || point.y>grC[0].length){
                return;
            }

            grC[point.x][point.y] = value;
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException{
        s.defaultWriteObject();
        for(String k : nodes){
            Color c = modelTags.get(k);

            s.writeDouble(c.getRed());
            s.writeDouble(c.getGreen());
            s.writeDouble(c.getBlue());
            s.writeDouble(c.getOpacity());
        }
    }
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
        s.defaultReadObject();
        this.modelTags = new HashMap<>();

        double r,g,b,a;

        for(String k : nodes){

            r = s.readDouble();
            g = s.readDouble();
            b = s.readDouble();
            a = s.readDouble();

            modelTags.put(k, new Color(r,g,b,a));
        }
    }

    public double GetLocalConcentration(String chemical, Point point, String type){

        if(type=="POINT") return modelGrid.get(chemical)[point.x][point.y];

        else if(type=="ARITHMETIC"){

            int c = cp.GetID(point);
            int cN = 0;
            double total = 0;

            for(int i=point.x-1; i<=point.x+1; i++){
                for(int j=point.y-1; j<=point.y+1; j++){
                    if(cp.GetID(i,j)==c){
                        cN++;
                        total+= modelGrid.get(chemical)[i][j];
                    }
                }
            }
            return total/cN;

        }

        else if(type=="GEOMETRIC"){

            int c = cp.GetID(point);
            double total = 0;
            double dS;
            int cN = 0;

            for(int i=point.x-1; i<=point.x+1; i++){
                for(int j=point.y-1; j<=point.y+1; j++){
                    if(cp.GetID(i,j)==c){
                        cN++;
                        dS = modelGrid.get(chemical)[i][j];
                        if(dS>0){
                            total+= Math.log(dS);
                        }
                    }
                }
            }
            return Math.exp(total/cN);

        }
        return 0;
    }



    /*@Override
    public List<String> GetParams(){
        if(modelParams==null){
            LoadParams();
        }
        return new ArrayList<String>(modelParams.keySet());
    }*/

    /*public double GetParam(String s){
        if(modelParams==null){
            LoadParams();
        }
        return modelParams.get(s);
    }

    @Override
    public void SetParam(String key, double val){
        if(modelParams==null){
            //LoadParams();
        }
        modelParams.put(key, val);
        UpdatePrimitivesFromParameterList();
    }   */

    public void LoadNodes(){

        this.modelGrid = new HashMap<>();
        this.modelTags = new HashMap<>();
        for(String s : nodes){
            modelTags.put(s, Color.TRANSPARENT);
            modelGrid.put(s, new double[][]{});
        }
    };

    //public abstract void LoadParams();
    //public abstract void UpdatePrimitivesFromParameterList();
}
