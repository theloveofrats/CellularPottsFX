package ecl;

import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import potts.CellularPotts;
import ui.UIController;
import ui.UILink;
import ui.UIValue;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;
import utils.Point;

/**
 * Created by luke on 04/03/17.
 */
public class DuFortDiffusion implements ExtracellularLattice {

    CellularPotts cpm;
    Map<String, Color> tags = new HashMap<>();

    int w;
    int h;
    int coarseGrained = 1;
    int[][] localIDgrid;

    @UILink(UILabel = "Diffusion rate")
    public double dc = 10;
    @UILink(UILabel = "Concentration")
    public double c0 = 1.;
    @UILink(UILabel = "kD")
    public double kD = 0.05;

    List<Point> sourcePoints = new ArrayList<Point>();
    List<Point> sinkPoints   = new ArrayList<Point>();
    List<Point> valvePoints  = new ArrayList<>();
    List<List<Point>> valves = new ArrayList<>();


    double[][] cm1, c, cp1;

    @Override
    public String GetName(){
        return "DuFort Diffuser";

    }

    public void Initialise(CellularPotts cpm){

        this.cpm = cpm;
        cpm.ecl = this;

        this.w = (int) Math.round(cpm.w*1.0/coarseGrained)+1;
        this.h = (int) Math.round(cpm.h*1.0/coarseGrained)+1;

        cm1 = new double[w][h];
        c   = new double[w][h];
        cp1 = new double[w][h];

        localIDgrid = new int[w][h];

        this.tags.put("Attractant", Color.TRANSPARENT);

        for(int i=0; i<w-1; i++){
            for(int j=0; j<h-1; j++){

                Point cpij = DiffuserToCP(new Point(i,j));

                int cpid = cpm.GetID(cpij);

                if(cpid==-1) localIDgrid[i][j] = -1;

                if(cpid==-5 || cpid==-25 || cpid==-30) { //if red
                    sourcePoints.add(new Point(i,j));
                }
                if(cpid==-10 || cpid==-20 || cpid==-30) { //if green
                    valvePoints.add(new Point(i,j));
                }
                if(cpid==-15 || cpid==-20 || cpid==-25) { //if blue
                    sinkPoints.add(new Point(i,j));
                }
            }
        }
        if(valvePoints.size()>0){
            FindValves();
            for(int i=0; i<valves.size(); i++){
                CloseValve(i);
            }
        }
    }

    public DuFortDiffusion(){
        this.tags.put("Attractant", Color.TRANSPARENT);
    }

    private void FindValves(){
        while(valvePoints.size()>0) {
            Point start = valvePoints.get(0);
            List<Point> newValve = GetConnected(start, valvePoints);
            valvePoints.removeAll(newValve);
            valves.add(newValve);
        }
        CreateValveUILinks();
        System.out.println("Valves: "+valves.size());
    }

    private void CreateValveUILinks(){
        UIController.current.UpdateUI(false);
    }

    @Override
    public void CloseValve(int valveNum){
        if(valveNum>=valves.size() || valveNum<0) return;
        for(Point p : valves.get(valveNum)){
            localIDgrid[p.x][p.y] = -1;
            cpm.SetID(p.x,p.y,-1);
        }
    }
    @Override
    public void OpenValve(int valveNum){
        if(valveNum>=valves.size() || valveNum<0) return;
        for(Point p : valves.get(valveNum)){
            localIDgrid[p.x][p.y] = 0;
            cpm.SetID(p.x,p.y,0);
        }
    }

    @Override
    public int GetNValves() {
        return valves.size();
    }

    // Brute force for now- maybe fix if slow
    private List<Point> GetConnected(Point start, List<Point> opts){
        List<Point> output = new ArrayList<>();
        List<Point> input = new ArrayList<>();
        output.add(start);
        input.addAll(opts);
        while(TryTransferNeighbours(output,input)){}
        return output;
    }

    private boolean TryTransferNeighbours(List<Point> target, List<Point> points){
        Point transfer = null;

        for (Point p : points){
            for (Point t : target){
                if(IsNeighbour(p,t)){
                    transfer = p;
                }
            }
        }
        if(transfer==null) return false;
        points.remove(transfer);
        target.add(transfer);
        return true;
    }

    private boolean IsNeighbour(Point p1, Point p2){
        return (p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y) == 1;
    }

    @Override
    public Map<String, Color> GetTags(){
        return tags;
    }

    @Override
    public void TakeCommand(String sCom, Object[] args){

        if(sCom=="FILL"){

            int xMin, xMax, yMin, yMax;
            xMin = xMax = yMin = yMax = 0;
            if(args.length>0 && (args[0] instanceof Rectangle)) {
                Rectangle rect = (Rectangle) args[0];
                xMin = rect.x;
                xMax = rect.x+rect.width;
                yMin = rect.y;
                yMax = rect.y+rect.height;
            }
            else{
                xMin = yMin = 0;
                xMax = w;
                yMax = h;
            }

            for(int i=xMin; i<xMax; i++){
                for(int j=yMin; j<yMax; j++){
                    if(localIDgrid[i][j]!=-1) cm1[i][j] = c[i][j] = cp1[i][j] = c0;
                }
            }
        }
    }

