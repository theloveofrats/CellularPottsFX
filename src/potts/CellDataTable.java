package potts;

import biophysics.BiophysicalModel;
import javafx.scene.paint.Color;
import models.Model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by luke on 21/08/16.
 */
public class CellDataTable implements Serializable{

    public static CellDataTable focus;

    public Model model;
    public BiophysicalModel biophysicalModel;


    public String TypeName = "";

    public double a0 = 0;
    public double aK = 0.;

    public double p0 = 0.;
    public double pK = 0.;


    public transient Color cellTag = Color.TRANSPARENT;
    public transient Color membraneTag = Color.TRANSPARENT;

    public CellDataTable(String typeName){

        this.TypeName = typeName;
    }

    private void writeObject(ObjectOutputStream s) throws IOException{
        s.defaultWriteObject();

        s.writeDouble(cellTag.getRed());
        s.writeDouble(cellTag.getGreen());
        s.writeDouble(cellTag.getBlue());
        s.writeDouble(cellTag.getOpacity());

        s.writeDouble(membraneTag.getRed());
        s.writeDouble(membraneTag.getGreen());
        s.writeDouble(membraneTag.getBlue());
        s.writeDouble(membraneTag.getOpacity());
    }
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException{
        s.defaultReadObject();

        double r = s.readDouble();
        double g = s.readDouble();
        double b = s.readDouble();
        double a = s.readDouble();

        cellTag = new Color(r,g,b,a);

        r = s.readDouble();
        g = s.readDouble();
        b = s.readDouble();
        a = s.readDouble();

        membraneTag = new Color(r,g,b,a);
    }


    public CellDataTable(String sName, double a0, double aK, double p0, double pK){

        this.TypeName = sName;
        this.a0 = a0;
        this.aK = aK;
        this.p0 = p0;
        this.pK = pK;
    }

    public void UpdateInformation(String sName, double a0, double aK, double p0, double pK){

        this.TypeName = sName;
        this.a0 = a0;
        this.aK = aK;
        this.p0 = p0;
        this.pK = pK;
    }
}
