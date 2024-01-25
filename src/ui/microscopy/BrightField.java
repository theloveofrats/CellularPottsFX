package ui.microscopy;

import ui.LightMicroscopy;
import utils.MyUtils;

import java.util.stream.IntStream;

/**
 * Created by luke on 24/08/16.
 */
public class BrightField implements LightMicroscopy {


    public String name;
    public int contrast = 50;
    public int brightness = 180;


    public BrightField(){
        this.name = "Bright Field";
    }


    @Override
    public void SetBrightness(int brightness){

        this.brightness = brightness;
    }

    @Override
    public String GetName(){

        return name;
    }

    @Override
    public int[][] GetImage(int[][] inputImage){

        final int w = inputImage.length;
        final int h = inputImage[0].length;

        int[][] output = new int[w][h];

        if(brightness==0) return output;

        //IntStream.range(0, w).parallel().forEach(i->{
        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                output[i][j] = inputImage[i][j]==0 ? 0 : brightness;
                output[i][j] = Math.max(0,Math.min(255, output[i][j]));
            }
        }

        int[][] edges = MyUtils.BoxBlur(MyUtils.DetectEdges(output),1,1);


        //IntStream.range(0, w).parallel().forEach(i->{
        for(int i=0; i<w; i++){
            for(int j=0; j<h; j++){
                output[i][j] = (int) ((0.975+0.05*Math.random()) * (brightness - 0.1*output[i][j] - 0.0001*edges[i][j]*edges[i][j]));
                output[i][j] = Math.max(0,Math.min(255, output[i][j]));
            }
        }

        return output;
    }

    @Override
    public int[] GetPixelArray(int[][] inputImage){
        int[][] outImage = GetImage(inputImage);

        int w = outImage.length;
        int h = outImage[0].length;

        int[] out = new int[w*h];

        short a = 255;
        short val = 0;

        for(int i=0; i<w; i++) {
            for (int j = 0; j < h; j++) {

                val = (short) outImage[i][j];

                out[i+w*j] = (a << 24) | (val << 16) | (val << 8) | val;
            }
        }

        return out;
    }
}
