package kbo;

import com.carrotsearch.hppc.IntObjectOpenHashMap;

public class CollocationsList {
	IntObjectOpenHashMap<TupleInt> list;
	int index;
	public CollocationsList(){
		index = 0;
		list = new IntObjectOpenHashMap<TupleInt>();
	}
	
	public int add(int first, int second){
		int[] keys = list.keys;
		Object[] values = list.values;
		boolean[] states = list.allocated;
		for (int i = 0; i<states.length; i++){
			if(!states[i]){
				continue;
			}
			TupleInt var = (TupleInt) values[i];
			if(var.x == first && var.y == second){
				return keys[i];
			}
		}
		//no similar tuple int was found
		list.put(index, new TupleInt(first, second));
		index++;
		return (index-1);
	}
	
	private boolean find(int first, int second){
		
		return false;
	}
}
