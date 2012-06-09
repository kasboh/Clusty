package kbo;

import dragon.nlp.*;
import dragon.util.*;
import dragon.nlp.tool.*;
import dragon.nlp.tool.xtract.WordPairStat;
//import dragon.matrix.*;
import dragon.nlp.compare.*;
import java.util.*;

//import com.carrotsearch.hppc.IntCharOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;

/**
 * <p>Expanding word pair (not necessary consecutive) to noun phrase</p>
 * <p> </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: IST, Drexel University</p>
 * @author Davis Zhou
 * @version 1.0
 */

public class WordPairExpandEng {
    protected SentenceList sentenceList;
//    protected SimpleElementList wordList;
    protected HashMap<Integer, String> wordList;
    protected int maxSpan;
    protected String indexFolder;
    protected List<PhraseSentenceAdjacencyList> arrayRightPairList, arrayLeftPairList;
    protected double threshold;
    protected IntObjectOpenHashMap<ArrayList<Token>> phraseSentenceRelation;
    private IndexList phraseIndex;

    /*public WordPairExpandEng(int maxSpan, String indexFolder, double threshold, HashMap<Integer, String> termWordIndexList) {
        this.maxSpan = maxSpan;
        this.threshold = threshold;
        this.indexFolder = indexFolder;

//        wordList = new SimpleElementList(indexFolder + "/wordkey.list", false); // implemented in dragontoolkit
        this.wordList = termWordIndexList;
        sentenceList = new SentenceList(indexFolder + "/sentenceList.list"); // not dragontoolkit
        //sentMatrix = new IntSuperSparseMatrix(indexFolder + "/sentencebase.index",indexFolder + "/sentencebase.matrix");
        arrayRightPairList = new ArrayList <PhraseSentenceAdjacencyList>();
		arrayLeftPairList = new ArrayList <PhraseSentenceAdjacencyList>();
		phraseIndex = new IndexList(indexFolder, "phraseIndexList.list"); // not dragontoolkit
		for(int i=1;i<=maxSpan;i++){
			// create adjacency list for each span, it will store histogram for word pair and sentences
			// <pair index> <sentence index> <frequency>
			arrayRightPairList.add(new PhraseSentenceAdjacencyList(indexFolder + "/rightPairSentList" + i +".list"));
			arrayLeftPairList.add(new PhraseSentenceAdjacencyList(indexFolder + "/leftPairSentList"+ i + ".list"));
		}
    }*/
    public WordPairExpandEng(int maxSpan, String indexFolder, double threshold, 
    		List <PhraseSentenceAdjacencyList> arrRight, List <PhraseSentenceAdjacencyList> arrLeft,
    		SentenceList sentList, IntObjectOpenHashMap<ArrayList<Token>> phraseSentenceRelation, HashMap<Integer, String> termWordIndexList) {
        this.maxSpan = maxSpan;
        this.threshold = threshold;
        this.indexFolder = indexFolder;
        phraseIndex = new IndexList(indexFolder, "phraseIndexList.list");
//        wordList = new SimpleElementList(indexFolder + "/wordkey.list", false); // implemented in dragontoolkit
        this.wordList = termWordIndexList;
        sentenceList = sentList; 
        //sentMatrix = new IntSuperSparseMatrix(indexFolder + "/sentencebase.index",indexFolder + "/sentencebase.matrix");
        arrayRightPairList = arrRight;
		arrayLeftPairList = arrLeft;
		this.phraseSentenceRelation = phraseSentenceRelation;
    }
    
