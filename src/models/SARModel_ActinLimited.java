package models;

import potts.Cell;
import ui.UILink;
import utils.Point;
import utils.RNG;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luke on 25/08/16.
 */
public class SARModel_ActinLimited extends BiochemicalModel {

    @UILink(UILabel = "SCAR attack bias")
    public double scar_bias    = 12;
    @UILink(UILabel = "Actin defend bias")
    public double actin_bias   = 12;
    @UILink(UILabel = "Max actin")
    public double actin_max    = 21;
    @UILink(UILabel = "Basal actin")
    public double actin_basal  = 0.3;
    @UILink(UILabel = "ScarP production")
    public double scarp_b      = 50.0;
    @UILink(UILabel = "ScarP max")
    public double scarp_max      = 10;
    @UILink(UILabel = "Scar on")
    public double scar_b      = 0.25;
    @UILink(UILabel = "Scar off")
    public double scar_d    = 0.15;
    @UILink(UILabel = "Scar* off")
    public double ascar_d    = 0.99;
    @UILink(UILabel = "Rac on")
    public double rac_b    = 0.18;
    @UILink(UILabel = "Rac off")
    public double rac_d    = 0.02;
    @UILink(UILabel = "Actin depolymerisation")
    public double act_depoly   = 0.03;

    protected Map<Cell,Double> scarP = new HashMap<Cell, Double>();
    protected Map<Point,Double> racDiffuser = new HashMap<Point, Double>();
    protected Map<Point,Double> scarrecruitmentpower = new HashMap<Point, Double>();

    @Override
    public String GetName() {
        return name;
    }

    public SARModel_ActinLimited(){
        super();
        name = "Scar-Act-Rac";
    }

    @Override
    public void LoadNodes() {

        nodes.add("Actin*");
        nodes.add("ActinOld");
        nodes.add("Scar");
        nodes.add("Scar*");
        nodes.add("Rac");
        super.LoadNodes();
    }

    @Override
    public double GetAttackBias(Point point) {

        double bias = 1+scar_bias*GetLocalConcentration("Scar*", point, "ARITHMETIC");

        bias*= GetLocalConcentration("Actin*", point, "ARITHMETIC");

        return -bias;
    }

    @Override
    public double GetDefendBias(Point point) {
        return actin_bias*GetLocalConcentration("Actin*", point, "ARITHMETIC")
                + actin_bias*GetLocalConcentration("ActinOld", point, "ARITHMETIC");
    }


    @Override
    public void InitialiseConqueredPoint(Point target, Point source) {
        double ascar = GetLocalConcentration("Scar*", source, "ARITHMETIC");
        double a_act = GetLocalConcentration("Actin*", source, "ARITHMETIC");

        double act    = actin_max*actin_basal*RNG.rnd.nextDouble();
        if(act<a_act) act = a_act;
        act   += a_act*(1+ascar*ascar);

        act = Math.min(actin_max,act);
        modelGrid.get("Actin*")[target.x][target.y] = act;

        double dscrS = modelGrid.get("Scar*")[target.x][target.y];
        double dscr  = modelGrid.get("Scar")[target.x][target.y];
        double rac1  = modelGrid.get("Rac")[target.x][target.y];



        modelGrid.get("Scar*")[target.x][target.y] = 0.1*dscrS;
        modelGrid.get("Scar")[target.x][target.y] = 0.1*dscr;
        modelGrid.get("Rac")[target.x][target.y] = 0.9*rac1;

        modelGrid.get("Scar*")[source.x][source.y] = 0.1*dscrS;
        modelGrid.get("Scar")[source.x][source.y] = 0.9*dscr;
        modelGrid.get("Rac")[source.x][source.y] = 0.1*rac1;
    }

    @Override
    public void Update(Cell c) {

        racDiffuser.clear();
        scarrecruitmentpower.clear();

        double[][] rac     = modelGrid.get("Rac");
        double[][] scar    = modelGrid.get("Scar");
        double[][] ascar   = modelGrid.get("Scar*");
        double[][] actin   = modelGrid.get("Actin*");
        double[][] act_old = modelGrid.get("ActinOld");

        double dScarP = 0;
        double dS = 0;


        if(!scarP.containsKey(c)){
            scarP.put(c,new Double(0));
        }
        double currentScarP = scarP.get(c).doubleValue();

        // Scar activation requires unphosphoylated scar and rac
        double maxScar = 0;
        for(Point p : c.points){
            ascar[p.x][p.y] +=  Math.min(scar[p.x][p.y], GetLocalConcentration("Rac", p, "ARITHMETIC"))
                               -  ascar_d*ascar[p.x][p.y];
            ascar[p.x][p.y] = Math.max(0, ascar[p.x][p.y]);
            if(ascar[p.x][p.y]>maxScar) maxScar = ascar[p.x][p.y];
        }
        //System.out.println("Cell "+c.cellnum+":  "+maxScar);



        // Scar dephosphorylation requires actin
        double totalRecruitment = 0;
        for(Point p : c.points) {
            double localRecruitment = scar_b * Math.min(currentScarP, GetLocalConcentration("Actin*", p, "ARITHMETIC"));
            scarrecruitmentpower.put(p, localRecruitment);
            totalRecruitment += localRecruitment;
        }
        double ratio = 1;
        if(totalRecruitment>currentScarP) ratio = currentScarP/totalRecruitment;
        for(Point p : c.points) {

            dS = ratio*scar_b*Math.min(currentScarP, GetLocalConcentration("Actin*", p, "ARITHMETIC"));
            dS-= scar_d*scar[p.x][p.y];

            // WE NEED TO SHARE WHN RESOURCES ARE LOW WITH SOME FUNCTION.

            dScarP+=dS;


            scar[p.x][p.y] += dS
                             -  Math.min(scar[p.x][p.y], GetLocalConcentration("Rac", p, "ARITHMETIC"));
            scar[p.x][p.y] = Math.max(0, scar[p.x][p.y]);
        }

        // Phosphorylated scar titrates in and is recruited.
        currentScarP+=scarp_b;
        currentScarP-=dScarP;
        currentScarP = Math.min(Math.max(0,currentScarP),scarp_max);
        scarP.put(c,currentScarP);

        // Rac recruited via actin.

        double mu_x = 0;
        for(Point p : c.points){
            racDiffuser.put(p, GetLocalConcentration("Rac", p, "ARITHMETIC"));
            mu_x+=p.x;
        }
        mu_x/=c.points.size();

        for(Point p : c.points){
            rac[p.x][p.y] = racDiffuser.getOrDefault(p,rac[p.x][p.y]);

            rac[p.x][p.y] +=    (rac_b+0.01*(p.x-mu_x))*GetLocalConcentration("Actin*", p, "ARITHMETIC")
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
            if(act_old[p.x][p.y]<0.1) act_old[p.x][p.y] = 0;
            act_old[p.x][p.y] = Math.max(0, act_old[p.x][p.y]);
        }
    }
}
