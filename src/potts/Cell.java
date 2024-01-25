package potts;

import models.Model;
import utils.MyUtils;
import utils.Point;
import utils.UtilityArrayList;

import java.io.Serializable;
import java.util.*;

/**
 * Created by luke on 20/08/16.
 */
public class Cell implements Serializable{

    // Physical constraints
    public CellDataTable cdt;
    public static Map<String, Double> perims = new HashMap<>();

    double a0 = 300.;
    double aK = 1.;
    double dA = 0;
    public int area = 1;

    double dP = 0;
    double p0 = 500.;
    double pK = 1.;
    public int perimeter = 8;

    double c0 = 0.1;
    double cK = -10;
    double cR = 3d;

    CellularPotts cp;

    public int cellnum;
    String cellType;

    public Model model = null;

    //Cell-cell constraints
    Map<String, Double> interactions = new HashMap<String, Double>();

    public Set<Point> boundary            =  new HashSet<Point>();
    public List<Point> potentialBoundaryGained =  new ArrayList<>();
    public List<Point> potentialBoundaryLost   =  new ArrayList<>();
    public HashMap<Point,Double> curvature = new HashMap<>();
    public HashMap<Point,Double> newCurvature = new HashMap<>();

    public List<Point> points   = new ArrayList<Point>();

    /*public static void InitCell(){

        char[] key = new char[8];

        short[][] pic = new short[3][3];

        for(short i1=0; i1<=1; i1++){
            pic[0][0]=i1;
            key[0] = Short.toString(i1).toCharArray()[0];
            for(short i2=0; i2<=1; i2++){
                pic[1][0]=i2;
                key[1] = Short.toString(i2).toCharArray()[0];
                for(short i3=0; i3<=1; i3++){
                    pic[2][0]=i3;
                    key[2] = Short.toString(i3).toCharArray()[0];
                    for(short i4=0; i4<=1; i4++){
                        pic[0][1]=i4;
                        key[3] = Short.toString(i4).toCharArray()[0];
                        for(short i5=0; i5<=1; i5++){
                            pic[2][1]=i5;
                            key[4] = Short.toString(i5).toCharArray()[0];
                            for(short i6=0; i6<=1; i6++){
                                pic[0][2]=i6;
                                key[5] = Short.toString(i6).toCharArray()[0];
                                for(short i7=0; i7<=1; i7++){
                                    pic[1][2]=i7;
                                    key[6] = Short.toString(i7).toCharArray()[0];
                                    for(short i8=0; i8<=1; i8++){
                                        pic[2][2]=i8;
                                        key[7] = Short.toString(i8).toCharArray()[0];

                                        String sKey = new String(key);

                                        double dVal = 0;

                                        dVal+=FindQuadPerimeterChangeN8(i2, i4, i1);
                                        dVal+=FindQuadPerimeterChangeN8(i2, i5, i3);
                                        dVal+=FindQuadPerimeterChangeN8(i7, i4, i6);
                                        dVal+=FindQuadPerimeterChangeN8(i7, i5, i8);

                                        //System.out.println("Key " + sKey + " -> " + dVal);

                                        perims.put(sKey, dVal);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }  */

    private static double FindQuadPerimeterChangeN8L2(int iH1, int iH2, int iDiag){

        double dVal = 0;

        double dC1 = 1;
        double dC2 = 0;
        double dC3 = Math.sqrt(2)-1;
        double dC4 = (Math.sqrt(2)/2) - 1;
        double dC5 = -Math.sqrt(2)/2;

        if(iDiag==0){
            if(iH1==0&&iH2==0)      dVal+=dC1;
            else if(iH1==1&&iH2==1) dVal+= dC5;
            else if(iH1==0&&iH2==1
                 || iH1==1&&iH2==0) dVal+=dC2;
        }
        if(iDiag==1){
            if(iH1==0&&iH2==0)      dVal+=dC3;
            else if(iH1==1&&iH2==1) dVal+= dC5;
            else if(iH1==0&&iH2==1
                    || iH1==1&&iH2==0) dVal+=dC4;
        }

        return dVal;
    }

