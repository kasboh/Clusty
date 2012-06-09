package kbo;

import java.util.HashMap;

/** IndexList is a list for storing 
 * string value of term and its index value
 * @author KBO
 *
 */
public class IndexList {
//	private String folder;
//	private String name;
	private int index;
	private HashMap<String, Integer> indexList;
	public IndexList (String folder, String name){
//		this.folder = folder;
//		this.name = name;
		index = -1;
		indexList = new HashMap<String, Integer>();
	}
	public int add(String content){
		if(indexList.containsKey(content)){
			return indexList.get(content);
		}
		else {
			index = index +1;
			indexList.put(content, index);
			return index;
		}
	}
}
