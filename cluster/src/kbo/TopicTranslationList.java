package kbo;

import com.carrotsearch.hppc.IntFloatOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;

public class TopicTranslationList{

	IntObjectOpenHashMap<IntFloatOpenHashMap> internalList;
	public boolean containsKey(int key){
		return false;
	}
	public IntFloatOpenHashMap get(int key){
		return null;
	}
	public IntFloatOpenHashMap put(int key, IntFloatOpenHashMap value){
		return null;
	}
	public int[] getKeys() {
		return internalList.keys;
	}
	public Object[] getValues() {
		return internalList.values;
	}
	public boolean[] getAllocated() {
		return internalList.allocated;
	}
	
}
