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
public class ScarModelGrantRacPlaced extends BiochemicalModel {

    @UILink(UILabel = "Actin attack bias")
    public double actin_attack_bias    = 50;
    @UILink(UILabel = "Actin defend bias")
    public double actin_defend_bias   = 15;
    @UILink(UILabel = "Max actin")
    public double actin_max = 42;
    @UILink(UILabel = "Basal actin")
    public double actin_basal = 0.3;
    @UILink(UILabel = "Inactive Scar production")
    public double scar_b = 10;
    @UILink(UILabel = "Active Scar degradation")
    public double a_scar_deg = 0.2;
    @UILink(UILabel = "Inactive Scar degradation")
    public double scar_deg = 0.02;
    @UILink(UILabel = "Scar on")
    public double scar_on = 0.8;
    @UILink(UILabel = "Scar off")
    public double scar_off = 0.01;
    @UILink(UILabel = "Scar/Rac KM")
    public double scar_rac_km = 0.1;
    @UILink(UILabel = "Rac on")
    public double rac_b    = 0.1;
    @UILink(UILabel = "Rac off")
    public double rac_d    = 0.03;
    @UILink(UILabel = "Rac hill coefficient")
    public double rac_hill   = 8;
    @UILink(UILabel = "Actin depolymerisation")
    public double act_depoly   = 0.1;

    protected Map<Cell,Double> scarPool     = new HashMap<Cell, Double>();
    protected Map<Point,Double> relActivity = new HashMap<Point, Double>();

    @Override
    public String GetName() {
        return name;
    }

    public ScarModelGrantRacPlaced(){
        super();
        name = "Scar Model RAC forced";
    }

    @Override
    public void LoadNodes() {

        nodes.add("Actin*");
        nodes.add("ActinOld");
        nodes.add("ScarO");
        nodes.add("Scar*");
        nodes.add("Rac");
        super.LoadNodes();
    }

    @Override
    public double GetAttackBias(Point point) {
        return -actin_attack_bias*(GetLocalConcentration("Actin*", point, "ARITHMETIC")+GetLocalConcentration("Scar*", point, "ARITHMETIC"));
    }

    @Override
    public double GetDefendBias(Point point) {
        return actin_defend_bias*GetLocalConcentration("Actin*", point, "ARITHMETIC")
                + actin_defend_bias*GetLocalConcentration("ActinOld", point, "ARITHMETIC");
    }


    // RAC STAYS IN MEMBRANE, SCAR STICKS TO OLD ACTIN & COMES OFF
    @Override
    public void InitialiseConqueredPoint(Point target, Point source) {
        double act = actin_max*actin_basal*RNG.rnd.nextDouble();
        act+= actin_max*(1-actin_basal)*GetLocalConcentration("Scar*", source, "Arithmetic");
        act        = Math.min(actin_max,act);
        modelGrid.get("Actin*")[target.x][target.y] = act;

        double oscr  = modelGrid.get("ScarO")[source.x][source.y];
        double dscr  = modelGrid.get("Scar*")[source.x][source.y];
        double rac1  = modelGrid.get("Rac")[source.x][source.y];

        modelGrid.get("ScarO")[target.x][target.y] = 0.8*oscr;
        modelGrid.get("Scar*")[target.x][target.y] = 0.2*dscr;
        modelGrid.get("Rac")[target.x][target.y] = 0.9*rac1;

        modelGrid.get("ScarO")[source.x][source.y] = 0.2*oscr;
        modelGrid.get("Scar*")[source.x][source.x] = 0.8*dscr;
        modelGrid.get("Rac")[source.x][source.y] = 0.1*rac1;
    }

    @Override
    public void Update(Cell c) {

        double[][] rac     = modelGrid.get("Rac");
        double[][] scar    = modelGrid.get("ScarO");
        double[][] scar_a  = modelGrid.get("Scar*");
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
            //Scar is requested from a ubiquitous pool based on local rac concentration (with MM kinetics).
            double local_rac = GetLocalConcentration("Rac", pt, "ARITHMETIC");
            double rac_activity = Math.pow(local_rac, rac_hill);
            rac_activity = scar_on *rac_activity/(rac_activity+scar_rac_km);
            double curvature = 0;

            // we build a dictionary of all requests for Scar.
            double recruitmentPower = Math.max(0, rac_activity);
            totalRecruitment+=recruitmentPower;
            relActivity.put(pt,recruitmentPower);
        }

        //We then assign scar evenly, but everything is scaled down if more is requested than we have in the pool.
        double currentScarPool = scarPool.get(c).doubleValue();
        double limiter = (totalRecruitment>currentScarPool ? currentScarPool/totalRecruitment : 1);

        for(Point pt : c.boundary){
            double dS = limiter*relActivity.getOrDefault(pt,0d);
            dScarPool-= dS;
            scar[pt.x][pt.y] += dS;
        }

        // Put all scar from cytosolic rafts & off membranes into pool, then degrade some & update.
        currentScarPool+= dScarPool;
        currentScarPool-= currentScarPool*scar_deg;
        currentScarPool+= scar_b;
        currentScarPool = Math.max(0,currentScarPool);
        scarPool.put(c, currentScarPool);


        //Scar updated based on local open scar and actin.
        double max_scar = 0;
        for(Point p : c.points){
            scar_a[p.x][p.y] -= scar_a[p.x][p.y]*a_scar_deg;
            double converted = Math.min(GetLocalConcentration("ScarO", p, "POINT"), GetLocalConcentration("Actin*", p, "ARITHMETIC"));
            scar_a[p.x][p.y] += converted;
            scar[p.x][p.y] -= converted;
            scar_a[p.x][p.y] =  Math.max(0, scar_a[p.x][p.y]);
            if(scar_a[p.x][p.y]>max_scar) max_scar =  scar_a[p.x][p.y];
        }
        System.out.println(max_scar);

        // Rac recruited to the RHS.
        double mean_x =0;
        double mean_y = 0;
        for(Point p : c.points){

            rac[p.x][p.y]-= rac_d*rac[p.x][p.y];

            mean_x+=p.x;
            mean_y+=p.y;


        }
        mean_x/=c.points.size();
        mean_y/=c.points.size();
        for(Point p : c.points){

            if(p.x-mean_x<=10) continue;

            rac[p.x][p.y] += 0.02*(p.x-(mean_x+10));

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
            if(act_old[p.x][p.y]>0) act_old[p.x][p.y]-=act_depoly;
            act_old[p.x][p.y] = Math.max(0, act_old[p.x][p.y]);
        }
    }
}
