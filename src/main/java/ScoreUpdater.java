import indexer.DBWriter;

public class ScoreUpdater {
    public static  void main (String[] args){
        DBWriter db = new DBWriter();
        db.updateAllScores();
        db.close();
    }
}