    // Just does an N-4 perimeter!
    private static double FindQuadPerimeterChangeN4(int iH1, int iH2, int iDiag){

        double dVal = 0;

        dVal = (0.5-iH1)+(0.5-iH2);

        return dVal;
    }

    // Just does an N-8 perimeter!
    private static double FindQuadPerimeterChangeN8(int iH1, int iH2, int iDiag){

        double dVal = 0;

        dVal = (0.5-iH1)+(0.5-iH2)+(1.0)*(1-2*iDiag);

        return dVal;
    }

    public Cell(Point seedPoint, CellularPotts cp, CellDataTable cdt) throws Exception{

        this.cp = cp;
        if((seedPoint.x<0||seedPoint.x>=cp.w)
        || (seedPoint.y<0||seedPoint.y>=cp.h)){

            throw (new Exception());
        }

        this.cdt = cdt;
        this.cellType = cdt.TypeName;
        cp.AddCell(this);
        this.cellnum = cp.cells.size();
        AddPoint(seedPoint);
        boundary.add(seedPoint);
        newCurvature.put(seedPoint, 0.5 * Math.PI);
    }

    public void AddPoint(Point point){

        synchronized (points){

            if(!points.contains(point)) {

                    points.add(point);
            }
            cp.SetID(point.x, point.y, cellnum);
        }

    }
    public void RemovePoint(Point point){

        synchronized (points){
            int i = points.indexOf(point);
            if(i==-1) return;

            if(model!=null){
                model.ClearLostPoint(points.get(i));
            }

            points.remove(i);
        }
    }

    //public void SetInteractionStrength(String otherType, double strength){
    //    interactions.put(otherType, strength);
    //}

    private void SetPotentialBoundary(Point target, Point source, int newID){
        potentialBoundaryGained.clear();
        potentialBoundaryLost.clear();
        //System.out.println("Resetting for new attack on "+target.x+":"+target.y+".");

        boolean isBoundary;
        boolean isBoundary2;

        if(newID==cellnum){
            isBoundary2=false;
            for(Point pt : MyUtils.neighbours(target, cp)){
                // If target neighbour is a current part of the cell
                if(cp.GetID(pt)==cellnum) {
                    isBoundary = false;
                    // If any of the non-target neighbours are non-cell, still boundary.
                    for (Point pt2 : MyUtils.neighbours(pt, cp)) {
                        if (pt2 == target) continue;
                        if (cp.GetID(pt2) != cellnum) {
                            isBoundary = true;
                            break;
                        }
                    }
                    // Otherwise, this point might lose its final non-cell neighbour.
                    if (!isBoundary) {
                        potentialBoundaryLost.add(pt);
                    }
                }
                // Else the target remains next to non-cell, and is boundary!
                else{
                    isBoundary2=true;
                }
            }
            if(isBoundary2){
                if(!boundary.contains(target)) potentialBoundaryGained.add(target);
            }
        }
        else{
            potentialBoundaryLost.add(target);
            for(Point pt : MyUtils.neighbours(target, cp)){
                if(cp.GetID(pt)==cellnum){
                    if (!boundary.contains(pt)) potentialBoundaryGained.add(pt);
                }
            }
        }
    }

    // Calculates shape-based energy change-
    // if newID is this cell's ID, assumes it's attempting to gain point.
    // else assumes it might loose point.
    public double GetDH(Point target, Point source, int newID){

        //System.out.println("CELL.GetDH");

        SetPotentialBoundary(target,source,newID);

        double h_dP;
        double h_dA;
        double h_dC = 0;

        dP = GetLocalDP2(target);
        dP = newID==this.cellnum ? dP : -dP;

        dA = newID==this.cellnum ? 1 : -1;

        h_dP = pK*(dP*dP + 2*dP*(perimeter-p0));
        h_dA = aK*(dA*dA + 2*dA*(area-a0));
        h_dC = cK*GetHDC(source, target, newID);

        double h_model = 0;

        if(this.model!=null) {
            h_model = (newID == cellnum) ? model.GetAttackBias(source) : model.GetDefendBias(target);
        }

        return h_dA+h_dP+h_dC+h_model;
    }