    private Point CPtoDiffuser(Point point){

        return new Point((int) Math.round(point.x*1.0/coarseGrained), (int) Math.round(point.y*1.0/coarseGrained));

    }

    private Point DiffuserToCP(Point point){

        return new Point((int) Math.round(point.x * 1.0*coarseGrained), (int) Math.round(point.y * 1.0*coarseGrained));

    }


    @Override
    public double GetConcentration(String sSpecies, Point pIn, String type) {

        Point p = CPtoDiffuser(pIn);

        if(type=="POINT") {

            return c[p.x][p.y];
        }
        if(type=="ARITHMETIC"){

            int cN = 0;
            double total = 0;

            for(int i=p.x-1; i<=p.x+1; i++){
                if(i<0 || i>=c.length) continue;

                for(int j=p.y-1; j<=p.y+1; j++){
                    if(j<0 || j>=c[i].length) continue;
                    if(localIDgrid[i][j]!=0) continue;

                    cN+=1;
                    total += c[i][j];

                }
            }
            if(cN==0) return 0;
            return total/cN;
        }

        if(type=="GEOMETRIC"){

            int cN = 0;
            double total = 0;
            double dS;

            for(int i=p.x-1; i<=p.x+1; i++){
                if(i<0 || i>=c.length) continue;

                for(int j=p.y-1; j<=p.y+1; j++){
                    if(j<0 || j>=c[i].length) continue;
                    if(localIDgrid[i][j]!=0) continue;

                    cN+=1;

                    dS = c[i][j];

                    if(dS>0){
                        total += dS;
                    }

                }
            }
            if(cN==0) return 0;
            return Math.exp(total/cN);

        }

        return  0;
    }

    @Override
    public double GetOccupancy(String sSpecies, Point pIn, String type) {

        Point p = CPtoDiffuser(pIn);

        if(type=="POINT") {

            return c[p.x][p.y] / (c[p.x][p.y]+kD);
        }
        if(type=="ARITHMETIC"){

            int cN = 0;
            double total = 0;

            for(int i=p.x-1; i<=p.x+1; i++){
                if(i<0 || i>=c.length) continue;

                for(int j=p.y-1; j<=p.y+1; j++){
                    if(j<0 || j>=c[i].length) continue;
                    if(localIDgrid[i][j]!=0) continue;

                    cN+=1;
                    total += c[i][j];

                }
            }
            if(cN==0) return 0;
            return total/(total+cN*kD);
        }

        if(type=="GEOMETRIC"){

            int cN = 0;
            double total = 0;
            double dS;

            for(int i=p.x-1; i<=p.x+1; i++){
                if(i<0 || i>=c.length) continue;

                for(int j=p.y-1; j<=p.y+1; j++){
                    if(j<0 || j>=c[i].length) continue;
                    if(localIDgrid[i][j]!=0) continue;

                    cN+=1;

                    dS = c[i][j];

                    if(dS>0){
                        total += dS;
                    }

                }
            }
            if(cN==0) return 0;
            total = Math.exp(total/cN);
            return (total/total+kD);
        }

        return  0;
    }

    @Override
    public void ChangeConcentration(int i, int j, double dC) {

        //System.out.println("Conc alteration");

        Point p = CPtoDiffuser(new Point(i,j));

        //System.out.println(i+":"+j+" -> "+p[0]+":"+p[1]);

        c[p.x][p.y] +=dC;
        c[p.x][p.y] = Math.max(c[p.x][p.y], 0);
    }

    @Override
    public void Update(){

        IntStream.range(0, w).parallel().forEach(i->{
            for(int j=0; j<h; j++){
                UpdatePoint(i, j);
            }
        });
        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                cm1[i][j] = c[i][j];
                c[i][j] = cp1[i][j];
            }
        }
        for(Point p : sourcePoints){
            cm1[p.x][p.y] = c[p.x][p.y] = c0;
        }
        for(Point p: sinkPoints){
            cm1[p.x][p.y] = c[p.x][p.y] = 0;
        }
    }

    public void UpdatePoint(int i, int j){
        if(localIDgrid[i][j]==-1) return;

        int i1, i2, i3, i4;
        double d1, d2, d3, d4;

        double k = 2*dc/(coarseGrained*coarseGrained);

        /*i1 = ((i-1)+w) % w;
        i2 = ((i+1)+w) % w;
        i3 = ((j-1)+h) % h;
        i4 = ((j+1)+h) % h;    */

        i1 = Math.max(0,i-1);
        i2 = Math.min(w-1,i+1);
        i3 = Math.max(0,j-1);
        i4 = Math.min(h-1,j+1);

        d1 = localIDgrid[i1][j]== -1 ? c[i][j] : c[i1][j];
        d2 = localIDgrid[i2][j]== -1 ? c[i][j] : c[i2][j];
        d3 = localIDgrid[i][i3]== -1 ? c[i][j] : c[i][i3];
        d4 = localIDgrid[i][i4]== -1 ? c[i][j] : c[i][i4];

        cp1[i][j] = ((1.0-2.0*k)/(1.0+2.0*k))*cm1[i][j] + (k/(1.0+2.0*k))*(d1 + d2 + d3 + d4);
        cp1[i][j] = Math.max(0,cp1[i][j]);
    }
}
