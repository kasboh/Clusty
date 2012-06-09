package kbo;

//import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dragon.util.FastBinaryReader;
import dragon.util.FastBinaryWriter;
import dragon.util.FileUtil;

public class PhraseSentenceAdjacencyList {
	private String listFilename;
	//private ArrayList <Map<Integer, Object>> list;
	private HashMap <Integer, ArrayList<TupleInt>> list; //FIXME convert to intobjectopenhashmap and write to disk
	//private File hddFile;
	RandomAccessFile phraseAdjacencyList;
	public PhraseSentenceAdjacencyList(String listFilename){
		this.listFilename = listFilename;
		list = new HashMap <Integer, ArrayList<TupleInt>>();
		initData();
	}
	private void initData(){
		//hddFile = new File(listFilename);
		//if(hddFile.exists()){	
		if(FileUtil.exist(listFilename)){
			try {
				phraseAdjacencyList = new RandomAccessFile(listFilename, "r");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		if(listFilename!=null && FileUtil.exist(listFilename)){
			//readListFile(listFilename);
			//adjList = new RandomAccessFile(listFilename, "rw");
		}
	}

	public void add(int phraseIndex, int sentenceIndex, int frequency){
		ArrayList<TupleInt> l = null;
		if(!list.containsKey(phraseIndex)){
			list.put(phraseIndex, l=new ArrayList<TupleInt>());
			l.add(new TupleInt(sentenceIndex, frequency));
		}
		else {
			l = list.get(phraseIndex);
			l.add(new TupleInt(sentenceIndex, frequency));
		}
	}
	public void close() {
		try {
			FastBinaryWriter listWriter = new FastBinaryWriter(listFilename);
			int i = list.size();
			listWriter.writeInt(i); // 32 bit number of elements(phrases) in file
			for (Map.Entry<Integer, ArrayList<TupleInt>> entry : list.entrySet()){
				//32 bit integer phrase id
				listWriter.writeInt(entry.getKey()); 
				ArrayList<TupleInt> curPhrases = entry.getValue();
				// 32 bit number of sentences phrase is in
				listWriter.writeInt(curPhrases.size());
				for(TupleInt tuple : curPhrases){
					// 32 bit sentence id
					listWriter.writeInt(tuple.x);
					// 32 bit phrase frequency in sentence
					listWriter.writeInt(tuple.y);
				}
				listWriter.flush();
			}
			listWriter.close();
			list.clear();
			System.out.println("Finished writing phrases to disk...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void read(){
		try {
			FastBinaryReader listReader = new FastBinaryReader(listFilename);
			list.clear();
			int listSize = listReader.readInt(); //number of phrases in file
			for(int el=0; el<listSize; el++){
				int phraseId = listReader.readInt();
				int numberOfSentences = listReader.readInt();
				for(int em=0; em<numberOfSentences; em++){
					int sentenceId = listReader.readInt();
					int sentPhraseFreq = listReader.readInt();
					add(phraseId,sentenceId,sentPhraseFreq);
				}
			}
			listReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public int getNumberOfSentences(int phraseID){
		return list.get(phraseID).size(); 
		//TODO check if null
	}
	public ArrayList<TupleInt> getSentences (int phraseID){
		return list.get(phraseID);
	}
}
