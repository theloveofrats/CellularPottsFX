package models;

import potts.Cell;
import ui.UILink;
import utils.MyUtils;
import utils.Point;
import utils.RNG;

import java.util.*;

/**
 * Created by luke on 25/08/16.
 */
public class SAR_Curvature extends BiochemicalModel {

    @UILink(UILabel = "SCAR attack bias")
    public double scar_bias    = 50;
    @UILink(UILabel = "Actin defend bias")
    public double actin_bias   = 15;
    @UILink(UILabel = "Max actin")
    public double actin_max    = 18;
    @UILink(UILabel = "Basal actin")
    public double actin_basal  = 0.5;
    @UILink(UILabel = "Scar production")
    public double scar_b = 10;
    @UILink(UILabel = "Scar target curvature")
    public double scar_curve = 0.4;
    @UILink(UILabel = "Cutoff sq. curvature")
    public double scar_cutoff = 0.1;
    @UILink(UILabel = "Scar degradation")
    public double scar_deg = 0.1;
    @UILink(UILabel = "Scar on")
    public double scar_on = 0.9;
    @UILink(UILabel = "Scar off")
    public double scar_off = 0.1;
    @UILink(UILabel = "Rac on")
    public double rac_b    = 0.2;
    @UILink(UILabel = "Rac off")
    public double rac_d    = 0.03;
    @UILink(UILabel = "Rac hill coefficient")
    public double rac_hill   = 8;
    @UILink(UILabel = "Actin depolymerisation")
    public double act_depoly   = 0.03;

    protected Map<Cell,Double> scarPool     = new HashMap<Cell, Double>();
    protected Map<Point,Double> relActivity = new HashMap<Point, Double>();

    @Override
    public String GetName() {
        return name;
    }

    public SAR_Curvature(){
        super();
        name = "Scar Curvature Model";
    }

    @Override
    public void LoadNodes() {

        nodes.add("Actin*");
        nodes.add("ActinOld");
        nodes.add("Scar");
        nodes.add("Rac");
        super.LoadNodes();
    }

    @Override
    public double GetAttackBias(Point point) {
        return -scar_bias*GetLocalConcentration("Scar", point, "ARITHMETIC");
    }

    @Override
    public double GetDefendBias(Point point) {
        return actin_bias*GetLocalConcentration("Actin*", point, "ARITHMETIC")
                + actin_bias*GetLocalConcentration("ActinOld", point, "ARITHMETIC");
    }


    // RAC STAYS IN MEMBRANE, SCAR STICKS TO OLD ACTIN & COMES OFF
    @Override
    public void InitialiseConqueredPoint(Point target, Point source) {
        double lclscar = GetLocalConcentration("Scar", source, "POINT");
        double act   = 0.1*lclscar*lclscar*lclscar*actin_max;
        act         += actin_max*actin_basal*RNG.rnd.nextDouble();
        act = Math.min(actin_max,act);
        modelGrid.get("Actin*")[target.x][target.y] = act;

        double dscr  = modelGrid.get("Scar")[target.x][target.y];
        double rac1  = modelGrid.get("Rac")[target.x][target.y];

        modelGrid.get("Scar")[target.x][target.y] = 0.2*dscr;
        modelGrid.get("Rac")[target.x][target.y] = 0.9*rac1;

        modelGrid.get("Scar")[source.x][source.x] = 0.8*dscr;
        modelGrid.get("Rac")[source.y][source.y] = 0.1*rac1;
    }

    @Override
    public void Update(Cell c) {

        double[][] rac     = modelGrid.get("Rac");
        double[][] scar    = modelGrid.get("Scar");
        double[][] actin   = modelGrid.get("Actin*");
        double[][] act_old = modelGrid.get("ActinOld");

        if(!scarPool.containsKey(c)){
            synchronized (scarPool) {
                scarPool.put(c, new Double(0));
            }
        }

        relActivity.clear();
        double totalRecruitment = 0;
        double dScarPool = 0;
        double dLclScar;

        // Scar stays put on membrane, but completely breaks down in cytosol...
        for(Point p : c.points){
            if(c.boundary.contains(p)) {
                //Work out losses
                dLclScar = scar[p.x][p.y] * scar_off;
            }
            else{
                dLclScar =  scar[p.x][p.y];
            }

            dScarPool+=dLclScar;
            scar[p.x][p.y] -= dLclScar;
            //System.out.println("Scar remaining:: "+scar[p.x][p.y]);
        }


        for(Point pt : c.boundary){
            //Work out recruitment
            double local_rac = GetLocalConcentration("Rac", pt, "ARITHMETIC");
            double ractivity = Math.pow(local_rac, rac_hill);
            ractivity = 1d* scar_on *ractivity/(ractivity+0.1);
            double curvature = 0;
            if(c.curvature.containsKey(pt)){
                curvature = c.curvature.get(pt);
            }
            else{
                System.out.println("Not included!!");
            }
            double recruitmentPower = Math.max(0, ractivity * (1d/scar_cutoff) * (scar_cutoff - (scar_curve - curvature) * (scar_curve - curvature)));
            totalRecruitment+=recruitmentPower;
            relActivity.put(pt,recruitmentPower);
        }

        double currentScarPool = scarPool.get(c).doubleValue();
        double limiter = (totalRecruitment>currentScarPool ? currentScarPool/totalRecruitment : 1);

        for(Point pt : c.boundary){
            double dS = limiter*relActivity.getOrDefault(pt,0d);
            dScarPool-= dS;
            scar[pt.x][pt.y] += dS;
        }

        // Put all scar from cytosolic rafts & off membranes into pool, then degrade some & update.
        currentScarPool+= dScarPool;
        currentScarPool+= scar_b;
        currentScarPool-= currentScarPool*scar_deg;
        currentScarPool = Math.max(0,currentScarPool);
        scarPool.put(c, currentScarPool);


        // Rac recruited via actin.
        for(Point p : c.points){
            rac[p.x][p.y] +=    rac_b*GetLocalConcentration("Actin*", p, "ARITHMETIC")
                    - rac_d*rac[p.x][p.y];
            rac[p.x][p.y] = Math.max(0, rac[p.x][p.y]);
        }

        // Actin ages.
        for(Point p : c.points){
            if(actin[p.x][p.y]>0) {
                actin[p.x][p.y] -= 1;
                act_old[p.x][p.y] += 1;
            }

            actin[p.x][p.y] = Math.max(0, actin[p.x][p.y]);
        }

        // Old actin depolymerises.
        for(Point p : c.points){
            act_old[p.x][p.y]*=(1.-act_depoly);
            if(act_old[p.x][p.y]<0.01) act_old[p.x][p.y] = 0;
        }
    }
}
