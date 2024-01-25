package ui;

/**
 * Created by luke on 24/08/16.
 */
public interface LightMicroscopy {

    public String GetName();
    public void SetBrightness(int brightness);

    public int[][] GetImage(int[][] inputImage);
    public int[] GetPixelArray(int[][] inputImage);
}
