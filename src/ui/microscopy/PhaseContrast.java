package ui.microscopy;

import ui.microscopy.BrightField;
import utils.MyUtils;

import java.util.stream.IntStream;

/**
 * Created by luke on 24/08/16.
 */
public class PhaseContrast extends BrightField {

    public PhaseContrast(){
        this.name = "Phase Contrast";
    }

    @Override
    public int[][] GetImage(int[][] inputImage){

        int w = inputImage.length;
        int h = inputImage[0].length;

        int[][] transmitted = new int[w][h];

        if(brightness==0) return transmitted;

        int[][] scattered   = new int[w][h];
        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                transmitted[i][j] = inputImage[i][j]==0 ? brightness : brightness-contrast;
                scattered  [i][j] = inputImage[i][j]==0 ? 0          : brightness;
            }
        }

        int[][] blurred = MyUtils.BoxBlur(scattered, 4, 3);

        // Final image is Cos(blurred)-sin(transmitted)

        double xf = 0.5*Math.PI/brightness;

        //IntStream.range(0, w).parallel().forEach(i->{
        for(int i=0; i<w; i++) {
            for (int j = 0; j < h; j++) {
                transmitted[i][j] = (int) Math.max(0, Math.min(255,   (0.975+0.05*Math.random()) * (0.5*brightness*(Math.cos(1-xf*blurred[i][j]) - 2*Math.sin(1-xf*transmitted[i][j])))));
            }
        }

        return transmitted;
    }
}
