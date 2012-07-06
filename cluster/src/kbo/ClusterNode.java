package kbo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntDoubleOpenHashMap;
import com.carrotsearch.hppc.IntFloatOpenHashMap;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntShortOpenHashMap;

import dragon.onlinedb.Article;
import dragon.util.FastBinaryReader;
import dragon.util.FastBinaryWriter;

public class ClusterNode {

	private double s;
	private int type;
	public int clusterID;
	private String filename;
	private double totalSumDF2;
	private IntDoubleOpenHashMap vecDF1;
	private IntDoubleOpenHashMap vecDF2;
	private ArrayList<ClusterNode> childNodes;
	private ArrayList<String> clusterArticles;
	private HashMap<Integer, Double> vecSmooth;

	static private long time;
	static public boolean initialized;
	static public String workingDir;
	static final int CLUSTERLEAF = 0;
	static final int CLUSTERNODE = 1;
	static public int clusterCounter;
	static public Map<String, Long> docAge;
	static public double minClusterSimilarity;
	static public Map<String, Integer> articleIndexes;
	static public IntIntOpenHashMap termFreqIndexList;
	static public ExecutorService threadExecutor;
	static public IntObjectOpenHashMap<IntShortOpenHashMap> docPhraseList;
	static public IntObjectOpenHashMap<IntShortOpenHashMap> documentTermList;
	static public IntObjectOpenHashMap<IntFloatOpenHashMap> termPhraseTranslationList;


	public ClusterNode(){
		this.clusterID = clusterCounter;
		clusterCounter++;
		type = CLUSTERNODE;
		this.filename = String.valueOf(clusterID);
		this.filename = workingDir + "\\" + filename;
		childNodes = new ArrayList<ClusterNode>();
		clusterArticles = new ArrayList<String>();
		vecDF2 = new IntDoubleOpenHashMap();
		vecDF1 = new IntDoubleOpenHashMap();
	}

	public static void initialize(){
		if(!initialized){
			docAge = new HashMap<String, Long>();
		}
	}