    @SuppressWarnings("unchecked")
	public ArrayList<Token> expand(WordPairStat wordPairStat, int span) {
        ArrayList<Token> sentList, phraseList;
        Token token;
        String expandStr;
        int sentNum, firstWord, secondWord;
        boolean pass;

        try{
            firstWord = wordPairStat.getFirstWord();
            secondWord = wordPairStat.getSecondWord();
            expandStr = null;

            //return all sentences containing the word pair and the position of the first word in the corresponding sentence
            sentList =getSentenceList(wordPairStat,span);
            sentNum = sentList.size();

            //expand middle
            pass = true;
            if (span > 1 || span < -1) {
                if (span > 1)
                    token = expandSecion(1, span - 1, sentNum, false, 0, sentList);
                else
                    token = expandSecion(1, -span - 1, sentNum, true, 0, sentList);
                if (token == null)
                    pass = false;
                else {
                    pass = true;
                    sentList = (ArrayList<Token>) token.getMemo();
                    if (span > 1)
                        expandStr = getWordContent(firstWord) + " " + token.getName().trim() + " " +getWordContent(secondWord);
                    else
                        expandStr = getWordContent(secondWord) + " " + token.getName().trim() + " " +getWordContent(firstWord);
                }
            }
            else {
                if (span == 1)
                    expandStr = (getWordContent(firstWord)+ " " + getWordContent(secondWord)).trim();
                else
                    expandStr = (getWordContent(secondWord).trim() + " " + getWordContent(firstWord)).trim();
            }

            if (!pass)
                return null;

            //expand left
            if (span > 0)
                token = expandSecion(1, maxSpan, sentNum, true, -1, sentList);
            else
                token = expandSecion( -span + 1, maxSpan - span, sentNum, true, -1, sentList);
            if (token != null) {
                sentList = (ArrayList<Token>) token.getMemo();
                expandStr = token.getName().trim() + " " + expandStr;
            }

            //expand right
            if (span > 0)
                token = expandSecion(span + 1, span + maxSpan, sentNum, false, 1, sentList);
            else
                token = expandSecion(1, maxSpan, sentNum, false, 1, sentList);
            if (token != null) {
                sentList = (ArrayList<Token>) token.getMemo();
                expandStr = expandStr + " " + token.getName().trim();
            }

            phraseList=new ArrayList<Token>(1);
            Token phrase = new Token(expandStr.trim());
            int phraseCountNumber = phraseIndex.add(expandStr.trim());
            phrase.setIndex(phraseCountNumber);
            phraseList.add(phrase);
            // 
            if(phraseSentenceRelation.containsKey(phraseCountNumber)){
            	ArrayList<Token> oldList = (ArrayList<Token>) phraseSentenceRelation.get(phraseCountNumber);
            	oldList.addAll(sentList);
            	phraseSentenceRelation.put(phraseCountNumber, oldList);
            }
            else
            	phraseSentenceRelation.put(phraseCountNumber, sentList);
            return phraseList;
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    protected ArrayList<Token> getSentenceList(WordPairStat wordPairStat, int span){
    	PhraseSentenceAdjacencyList phraseSentList;
        //ArrayList sentList;
        Token sentToken;
        int firstWord, secondWord, wordKey, sentIndex, sentLength, pairIndex;

        pairIndex = wordPairStat.getIndex();
        firstWord = wordPairStat.getFirstWord();
        secondWord = wordPairStat.getSecondWord();

        if (span < 0)
            phraseSentList = arrayLeftPairList.get( -span - 1); 
        else
            phraseSentList = arrayRightPairList.get(span - 1);
        
//        sentNum = phraseSentList.getNumberOfSentences(pairIndex);
        ArrayList<Token> sentListTokens = new ArrayList<Token>();
        ArrayList <TupleInt> listOfSentences = phraseSentList.getSentences(pairIndex);
        for (TupleInt tuple : listOfSentences){
        	sentIndex = tuple.x;
        	ArrayList<TupleInt> sentenceWords = sentenceList.getSentence(sentIndex);
        	
        	if (sentenceWords == null) 
        		continue;
        	sentLength = sentenceWords.size();
        	for(TupleInt word : sentenceWords){
        		int indexInList = sentenceWords.indexOf(word);
        		wordKey = word.x;
        		if (wordKey != firstWord)
        			continue;
        		if ( (indexInList + span) >= 0 && (indexInList + span) <sentLength && (sentenceWords.get(indexInList + span).x == secondWord)){
        			sentToken = new Token(String.valueOf(sentIndex));
        			sentToken.setIndex(indexInList);
        			sentToken.setFrequency(sentenceWords.get(indexInList + span).y);
        			sentListTokens.add(sentToken);
        		}
        	}
        }
        return sentListTokens;
    }

    //direction 0:middle -1:left, 1:right
    @SuppressWarnings("unchecked")
	protected Token expandSecion(int start, int end, int sentNum, boolean inverse, int direction, ArrayList<Token> sentList) {
        Token token;
        String expandStr, word, marginalWord;
        int posIndex, marginalPOS;
        int i, j, pos;

        expandStr="";
        marginalWord=null;
        marginalPOS=-1;

        for (i=start; i<= end; i++) { // for all position between two words in pair
            if(inverse)
                j=-i;
            else
                j=i;
            token =checkSentPos(j, sentList); // get most frequent word on position j
            if (token!= null){
                sentList = (ArrayList<Token>) token.getMemo();
                if( (token.getFrequency() / (double) sentNum) >=threshold) {
                    //if the direction is not middle, check if the word is valid as a part of the phrase
                    word = (getWordContent(Integer.parseInt(token.getName())));
                    posIndex=token.getIndex();
                    if (direction==0 || checkValidation(word, posIndex)) {
                       if (inverse)
                            expandStr = word+" " + expandStr;
                        else
                            expandStr = expandStr + " " + word;
                        expandStr=expandStr.trim();
                        if(direction==1 && !inverse || direction==-1 && inverse){
                            marginalWord=word;
                            marginalPOS=posIndex;
                        }
                    }
                    else
                        break;
                }
                else
                    break;
            }
            else
                break;
        }

        if(i<=end && direction==0)
            return null;
        else if (!expandStr.equals("")) {
            if(direction==1 && !inverse && !checkEndingWordValidation(marginalWord,marginalPOS)){
                pos=expandStr.lastIndexOf(' ');
                if(pos>=0)
                    expandStr=expandStr.substring(0,pos);
                else
                    return null;
            }
            else if(direction==-1 && inverse && !checkStartingWordValidation(marginalWord,marginalPOS)){
                pos=expandStr.indexOf(' ');
                if(pos>=0)
                    expandStr=expandStr.substring(pos+1);
                else
                    return null;
            }
            token = new Token(expandStr);
            token.setMemo(sentList);
            return token;
        }
        else
            return null;
    }
    /**
     * return the most frequent word on the specified distance to the word, word is stored in token
     * token memo contains list of all sentences where word occures
     * @param spanFromFirstWord
     * @param sentList
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	protected Token checkSentPos(int spanFromFirstWord, ArrayList<Token> sentList) {
        SortedArray tokenList;
        ArrayList<Token> sList;
        Token wordToken, sentToken;
        int i, sentIndex, sentLength, firstWordPos, wordKey, tokenIndex;

        tokenList = new SortedArray();

        for (i = 0; i < sentList.size(); i++) {
            sentToken = (Token) sentList.get(i);
            sentIndex = Integer.parseInt(sentToken.getName());
            sentLength = sentenceList.getSentenceSize(sentIndex);
            firstWordPos = sentToken.getIndex();

            if ( (firstWordPos + spanFromFirstWord) >= 0 && (firstWordPos + spanFromFirstWord < sentLength)) {
                wordKey = sentenceList.getSentence(sentIndex).get(firstWordPos + spanFromFirstWord).x;
                wordToken = new Token(String.valueOf(wordKey));
                tokenIndex = tokenList.binarySearch(wordToken);
                if (tokenIndex < 0) {
                    sList = new ArrayList<Token>();
                    sList.add(sentToken);
                    wordToken.setFrequency(1);
                    wordToken.setIndex(sentenceList.getSentence(sentIndex).get(firstWordPos + spanFromFirstWord).y); //the part of speech TODO check if it works correct
                    wordToken.setMemo(sList);
                    tokenList.add(wordToken);
                }
                else {
                    wordToken = (Token) tokenList.get(tokenIndex);
                    wordToken.addFrequency(1);
                    sList = (ArrayList) wordToken.getMemo();
                    sList.add(sentToken);
                }
            }
        }

        if(tokenList.size()>0){
            tokenList.setComparator(new FrequencyComparator(true));
            wordToken = (Token) tokenList.get(0);
            tokenList.clear();
            return wordToken;
        }
        else
            return null;
    }

    protected String getWordContent(int index){
//        return wordList.search(index).trim();
        return wordList.get(index).trim();
    }

    protected boolean checkValidation(String word, int posIndex){
        if(posIndex==Tagger.POS_ADJECTIVE || posIndex==Tagger.POS_NOUN || posIndex==0 && word.equals("-"))
            return true;
        return false;
    }

    protected boolean checkEndingWordValidation(String word, int posIndex){
        if(posIndex==Tagger.POS_NOUN)
            return true;
        else
            return false;
    }

    protected boolean checkStartingWordValidation(String word, int posIndex){
        if(posIndex==Tagger.POS_NOUN || posIndex==Tagger.POS_ADJECTIVE)
            return true;
        else
            return false;
    }
}
