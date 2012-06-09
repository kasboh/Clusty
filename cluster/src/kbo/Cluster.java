package kbo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.carrotsearch.hppc.IntDoubleOpenHashMap;
import com.carrotsearch.hppc.IntFloatOpenHashMap;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntShortOpenHashMap;

import dragon.onlinedb.Article;

public class Cluster {
	//String indexFolder; // folder where indexes for each cluster will be stored
	String indexAllFolder; // folder where data from train set is stored
	//	ArrayList<Double> vecSmooth;
	public HashMap<Integer, Double> vecSmooth;
	double totalSumDF2;
	long l; // time the cluster last updated
	long time; //current time for cluster
	double s;
	String phraseIndexFolderT;
	String tokenIndexFolderT;
	public ArrayList<String> clusterArticles;
	public Map<String, Long> docAge;
	Map<String, Integer> articleIndexes;
	public int clusterID;
	protected IntIntOpenHashMap termFreqIndexList;
	protected IntObjectOpenHashMap<IntShortOpenHashMap> documentTermList;
	protected IntObjectOpenHashMap<IntShortOpenHashMap> docPhraseList;
	private IntDoubleOpenHashMap vecDF1;
	private IntDoubleOpenHashMap vecDF2;
	private IntObjectOpenHashMap<IntFloatOpenHashMap> termPhraseTranslationList;
	private IntObjectOpenHashMap<IntDoubleOpenHashMap> phraseDocumentWeightedList;

