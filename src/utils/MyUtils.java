package utils;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;
import potts.CellularPotts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by luke on 20/08/16.
 */
public class MyUtils {


    public static <T extends Object> T GetNewInstanceOfType(T obj) {

        Class c = obj.getClass();

        T obj2 = null;
        //try{
        //    if(!c.getConstructor().isAccessible()) return null;
        //} catch(NoSuchMethodException e){e.printStackTrace();};


        try {
            obj2 = (T) c.newInstance();
            System.out.println("Instantiating...");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj2;
    }


    public static int[][] neighbours(int[] centre) {
        int[][] neigh = new int[8][];

        neigh[0] = (new int[]{centre[0] - 1, centre[1] - 1});
        neigh[1] = (new int[]{centre[0], centre[1] - 1});
        neigh[2] = (new int[]{centre[0] + 1, centre[1] - 1});
        neigh[3] = (new int[]{centre[0] - 1, centre[1]});
        neigh[4] = (new int[]{centre[0] + 1, centre[1]});
        neigh[5] = (new int[]{centre[0] - 1, centre[1] + 1});
        neigh[6] = (new int[]{centre[0], centre[1] + 1});
        neigh[7] = (new int[]{centre[0] + 1, centre[1] + 1});

        return neigh;
    }

    public static Point[] allNeighbours(Point centre, CellularPotts cp) {

        Point[] neigh = new Point[8];

        neigh[0] = (new Point(centre.x - 1, centre.y - 1));
        neigh[1] = (new Point(centre.x, centre.y - 1));
        neigh[2] = (new Point(centre.x + 1, centre.y - 1));
        neigh[3] = (new Point(centre.x - 1, centre.y));
        neigh[4] = (new Point(centre.x + 1, centre.y));
        neigh[5] = (new Point(centre.x - 1, centre.y + 1));
        neigh[6] = (new Point(centre.x, centre.y + 1));
        neigh[7] = (new Point(centre.x + 1, centre.y + 1));

        return neigh;
    }

    public static Point[] neighbours(Point centre, CellularPotts cp) {
        int size = 8;

        //if(centre.x==0 || centre.x==cp.w-1) size-=3;
        //if(centre.y==0 || centre.y==cp.h-1) size-=3;
        //if(size==2) size+=1;

        Point[] neigh = new Point[size];

        int n=0;
        for(int i=centre.x-1; i<=centre.x+1; i++){
            for(int j=centre.y-1; j<=centre.y+1; j++){
                if(i==centre.x&&j==centre.y) continue;

                if(i<0||i>=cp.w || j<0||j>=cp.h){
                    neigh[n] = new Point(i,j);
                }
                else {
                    neigh[n] = cp.pointsPool[i + j * cp.w];
                    //System.out.println("Neighbour n ::"+(neigh[n].x-centre.x)+":"+(neigh[n].y-centre.y));
                }
                n++;
            }
        }
        return  neigh;
    }

    public static <T> List<T> Union(List<T> list1, List<T> list2){
        HashSet<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }

    public static <T> List<T> Intersection(List<T> list1, List<T> list2){

        ArrayList inter = new ArrayList<T>(list1);
        inter.retainAll(list2);

        return inter;
    }


    public static int[] neighbour(int[] centre, int dir){

        int x = centre[0];
        int y = centre[1];

        switch(dir){
            case 0:
                x-=1;
                y-=1;
                break;
            case 1:
                x-=1;
                break;
            case 2:
                x-=1;
                y+=1;
                break;
            case 3:
                y-=1;
                break;
            case 4:
                y+=1;
                break;
            case 5:
                x+=1;
                y-=1;
                break;
            case 6:
                x+=1;
                break;
            case 7:
                x+=1;
                y+=1;
                break;
        }

        return new int[]{x,y};
    }

    public static Point neighbour(Point centre, int dir, CellularPotts cp){

        int x = centre.x;
        int y = centre.y;

        switch(dir){
            case 0:
                x-=1;
                y-=1;
                break;
            case 1:
                x-=1;
                break;
            case 2:
                x-=1;
                y+=1;
                break;
            case 3:
                y-=1;
                break;
            case 4:
                y+=1;
                break;
            case 5:
                x+=1;
                y-=1;
                break;
            case 6:
                x+=1;
                break;
            case 7:
                x+=1;
                y+=1;
                break;
        }

        if(x>0&&x<cp.w && y>0 && y<cp.h) return cp.pointsPool[x+y*cp.w];
        return new Point(x,y);
    }


    public static int[][] BoxBlur(int[][] in, int radius, int passes){

        int w = in.length;
        int h = in[0].length;

        double[][] dIn = new double[w][h];
        int[][] iOut   = new int[w][h];

        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                dIn[i][j] = in[i][j];
            }
        }

