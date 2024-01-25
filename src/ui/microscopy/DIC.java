package ui.microscopy;

import ui.microscopy.BrightField;
import utils.MyUtils;

import java.util.stream.IntStream;

/**
 * Created by luke on 24/08/16.
 */
public class DIC extends BrightField {


    public DIC(){
        this.name = "DIC";
    }

    @Override
    public int[][] GetImage(int[][] inputImage){
        int w = inputImage.length;
        int h = inputImage[0].length;

        int[][] output  = new int[w][h];

        if(brightness==0) return output;

        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                output [i][j] = inputImage[i][j]==0 ? 0 : brightness;
            }
        }

        // Get blurred image
        int[][] iX = MyUtils.EdgeFilter1D(output, false);
        int[][] iY = MyUtils.EdgeFilter1D(output, true);

        //IntStream.range(0, w).parallel().forEach(i->{
        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                output[i][j] = (int) Math.max(0, Math.min(255, (0.975+0.05*Math.random()) * (0.5*brightness + 0.5*iX[i][j]+0.1*iY[i][j])));
            }
        }

        return MyUtils.BoxBlur(output, 1, 2);
    }
}
