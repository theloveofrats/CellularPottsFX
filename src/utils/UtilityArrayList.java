package utils;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by luke on 23/08/16.
 */
public class UtilityArrayList<T> extends ArrayList<T> {

    @Override
    public synchronized boolean contains(Object obj){
        int[] objcast = (int[]) obj;

        for(T o : this){
            int[] ocast = (int[]) o;
            if(ocast[0]==objcast[0]&&ocast[1]==objcast[1]) return true;
        }
        return false;
    }

    @Override
    public synchronized int indexOf(Object obj){
        int[] objcast = (int[]) obj;

        int i=0;

        for(i=0; i<this.size(); i++){
            int[] ocast = (int[]) this.get(i);
            if(ocast[0]==objcast[0]&&ocast[1]==objcast[1]) return i;
        }
        return -1;
    }

    @Override
    public synchronized boolean add(T t) {
        return super.add(t);
    }
}
