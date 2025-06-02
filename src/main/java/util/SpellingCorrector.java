package util;

import java.util.*;
public class SpellingCorrector {
    private final Set<String> dictionary;

    public SpellingCorrector(Collection<String> words) {
        this.dictionary = new HashSet<>(words);
    }

    public String correct ( String word ){
        if (dictionary.contains(word)) {
            return  word;
        }

        Set<String> edits = edits1(word);
        for ( String edit: edits){
            if( dictionary.contains(edit)){
                return edit;
            }
        }
        return word;
    }


    private Set<String> edits1 ( String word){
        Set<String> edits = new HashSet<>();
        for ( int i = 0; i < word.length(); i++){
            //replace char
            for ( char c = 'a'; c <= 'z'; c++){
                edits.add(word.substring(0,i) + c + word.substring(i));
            }
            //delete
            edits.add(word.substring(0,i) + word.substring(i+1));

            //schimba doua litere intre ele
            if ( i < word.length() - 1){
                edits.add(word.substring(0,i) + word.charAt(i+1) + word.charAt(i) + word.substring(i+1));
            }
        }

        //insertion
        for (int i = 0; i <= word.length(); i++) {
            for (char c = 'a'; c <= 'z'; c++) {
                edits.add(word.substring(0, i) + c + word.substring(i));
            }
        }
        return edits;

    }
}
