package kbo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Map.Entry;
//import com.carrotsearch.hppc.IntCharOpenHashMap;
//import com.carrotsearch.hppc.IntFloatOpenHashMap;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntShortOpenHashMap;

import dragon.nlp.Document;
//import dragon.nlp.DocumentParser;
import dragon.nlp.Paragraph;
import dragon.nlp.Sentence;
//import dragon.nlp.SimpleElementList;
//import dragon.nlp.SimplePairList;
import dragon.nlp.Token;
import dragon.nlp.Word;
import dragon.nlp.extract.EngDocumentParser;
import dragon.nlp.tool.Lemmatiser;
import dragon.nlp.tool.Tagger;
//import dragon.nlp.tool.xtract.EngWordPairExpand;
//import dragon.nlp.tool.xtract.WordPairExpand;
import dragon.nlp.tool.xtract.WordPairFilter;
import dragon.nlp.tool.xtract.WordPairGenerator;
import dragon.nlp.tool.xtract.WordPairStat;
import dragon.onlinedb.Article;
import dragon.onlinedb.CollectionReader;
import dragon.util.FileUtil;
import dragon.util.SortedArray;
/**
 * Loosely based on Dragontoolkit implementation
 * @author KBO
 *
 */
public class Xtractor {
	EngDocumentParser parser;
	Tagger tagger;
	protected Lemmatiser lemmatiser;
	WordPairGenerator pairGenerator;
	protected SentenceList sentenceBase;
	//	protected SimplePairList pairKeyList;
	protected PairStats wordpairStatList;
	//	protected SimpleElementList docKeyList;
	//	protected SimpleElementList wordKeyList;
//	protected ArrayList<String> wordIdList;
	protected ArrayList<String> stopWords;
	protected HashMap<Integer, String> docIdList; // same as above, hashmap? also Integer type will be better later for memory consumption
	protected int maxSpan;
	protected List<PhraseSentenceAdjacencyList> arrayRightPairList, arrayLeftPairList;
//	protected HashMap<Integer, HashMap<Integer, Short>> phraseDocumentsRelation; // ArrayList<TupleIntShort>
	protected IntObjectOpenHashMap<IntShortOpenHashMap> phraseDocumentsRelation;
//	protected HashMap<Integer, HashMap<Integer, Short>> documentPhraseRelation; //
	protected IntObjectOpenHashMap<IntShortOpenHashMap> documentPhraseRelation;
//	protected HashMap<Integer, ArrayList<Token>> phraseSentenceRelation;
	protected IntObjectOpenHashMap<ArrayList<Token>> phraseSentenceRelation;
	//protected HashMap<Integer, Integer> sentenceDocumentsRelation;
	protected IntIntOpenHashMap sentenceDocumentsRelation;
	protected IntObjectOpenHashMap<IntShortOpenHashMap> documentTermRelation;//
//	protected HashMap<Integer, HashMap<Integer, Short>> termDocumentRelation;//
	protected IntIntOpenHashMap phraseFrequencyIndex;
	protected IntIntOpenHashMap termFreqIndexList; 
	//protected IntCharOpenHashMap termWordIndexList; 
	protected HashMap<Integer, String> termWordIndexList; // contains string word and its index;
	protected HashMap<String, Integer> wordTermIndexList;
	protected String folder;
	protected WordPairExpandEng expander;
	protected WordPairFilter pairFilter;
	protected ArrayList<WordPairStat> singleArticlePhrases;
	protected int pairKeyListCounter;
	protected int documentIndexCounter;
	protected int wordIndexCounter;
	protected boolean single; // used to tell index to store phrase for single article
	public Xtractor(String folder, int maxSpan, Tagger tagger, Lemmatiser lemmatiser, ArrayList<String> stopWords, WordPairGenerator pairGenerator){
		this.tagger =tagger;
		this.maxSpan = maxSpan;
		this.lemmatiser =lemmatiser;
		this.pairGenerator = pairGenerator;
		this.folder = folder;
		this.stopWords = stopWords;
		sentenceBase = new SentenceList(folder+"/sentenceList.list"); // not dragontoolkit
		parser=new EngDocumentParser();
		parser.setStopWords(stopWords);
		(new File(folder)).mkdirs();
		//		docKeyList=new SimpleElementList(folder+"/dockey.list",true);
		//		wordKeyList=new SimpleElementList(folder+"/wordkey.list",true);
//		wordIdList = new ArrayList<String>();
		docIdList = new HashMap<Integer, String>(20000);
		termWordIndexList = new HashMap<Integer, String>(60000);
		wordTermIndexList = new HashMap<String, Integer>(60000);
		//		pairKeyList=new SimplePairList(folder+"/pairkey.list",true);
		pairKeyListCounter = 0;
		documentIndexCounter = 0;
		wordIndexCounter = 0;
		wordpairStatList=new PairStats(folder+"/pairstat.list",maxSpan,true);
		arrayRightPairList = new ArrayList <PhraseSentenceAdjacencyList>();
		arrayLeftPairList = new ArrayList <PhraseSentenceAdjacencyList>();
		sentenceDocumentsRelation = new IntIntOpenHashMap();//new HashMap<Integer, Integer>();
		phraseSentenceRelation = new IntObjectOpenHashMap<ArrayList<Token>>();
		phraseDocumentsRelation = new IntObjectOpenHashMap<IntShortOpenHashMap>();
		documentPhraseRelation = new IntObjectOpenHashMap<IntShortOpenHashMap>(20000);
		documentTermRelation = new IntObjectOpenHashMap<IntShortOpenHashMap>(20000);
//		termDocumentRelation = new HashMap<Integer, HashMap<Integer, Short>>();
		phraseFrequencyIndex = new IntIntOpenHashMap();
		termFreqIndexList = new IntIntOpenHashMap(50000);
		//		phraseFrequencyIndex = new HashMap<Integer, Integer>();
		//		termFreqIndexList = new HashMap<Integer, Integer>();
		for(int i=1;i<=maxSpan;i++){
			// create adjacency list for each span, it will store histogram for word pair and sentences
			// <pair index> <sentence index> <frequency>
			arrayRightPairList.add(new PhraseSentenceAdjacencyList(folder+"/rightPairSentList" + i +".list"));
			arrayLeftPairList.add(new PhraseSentenceAdjacencyList(folder + "/leftPairSentList"+ i + ".list"));
		}
	}

