package kbo;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.carrotsearch.hppc.IntIntOpenHashMap;

//import dragon.nlp.Counter;
//import dragon.nlp.SimpleElementList;
import dragon.nlp.Token;
//import dragon.util.MathUtil;

public class TopicTranslationOld {
//	private String folder;
	@SuppressWarnings("unused")
	private int docThreshold;
	boolean init;
	HashMap<Integer, HashMap<Integer, Short>> phraseDocumentsRelation;
	HashMap<Integer,HashMap<Integer,Short>> documentTermRelation;
	HashMap<Integer, HashMap<Integer,Float>> termPhraseTranslation; //termPhrase translation
	//	private boolean useDocFrequency;
	double probThreshold, emBkgCoefficient;
	IntIntOpenHashMap phraseFrequencyIndex;
	private HashMap<Integer, Short> termFrequencyList; // used to store term and its Frequency, local
	private IntIntOpenHashMap termIndexList; //global list of term frequency
	private int iterationNum;
	public TopicTranslationOld(String folder){
		//this.folder = folder;
	}
	public void initialise(HashMap<Integer, HashMap<Integer, Short>> phraseDocumentList, HashMap<Integer, HashMap<Integer,Short>> documentTermList, 
			IntIntOpenHashMap phraseFrequencyIndex, IntIntOpenHashMap termIndexList){
		this.phraseDocumentsRelation = phraseDocumentList;
		this.documentTermRelation = documentTermList;
		this.phraseFrequencyIndex = phraseFrequencyIndex;
		this.termIndexList = termIndexList;
		init = true;
	}
	public void translate() {
//		SimpleElementList wordKeyList=new SimpleElementList(folder+"/wordkey.list",true); 
		//		HashMap<Integer, Integer> phraseFrequencyIndex = new HashMap<Integer, Integer>(); 
		//SimpleElementList phraseIndexList = new SimpleElementList(folder+"/phraseIndex.list",true); 
		//		phraseDocumentsRelation = new HashMap<Integer, HashMap<String, Integer>>(); 
		//		documentTermRelation = new HashMap<String, HashMap<Integer,Short>>();
		if(!init)
			return; 
		termPhraseTranslation = new HashMap<Integer, HashMap<Integer, Float>>(); 
		// document threshold: number of terms / / 8
//		docThreshold = (int) (wordKeyList.size() / computeAvgTermNum(documentTermRelation) / 8.0);
		//		useDocFrequency = true;
		int minFrequency=1;
		emBkgCoefficient=0.5;
		probThreshold=0.001;//
		iterationNum = 20;
		ArrayList<Token> tokenList;
		Token curToken;
		int j;
		// for each phrase
		int[] phraseFreqKeys = phraseFrequencyIndex.keys;
		int[] phraseFreqValues = phraseFrequencyIndex.values;
		boolean[] states = phraseFrequencyIndex.allocated;
		for (int phraseEntry = 0;phraseEntry<states.length;phraseEntry++){
			if(!states[phraseEntry]){
				continue;
			}
			if (phraseFreqValues[phraseEntry] < minFrequency)
				continue;
			tokenList=genSignatureTranslation(phraseFreqKeys[phraseEntry]); // i is phrase number
			for (j = 0; j <tokenList.size(); j++) {
				curToken=(Token)tokenList.get(j);
				if(termPhraseTranslation.containsKey(curToken.getIndex())){
					HashMap<Integer, Float> old = termPhraseTranslation.get(curToken.getIndex());
					if(old.containsKey(phraseFreqKeys[phraseEntry])){
						System.out.println("aha need correction");
					}
					old.put(phraseFreqKeys[phraseEntry], (float) curToken.getWeight()); //phrase, weight
				}
				else {
					HashMap<Integer, Float> newPhrase = new HashMap<Integer, Float>();
					newPhrase.put(phraseFreqKeys[phraseEntry], (float) curToken.getWeight());
					termPhraseTranslation.put(curToken.getIndex(), newPhrase);
				}
				//				outputTransMatrix.add(i,curToken.getIndex(),curToken.getWeight());
				//				outputTransTMatrix.add(curToken.getIndex(), i, curToken.getWeight());
				//TODO termPhrase exists, create PhraseTerm
			}
			tokenList.clear();
		}

	}
	private ArrayList<Token> genSignatureTranslation(Integer phrase){
		ArrayList<Token> tokenList;
		HashMap<Integer, Short> arrDoc;

		arrDoc = phraseDocumentsRelation.get(phrase); // get all documents where phrase occures 
		//		if (arrDoc.size() > docThreshold)
		tokenList = computeDistributionByArray(arrDoc);

		tokenList=emTopicSignatureModel(tokenList);
		return tokenList;
	}
	@SuppressWarnings({ "unchecked", "unused" })
	private double computeAvgTermNum(HashMap<Integer,HashMap<Integer,Short>> docTerm){
		Random random;
		int i, num;
		HashMap<Integer,Short> index;
		double sum;
		// selecting random elements from hash map
		Object[] values = docTerm.values().toArray();
		random=new Random();
		num=Math.min(50,values.length);
		sum=0;
		for(i=0;i<num;i++){
			index = (HashMap<Integer, Short>) values[random.nextInt(values.length)];
			sum+=index.size();
		}
		return sum/num;
	}
	private ArrayList<Token> computeDistributionByArray(HashMap<Integer, Short> arrDoc){
		ArrayList<Token> list;
		Token curToken;
		//        int[] arrIndex, arrFreq;
		//        int i, j, k, nonZeroNum;
		double rowTotal, mean;
		termFrequencyList = new HashMap<Integer, Short>();
		rowTotal=0;  
		//for each document
		for(Map.Entry<Integer, Short> document : arrDoc.entrySet()){
			// get terms from document
			HashMap<Integer, Short> termsOfDocument = documentTermRelation.get(document.getKey());
			//        	if(useDocFrequency) // don't understand what useDocFrequency is 
			//                arrFreq=null;
			//            else
			//                arrFreq=null;//destDocSignatureMatrix.getNonZeroIntScoresInRow(arrDoc[j]); 
			// fore each of terms in document
			for(Map.Entry<Integer, Short> term : termsOfDocument.entrySet()){
				if(termFrequencyList.containsKey(term.getKey())){
//					termFrequencyList.put(term.getKey(), (short) (termFrequencyList.get(term.getKey()) +1));
					termFrequencyList.put(term.getKey(), (short) (termFrequencyList.get(term.getKey()) +term.getValue()));
				}
				else {
//					termFrequencyList.put(term.getKey(), (short) 1);
					termFrequencyList.put(term.getKey(), term.getValue());
				}
			}
		}
		//        nonZeroNum=0; //?
		for(Map.Entry<Integer, Short> term : termFrequencyList.entrySet()){
			//        	nonZeroNum++;
			rowTotal +=term.getValue();
		}
		mean=0.5;
		if(mean<rowTotal*getMinInitProb())
			mean=rowTotal*getMinInitProb();

		rowTotal=0;
		Map<Integer, Short> termListAboveMean = new HashMap<Integer, Short>();
		for(Map.Entry<Integer, Short> term : termFrequencyList.entrySet()){
			if(term.getValue()<mean)
				continue;
			else{
				termListAboveMean.put(term.getKey(), term.getValue());
//				termFrequencyList.remove(term.getKey()); // modifying list while iterating is not allowed
				rowTotal+=term.getValue();
			}
		}
		list=new ArrayList<Token>();
		for(Map.Entry<Integer, Short> term : termListAboveMean.entrySet()){
			curToken = new Token(term.getKey(), (int)term.getValue());
			curToken.setWeight(term.getValue()/rowTotal);
			list.add(curToken);
		}
		termListAboveMean.clear();
		termFrequencyList.clear();
		return list;
	}

