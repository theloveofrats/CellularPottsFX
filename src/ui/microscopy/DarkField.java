package ui.microscopy;

import ui.microscopy.BrightField;
import utils.MyUtils;

import java.util.stream.IntStream;

/**
 * Created by luke on 24/08/16.
 */
public class DarkField extends BrightField {


    public DarkField(){
        this.name = "Dark Field";
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
        int[][] blurred = MyUtils.BoxBlur(output, 2, 2);
        //IntStream.range(0, w).parallel().forEach(i->{
        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                output[i][j] = (int) Math.max(0, Math.min(255, (0.1*brightness*Math.random()) + 0.4*output[i][j]+0.8*blurred[i][j]));
            }
        }

        return output;
    }

}
