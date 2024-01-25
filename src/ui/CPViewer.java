package ui;
import javax.imageio.*;

import ecl.ExtracellularLattice;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import potts.Cell;
import potts.CellularPotts;
import utils.MyUtils;
import utils.Point;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.Set;

/**
 * Created by luke on 20/08/16.
 */
public class CPViewer{

    public static int padding = 20;

    Random rnd = new Random();
    public  CellularPotts cp;
    public ExtracellularLattice ec;
    private ResizableCanvas canvas;
    private GraphicsContext gc;
    private WritableImage img;
    private int[] dragStartPoint;
    private Rectangle dragRect;
    private Effect eft = new GaussianBlur(2.0);
    private Stage stage;


    public CPViewer(CellularPotts cellularpotts) {

        this.cp = cellularpotts;
        canvas = new ResizableCanvas(cp.w, cp.h);
        img = new WritableImage(cp.w,cp.h);
        gc = canvas.getGraphicsContext2D();

        // Add support for mouse clicks
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent t) {
                        dragRect = null;
                        if(t.getButton()== MouseButton.PRIMARY){
                            dragStartPoint = new int[]{(int) ((cp.w/canvas.getWidth())*(t.getSceneX())), (int) ((cp.h/canvas.getHeight())*(t.getSceneY()))};
                        }
                    }
                });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent t) {

                        if(t.getButton()==MouseButton.PRIMARY) {
                            int[] endPoint = new int[]{(int) ((cp.w / canvas.getWidth()) * (t.getSceneX())), (int) ((cp.h / canvas.getHeight()) * (t.getSceneY()))};
                            if (endPoint[0] == dragStartPoint[0] || endPoint[1] == dragStartPoint[1]) {
                                dragRect = null;
                            } else {

                                int xMin, xw, yMin, yh;
                                xMin = Math.min(dragStartPoint[0], endPoint[0]);
                                xw = Math.abs(dragStartPoint[0] - endPoint[0]);
                                yMin = Math.min(dragStartPoint[1], endPoint[1]);
                                yh = Math.abs(dragStartPoint[1] - endPoint[1]);

                                if (dragRect == null) dragRect = new Rectangle(xMin, yMin, xw, yh);
                                else {
                                    dragRect.x = xMin;
                                    dragRect.y = yMin;
                                    dragRect.width = xw;
                                    dragRect.height = yh;
                                }
                            }
                        }
                    }
                });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent t) {
                        if(t.getButton()==MouseButton.PRIMARY){
                            // Check that the mouse hasn't moved before adding a cell.
                            Point point = new Point((int) ((cp.w/canvas.getWidth())*(t.getSceneX())), (int) ((cp.h/canvas.getHeight())*(t.getSceneY())));

                            if(point.x==dragStartPoint[0] && point.y == dragStartPoint[1]){
                                if(t.isShiftDown()){

                                }
                                else AddCellToSimulation(point);
                            }
                        }
                        cp.SetFocusRect(dragRect);
                    }
                });

                stage = new Stage();
        Group root = new Group();
        root.getChildren().add(canvas);
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(new EventHandler<KeyEvent>()

        {
            @Override
            public void handle(KeyEvent ke) {
                if (ke.getCode() == KeyCode.ENTER && ke.isControlDown()) {
                    UIController.current.btnPlayHandler();
                }
            }
        });


        Scene scene = new Scene(root, cp.w, cp.h);

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
                //System.out.println("Width: " + newSceneWidth);
                canvas.setWidth(newSceneWidth.doubleValue());
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
                //System.out.println("Height: " + newSceneHeight);
                canvas.setHeight(newSceneHeight.doubleValue());
            }
        });
            stage.setScene(scene);
            stage.show();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                cp.paused = true;
                cp.terminated = true;
            }
        });
    }

    public void AddCellToSimulation(Point point){
        Cell newCell;
        try{
            newCell = new Cell(point, cp, UIController.current.cdt);
            newCell.SetRecords();
            draw();
        } catch(Exception e){;};
    }

    public void draw(){
        //System.out.println("Drawing");
        //int[] microimg = cp.uic.microscope.GetPixelArray(cp.idGrid);

        int[][] gray = UIController.current.microscope.GetImage(cp.GetIDGrid());

        int[][][] fluo = new int[cp.w][cp.h][3];

        FillFluorescenceImage(fluo);

        int[] combined = MyUtils.GetARGBPixelArrayFromIntImages(gray, fluo);

        img.getPixelWriter().setPixels(0, 0, cp.w, cp.h, PixelFormat.getIntArgbInstance(), combined, 0, cp.w);
        gc.drawImage(img, 0, 0, canvas.getWidth(), canvas.getHeight());
        gc.applyEffect(eft);

        DrawOverlays();
    }

    protected void DrawOverlays(){
        if(dragRect!=null) {
            gc.setStroke(Color.AZURE);
            gc.setLineWidth(1);
            gc.strokeRect((canvas.getWidth()/cp.w) * dragRect.getX(), (canvas.getHeight()/cp.h) * dragRect.getY(), (canvas.getWidth()/cp.w) * dragRect.getWidth(), (canvas.getHeight()/cp.h) * dragRect.getHeight());
        }
    }




    public void Close(){
        cp.paused = true;
        cp.terminated = true;

    }

    public void Highlight(Rectangle rect){
        dragRect = rect;
    }

    public synchronized void FillFluorescenceImage(int[][][] img){

        double dLaser = UIController.current.GetLaserBrightness();

        int[] ij = new int[2];

        if(dLaser<=0) return;

        if(ec!=null) {
            double conc;
            Color cEx;
            for(String s: ec.GetTags().keySet()) {
                cEx = ec.GetTags().get(s);
                if(cEx==Color.TRANSPARENT) continue;

                for (int i = 0; i < cp.w; i++) {
                    for (int j = 0; j < cp.h; j++) {

                        Point pt = cp.pointsPool[i+j*cp.w];

                        conc = cp.ecl.GetConcentration("Attractant", pt, "POINT");

                        img[i][j][0] += 1.5 * conc * dLaser * cEx.getRed()   * (1 + 0.4 * rnd.nextDouble());
                        img[i][j][1] += 1.5 * conc * dLaser * cEx.getGreen() * (1 + 0.4 * rnd.nextDouble());
                        img[i][j][2] += 1.5 * conc * dLaser * cEx.getBlue()  * (1 + 0.4 * rnd.nextDouble());
                    }
                }
            }
        }


        //for(Cell c : cp.cells){
        cp.cells.stream().parallel().forEach(c->{
            Color m;
            Color e;
            Color f;

            double dConc = 0;

            e = c.cdt.cellTag;
            m = c.cdt.membraneTag;

            if(m!=null && m!= Color.TRANSPARENT){
                synchronized (c.boundary) {
                    for (Point p : c.boundary) {
                        img[p.x][p.y][0] += dLaser * m.getRed() * (0.2 + 0.1 * rnd.nextDouble());
                        img[p.x][p.y][1] += dLaser * m.getGreen() * (0.2 + 0.1 * rnd.nextDouble());
                        img[p.x][p.y][2] += dLaser * m.getBlue() * (0.2 + 0.1 * rnd.nextDouble());
                    }
                }
            }
            if(e!=null && e!= Color.TRANSPARENT){

                synchronized (c.points) {
                    for (Point p : c.points) {
                        img[p.x][p.y][0] += dLaser * e.getRed() * (0.2 + 0.1 * rnd.nextDouble());
                        img[p.x][p.y][1] += dLaser * e.getGreen() * (0.2 + 0.1 * rnd.nextDouble());
                        img[p.x][p.y][2] += dLaser * e.getBlue() * (0.2 + 0.1 * rnd.nextDouble());
                    }
                }
            }

            if(c.model==null) return;

            ArrayList<String> keys = new ArrayList<>(c.model.GetTags().keySet());

            for(String s : keys){

                f = c.model.GetTags().get(s);
                if(f==Color.TRANSPARENT) continue;

                synchronized (c.points) {
                    for (Point p : c.points) {
                        dConc = c.model.GetLocalConcentration(s, p);

                        img[p.x][p.y][0] += dConc * (dLaser / 24.) * f.getRed() * (1 + 0.4 * rnd.nextDouble());
                        img[p.x][p.y][1] += dConc * (dLaser / 24.) * f.getGreen() * (1 + 0.4 * rnd.nextDouble());
                        img[p.x][p.y][2] += dConc * (dLaser / 24.) * f.getBlue() * (1 + 0.4 * rnd.nextDouble());
                    }
                }
            }
        });
    }
    //This is a temp insert of the below method for machine learning. This should be made into its own properly integrated method!
    public synchronized void SaveToFile(String subfolder){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Saving to file");
                if(!cp.recording)  return;
                System.out.println("Is recording!");
                String dir = cp.recordPath;

                File fDir = new File(dir);
                if (!fDir.exists()) {
                    if (fDir.mkdirs()) System.out.println("Made cell directory "+fDir.getAbsolutePath());
                    else System.out.println("Failed to make output directory "+fDir.getAbsolutePath());
                }

                for(int cell_num=1; cell_num<=cp.cells.size(); cell_num++){

                    File fCellDir = new File(fDir.getAbsolutePath()+File.pathSeparator+subfolder+File.pathSeparator+Integer.toString(cell_num));
                    if (!fCellDir.exists()) {
                        if (fCellDir.mkdirs()) System.out.println("Made cell directory "+fDir.getAbsolutePath());

                    }
                    //File fCellDir = new File(fDir+File.pathSeparator+"Cell "+ Integer.toString(cell_num));
                    //if (!fCellDir.exists()) {
                    //    if (fCellDir.mkdirs()) System.out.println("Made cell directory");
                    //    else System.out.println("Failed to make output directory");
                    //}

                    String sFiles = Integer.toString(fCellDir.listFiles().length);

                    while (sFiles.length() < 6) {
                        sFiles = "0" + sFiles;
                    }

                    double mu_x = 0;
                    double mu_y = 0;

                    for(Point pt : cp.cells.get(cell_num-1).points){
                        mu_x+=pt.x;
                        mu_y+=pt.y;
                    }

                    mu_x/=cp.cells.get(cell_num-1).points.size();
                    mu_y/=cp.cells.get(cell_num-1).points.size();

                    WritableImage wim = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());

                    double mult_x = cp.w/canvas.getWidth();
                    double mult_y = cp.h/canvas.getHeight();

                    int x0 = (int) Math.max(0,(mu_x-63)*mult_x);
                    int y0 = (int) Math.max(0,(mu_y-63)*mult_y);

                    int w = (int) Math.min(canvas.getWidth()-x0,128*mult_x);
                    int h = (int) Math.min(canvas.getHeight()-y0,128*mult_y);

                    canvas.snapshot(null, wim);
                    
                    //int[] pixels = new int[(x1-x0)*(y1-y0)*4];
                    //wim.getPixelReader().getPixels(x0,x1,y0,y1,PixelFormat.getIntArgbInstance(),pixels,0,(int) (wim.getWidth())*4);
                    //cim.getPixelWriter().setPixels(0,0,x1-x0,y1-y0,PixelFormat.getIntArgbInstance(), pixels, 0, 4*(x1-x0));

                    WritableImage cim = new WritableImage(wim.getPixelReader(),x0,y0,w,h);

                    try {
                        ImageIO.write(SwingFXUtils.fromFXImage(cim, null), "png", new File(fCellDir + "/" + sFiles + ".png"));
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    //This is the save system for full scale videos
    /*public synchronized void SaveToFile(){
        Platform.runLater(new Runnable() {
                              @Override
                              public void run() {
                                  if(!cp.recording)  return;

                                  String dir = cp.recordPath;

                                  File fDir = new File(dir);
                                  if(!fDir.exists()) return;

                                  String sFiles = Integer.toString(fDir.listFiles().length);

                                  while(sFiles.length()<6){
                                      sFiles = "0"+sFiles;
                                  }

                                  WritableImage wim = new WritableImage(  (int) canvas.getWidth(), (int) canvas.getHeight());

                                  canvas.snapshot(null, wim);

                                  try {
                                      ImageIO.write(SwingFXUtils.fromFXImage(wim, null), "png", new File(fDir+"/"+sFiles+".png"));
                                  } catch (Exception e) {

                                  }
                              }
                          });
    }*/
}
