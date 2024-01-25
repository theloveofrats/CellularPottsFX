package models;

import models.BiochemicalModel;
import potts.Cell;
import potts.CellularPotts;
import ui.UILink;
import utils.Point;

import java.util.HashMap;

/**
 * Created by luke on 23/08/16.
 */
public class MaxActModel extends BiochemicalModel {

    @UILink(UILabel = "Actin max")
    public double act_max  = 4;
    @UILink(UILabel = "Actin attack bias")
    public double act_bias = 350;

    public MaxActModel(){
        super();
        name = "MaxAct";
    }

    @Override
    public void Initialise(CellularPotts cp){
        super.Initialise(cp);
    }

    @Override
    public void LoadNodes() {

        nodes.add("Actin");
        super.LoadNodes();
    }

    @Override
    public String GetName(){
        return name;
    }

    @Override
    public double GetAttackBias(Point point) {
        double actin = GetLocalConcentration("Actin", point, "ARITHMETIC");
        return -(act_bias/act_max)*actin;
    }

    @Override
    public double GetDefendBias(Point point) {

        double actin = GetLocalConcentration("Actin", point, "ARITHMETIC");
        return (act_bias/act_max)*actin;
    }

    @Override
    public void Update(Cell c){
        for(Point p : c.points){
            UpdatePoint(p);
        }
    }

    private void UpdatePoint(Point point) {
        double actin = GetLocalConcentration("Actin", point);

        double c = 1;
        if(ecl!=null){
            c = ecl.GetOccupancy("Attractant", point, "ARITHMETIC");
            c = Math.max(0, Math.min(1, c));
            c = c*c;
        }

        if(actin>0) actin-=(1. - 0.9*c);
        if(actin<0) actin = 0;

        modelGrid.get("Actin")[point.x][point.y] = actin;

        if(ecl!=null){

            double eclc = ecl.GetConcentration("Attractant", point, "POINT");

            //System.out.println(eclc);

            if(eclc>1E-12){
                //System.out.println("GRE");
                ecl.ChangeConcentration(point.x, point.y, -0.01*eclc);
            }
        }
    }


    @Override
    public void InitialiseConqueredPoint(Point target, Point source) {
        //System.out.println("Actin Conquer");

        double c = 1;
        if(ecl!=null){
            c = ecl.GetOccupancy("Attractant", target, "ARITHMETIC");
            c = Math.max(0, Math.min(1, c));
            c = c*c;
            c = (0.85 + 0.15*c);
        }

        modelGrid.get("Actin")[target.x][target.y] = c*act_max;
    }
}
