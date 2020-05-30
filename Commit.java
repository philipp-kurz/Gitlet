package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Formatter;

/** Commit class for gitlet.
 *  @author Philipp
 */
public class Commit implements Serializable {

    /** Message of commit. */
    private String _message;

    /** Timestamp oof commit. */
    private Date _timestamp;

    /** List of commit's parents commits. */
    private List<String> _parents;

    /** List of files that are tracked by this commit. */
    private List<FileToShaMapping> _trackedFiles;

    /** Creates new commit with commit message MESSAGE. */
    public Commit(String message) {
        _message = message;
        _timestamp = new Date();
        _trackedFiles = new ArrayList<FileToShaMapping>();
    }

    /** Creates new commit with commit message MESSAGE and
     *  timestamp TIMESTAMPMS.
     */
    public Commit(String message, long timestampMS) {
        _message = message;
        _timestamp = new Date(timestampMS);
    }

    /** Returns new initial commit.
     */
    public static Commit newInitialCommit() {
        return new Commit("initial commit", 0);
    }

    /** Returns list of files that are tracked in this commit.
     */
    public List<FileToShaMapping> getTrackedFiles() {
        if (_trackedFiles == null) {
            _trackedFiles = new ArrayList<FileToShaMapping>();
        }
        return _trackedFiles;
    }

    /** Sets list of tracked files to list PREVIOUS.
     */
    public void setTrackedFiles(List<FileToShaMapping> previous) {
        _trackedFiles = previous;
    }

    /** Update tracked file FILENAME in this commit with commit hash HASH.
     */
    public void updateTrackedFile(String filename, String hash) {
        boolean existing = false;
        if (_trackedFiles == null) {
            _trackedFiles = new ArrayList<FileToShaMapping>();
        } else {
            for (FileToShaMapping mapping : _trackedFiles) {
                if (mapping.getFilename().equals(filename)) {
                    mapping.setHash(hash);
                    existing = true;
                    break;
                }
            }
        }
        if (!existing) {
            _trackedFiles.add(new FileToShaMapping(filename, hash));
        }
    }

    /** Adds parent COMMITHASH to this commit.
     */
    public void addParent(String commitHash) {
        if (_parents == null) {
            _parents = new LinkedList<String>();
        }
        _parents.add(commitHash);
    }

    /** Remove file FILENAME from this commit's tracking.
     */
    public void removeTracking(String filename) {
        int index = -1;
        for (int i = 0; i < _trackedFiles.size(); i++) {
            if (_trackedFiles.get(i).getFilename().equals(filename)) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            _trackedFiles.remove(index);
        }
    }

    /** Returns date string from this commit's timestamp.
     */
    public String getDateString() {
        StringBuilder sbuf = new StringBuilder();
        Formatter fmt = new Formatter(sbuf);
        fmt.format("%ta %tb %td %tH:%tM:%tS %tY %tz", _timestamp, _timestamp,
                _timestamp, _timestamp, _timestamp, _timestamp, _timestamp,
                _timestamp);
        return sbuf.toString();
    }

    /** Return this commit's commit message.
     */
    public String getMessage() {
        return _message;
    }

    /** Return this commit's first parent.
     */
    public String getParent() {
        if (_parents == null || _parents.size() == 0) {
            return null;
        }
        return _parents.get(0);
    }

    /** Returns this commit's parent with index IND.
     */
    public String getParent(int ind) {
        if (_parents == null || _parents.size() <= ind) {
            return null;
        }
        return _parents.get(ind);
    }

    /** Returns FileToShaMapping for file FILENAME from this commit.
     */
    public FileToShaMapping getMapping(String filename) {
        FileToShaMapping res = null;
        for (FileToShaMapping map : _trackedFiles) {
            if (map.getFilename().equals(filename)) {
                res = map;
                break;
            }
        }
        return res;
    }

    /** Returns true iff this commit is a merge commit.
     */
    public boolean isMergeCommit() {
        return _parents != null && _parents.size() > 1;
    }

}
