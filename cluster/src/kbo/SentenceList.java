package kbo;

import java.util.ArrayList;
//import java.util.HashMap;

import com.carrotsearch.hppc.IntObjectOpenHashMap;

import dragon.nlp.Sentence;
import dragon.nlp.Word;

/**
 * <p>Sentence base class provides functions of adding and saving sententce to matrix</p>
 * <p> </p>
 * <p>Copyright: Copyright (c) 2012</p>
 */
public class SentenceList {
	private int sentenceNum;
	private IntObjectOpenHashMap<ArrayList<TupleInt>> sentenceIndex;
//	private HashMap<Integer, ArrayList<TupleInt>> sentenceIndex;
	//SentenceListWriter sentWriter;
	public SentenceList(){
		sentenceNum=0;
		sentenceIndex = new IntObjectOpenHashMap<ArrayList<TupleInt>>();
		//sentWriter = new SentenceListWriter(file);
	}
	public int addSentence(Sentence sent){
		Word cur;
		int sentIndex;
		sentIndex=sentenceNum;
        sentenceNum++;
        cur=sent.getFirstWord();
        // do I really need XtractSentence class? replace with ArrayList<Tuple>
        /*XtractSentence curSent = new XtractSentence();
        while(cur!=null){
        	curSent.add(cur);
            cur=cur.next;
        }*/
        ArrayList<TupleInt> words = new ArrayList<TupleInt>();
        while(cur!=null){
        	TupleInt word = new TupleInt(cur.getIndex(),cur.getPOSIndex());
        	words.add(word);
        	cur=cur.next;
        }
        sentenceIndex.put(sentIndex, words);
        /*if(cacheMatrix.getNonZeroNum()>=threshold)
        {
            cacheMatrix.finalizeData(false);
            factory.add(cacheMatrix);
            cacheMatrix.close();
        */
        return sentIndex;
	}
	public ArrayList<TupleInt> getSentence (int sentIndex){
		return sentenceIndex.get(sentIndex);
	}
	public int getSentenceSize (int sentIndex){
		return sentenceIndex.get(sentIndex).size();
	}
	 public void close(){
	        //cacheMatrix.finalizeData(false);
	        //factory.add(cacheMatrix);
	        //cacheMatrix.close();
	        //factory.genIndexFile(indexFile);
//		 sentWriter.write(sentenceIndex);
		 sentenceIndex.clear();
	    }
}
