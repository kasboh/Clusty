package kbo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
//import java.util.Map;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;

import dragon.util.FastBinaryReader;
import dragon.util.FastBinaryWriter;

public class SentenceListWriter {
	private String fileName;
	public SentenceListWriter(String fileName){
		this.fileName = fileName;
	}
	public void write(IntObjectOpenHashMap<ArrayList<TupleInt>> sentenceIndex){

		try {
			FastBinaryWriter listWriter = new FastBinaryWriter(fileName);
			int i = sentenceIndex.size();
			listWriter.writeInt(i); // 32 bit number of elements(sentences) in file
//			for (Map.Entry<Integer, ArrayList<TupleInt>> entry : sentenceIndex.entrySet()){
			for (IntObjectCursor<ArrayList<TupleInt>> entry : sentenceIndex){
				//32 bit integer sentence id
				listWriter.writeInt(entry.key); 
				//XtractSentence curSent = (XtractSentence) entry.getValue(); //delete line
				ArrayList<TupleInt> curSent = entry.value;
				//Map<Integer, Integer> words = curSent.getWords();
				// 16 bit number of words in sentence
				listWriter.writeShort(curSent.size());
				for(TupleInt word : curSent){
					// 32 bit word id
					listWriter.writeInt(word.x);
					// 32 bit word POS
					listWriter.writeInt(word.y);
				}
				listWriter.flush();
			}
			listWriter.close();
			System.out.println("Finished writing sentences to disk...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public HashMap<Integer, ArrayList<TupleInt>> read (String fileToRead){ //TODO
		FastBinaryReader listReader = new FastBinaryReader(fileToRead);
		HashMap<Integer, ArrayList<TupleInt>> result = new HashMap<Integer, ArrayList<TupleInt>>();
		try {
			int size = listReader.readInt(); // number of elements
			for (int i =0; i<size; i++){
				int sentenceId = listReader.readInt();
				short sentSize = listReader.readShort();
				ArrayList<TupleInt> words = new ArrayList<TupleInt>();
				for (int k=0;k<sentSize;k++){
					int wordId = listReader.readInt();
					int wordPos = listReader.readInt();
					TupleInt word = new TupleInt(wordId, wordPos);
					words.add(word);
				}
				result.put(sentenceId, words);
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
