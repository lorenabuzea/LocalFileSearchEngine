package search;

public class QueryProcessor {

    public String prepare(String rawQuery) {
        //Basic cleanup for now, just trim it
        return rawQuery.trim();
    }
}
