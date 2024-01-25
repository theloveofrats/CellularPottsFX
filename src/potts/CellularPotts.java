package potts;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ecl.ExtracellularLattice;
import org.w3c.dom.css.Rect;
import ui.*;
import utils.MyUtils;
import utils.Point;
import utils.RNG;

/**
 * Created by luke on 20/08/16.
 */
public class CellularPotts implements Serializable{

    public int w;
    public int h;

    public boolean terminated = false;
    public int mcsCount = 0;

    public boolean recording = false;
    public String recordPath = "";

    public double BoltzT = 5.;
    public double taxis = 0;
    public boolean paused = true;
    public boolean drawing = false;
    private long timeAtFirstMCS;
    private long timeAtLastMCS;

    private Rectangle focusRect;

    public Point[] pointsPool;


    public transient CPViewer viewer;
    Random rnd = RNG.rnd;
    public List<Cell> cells = new CopyOnWriteArrayList<Cell>();
    private int[][] idGrid;
    //private Object lock = new Object();
    //private Lock[] locks = new Lock[4];
    public ExtracellularLattice ecl;

    public CellularPotts(int width, int height) {

        this.w = width;
        this.h = height;
        this.idGrid = new int[w][h];
        //for(int i=0; i<locks.length; i++) locks[i] = new ReentrantLock();
        this.viewer = new CPViewer(this);

        pointsPool = new Point[w*h];
        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++) {
                pointsPool[j * w + i] = new Point(i,j);
            }
        }
    }

    public void Run(){
        Thread th = new Thread() {
            public void run() {
                while(!terminated){
                    if(!paused && !drawing) {
                        MCS();
                    }
                    else try {
                        sleep(15);                 //1000 milliseconds is one second.
                    } catch (InterruptedException ex) {
                        currentThread().interrupt();
                    }
                }
            }
        };
        Thread th2 = new Thread() {
            public void run() {
                while(!terminated){
                    //if(!sim.paused) {
                    //sim.MCS();
                    //}
                    try {
                        drawing = true;
                        draw();
                        drawing = false;
                        sleep(50);                 //1000 milliseconds is one second.
                    } catch (InterruptedException ex) {
                        currentThread().interrupt();
                    }
                }
            }
        };

        th.start();
        th2.start();
    }

    public synchronized void MCS() {

        if (mcsCount == 0) {
            timeAtFirstMCS = System.currentTimeMillis();
        } else if (mcsCount % 5000 == 0) {
            timeAtLastMCS = System.currentTimeMillis();
            System.out.println("MCS/s :: " + (5000 * 1000d / (timeAtLastMCS - timeAtFirstMCS)));
            timeAtFirstMCS = System.currentTimeMillis();
        }

        mcsCount++;

        //Serial update of cell positions, as these can interact unpredictably if run in parallel.
        for(Cell cell : cells){
            SingleCellAttackStep(cell);
        }

        // Parallel update of biochemistry of each cell according to internal models
        cells.stream().parallel().forEach(cell -> SafeUpdateModel(cell));
        //for(Cell cell : cells){
        //    if(cell.model!=null) cell.model.Update(cell);
        //}

        if (ecl != null) {
            ecl.Update();
        }
    }

    public void SafeUpdateModel(Cell cell) {
        if (cell.model != null) cell.model.Update(cell);
    }

    public void SingleCellAttackStep(Cell cell) {
        Point target;
        boolean flip;
        int boundsize = cell.boundary.size();
        //System.out.print("Bounds size: "+boundsize+"\n");

        Point[] workingBoundary = cell.boundary.toArray(new Point[cell.boundary.size()]);

        for (int i = 0; i < 2 * boundsize; i++) {
            // Select random target cell from neighbours
            Point coCell = workingBoundary[rnd.nextInt(workingBoundary.length)];
            target = MyUtils.neighbour(coCell, rnd.nextInt(8), this);
            flip = rnd.nextBoolean();
            if (flip) AttemptCopy(target, coCell, true);
            else AttemptCopy(coCell, target, false);

        }
    }

    public void AddCell(Cell c) {

        if (!cells.contains(c)) cells.add(c);

    }

    private boolean CausesSplit(int[] source, int[] target){

        int c = GetID(target);

        if(c==0) return false;

        int T = 0;
        int t = 0;
        int b = 0;
        int l = 0;
        int r = 0;

        for(int i=target[0]-1; i<=target[0]+1; i++){
            for(int j=target[1]-1; j<=target[1]+1; j++){
                if(GetID(i,j)==c){
                    T++;
                    if(i==target[0]-1) l+=1;
                    if(i==target[0]+1) r+=1;
                    if(j==target[1]-1) t+=1;
                    if(j==target[1]+1) b+=1;
                }
            }
        }

        if(T<3) return  false;
        if(T>7) return  false;
        if((t>0&&b>0&&t+b==T-1) || (l>0&&r>0&&l+r==T-1)) return true;
        if(l>0&&r>0&&t>0&&b>0&&2*T==4+l+r+t+b) return true;
        return false;
    }

    void AttemptCopy(Point source, Point target, boolean bg_copy) {

        try{
            //Thread.currentThread().sleep(2000);
        } catch(Exception e){};
        //System.out.println("ATTEMPT COPY");

        int source_cellnum;
        int target_cellnum;

        source_cellnum = GetID(source);
        target_cellnum = GetID(target);

        if (source_cellnum == target_cellnum) return;

        if (bg_copy) source_cellnum = 0;

        // If they're the same cell, or invalid, return.
        if (source_cellnum <= -1 || target_cellnum <= -1) return;

        Cell cS, cT;

        // Find dH for each cell for the proposed change.
        // If a cellnum indicates background, set background dH to zero!

        cS = source_cellnum <= 0 ? null : cells.get(source_cellnum - 1);
        cT = target_cellnum <= 0 ? null : cells.get(target_cellnum - 1);

        AttemptCopyFromTo(cS, cT, target, source, source_cellnum, bg_copy);
    }

    public void AttemptCopyFromTo(Cell cS, Cell cT, Point target, Point source, int source_cellnum, boolean bg_copy){
        double dHS;
        double dHT;
        double dH;

        dHS = (cS == null) ? 0 : cS.GetDH(target, source, source_cellnum);
        dHT = (cT == null) ? 0 : cT.GetDH(target, source, source_cellnum);
        dH = dHS + dHT;
        /*if (CausesSplit(source, target)) {
            dH+=5000;
        }*/
        //TaxisDirection
        //int iDir = (target[0]>source[0]) ? 1 : target[0]==source[0] ? 0 : -1;
        //dH -= iDir*taxis; //Decrease in energy due to taxis bias.

        if (dH < 0 || rnd.nextDouble() < Math.exp(-dH / BoltzT)) {
            // Copy attempts wins, so tell cells to commit putative changes!

            idGrid[target.x][target.y] = source_cellnum;
            if (cS != null) {
                cS.AddPoint(target);
                cS.Commit(target);
            }
            if (cT != null) {
                cT.RemovePoint(target);
                cT.Commit(target);
            }
            //System.out.println("Cell "+source_cellnum+" won at "+target[0]+":"+target[1]);
            if (cS != null && cS.model != null) {
                cS.model.InitialiseConqueredPoint(target, source);
            }
            // After here, put in repetitions based on excessive extra energy
            if (dH < 0 && Math.exp(dH) < rnd.nextDouble()) {

                Point newSource = target;
                Point newTarget = MyUtils.neighbour(newSource, rnd.nextInt(8),this);
                AttemptCopy(newSource, newTarget, bg_copy);
            }
        }
    }

    int example_num = 0;
    private int ticks = 0;
    public void draw () {
        viewer.draw();
        ticks++;
        if (recording & ticks<80 && ticks%4==0) {

            viewer.SaveToFile(Integer.toString(example_num));
        }
        if(ticks>=105) {
            example_num++;
            ticks = 0;
        }
    }

    public int GetID ( int x, int y){
        if (x < 0 || x >= w || y < 0 || y >= h) return -1;
        return idGrid[x][y];
    }

    public int GetID (Point p){
        if (p.x < 0 || p.x >= w || p.y < 0 || p.y >= h) return -1;
        return idGrid[p.x][p.y];
    }

    public int GetID ( int[] p){
        int x = p[0];
        int y = p[1];
        if (x < 0 || x >= w || y < 0 || y >= h) return -1;
        return idGrid[x][y];
    }

    public int[][] GetIDGrid () {
        return idGrid;
    }

    public void SetID ( int[] p, int v){
        idGrid[p[0]][p[1]] = v;
    }

    public void SetID ( int x, int y, int v){
        idGrid[x][y] = v;
    }

    public Rectangle GetFocusRect(){
        return focusRect;
    }
    public void SetFocusRect(Rectangle rect){
        focusRect = rect;
    }

    public void UpdateCells (String tag) {
        for (Cell c : cells) {
            if (c.cdt.TypeName == tag) {
                c.SetRecords();
            }
        }
    }

    public void TryPhotoactivate(String target, double value){
        Rectangle focus = GetFocusRect();
        if(focus!=null){
            for(Cell c : cells){
                if(c.model!=null){
                    if(c.model.GetElements().contains(target)) {
                        for (Point p : c.points) {
                            if (focus.contains(p.x, p.y)) {
                                c.model.SetLocalConcentration(target, p, value);
                            }
                        }
                    }
                }
            }
        }
    }
}