	public Cluster (String indexFolder, int k, Map<String, Integer> articleIndex, 
			IntIntOpenHashMap termFreqIndexList, IntObjectOpenHashMap<IntShortOpenHashMap> documentTermList, 
			IntObjectOpenHashMap<IntShortOpenHashMap> docPhraseList, IntObjectOpenHashMap<IntFloatOpenHashMap> translationProbabilityList){
		//this.indexFolder = indexFolder + "\\" + k;
		//File dir = new File(this.indexFolder);
		//if (!dir.exists()) dir.mkdir();
		this.indexAllFolder = indexFolder;
		//phraseIndexFolderT = this.indexAllFolder + "\\phrase";
		//tokenIndexFolderT = this.indexAllFolder + "\\token";
		clusterArticles = new ArrayList<String>();
		docAge = new HashMap<String, Long>();
		this.clusterID = k;

		this.articleIndexes = articleIndex;
		this.termFreqIndexList = termFreqIndexList;
		this.documentTermList = documentTermList;
		this.docPhraseList = docPhraseList;
		this.termPhraseTranslationList = translationProbabilityList;
		phraseDocumentWeightedList = new IntObjectOpenHashMap<IntDoubleOpenHashMap>();
		vecDF2 = new IntDoubleOpenHashMap();
		vecDF1 = new IntDoubleOpenHashMap();
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public void CreateProfile (Article article){
		// get words and terms from article
		// TODO article was indexed
		clusterArticles.add(article.getKey());
		docAge.put(article.getKey(), article.getDate().getTime());
		l = System.currentTimeMillis(); //initialize the time of last cluster update

		//		calculateDF2();
		//		calculateDF1();
		// cluster model with semantic smoothing
		//		semanticSmoothing(0.5);
		updateClusterProfile();
	}
	private void calculateDF2(){
		// df1 vector is the weighted sum of topic signature translation probability in cluster for word wi
		int frequency;
		double oldTermFrequency, newTermFrequency;
		/* for each phrase t_k we have a set of documents D_k where this phrase occurs
		 * since not all terms in document center arround t_k, D_k document is generated
		 * by mixture language model D_k
		 * p(w|C) = number_of_word_occurence/number of words in C
		 */

		/* DF2 is weighted frequency count of all words that are in documents (documents belong to cluster)
		 * as this is cluster initialization
		 * we have only one document
		 * get all terms from this document
		 * DF2 matrix will contain weighted frequencies of terms in cluster
		 * DF2 vector which is a part of cluster profile will contain summed values for words
		 */
		totalSumDF2 = 0.0;
		for (String articleKey : clusterArticles){ //for each article in cluster
			int articleIndex = articleIndexes.get(articleKey); 
			IntShortOpenHashMap clusterTermsIndixes = (IntShortOpenHashMap) documentTermList.get(articleIndex); // all terms from document
			int[] clusterTermsIndixesKeys = clusterTermsIndixes.keys;
			short[] clusterTermsIndixesValues = clusterTermsIndixes.values;
			boolean[] clusterTermsIndixesStates = clusterTermsIndixes.allocated;
			for(int term = 0; term < clusterTermsIndixesStates.length; term++){
				if(!clusterTermsIndixesStates[term])
					continue;
				//for test purpose we include only words that occur in corpus more than 1 time
				if(termFreqIndexList.get(clusterTermsIndixesKeys[term])<2){
					continue;
				}
				frequency = clusterTermsIndixesValues[term];
				// df2 will contain only documents that are in cluster
				long docTime = docAge.get(articleKey);
				if (vecDF2.containsKey(clusterTermsIndixesKeys[term])){
					oldTermFrequency = vecDF2.get(clusterTermsIndixesKeys[term]);
					newTermFrequency = frequency * fadingFunction(time - docTime); // System.currentTimeMillis() = time
					vecDF2.put(clusterTermsIndixesKeys[term], oldTermFrequency + newTermFrequency);
					totalSumDF2+=newTermFrequency;
				}
				else {
					newTermFrequency = frequency * fadingFunction(time - docTime); 
					vecDF2.put(clusterTermsIndixesKeys[term], newTermFrequency);
					totalSumDF2+=newTermFrequency;
				}
			}
		}
	}
	private void calculateDF1(){
		int frequency;	
		double weightedFrequency = 0.0;
		double weightedTranslationSum = 0.0;
		for (String articleKey : clusterArticles){ // for each document in cluster we weight frequency of topic signature with doc age
			int articleIndex = articleIndexes.get(articleKey); //indexedArticles.search(articleKey); 
			IntShortOpenHashMap phrasesFromArticle = (IntShortOpenHashMap) docPhraseList.get(articleIndex);
			if(phrasesFromArticle != null){ 
				int[] phrasesFromArticleKeys = phrasesFromArticle.keys;
				short[] phrasesFromArticleValues = phrasesFromArticle.values;
				boolean[] phrasesFromArticleStates = phrasesFromArticle.allocated;
				for(int phrase = 0; phrase < phrasesFromArticleStates.length; phrase++){
					if(!phrasesFromArticleStates[phrase])
						continue;
					frequency = phrasesFromArticleValues[phrase];
					long docTime = docAge.get(articleKey);
					weightedFrequency = frequency * fadingFunction(time-docTime);
					if (phraseDocumentWeightedList.containsKey(phrasesFromArticleKeys[phrase])){
						IntDoubleOpenHashMap documentsWithPhrase = (IntDoubleOpenHashMap) phraseDocumentWeightedList.get(phrasesFromArticleKeys[phrase]);
						documentsWithPhrase.put(articleIndex, weightedFrequency);
						weightedTranslationSum += weightedFrequency;//test
					}
					else {
						IntDoubleOpenHashMap documentsWithPhrase = new IntDoubleOpenHashMap();
						documentsWithPhrase.put(articleIndex, weightedFrequency);
						weightedTranslationSum += weightedFrequency;//test
						phraseDocumentWeightedList.put(phrasesFromArticleKeys[phrase], documentsWithPhrase);
					}
				}
			}
		}
		// test
		/*
		for(Map.Entry<Integer,HashMap<Integer,Double>> ph : phraseDocumentWeightedList.entrySet()){
			HashMap<Integer,Double> tm = ph.getValue();
			for(Map.Entry<Integer,Double> dk : tm.entrySet()){
				dk.setValue(dk.getValue()/weightedTranslationSum);
			}
		}
		 */
		// test
		// it look like we have to run generation of translation probabilities here again
		//updateTranslations();
		// first the frequency count of phrases that come in documents belonging to cluster are weighted
		weightedTranslationSum = 0.0;
		weightedFrequency = 0.0;

		int[] termPhraseTranslKeys = termPhraseTranslationList.keys;
		//		IntFloatOpenHashMap[] termPhraseTranslValues = termPhraseTranslationList.values;
		Object[] termPhraseTranslValues = termPhraseTranslationList.values;
		boolean[] states = termPhraseTranslationList.allocated;			

		for (int term = 0; term<states.length; term++){ // for each term that translates to phrases
			if(!states[term]){
				continue;
			}
			IntFloatOpenHashMap phrasesTermTranslatesTo = (IntFloatOpenHashMap) termPhraseTranslValues[term];// get the topic signature term translates to
			if (phrasesTermTranslatesTo != null){
				int[] phraseTermTranslatesToKeys = phrasesTermTranslatesTo.keys;
				float[] phraseTermTranslatesToValues = phrasesTermTranslatesTo.values;
				boolean[] phraseTermStates = phrasesTermTranslatesTo.allocated;
				for(int phrase = 0; phrase < phraseTermStates.length; phrase++){ // for each phrase(topic signature) that term translates to
					if(!phraseTermStates[phrase]){
						continue;
					}
					IntDoubleOpenHashMap documentsWithPhrase = (IntDoubleOpenHashMap) phraseDocumentWeightedList.get(phraseTermTranslatesToKeys[phrase]); // get documents from this cluster with this phrase
					if (documentsWithPhrase != null){
						double[] documentsWithPhraseValues = documentsWithPhrase.values;
						boolean[] documentsWithPhraseStates = documentsWithPhrase.allocated;
						for (int document = 0; document < documentsWithPhraseStates.length; document++){
							if(!documentsWithPhraseStates[document])
								continue;
							weightedFrequency += documentsWithPhraseValues[document]; // the weighted sum of frequency count for topic signature
						}
					}
					// get translation probability of term to topic signature and multiply it with weighted frequency
					weightedTranslationSum = weightedTranslationSum + (weightedFrequency * phraseTermTranslatesToValues[phrase]);
					weightedFrequency = 0.0;
				}
				vecDF1.put(termPhraseTranslKeys[term], weightedTranslationSum);
				weightedTranslationSum = 0.0;
			}
		}
		// calculate s ->  denotes  the  summation  of w_c(t k , c) for all the topic signature t k in the cluster c.
		// the frequency of each phrase is weighted with time of the document and all frequencies are summed up
		s = 0;
		Object[] phraseDocumentWeightedListValues = phraseDocumentWeightedList.values;
		boolean[] phDocWeightStates = phraseDocumentWeightedList.allocated;
		for (int phrase = 0; phrase < phDocWeightStates.length; phrase++){
			if(!phDocWeightStates[phrase])
				continue;
			IntDoubleOpenHashMap docs = (IntDoubleOpenHashMap) phraseDocumentWeightedListValues[phrase];
			double[] docsValues = docs.values;
			boolean[] docsStates = docs.allocated;
			for(int weight = 0; weight < docsStates.length; weight++){
				if(!docsStates[weight])
					continue;
				s = s + docsValues[weight];
			}
		}
		phraseDocumentWeightedList.clear();// to free memory
	}
	private void semanticSmoothing(double lambda){
		vecSmooth = new HashMap <Integer, Double>();
		//		vecSmooth = new ArrayList<Double>(termFreqIndexList.size());
		double df2Value, df1Value, value = 0.0;
		int[] termKeys = termFreqIndexList.keys;
		boolean[] termStates = termFreqIndexList.allocated;
		//		for (Map.Entry<Integer,Integer> term : termFreqIndexList.entrySet()){
		for (int i =0; i<termStates.length;i++){
			if(!termStates[i])
				continue;
			int termId = termKeys[i];//term.getKey();

			if(vecDF2.containsKey(termId)) {
				df2Value = vecDF2.get(termId);
			}
			else {
				df2Value = 0.0;
			}

			if(vecDF1.containsKey(termId)){
				df1Value = vecDF1.get(termId);
			}
			else {
				df1Value = 0.0;
			}

			if((df2Value == 0.0) && (df1Value == 0.0)){
				continue;
			}
			if (s==0.0){
				// to avoid division by 0
				s = 1.0; // df1Value will be 0 anyway TODO check why article has no phrases
			}
			value = (1-lambda)*(df2Value/totalSumDF2)+lambda*(df1Value/s);
			vecSmooth.put(termId,value);
			//			vecSmooth.put(termId,value);
		}

		vecDF1.clear(); 
		vecDF2.clear();
		//test
		vecDF2 = new IntDoubleOpenHashMap();
		vecDF1 = new IntDoubleOpenHashMap();
		// end test
		//Normalize vecSmooth
		double vecSum = 0.0;
		for (Double val : vecSmooth.values()){
			vecSum+=val;
		}
		for (Map.Entry<Integer, Double> entry : vecSmooth.entrySet()){

			vecSmooth.put(entry.getKey(), vecSmooth.get(entry.getKey())/vecSum );

		}
	}
	public void updateClusterProfile(){
		calculateDF2(); 
		calculateDF1();
		semanticSmoothing(0.6);
	}

	public double compareSimilarity (Article article){
		double logLikelihood = 0.0;
		int intersection  = 0;
		// vecSmooth should be replaced with allTerms as described in paper, but we will have growing vocabulary
		// so vecSmooth is used
		int articleIndex = articleIndexes.get(article.getKey());
		IntShortOpenHashMap articleTerms = (IntShortOpenHashMap) documentTermList.get(articleIndex); 
		// debug
		if (articleTerms == null){
			logLikelihood = Double.MIN_VALUE;
			return logLikelihood;
		}
		if(vecSmooth.isEmpty()){
			System.out.println("vec smooth cannot be empty");
		}
		// end debug
		int[] articleTermsKeys = articleTerms.keys;
		short[] articleTermsValues = articleTerms.values;
		boolean[] articleTermsStates = articleTerms.allocated;
		for(int term = 0; term < articleTermsStates.length; term++){
			if(!articleTermsStates[term])
				continue;
			if(vecSmooth.containsKey(articleTermsKeys[term])){
				if(vecSmooth.get(articleTermsKeys[term]) == 0.0){
					// log of 0 is not defined so skip it
					continue;
				}
				int value = articleTermsValues[term];
				intersection++;
				//				logLikelihood += (value*Math.log(vecSmooth.get(term.getKey())));
				logLikelihood = logLikelihood + (value*(Math.log(vecSmooth.get(articleTermsKeys[term]))));
			}
		}
		if(logLikelihood == 0.0 && (intersection == 0)){
			return Double.MAX_VALUE;
		}
		return -logLikelihood;
	}
	public void include(Article article){
		clusterArticles.add(article.getKey());
		docAge.put(article.getKey(), article.getDate().getTime());
		// no need to update cluster profile (vecDF1,vecDF2,s) because they are updated each time new document comes
		updateClusterProfile();
		l = System.currentTimeMillis();
		System.out.println("Added "+ article.getKey() + " to cluster "+ clusterID);
	}
	public void updateTime(){
		l = System.currentTimeMillis();
	}
	private double fadingFunction(long time){
		long oneSecond = 1000;
		long sigma = 1/oneSecond; //half life of stream is one second
		long power = -sigma * time;
		return Math.pow(2, power);
//				return 1.0;
	}
	public void printArticles(){
		System.out.print("Cluster "+clusterID+": ");
		for (String ar : clusterArticles){
			System.out.print(ar+" ");
		}
		System.out.println(" + + + + + ");
	}
	public void printArticlesToFile(String filename){
		try {
			File file = new File(filename);
			//if file doesn't exists, then create it
			if(!file.exists()){
				file.createNewFile();
			} 
			FileWriter fileWriter = new FileWriter(file.getName(), true);
			BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
			bufferWriter.write(clusterID);
			bufferWriter.write("\n");
			for (String ar : clusterArticles){
				bufferWriter.write(ar+"\t");
			}
			bufferWriter.write("\n");
			bufferWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public long getL() {
		return l;
	}
	public ArrayList<String> getClusterArticles() {
		return clusterArticles;
	}
}
