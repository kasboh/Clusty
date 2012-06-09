package kbo;

import java.util.HashMap;
import java.util.Map;

import dragon.nlp.Word;

public class XtractSentence {
	private Map<Integer, Integer> words;
	public XtractSentence(){
		words = new HashMap<Integer, Integer>();
	}
	public void add(Word word){
		words.put(word.getIndex(), word.getPOSIndex());
	}
	public Map<Integer, Integer> getWords(){
		return words;
	}
}
