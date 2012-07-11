package kbo;

import java.util.ArrayList;

import com.carrotsearch.hppc.IntObjectOpenHashMap;

public class CollocationsList {
	/*
	 * key in hasmap is a first word, the value is object
	 * in form ArrayList that contains TupleInt
	 * TupleInt contains second word from a phrase and index of a pair
	 * as a second element
	 * As a result hasmap contains first word that can map to multiple second words
	 */ 
	IntObjectOpenHashMap<ArrayList<TupleInt>> list; 
	int index;
	public CollocationsList(){
		index = 0;
		list = new IntObjectOpenHashMap<ArrayList<TupleInt>>();
	}
	
	public int add(int first, int second){
		if(list.containsKey(first)){
			ArrayList<TupleInt> old = list.get(first);
			for(TupleInt tuple : old){
				if(tuple.x == second){
					return tuple.y;
				}
			}
			old.add(new TupleInt(second, index));
			index++;
			return (index-1);
		}
		else {
			ArrayList<TupleInt> element = new ArrayList<TupleInt>();
			element.add(new TupleInt(second, index));
			list.put(first, element);
			index++;
			return (index-1);
		}
		/*int[] keys = list.keys;
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
		*/
	}
	
	private boolean find(int first, int second){
		
		return false;
	}
}