	public void setTime(long time) {
		ClusterNode.time = time;
	}
	protected void calculateDF2(){
		int frequency;
		double oldTermFrequency, newTermFrequency;
		totalSumDF2 = 0.0;
		for (String articleKey : clusterArticles){ //for each article in cluster
			int articleIndex = articleIndexes.get(articleKey); 
			IntShortOpenHashMap clusterTermsIndixes = (IntShortOpenHashMap) documentTermList.get(articleIndex); // all terms from document
			int[] clusterTermsIndixesKeys = clusterTermsIndixes.keys; //FIXME nullpointer???
			short[] clusterTermsIndixesValues = clusterTermsIndixes.values;
			boolean[] clusterTermsIndixesStates = clusterTermsIndixes.allocated;
			long docTime = docAge.get(articleKey);
			for(int term = 0; term < clusterTermsIndixesStates.length; term++){
				if(!clusterTermsIndixesStates[term])
					continue;
				//for test purpose we include only words that occur in corpus more than 1 time
				if(termFreqIndexList.get(clusterTermsIndixesKeys[term])<2){
					continue;
				}
				frequency = clusterTermsIndixesValues[term];
				// df2 will contain only documents that are in cluster
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
	protected void calculateDF1(){
		IntObjectOpenHashMap<IntDoubleOpenHashMap> phraseDocumentWeightedList = new IntObjectOpenHashMap<IntDoubleOpenHashMap>();
		int frequency;	
		double weightedFrequency = 0.0;
		double weightedTranslationSum = 0.0;
		for (String articleKey : clusterArticles){ // for each document in cluster we weight frequency of topic signature with doc age
			int articleIndex = articleIndexes.get(articleKey); //indexedArticles.search(articleKey); 
			IntShortOpenHashMap phrasesFromArticle = (IntShortOpenHashMap) docPhraseList.get(articleIndex);
			long docTime = docAge.get(articleKey);
			if(phrasesFromArticle != null){ 
				int[] phrasesFromArticleKeys = phrasesFromArticle.keys;
				short[] phrasesFromArticleValues = phrasesFromArticle.values;
				boolean[] phrasesFromArticleStates = phrasesFromArticle.allocated;
				for(int phrase = 0; phrase < phrasesFromArticleStates.length; phrase++){
					if(!phrasesFromArticleStates[phrase])
						continue;
					frequency = phrasesFromArticleValues[phrase];
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
	protected void semanticSmoothing(double lambda){
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
	protected void CreateProfile (Article article){ 
		clusterArticles.add(article.getKey());
		//docAge.put(article.getKey(), article.getDate().getTime());
		//l = System.currentTimeMillis(); //initialize the time of last cluster update
		/*
		new Thread(){
			public void run(){
				updateClusterProfile();
			}
		}.start();
		*/
		threadExecutor.execute(new Runnable(){
			public void run(){
				updateClusterProfile();
			}
		});
//		updateClusterProfile();
	}
	/**
	 *  called by parent node to determine similarity of article with child
	 * @param article
	 * @return double similarity of article with Node
	 */
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
	public void appendChild(ClusterNode node){
		ArrayList<String> childArticles = node.getClusterArticles();
		for(String article : childArticles){
			clusterArticles.add(article);
		}
		childNodes.add(node);
		threadExecutor.execute(new Runnable(){
			public void run(){
				updateClusterProfile();
			}
		});
//		updateClusterProfile();
	}
	// same as above but update not in a separate thread
	public void appendChildSync(ClusterNode node){
		ArrayList<String> childArticles = node.getClusterArticles();
		for(String article : childArticles){
			clusterArticles.add(article);
		}
		childNodes.add(node);
		updateClusterProfile();
	}
	public void appendChild(Article article){
		createChild(article);
		//updateClusterProfile();
	}
	public void appendChild(ClusterNode[] nodes){
		for(int i=0;i<nodes.length;i++){
			ArrayList<String> childArticles = nodes[i].getClusterArticles();
			for(String article : childArticles){
				clusterArticles.add(article);
			}
			childNodes.add(nodes[i]);
		}
		threadExecutor.execute(new Runnable(){
			public void run(){
				updateClusterProfile();
			}
		});
//		updateClusterProfile();
	}
	/**
	 * same as appendChild but profile update
	 * is executed in the same tread
	 * @param nodes
	 */
	public void appendChildSync(ArrayList<ClusterNode> nodes){
		for(ClusterNode child : nodes){
			ArrayList<String> childArticles = child.getClusterArticles();
			clusterArticles.addAll(childArticles);
			childNodes.add(child);
		}
	updateClusterProfile();
	}
	/**
	 * if similarity of Article with child nodes is too low
	 * Article is added as a new child node to this parent node
	 * @param article, parent
	 * @return
	 */
	protected void createChild(Article article){
		ClusterNode child = new ClusterNode();
		child.CreateProfile(article);
		clusterArticles.add(article.getKey());
		childNodes.add(child);
		//		updateClusterProfile();
	}
	/**
	 * called by parent cluster if this cluster is node
	 * @param article
	 * @param similarity
	 */
	public void cluster(Article article, double similarity){
		if((clusterArticles.size() == 1) && (childNodes.isEmpty()) && (similarity <= minClusterSimilarity)){ 
			//middle && condition is not really required
			// If node has only one article and high similarity, it means that documents are very similar, share the same topic
			// and belong to one cluster
			childNodes = null;
			type = CLUSTERLEAF;
			include(article);
		}
		else if(clusterArticles.size() == 1){
			// if Node has only one article then it shouldn't create one child
			// but instead create two children: one with existing document and one with document
			// 1. create child with current article 
			for(String art : clusterArticles){
				ClusterNode child = new ClusterNode();
				childNodes.add(child);
				child.getClusterArticles().add(art);
				child.updateClusterProfile();
			}
			// 2. create second child
			createChild(article);
			// update own cluster centroid
			threadExecutor.execute(new Runnable(){
				public void run(){
					updateClusterProfile();
				}
			});
		}
		else{
			cluster(article);
		}
	}
	public void cluster(Article article) {
		double similarity = 0.0;
		double maxSimilarity = Double.MAX_VALUE;
		ClusterNode bestMatch = null;
//		long t2 = System.currentTimeMillis();
		for (ClusterNode cl : childNodes){
			similarity = cl.compareSimilarity(article);
			if(similarity == Double.MIN_VALUE){
				// This is done for a rare case when article has only title and no body text
				// Error should be fixed in article reader that shouldn't pass such articles
				maxSimilarity = similarity;
				break;
			}
			if (similarity < maxSimilarity){
				maxSimilarity = similarity;
				bestMatch = cl;
			}
		}
//		long t3 = System.currentTimeMillis();
//		System.out.println("Compared similarity in: " + (t3-t2));
		if(maxSimilarity == Double.MIN_VALUE){ // Article had no body text, only heading, ignore such article
			return;
		}
		if(maxSimilarity<=minClusterSimilarity){
			//System.out.print(maxSimilarity);
			if(bestMatch.getType() == CLUSTERNODE){
				bestMatch.cluster(article, maxSimilarity); 
			}
			else{
				bestMatch.include(article);
			}
			clusterArticles.add(article.getKey());
			//mergeClustersKLSSingle(mergeFactor, clusters, freeIndexes,bestMatch);
		}
		else if(maxSimilarity<=(minClusterSimilarity*10)){
			//Merge to new node
			if(bestMatch.getType() == CLUSTERNODE){
				bestMatch.cluster(article, maxSimilarity); 
			}
			else{
				ClusterNode child = new ClusterNode();
				childNodes.add(child);
				child.appendChild(article);
				child.appendChild(bestMatch);
				childNodes.remove(bestMatch);
			}
			clusterArticles.add(article.getKey());
		}
		else {
			//Add new child
			createChild(article);
		}
		if(clusterID != 0){ // root node doesn't need to have cluster vecSmooth
			threadExecutor.execute(new Runnable(){
				public void run(){
					updateClusterProfile();
				}
			});
//			updateClusterProfile();
		}
	}
	/**
	 * includes article to the list of own articles
	 * node becomes cluster leaf
	 * "real" cluster with articles belonging together
	 * @param article
	 */
	public void include(Article article){
		clusterArticles.add(article.getKey());
		threadExecutor.execute(new Runnable(){
			public void run(){
				updateClusterProfile();
			}
		});
//		updateClusterProfile();
		//l = System.currentTimeMillis();
		System.err.println("Added "+ article.getKey() + " to cluster "+ clusterID);
	}
	//void updateTime();
	protected double fadingFunction(long time){
		//		long oneSecond = 1000;
		//		long sigma = 1/oneSecond; //half life of stream is one second
		//		long power = -sigma * time;
		//		return Math.pow(2, power);
		return 1.0;
	}
	public void printArticles(){
		//TODO print articles in cluster
	}
	public void printArticlesToFile(String filename){
		//TODO save articles in cluster to a document
	}
	//protected long getL();
	/**
	 * compare nodes to see if it is possible to merge them
	 * @return
	 */
	boolean merge(double mergeFactor){
		if(childNodes == null){
			//cluster leaf
			return false;
		}
		if(childNodes.isEmpty()){
			//non-cluster leaf
			return false;
		}
		else {
			for (ClusterNode cn : childNodes){
				cn.merge(mergeFactor);
			}
		}
		// mergeClustersKLS should be in else and here return false
		return mergeClustersKLS(mergeFactor);
	}
	//public ArrayList<String> getClusterArticles();
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public ArrayList<String> getClusterArticles() {
		return clusterArticles;
	}
	/**
	 * writes vecSmooth to disk to free RAM
	 */
	protected void writeToFile(){
		try {
			FastBinaryWriter fbw = new FastBinaryWriter(filename);
			// file will be overwritten
			fbw.writeInt(vecSmooth.size());
			for(Map.Entry<Integer, Double> value : vecSmooth.entrySet()){
				fbw.writeInt(value.getKey());
				fbw.writeDouble(value.getValue());
			}
			fbw.flush();
			fbw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	/**
	 * reads previously written vecSmooth from disk to RAM
	 */
	protected void readFromFile(){
		try{
			FastBinaryReader fbr = new FastBinaryReader(filename);
			int size = fbr.readInt();
			for(int i=0;i<size;i++){
				int key = fbr.readInt();
				double value = fbr.readDouble();
				vecSmooth.put(key, value);
			}
			fbr.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	private HashMap<Integer, Double> getVecSmooth() {
		return vecSmooth;
	}
	/**
	 * compare all clusters and merge similar ones
	 * @param mergeFactor
	 * @param clusters
	 * @param freeIndexes
	 * @return
	 */
	private boolean mergeClustersKLS(double mergeFactor){ //ArrayList<ClusterNode> clusters
		boolean deleted = false;
		double minDistance = Double.MAX_VALUE;
		double distance;
		ClusterNode a = null;
		ClusterNode b = null; // indexes of clusters to be merged
		// find two clusters with smallest distance
		//to improve the speed we shouldn't compare two clusters twice
		// for this purpose hashmap is created that contains number of two clusters that were compared
		IntObjectOpenHashMap<IntArrayList> comparedTable = new IntObjectOpenHashMap<IntArrayList>();
		for (ClusterNode cl1 : childNodes){
			for(ClusterNode cl2 : childNodes){ 
				//
				if(comparedTable.containsKey(cl2.clusterID) && comparedTable.get(cl2.clusterID).contains(cl1.clusterID)){
					continue;
				}
				else {
					if(comparedTable.containsKey(cl1.clusterID)){
						IntArrayList existing = comparedTable.get(cl1.clusterID);
						existing.add(cl2.clusterID);
					}
					else {
						IntArrayList tableRow = new IntArrayList();
						tableRow.add(cl2.clusterID);
						comparedTable.put(cl1.clusterID, tableRow);
					}

					if(cl1.clusterID == cl2.clusterID){
						//distance for same cluster is 0
						continue;
					}
					distance = compareKLsymetric(cl1.getVecSmooth(), cl2.getVecSmooth());
					if(distance<0){
						//negative distance is not counted
						continue;
					}
					if(distance<minDistance){
						minDistance = distance;
						//a = cl1.clusterID;
						//b = cl2.clusterID;
						a = cl1;
						b = cl2;
					}
				}
			}
		}
		if (a == null || b == null){
			return deleted;
		}
		if((minDistance<mergeFactor)&&(minDistance>0)){
			int res = merge(a, b);
			if(res == 1){
				childNodes.remove(b);
			}
			else if (res == 0){
				childNodes.remove(a);
			}
			deleted = true;
		}
		return deleted;
	}

	private double compareKLsymetric(HashMap<Integer, Double> c1,
			HashMap<Integer, Double> c2){
		double distance = 0.0;
		int intersection = 0; // number of words in both clusters;
		int vocabdiff = 0;
		for (Map.Entry<Integer, Double> p1 : c1.entrySet()){
			if(c2.containsKey(p1.getKey())){
				intersection++;
			}
			else {
				vocabdiff++;
			}
		}

		double minC1 = Collections.min(c1.values());
		double minC2 = Collections.min(c2.values());
		double epsilon = Math.min(minC1,minC2)*0.001;
		double gamma = 1-vocabdiff*epsilon;
		double c1Sum = 0.0, c2Sum = 0.0;
		for (Double val : c1.values()){
			c1Sum+=val;
		}
		for (Double val : c2.values()){
			c2Sum+=val;
		}
		if((c1Sum<9e-6)||(c2Sum<9e-6)){ // check if sum is near 1
			return Double.MAX_VALUE;
		}
		for (Map.Entry<Integer, Double> p1 : c1.entrySet()){
			double ptt;
			if(c2.containsKey(p1.getKey())){
				ptt = gamma * c2.get(p1.getKey());
			}
			else {
				ptt = epsilon;
			}
			distance += (p1.getValue()-ptt) *(Math.log(p1.getValue()/ptt));
		}
		if(intersection==0 && distance < 5){ //<5 is small hack for debug
			return Double.MAX_VALUE; // if two cluster have nothing in common similarity should not be 0
		}
		return distance;
	}
	private int merge(ClusterNode cl1, ClusterNode cl2){
		if(cl1 == null || cl2 == null){
			return -1;
		}
		// we also shouldn't merge clusterleaf and clusternode
		// because cluterleaf has only documents and no childs
		if (cl1.getType() != cl2.getType()){
			return -1;
		}
		// two objects are clusters (leafs)
		if(cl1.getType() == ClusterNode.CLUSTERLEAF){
			ArrayList<String> articlesToMove = cl2.getClusterArticles();
			ArrayList<String> articlesToUpdate = cl1.getClusterArticles();
			articlesToUpdate.addAll(articlesToMove);
			cl1.updateClusterProfile();
		}
		// two nodes are nodes
		else {
			ArrayList<ClusterNode> childToMove = cl2.getChildNodes();
			if(childToMove.isEmpty()){
				// we have child node with only one article
				cl1.appendChildSync(cl2);
				//ArrayList<String> articlesToMove = cl2.getClusterArticles();
				//ArrayList<String> articlesToUpdate = cl1.getClusterArticles();
				//articlesToUpdate.addAll(articlesToMove);
			}
			else if (cl1.getChildNodes().isEmpty()){
				cl2.appendChildSync(cl1);
				return 0;
			}
			else {
				cl1.appendChildSync(childToMove);
			}
		}
		return 1;
	}

	public ArrayList<ClusterNode> getChildNodes() {
		return childNodes;
	}
}