    public double GetHDC(Point source, Point target, int newID)
    {

        //System.out.println("Cell.GetDHC");

        double currentCurvature = 0;
        double possibleCurvature = 0;
        double workingCurvature = 0;

        Point p1;

        newCurvature.clear();


        for (int i = target.x - (int) (cR + 1); i <= target.x + (int) (cR + 1); i++) {
            for (int j = target.y - (int) (cR + 1); j <= target.y + (int) (cR + 1); j++) {
                if(i<0||i>=cp.w||j<0||j>=cp.h) continue;
                if ((target.x - i) * (target.x - i) + (target.y - j) * (target.y - j) > cR*cR) continue;

                p1 = cp.pointsPool[i+j*cp.w];

                // Running total of current local curvature
                if(curvature.containsKey(p1)){
                    currentCurvature+=curvature.get(p1);
                }
                if(potentialBoundaryGained.contains(p1)){
                    workingCurvature = MyUtils.PotentialCurvature(new double[]{p1.x,p1.y},target,cR,cp,cellnum,newID);
                    possibleCurvature+=workingCurvature;
                    newCurvature.put(p1,workingCurvature);
                }
                if(boundary.contains(p1) && !potentialBoundaryLost.contains(p1)){
                    workingCurvature = MyUtils.PotentialCurvature(new double[]{p1.x,p1.y},target,cR,cp,cellnum,newID);
                    possibleCurvature+=workingCurvature;
                    newCurvature.put(p1,workingCurvature);
                }
            }
        }

        return -(possibleCurvature*possibleCurvature - currentCurvature*currentCurvature) - 2*c0*(possibleCurvature-currentCurvature);
    }

    public double GetLocalDP(int[] point){
        return perims.get(perimString(point));
    }


    // Returns perimeter change on winning point, assuming we don't have it.
    public double GetLocalDP2(Point point){

        int iS = 0;
        int iD = 0;

        for(Point p : MyUtils.neighbours(point, cp)){
            if(cp.GetID(p)==cellnum) iS += 1;
            else iD+=1;
        }

        return (iD-iS);
    }

    private String perimString(int[] p){

        char[] s = new char[8];
        int n = 0;

        for(int i=p[0]-1; i<=p[0]+1; i++){
            for(int j=p[1]-1; j<=p[1]+1; j++){

                if(i==p[0] && j==p[1]) continue;

                if(cp.GetID(i,j)==cellnum) s[n]='1';
                else s[n]='0';
                n++;
            }
        }
        return new String(s);
    }

    public synchronized void SetRecords(){

        //System.out.println("UPDATING CELL TYPE "+cdt.TypeName);

        this.cellType =cdt.TypeName;
        this.a0 = cdt.a0;
        this.aK = cdt.aK;
        this.p0 = cdt.p0;
        this.pK = cdt.pK;

        // Clean out old model before updating!
        if(this.model!=null) {
            if (this.model != cdt.model) {
                for (Point p : this.points) {
                    this.model.ClearLostPoint(p);
                }
            }
        }
        this.model = cdt.model;
    }

    public void UpdateBoundary(){
        for(Point p : potentialBoundaryLost){
            synchronized (boundary) {
                boundary.remove(p);
                curvature.remove(p);
            }
        }
        for(Point p : potentialBoundaryGained){
            SafeAddPointToBoundary(p);
        }
    }

    private void SafeAddPointToBoundary(Point p){

        synchronized (boundary) {
            boundary.add(p);
        }
    }

    private void UpdateCurvature(Point p){

        for(int i= (int) Math.max(0,p.x-cR); i<=Math.min(cp.w-1,p.x+cR); i++) {
            for (int j = (int) Math.max(0, p.y - cR); j <= Math.min(cp.h-1, p.y + cR); j++) {
                if (newCurvature.containsKey(cp.pointsPool[i+j*cp.w])) {
                    curvature.put(cp.pointsPool[i+j*cp.w], newCurvature.get(cp.pointsPool[i+j*cp.w]));
                }
            }
        }
    }

    public void Commit(Point p){

        perimeter+=dP;
        area+=dA;

        UpdateBoundary();
        UpdateCurvature(p);

        //System.out.println(boundary.size()+"::"+curvature.size());

        dP = 0;
        dA = 0;
    }
}
