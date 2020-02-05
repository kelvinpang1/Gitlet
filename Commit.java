package gitlet;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
/** ID.
 * @author Kelvin Pang
 * */

/** Class for committing. */
public class Commit implements Serializable {

    /** Commit method.
     *  @param parentSHA SHA of parent.
     *  @param message contents.
     * */
    public Commit(String parentSHA, String message) {
        _trackedFiles = new HashMap<String, String>();
        _parent1 = parentSHA;
        _message = message;
        Formatter f = new Formatter();
        Date date = Calendar.getInstance().getTime();
        _date = f.format("%ta %tb %te %tH:%tM:%tS %tY %tz",
                date, date, date, date, date, date, date, date).toString();
    }

    /** Commit method with data parameter.
     *  *  @param parentSHA SHA of parent.
     *  *  @param message the message.
     *  *  @param date the date.
     * */
    public Commit(String parentSHA, String message, Date date) {
        _trackedFiles = new HashMap<String, String>();
        _parent1 = parentSHA;
        _message = message;
        Formatter f = new Formatter();
        _date = f.format("%ta %tb %te %tH:%tM:%tS %tY %tz",
                date, date, date, date, date, date, date, date).toString();
    }

    /** My log message.
     * @param x the commit.
     * */
    public void copyTrackedFiles(Commit x) {
        _trackedFiles
                = (HashMap<String, String>) x._trackedFiles.clone();
    }

    /** Return the tracked files.
     * @return
     * */
    public HashMap<String, String> gettrackedFiles() {
        return _trackedFiles;
    }

    /** Return the date.
     * @return
     * */
    public String getdate() {
        return _date;
    }

    /** Return the parent.
     * @return
     * */
    public String getparent1() {
        return _parent1;
    }

    /** Set the parent.
     * @param x what is this.
     * */
    public void setparent1(String x) {
        _parent1 = x;
    }

    /** Get the message.
     * @return
     * */
    public String getmessage() {
        return _message;
    }

    /** My log message.*/
    private String _message;
    /** My timestamp. */
    private String _date;
    /** SHA1 hash of parent 1. */
    private String _parent1;
    /** HashMap of blobs. */
    private HashMap<String, String> _trackedFiles;

}
