package models;

import potts.Cell;
import ui.*;
import utils.MyUtils;
import utils.Point;
import utils.RNG;

/**
 * Created by luke on 25/08/16.
 */
public class SAR_FAM extends BiochemicalModel {

    @UILink(UILabel = "SCAR attack bias")
    public double scar_bias    = 30;
    @UILink(UILabel = "Actin defend bias")
    public double actin_bias   = 5;
    //@UILink(UILabel = "Max actin")
    //public double actin_max    = 12;
    @UILink(UILabel = "Basal actin")
    public double actin_basal  = 1.35;
    @UILink(UILabel = "Scar on")
    public double scar_b      = 1.2;
    @UILink(UILabel = "Scar off")
    public double scar_d    = 0.225;
    @UILink(UILabel = "Scar* off")
    public double ascar_d    = 0.99;
    @UILink(UILabel = "Rac on")
    public double rac_b    = 0.2;
    @UILink(UILabel = "Rac off")
    public double rac_d    = 0.02;
    @UILink(UILabel = "FAM49 on")
    public double fam49_on    = 0.5;
    @UILink(UILabel = "FAM49 off")
    public double fam49_off   = 0.25;
    @UILink(UILabel = "Actin depolymerisation")
    public double act_depoly   = 0.04;
    @UILink(UILabel = "kD")
    public double kD   = 0.02;
    @UILink(UILabel = "kM")
    public double kM   = 0.5;
    @UILink(UILabel = "sMAX")
    public double sMax = 0.01;


    @Override
    public String GetName() {
        return name;
    }

    public SAR_FAM(){
        super();
        name = "SAR+FAM49";
    }

    @Override
    public void LoadNodes() {

        nodes.add("Actin*");
        nodes.add("ActinOld");
        nodes.add("Scar");
        nodes.add("Scar*");
        nodes.add("FAM49");
        nodes.add("Rac");
        super.LoadNodes();
    }

    @Override
    public double GetAttackBias(Point  point) {
        return -scar_bias*GetLocalConcentration("Scar*", point, "ARITHMETIC");
    }

    @Override
    public double GetDefendBias(Point point) {
        /*if(CheckSplit(point)) {
            System.out.print("Will Split!");
            return 10000;
        } */
        return actin_bias*GetLocalConcentration("Actin*", point, "ARITHMETIC")
                + actin_bias*GetLocalConcentration("ActinOld", point, "ARITHMETIC");
    }


    public boolean CheckSplit(Point point){
         /*
        int c = cp.GetID(point.x, point.y);
        int cT = 0;
        int L = 0;
        int R = 0;
        int T = 0;
        int B = 0;

        for(int i=point[0]-1; i<=point[0]+1; i++){
            for(int j=point[1]-1; j<=point[1]+1; j++){
                if(cp.GetID(i,j)==c){
                    if(i<point[0]) L+=1;
                    else if(i>point[0]) R+=1;
                    if(j<point[1]) B+=1;
                    else if(j>point[1]) T+=1;
                    cT++;
                }
            }
        }
        if(cT>5) return false;
        if(cT==5) {
            if ((L == 2 && R == 2) || (T == 2 && B == 2)) return true;
        }
        else if(cT==4){
            if((T==2 && B==1) || (T==1 && B==2) || (L==2 && R==1) || (L==1 && R==2)) return  true;
        }
        else if(cT==3){
            if((L==1&&R==1) || (T==1 && B==1)) return true;
        } */
        return false;
    }

    @Override
    public void ClearLostPoint(Point point){




        //Move FAM49 to any other nearby points;
        int c      = cp.GetID(point);
        double fam = GetLocalConcentration("FAM49", point, "POINT");

        int cN = 0;
        for(int i=point.x-1; i<=point.x+1; i++){
            for(int j=point.y-1; j<=point.y+1; j++){
                if(cp.GetID(i,j)==c){
                    cN++;
                }
            }
        }

        fam/=(1d*cN);

        double[][] famGrid = modelGrid.get("FAM49");

        for(int i=point.x-1; i<=point.x+1; i++){
            for(int j=point.y-1; j<=point.y+1; j++){
                if(cp.GetID(i,j)==c){
                    famGrid[i][j]+=fam;
                }
            }
        }

        super.ClearLostPoint(point);
    }

    @Override
    public void InitialiseConqueredPoint(Point target, Point source) {
        double ascar = GetLocalConcentration("Scar*", source, "ARITHMETIC");
        double fam   = GetLocalConcentration("FAM49", source, "POINT")/3d;
        double act   = Math.max(RNG.rnd.nextDouble() * actin_basal, ascar * actin_basal);

        int c = cp.GetID(source);

        modelGrid.get("Actin*")[target.x][target.y] = act;
    }

    @Override
    public void Update(Cell c) {

        double[][] rac     = modelGrid.get("Rac");

        double[][] scar    = modelGrid.get("Scar");
        double[][] ascar   = modelGrid.get("Scar*");
        double[][] actin   = modelGrid.get("Actin*");
        double[][] act_old = modelGrid.get("ActinOld");
        double[][] fam49   = modelGrid.get("FAM49");

        for(Point p : c.points){

            double fml = GetLocalConcentration("FAM49", p, "ARITHMETIC");
            double racA = GetLocalConcentration("Rac", p, "ARITHMETIC");
            racA/=(1+fml*fml);
            double sig = 1d;//(1d+Math.exp(-2d*(racA-0.2)));

            ascar[p.x][p.y] +=  sig*scar[p.x][p.y]
                               -  ascar_d*ascar[p.x][p.y];
            ascar[p.x][p.y] = Math.max(0, ascar[p.x][p.y]);

            scar[p.x][p.y] +=   scar_b*GetLocalConcentration("Actin*", p, "ARITHMETIC")
                    - sig*scar[p.x][p.y]
                    - scar_d*scar[p.x][p.y];
            scar[p.x][p.y] = Math.max(0, scar[p.x][p.y]);
        }
        for(Point p : c.boundary) {
            fam49[p.x][p.y] +=    fam49_on;//fam49_on*GetLocalConcentration("Scar*", p, "ARITHMETIC");
        }
        for(Point p : c.points){
            fam49[p.x][p.y] -= fam49_off*fam49[p.x][p.y];
            fam49[p.x][p.y] = Math.max(0, fam49[p.x][p.y]);
        }



        for(Point p : c.points){
            rac[p.x][p.y] +=    rac_b*GetLocalConcentration("Actin*", p, "ARITHMETIC")
                                - rac_d*rac[p.x][p.y];
            rac[p.x][p.y] = Math.max(0, rac[p.x][p.y]);
        }

        for(Point p : c.points){
            if(actin[p.x][p.y]>0) {

                actin[p.x][p.y]   -= 1;
                act_old[p.x][p.y] += 1;
            }
            actin[p.x][p.y] = Math.max(0, actin[p.x][p.y]);
        }
        for(Point p : c.points){
            act_old[p.x][p.y]*=(1.-act_depoly);
            if(act_old[p.x][p.y]<0.1) act_old[p.x][p.y] = 0;
            act_old[p.x][p.y] = Math.max(0, act_old[p.x][p.y]);
        }
        if(cp.ecl!=null){
            double cn = 0;
            for(Point p : c.points){

                cn = cp.ecl.GetConcentration("Attractant", p, "POINT");
                cp.ecl.ChangeConcentration(p.x, p.y, -sMax*cn/(cn+kM));

            }
        }
    }
}
