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
public class ScarModelGrant extends BiochemicalModel {

    @UILink(UILabel = "Actin attack bias")
    public double actin_attack_bias    = 70;
    @UILink(UILabel = "Actin defend bias")
    public double actin_defend_bias   = 15;
    @UILink(UILabel = "Max actin")
    public double actin_max = 65;
    @UILink(UILabel = "Basal actin")
    public double actin_basal = 0.25;
    @UILink(UILabel = "Inactive Scar production")
    public double scar_b = 50;
    @UILink(UILabel = "Active Scar degradation")
    public double a_scar_deg = 0.2;
    @UILink(UILabel = "Inactive Scar degradation")
    public double scar_deg = 0.01;
    @UILink(UILabel = "Scar min membrane curvature")
    public double curvature_cutoff = 0.05;

    @UILink(UILabel = "Scar on")
    public double scar_on = 0.8;
    @UILink(UILabel = "Scar off")
    public double scar_off = 0.1;
    @UILink(UILabel = "Scar/Rac KM")
    public double scar_rac_km = 8;
    @UILink(UILabel = "Rac on")
    public double rac_b    = 0.1;
    @UILink(UILabel = "Rac off")
    public double rac_d    = 0.025;
    @UILink(UILabel = "Rac hill coefficient")
    public double rac_hill   = 8;
    @UILink(UILabel = "Actin depolymerisation")
    public double act_depoly   = 0.1;

    protected Map<Cell,Double> scarPool     = new HashMap<Cell, Double>();
    protected Map<Point,Double> relActivity = new HashMap<Point, Double>();
    protected Map<Point,Double> racDiffuser = new HashMap<Point, Double>();

    @Override
    public String GetName() {
        return name;
    }

    public ScarModelGrant(){
        super();
        name = "Scar Grant Model";
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
        double actS = modelGrid.get("Actin*")[source.x][source.y];

        double act = actin_max*actin_basal*RNG.rnd.nextDouble();
        act = Math.max(act, actS);
        act+= actin_max*(1-actin_basal)*GetLocalConcentration("Scar*", source, "Arithmetic");
        act        = Math.min(actin_max,act);
        modelGrid.get("Actin*")[target.x][target.y] = act;

        double oscr  = modelGrid.get("ScarO")[source.x][source.y];
        double dscr  = modelGrid.get("Scar*")[source.x][source.y];
        double rac1  = modelGrid.get("Rac")[source.x][source.y];

        modelGrid.get("ScarO")[target.x][target.y] = 0.9*oscr;
        modelGrid.get("Scar*")[target.x][target.y] = 0.0*dscr;
        modelGrid.get("Rac")[target.x][target.y] = 0.9*rac1;

        modelGrid.get("ScarO")[source.x][source.y] = 0.1*oscr;
        modelGrid.get("Scar*")[source.x][source.y] = 0.0*dscr;
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

        double mu_x = 0;
        double mu_y = 0;
        double mu_o = 0;
        double mx_o = 0;
        double mn_o = 1;
        for(Point p : c.points){
            mu_x+=p.x;
            mu_y+=p.y;
            if(ecl!=null) {
                double occ = ecl.GetOccupancy("Attractant", p, "POINT");
                mu_o += occ;
                if(occ>mx_o) mx_o = occ;
                if(occ<mn_o) mn_o = occ;
            }
        }
        mu_x/=c.points.size();
        mu_y/=c.points.size();
        mu_o/=c.points.size();


        // Scar stays put on membrane, but completely breaks down in cytosol...
        for(Point p : c.points){
            if(c.boundary.contains(p)) {
                //Work out losses
                double bias = 0;//Math.min(0.6,Math.max(-0.6,0.05*(p.x-mu_x)));
                /*if(ecl!=null && mu_o>1E-7) {
                    bias = 1.6*(ecl.GetOccupancy("Attractant", p, "POINT") - mu_o)/(mx_o-mn_o);
                }*/
                dLclScar = scar[p.x][p.y] * (1-bias)*scar_off;
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
            rac_activity = scar_on * rac_activity/(rac_activity+scar_rac_km);


            double curvature = c.curvature.getOrDefault(pt,0d);
            if(curvature<curvature_cutoff) {
                rac_activity = 0;
                //System.out.println("Curvy");
            }
            
            // we build a dictionary of all requests for Scar.
            double recruitmentPower = Math.max(0, rac_activity);
            totalRecruitment+=recruitmentPower;
            relActivity.put(pt,recruitmentPower);
        }

        //We then assign scar evenly, but everything is scaled down if more is requested than we have in the pool.
        double currentScarPool = scarPool.get(c).doubleValue();
        double limiter = (totalRecruitment>currentScarPool ? currentScarPool/totalRecruitment : 1);

        // rac diffuses
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
        for(Point p : c.points){
            double bias = 0;//Math.min(0.6,Math.max(-0.6,0.05*(p.x-mu_x)));
            /*if(ecl!=null && mu_o>1E-7) {
                bias = 1.6*(ecl.GetOccupancy("Attractant", p, "POINT") - mu_o)/(mx_o-mn_o);
            }*/

            scar_a[p.x][p.y] -= scar_a[p.x][p.y]*a_scar_deg;
            double converted = Math.min(GetLocalConcentration("ScarO", p, "POINT"), GetLocalConcentration("Actin*", p, "ARITHMETIC"));
            scar_a[p.x][p.y] += converted*(1+bias);
            scar[p.x][p.y] -= converted*(1+bias);
            scar_a[p.x][p.y] =  Math.max(0, scar_a[p.x][p.y]);
        }

        // rac diffuses

        for(Point p : c.points){
            racDiffuser.put(p, 0.9*GetLocalConcentration("Rac", p, "POINT") + 0.1*GetLocalConcentration("Rac", p, "ARITHMETIC"));
        }

        double racMax = 0;
        // Rac recruited via actin.
        for(Point p : c.points){
            rac[p.x][p.y] = racDiffuser.getOrDefault(p,rac[p.x][p.y]);

            double bias = 0;//Math.min(0.8,Math.max(-0.8,0.1*(p.x-mu_x)));
            //if(ecl!=null && mu_o>1E-7) {
            //    bias = 1.8*(ecl.GetOccupancy("Attractant", p, "POINT") - mu_o)/(mx_o-mn_o);
                //System.out.println(bias);
            //}
            rac[p.x][p.y] +=    rac_b*(1+bias)*GetLocalConcentration("Actin*", p, "ARITHMETIC")
                    - rac_d*(1-bias)*rac[p.x][p.y];
            rac[p.x][p.y] = Math.max(0, rac[p.x][p.y]);

            if(rac[p.x][p.y]>racMax) racMax = rac[p.x][p.y];
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
        //System.out.println(racMax);
    }
}