        for(int i=0; i<passes; i++){
            dIn = TransposingBoxBlur1D(dIn, radius);
            dIn = TransposingBoxBlur1D(dIn, radius);
        }

        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                iOut[i][j] = (int) dIn[i][j];
            }
        }

        return iOut;
    }

    private static double[][] TransposingBoxBlur1D(double[][] input, int radius){

        int w = input.length;
        int h = input[0].length;

        double[][] out = new double[h][w];



        //for(int j=0; j<h; j++){
        IntStream.range(0, h).parallel().forEach(j->{

            // Initialise the kernel
            double mu = input[0][j];
            for(int i=1; i<=radius; i++) mu += (input[wrapped(i, 0, w)][j]+input[wrapped(-i, 0, w)][j]);

            out[j][0] = mu/(1.+2.*radius);

            // Then run along, adding the new entry and removing the old one.
            for(int i=1; i<w; i++){
                mu+= (input[wrapped(i+radius, 0, w)][j]-input[wrapped(i-(radius+1), 0, w)][j]);
                out[j][i] = mu/(1.+2.*radius);
            }

        });
        return out;
    }

    private static int wrapped(int i, int min, int max){
        return ((i+(max-min))%(max-min)) + min;
    }

    //public static int[][] VarianceFilter(){


    //}

    public static int[][] DetectEdges(int[][] input){

        int[][] iX = EdgeFilter1D(input, false);
        int[][] iY = EdgeFilter1D(input, true);

        int w = input.length;
        int h = input[0].length;

        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                iX[i][j] = (int) Math.sqrt(iX[i][j]*iX[i][j]+iY[i][j]*iY[i][j]);
            }
        }
        return iX;
    }


    public static int[][] EdgeFilter1D(int[][] input, boolean vertical){

        int[][] sX = new int[][] {{-1,-2,-1},{0,0,0},{1,2,1}};

        if(vertical) sX = new int[][] {{-1,0,1},{-2,0,2},{-1,0,1}};

        int w = input.length;
        int h = input[0].length;

        int[][] out = new int[w][h];

        for(int i=1; i<w-1; i++){
            for(int j=1; j<h-1; j++){
                for(int x=-1; x<=1; x++){
                    for(int y=-1; y<=1; y++){
                        out[i][j] += sX[x+1][y+1]*input[i+x][j+y];
                    }
                }
            }
        }

        return out;
    }



    /*public static void BindListViewToDoubleList(ListView labels, ListView vals, List<Double> ld){

        // Set listeners for user input
        vals.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView listView) {
                TextFieldListCell<Double> cell = new TextFieldListCell<>();
                cell.setConverter(new DoubleStringConverter());
                //if (cell.itemProperty().getValue() != null) {
                cell.itemProperty().addListener(new ChangeListener<Double>() {
                    @Override
                    public void changed(ObservableValue<? extends Double> observableValue, Double oldVal, Double newVal) {
                        //System.out.println("value: "+oldVal+" -> "+newVal);
                        if (newVal == null) return;
                        try {
                            //handleValueChanged2(newVal);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ;
                    }
                });
                //}
                return cell;
            }
        });
        lv.setEditable(true);

    }        */





    public static int[] GetARGBPixelArrayFromIntImages(int[][] grey, int[][][] fluo){

        int w = grey.length;
        int h = grey[0].length;

        int[] out = new int[w*h];

        short val = 0;

        IntStream.range(0, w).parallel().forEach(i->{
            for (int j = 0; j < h; j++) {
                short a = 255;
                short r = 0;
                short g = 0;
                short b = 0;

                r = (short) Math.max(0, Math.min(255, grey[i][j]+fluo[i][j][0]));
                g = (short) Math.max(0, Math.min(255, grey[i][j]+fluo[i][j][1]));
                b = (short) Math.max(0, Math.min(255, grey[i][j]+fluo[i][j][2]));

                out[i+w*j] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        });

        return out;
    }

    public static double[] GetNormal(double[] point, double radius, CellularPotts cp, int cellNum){

        double mx = 0;
        double my = 0;
        int iN    = 0;

        // Find mean x,y of the cell in question...
        for(int i = (int) (point[0]-radius); i<=point[0]+radius; i++){
            for(int j = (int) (point[1]-radius); j<=point[1]+radius; j++){
                if((point[0]-i)*(point[0]-i)+(point[1]-j)*(point[1]-j) <= radius*radius){
                    if(cp.GetID(i,j)==cellNum) {
                        iN+=1;
                        mx+=i;
                        my+=j;
                    }
                }

            }
        }

        mx/=iN;
        my/=iN;

        // Normal direction is point-this-mean x,y

        mx = mx-point[0];
        my = my-point[1];

        if(mx==0 && my==0) return new double[]{0,0};

        double nrm = Math.sqrt(mx*mx+my*my);

        mx/=nrm;
        my/=nrm;

        return  new double[]{mx, my};
    }

    public static double[] GetPotentialNormal(double[] point, Point target, double radius, CellularPotts cp, int cellNum, int newID){

        double mx = 0;
        double my = 0;
        int iN    = 0;

        // Find mean x,y of the cell in question...
        if(newID==cellNum) {
            for (int i = (int) (point[0] - radius); i <= point[0] + radius; i++) {
                for (int j = (int) (point[1] - radius); j <= point[1] + radius; j++) {
                    if ((point[0] - i) * (point[0] - i) + (point[1] - j) * (point[1] - j) <= radius * radius) {
                        if (cp.GetID(i, j) == cellNum || (i == target.x && j == target.y)) {
                            iN += 1;
                            mx += i;
                            my += j;
                        }
                    }

                }
            }
        }
        else{
            for (int i = (int) (point[0] - radius); i <= point[0] + radius; i++) {
                for (int j = (int) (point[1] - radius); j <= point[1] + radius; j++) {
                    if ((point[0] - i) * (point[0] - i) + (point[1] - j) * (point[1] - j) <= radius * radius) {
                        if (cp.GetID(i, j) == cellNum && (i != target.x && j != target.y)) {
                            iN += 1;
                            mx += i;
                            my += j;
                        }
                    }

                }
            }
        }

        if(iN==0) return new double[]{0,0};

        mx/=iN;
        my/=iN;

        // Normal direction is point-this-mean x,y

        mx = mx-point[0];
        my = my-point[1];

        if(mx==0 && my==0) return new double[]{0,0};

        double nrm = Math.sqrt(mx*mx+my*my);

        mx/=nrm;
        my/=nrm;

        return  new double[]{mx, my};
    }

    public static double Curvature(double[] point, double radius, CellularPotts cp, int cellNum){

        double[] normalC = GetNormal(point,radius,cp,cellNum);

        if(normalC[0]==0 && normalC[1]==0) return 2*Math.PI;

        double[] pointL = new double[]{point[0]-0.5*radius*normalC[1],point[1]+0.5*radius*normalC[0]};
        double[] pointR = new double[]{point[0]+0.5*radius*normalC[1],point[1]-0.5*radius*normalC[0]};

        double[] normalL = GetNormal(pointL, radius, cp, cellNum);
        double[] normalR = GetNormal(pointR, radius, cp, cellNum);

        return -Math.acos(normalL[0]*normalR[0]+normalL[1]*normalR[1])/radius;
    }

    public static double PotentialCurvature(double[] point, Point target, double radius, CellularPotts cp, int cellNum, int targetNum){

        double[] normalC = GetPotentialNormal(point, target, radius, cp, cellNum, targetNum);

        if(normalC[0]==0 && normalC[1]==0) return 2*Math.PI;

        double[] pointL = new double[]{point[0]-0.35*radius*normalC[1],point[1]+0.35*radius*normalC[0]};
        double[] pointR = new double[]{point[0]+0.35*radius*normalC[1],point[1]-0.35*radius*normalC[0]};

        double[] normalL = GetPotentialNormal(pointL, target, radius, cp, cellNum, targetNum);
        double[] normalR = GetPotentialNormal(pointR, target, radius, cp, cellNum, targetNum);

        return Math.acos(normalL[0]*normalR[0]+normalL[1]*normalR[1])/radius;
    }
}