	public void index(CollectionReader cr, int trainset){
		Article curArticle;
		Document curDoc;
		Paragraph curParagraph;
		Sentence curSent;
		//		String documentKey;
		int documentIndex = -1;
		int k = 0;
		single = false;
		try {
			//			curArticle = cr.getNextArticle();
			curArticle = cr.getNextRandom();
			while (curArticle!=null){
				if (k>=trainset){break;}
				k++;
				System.out.println(new java.util.Date().toString()+" "+curArticle.getKey());
				//				documentKey = curArticle.getKey();
				//				documentIndex = docKeyList.add(curArticle.getKey());
				documentIndex = -1;
				for(Map.Entry<Integer, String> dId : docIdList.entrySet()){
					if(dId.getValue().equals(curArticle.getKey())){
						documentIndex = dId.getKey();
					}
				}
				if (documentIndex == -1){
					documentIndex = documentIndexCounter;
					docIdList.put(documentIndex, curArticle.getKey());
					documentIndexCounter++;
				}
				
				curDoc=new Document();
				curDoc.addParagraph(parser.parseParagraph(curArticle.getTitle())); //array of sentences?
				curDoc.addParagraph(parser.parseParagraph(curArticle.getAbstract()));
				curDoc.addParagraph(parser.parseParagraph(curArticle.getBody()));
				curParagraph=curDoc.getFirstParagraph();
				while(curParagraph!=null){
					curSent=curParagraph.getFirstSentence();
					while(curSent!=null){
						indexSentence(curSent,documentIndex);
						curSent=curSent.next;
					}
					curParagraph=curParagraph.next;
				}
				curArticle = cr.getNextRandom();
			}
			closeIndex();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public int indexSingle(Article curArticle){
		Document curDoc;
		Paragraph curParagraph;
		Sentence curSent;
		//		String documentKey;
		int documentIndex = -1;
		single = true;
		if(singleArticlePhrases != null){ 
			singleArticlePhrases.clear();
		}
		singleArticlePhrases = new ArrayList<WordPairStat>();
		//		wordpairStatList=new PairStats(folder+"/pairstat.list",maxSpan,true); 
		//		docKeyList=new SimpleElementList(folder+"/dockey.list",true);
		//		wordKeyList=new SimpleElementList(folder+"/wordkey.list",true);
		//		pairKeyList=new SimplePairList(folder+"/pairkey.list",true); 
		try {
			if (curArticle!=null){
				//				documentKey = curArticle.getKey();
				//				documentKey = "d"+documentKey;
				//				documentIndex = docKeyList.add(curArticle.getKey());
				for(Map.Entry<Integer, String> dId : docIdList.entrySet()){
					if(dId.getValue().equals(curArticle.getKey())){
						documentIndex = dId.getKey();
					}
				}
				if (documentIndex == -1){
					documentIndex = documentIndexCounter;
					docIdList.put(documentIndex, curArticle.getKey());
					documentIndexCounter++;
				}
				System.out.println(new java.util.Date().toString()+" "+curArticle.getKey() + " id:" + documentIndex);
				curDoc=new Document();
				curDoc.addParagraph(parser.parseParagraph(curArticle.getTitle())); //array of sentences?
				curDoc.addParagraph(parser.parseParagraph(curArticle.getAbstract()));
				curDoc.addParagraph(parser.parseParagraph(curArticle.getBody()));
				curParagraph=curDoc.getFirstParagraph();
				while(curParagraph!=null){
					curSent=curParagraph.getFirstSentence();
					while(curSent!=null){
						indexSentence(curSent,documentIndex);
						curSent=curSent.next;
					}
					curParagraph=curParagraph.next;
				}
			}
			closeIndex();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return documentIndex;
	}
	private boolean indexSentence(Sentence sent, Integer docKey){
		WordPairStat curPair;
		int i,j, num,sentIndex;

		try{
			if(sent.getWordNum()<2) // how much words have to be in sentence
				return true;

			//preprocess a sentence including tagging, lemmatising and indexing
			preprocessSentence(sent, docKey);


			//generate and save word pairs
			num=pairGenerator.generate(sent);
			//output the sentence
			if(num>0){
				sentIndex=sentenceBase.addSentence(sent);
				sentenceDocumentsRelation.put(sentIndex, docKey);
			}
			else
				return true;
			for(i=0;i<num;i++){
				curPair=pairGenerator.getWordPairs(i);
				//				curPair.setIndex(pairKeyList.add(curPair.getFirstWord(),curPair.getSecondWord()));
				curPair.setIndex(pairKeyListCounter);
				pairKeyListCounter++;
				if(!single) {
					wordpairStatList.add(curPair); 
				}
				else {
					singleArticlePhrases.add(curPair);
				}
				for(j=1;j<=maxSpan;j++)
				{
					if(curPair.getFrequency(j)>0)
						arrayRightPairList.get(j-1).add(curPair.getIndex(),sentIndex, curPair.getFrequency(j));
				}
				for(j=1;j<=maxSpan;j++)
				{
					if(curPair.getFrequency(-j)>0)
						arrayLeftPairList.get(j-1).add(curPair.getIndex(),sentIndex, curPair.getFrequency(j));
				}
			}
			return true;
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	public void extract(double minStrength, double minSpread, double minZScore, double minExpandRatio, String outputFile){
		expander=new WordPairExpandEng(maxSpan,folder, minExpandRatio, arrayRightPairList, arrayLeftPairList, sentenceBase, phraseSentenceRelation, termWordIndexList);
		pairFilter=new WordPairFilter(folder,maxSpan,minStrength,minSpread,minZScore);
		extract(expander,minStrength,minSpread,minZScore,outputFile);
	}
	public void extract(WordPairExpandEng expander, double minStrength, double minSpread, double minZScore, String outputFile){
		WordPairStat[] arrStat;
		SortedArray phraseList;
		PrintWriter screen;
		int i, j;

		phraseList=new SortedArray();
		screen=FileUtil.getScreen();
		arrStat=filter(minStrength,minSpread,minZScore);
		for(i=0;i<arrStat.length ;i++){
			for(j=1;j<maxSpan;j++){
				if(arrStat[i].getFrequency(j)>0)
					addPhrase(expander.expand(arrStat[i],j),phraseList,screen);
				if(arrStat[i].getFrequency(-j)>0)
					addPhrase(expander.expand(arrStat[i],-j),phraseList,screen);
			}
		}
		//save phrases to a textual file
		int maxPhraseLength = 3;
		storePhrases(phraseList);
		printPhrase(phraseList,outputFile);
		generateVocabulary(outputFile,maxPhraseLength,folder+"\\phraseOut.list");
	}
	public void extractSingle(Article article, String outputFile) {
		WordPairStat[] arrStat;
		SortedArray phraseList;
		PrintWriter screen;
		screen=FileUtil.getScreen();
		int i, j;
		arrStat=filterSingle();
		phraseList=new SortedArray();
		for(i=0;i<arrStat.length ;i++){
			for(j=1;j<maxSpan;j++){
				if(arrStat[i].getFrequency(j)>0)
					addPhrase(expander.expand(arrStat[i],j),phraseList,screen);
				if(arrStat[i].getFrequency(-j)>0)
					addPhrase(expander.expand(arrStat[i],-j),phraseList,screen);
			}
		}
		//save phrases to a textual file
		//		int maxPhraseLength = 3;
		storePhrases(phraseList);
		//		printPhrase(phraseList,outputFile);
		//		generateVocabulary(outputFile,maxPhraseLength,folder+"\\phraseOut.list");

	}
	private void addPhrase(ArrayList<Token> inputList, SortedArray phraseList, PrintWriter screen){
		Token token, old;
		int i;

		if(inputList==null)
			return;
		for(i=0;i<inputList.size();i++){
			token=(Token)inputList.get(i);
			if(!phraseList.add(token)){
				old=(Token) phraseList.get(phraseList.insertedPos());
				old.setFrequency(old.getFrequency()+token.getFrequency());
				//				old.setFrequency(Math.max(old.getFrequency(),token.getFrequency()));
			}
			//			screen.println(token.getValue()+"  "+token.getFrequency()); //phrases output
		}
	}

	private WordPairStat[] filter(double minStrength, double minSpread, double minZScore){
		//		WordPairFilter pairFilter;
		WordPairStat arrStat[];

		pairFilter.setWordNumber(wordIndexCounter);
		arrStat=pairFilter.execute(); //array of pairs with index
		return arrStat;
	}
	private WordPairStat[] filterSingle(){
		WordPairStat arrStat[];

		//		pairFilter=new WordPairFilter(folder,maxSpan,minStrength,minSpread,minZScore);
		pairFilter.setWordNumber(wordIndexCounter);
		arrStat=pairFilter.executeUpdate(singleArticlePhrases); //array of pairs with index
		return arrStat;
	}

	private void printPhrase(SortedArray list, String filename){
		BufferedWriter bw;
		Token phrase;
		int i;

		try{
			bw=FileUtil.getTextWriter(filename);
			bw.write(list.size()+"\n");
			for(i=0;i<list.size();i++){
				phrase=(Token)list.get(i);
				bw.write(phrase.getValue());
				bw.write('\t'+phrase.getFrequency());
				bw.write('\n');
				bw.flush();
			}
			bw.close();
		}

		catch(Exception e){
			e.printStackTrace();
		}
	}
	private void storePhrases(SortedArray list) {
		int i;
		Token phrase;
		for(i=0;i<list.size();i++){ // for each phrase
			phrase=(Token)list.get(i); // token is phrase
			ArrayList<Token> sentencesWithPhrase = phraseSentenceRelation.get(phrase.getIndex());
			IntShortOpenHashMap documents = new IntShortOpenHashMap(); //doc id, frequency of phrase in this doc
			for(Token sentece : sentencesWithPhrase){
				int sentIndex = Integer.parseInt(sentece.getValue()); // here value is index of sentence
				int docIndex = sentenceDocumentsRelation.get(sentIndex);
				//storing phraseDocument list
				if(documents.containsKey(docIndex)){
					documents.put(docIndex, (short) (documents.get(docIndex)+1));
				}
				else
					documents.put(docIndex,(short) 1);
				//extending docPhrase (matrix) list
				if(documentPhraseRelation.containsKey(docIndex)){
					// frequency for each document should be stored
					IntShortOpenHashMap old = (IntShortOpenHashMap) documentPhraseRelation.get(docIndex);
					if(old.containsKey(phrase.getIndex())){
						old.put(phrase.getIndex(), (short) (old.get(phrase.getIndex())+1));
					}
					else {
						old.put(phrase.getIndex(), (short) 1);
					}
					//					documentPhraseRelation.put(docIndex, old); // is done through reference
				}
				else {
					IntShortOpenHashMap newList = new IntShortOpenHashMap();
					newList.put(phrase.getIndex(), (short) 1);
					documentPhraseRelation.put(docIndex, newList);
				}
			}
			if(phraseDocumentsRelation.containsKey(phrase.getIndex())){
				IntShortOpenHashMap oldDocs = (IntShortOpenHashMap)phraseDocumentsRelation.get(phrase.getIndex());
				int[] documentsKeys = documents.keys;
				short[] documentsValues = documents.values;
				boolean[] docStates = documents.allocated;
				for(int doc = 0; doc < docStates.length; doc ++){
					if(!docStates[doc])
						continue;
					if(oldDocs.containsKey(documentsKeys[doc])){
						short oldVal = oldDocs.get(documentsKeys[doc]);
						oldDocs.put(documentsKeys[doc], (short) (documentsValues[doc] + oldVal));
					}
					else {
						oldDocs.put(documentsKeys[doc], documentsValues[doc]);
					}
				}
			}
			else
				phraseDocumentsRelation.put(phrase.getIndex(), documents);
			//			phraseIndexList.put(token.getValue(), token.getFrequency());
			if(phraseFrequencyIndex.containsKey(phrase.getIndex())){
				int oldFrequency = phraseFrequencyIndex.get(phrase.getIndex());
				phraseFrequencyIndex.put(phrase.getIndex(),phrase.getFrequency()+oldFrequency);
			}
			phraseFrequencyIndex.put(phrase.getIndex(), phrase.getFrequency());
		}

	}
	/*protected void printWordPair(WordPairStat[] arrStat, String filename){
		SortedArray pairList;
		SimpleElementList wordkeyList;
		String pair;
		int i;

		try{
			wordkeyList=new SimpleElementList(folder+"/wordkey.list",false);
			pairList=new SortedArray();
			for(i=0;i<arrStat.length;i++){
				pair=wordkeyList.search(arrStat[i].getFirstWord())+" "+wordkeyList.search(arrStat[i].getSecondWord());
				pairList.add(pair.toLowerCase());
			}
			printPhrase(pairList,filename);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}*/
	/** Preprocess a sentence including tagging, lemmatising and indexing. This method can be overrided.
	 * @param sent the sentence for preprocessing
	 */
	protected void preprocessSentence(Sentence sent, Integer docKey){
		Word cur;
		int wordIndex;
		//part of speech tagging
		if(tagger!=null) tagger.tag(sent);

		//lemmatising and indexing words
		cur=sent.getFirstWord();
		while(cur!=null){
			//if(cur.getPOSIndex()==Tagger.POS_NOUN || cur.getPOSIndex()==Tagger.POS_VERB)
				if(lemmatiser!=null)
					cur.setLemma(lemmatiser.lemmatize(cur.getContent()));
				else
					cur.setLemma(cur.getContent().toLowerCase());
			//else
			//	cur.setLemma(cur.getContent().toLowerCase());
			
			//do not index stop words
			//if(stopWords.contains(cur.getContent())){
			//	cur=cur.next;
			//	continue;
			//}
			
			if(wordTermIndexList.containsKey(cur.getLemma())){
				wordIndex = wordTermIndexList.get(cur.getLemma());
				cur.setIndex(wordIndex);
			}
			else {
				wordIndex = wordIndexCounter;
				wordIndexCounter++;
				cur.setIndex(wordIndex);
				termWordIndexList.put(wordIndex, cur.getLemma());
				wordTermIndexList.put(cur.getLemma(), wordIndex);
			}
			//

			//docterm "matrix"
			if(documentTermRelation.containsKey(docKey)){
				IntShortOpenHashMap wordsInDocument = (IntShortOpenHashMap) documentTermRelation.get(docKey);
				if(wordsInDocument.containsKey(wordIndex)){
					wordsInDocument.put(wordIndex, (short) (wordsInDocument.get(wordIndex) + 1));
				}
				else {
					wordsInDocument.put(wordIndex, (short) 1);
				}
			}
			else {
				IntShortOpenHashMap newWord = new IntShortOpenHashMap();
				newWord.put(wordIndex, (short) 1);
				documentTermRelation.put(docKey, newWord);
			}
			//termDoc matrix
			/*if(termDocumentRelation.containsKey(wordIndex)){
				HashMap<Integer, Short> documentsWithTerm = termDocumentRelation.get(wordIndex);
				if(documentsWithTerm.containsKey(docKey)){
					documentsWithTerm.put(docKey, (short) (documentsWithTerm.get(docKey) + 1));
				}
				else
					documentsWithTerm.put(docKey, (short) 1);
			}
			else {
				HashMap<Integer, Short> documentsWithTerm = new HashMap<Integer, Short>();
				documentsWithTerm.put(docKey, (short) 1);
				termDocumentRelation.put(wordIndex,documentsWithTerm);
			}
			*/
			// termIndexList
			if(termFreqIndexList.containsKey(wordIndex)){
				termFreqIndexList.put(wordIndex, termFreqIndexList.get(wordIndex) + 1);
			}
			else
				termFreqIndexList.put(wordIndex, 1);
			cur=cur.next;
		}
	}
	public void closeIndex(){
		int i;

		//sentenceBase.close(); //done
		//		docKeyList.close(); // dragontoolkit
		//		wordKeyList.close(); //dragontoolkit
		wordpairStatList.close(); //dragontoolkit
		//		pairKeyList.close(); //dragontoolkit
		for(i=0;i<maxSpan;i++){
			//arrayRightPairList.get(i).close();
		}
		for(i=0;i<maxSpan;i++){
			//arrayLeftPairList.get(i).close();
		}
	}
	public static void generateVocabulary(String phraseFile, int maxPhraseLen, String vobFile){
		generateVocabulary(postProcessExtractedPhrase(phraseFile),maxPhraseLen,vobFile);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void generateVocabulary(ArrayList phraseList, int maxLen, String outputFile){
		BufferedWriter bw;
		ArrayList newList;
		int num, min, max;
		int i;

		try{
			System.out.println((new java.util.Date()).toString()+" Printing vocabulary file...");
			bw=FileUtil.getTextWriter(outputFile);
			min=Integer.MAX_VALUE;
			max=0;
			newList=new ArrayList(phraseList.size());
			for(i=0;i<phraseList.size();i++){
				num=getTokenNum((String)phraseList.get(i));
				if(num<=maxLen){
					newList.add(phraseList.get(i));
					if (num > max)
						max = num;
					if (num < min)
						min = num;
				}
			}

			bw.write(newList.size()+"\t"+min+"\t"+max+"\n");
			for(i=0;i<newList.size();i++)
			{
				bw.write((String)newList.get(i));
				bw.write('\t');
				bw.write(String.valueOf(i));
				bw.write('\n');
			}
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private static SortedArray postProcessExtractedPhrase(String phraseFile){
		SortedArray list;
		BufferedReader br;
		String line;
		int pos;

		try{
			System.out.println((new java.util.Date()).toString()+" Postprocessing Extracted Phrases...");
			list = new SortedArray();
			br=FileUtil.getTextReader(phraseFile);
			br.readLine();//skip the first line

			while((line=br.readLine())!=null){
				pos=line.indexOf('\t');
				if(pos>=0)
					line=line.substring(0,pos);
				line=postProcessPhrase(line);
				if(line.indexOf(' ')>0)
					list.add(line);
			}
			return list;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	private static String postProcessPhrase(String content){
		try{

			content = content.replace('-', ' ');
			content = content.replace('_', ' ');
			content = content.replace('\'', ' ');
			content = content.replaceAll("   ", " ");
			content = content.replaceAll("  ", " ");
			content = content.replaceAll("  ", " ");
			content=removePersonTitle(content);
			return content.toLowerCase();
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	private static String removePersonTitle(String content){
		int pos;

		content=content.trim();
		pos=content.indexOf(' ');
		if(pos>0){
			if(content.charAt(pos-1)=='.' && content.lastIndexOf('.',pos-2)<0){
				return removePersonTitle(content.substring(pos + 1));
			}
		}
		return content;
	}
	private static int getTokenNum(String term){
		int count, i;

		count = 0;
		for (i = 0; i < term.length(); i++)
			if (Character.isWhitespace(term.charAt(i)))
				count++;
		return count + 1;
	}
	
	public IntObjectOpenHashMap<IntShortOpenHashMap> getPhraseDocumentsRelation() {
		return phraseDocumentsRelation;
	}

	public IntObjectOpenHashMap<IntShortOpenHashMap> getDocumentTermRelation() {
		return documentTermRelation;
	}

	public IntIntOpenHashMap getPhraseFrequencyIndex() {
		return phraseFrequencyIndex;
	}
	public IntObjectOpenHashMap<IntShortOpenHashMap> getDocumentPhraseRelation() {
		return documentPhraseRelation;
	}
	public IntIntOpenHashMap getTermFreqIndexList() {
		return termFreqIndexList;
	}
//	public HashMap<Integer, String> getTermWordIndexList() {
//		return termWordIndexList;
//	}
}
