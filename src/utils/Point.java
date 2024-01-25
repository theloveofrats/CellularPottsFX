package utils;

import potts.CellularPotts;

/**
 * Created by luke on 22/03/19.
 */
public class Point {
    public int x;
    public int y;

    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {

        if(this==obj) return true;
        if(obj.getClass()==getClass()) {
            Point pt = (Point) obj;
            if(pt.x==x&&pt.y==y) return true;
            else return false;
        }

        int[] cast = (int[]) obj;
        if(cast!=null && cast.length==2 && cast[0]==x && cast[1]==y) return true;
        return false;
    }

    public boolean isIn(CellularPotts cp){
        return x>=0 && x<cp.w && y>=0 && y<cp.h;
    }

    public int hashCode(){
        int result = 7;
        result = 31*result+x;
        result = 31*result+y;
        return result;
    }
}