	private double getMinInitProb(){
		/*
        if(useEM)
            return Math.min(0.0001,probThreshold);
        else
            return probThreshold;*/
		return probThreshold;
	}

	private ArrayList<Token> emTopicSignatureModel(ArrayList<Token> list){
		Token curToken;
		double[]  arrCollectionProb, arrProb;
		double weightSum;
		int termNum;
		int i, j;

		termNum =list.size();
		arrProb = new double[termNum];

		//initialize the background model;
		arrCollectionProb=new double[termNum];
		weightSum=0;
		for(i=0;i<termNum;i++){
			curToken=(Token)list.get(i);
			//            if(useDocFrequency)
			arrCollectionProb[i]=termIndexList.get(curToken.getIndex());
			weightSum+=arrCollectionProb[i];
		}

		for(i=0;i<termNum;i++)
			arrCollectionProb[i]=arrCollectionProb[i]/weightSum;

		//start EM
		for (i = 0; i < iterationNum; i++) {
			weightSum = 0;
			for (j = 0; j < termNum; j++) {
				curToken=(Token)list.get(j);
				arrProb[j] = (1 - emBkgCoefficient) * curToken.getWeight() /
						( (1 - emBkgCoefficient) * curToken.getWeight() + emBkgCoefficient * arrCollectionProb[j]) * curToken.getFrequency();
				weightSum += arrProb[j];
			}
			for (j = 0; j < termNum; j++){
				curToken=(Token)list.get(j);
				curToken.setWeight(arrProb[j]/ weightSum);
			}
		}
		
		ArrayList<Token> newList=new ArrayList<Token>(list.size());
        for (j = 0; j < termNum; j++){
            curToken=(Token)list.get(j);
            if(curToken.getWeight()>=probThreshold)
                newList.add(curToken);
        }
        list = newList;
		return list;
	}
	public HashMap<Integer, HashMap<Integer, Float>> getTranslationProbability() {
		return termPhraseTranslation;
	}
}
