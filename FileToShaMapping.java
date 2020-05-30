package gitlet;

import java.io.File;
import java.io.Serializable;

/** FileToShaMapping class for gitlet.
 *  @author Philipp
 */
public class FileToShaMapping implements Serializable {

    /** File name of mapping. */
    private File _file;

    /** Commit hash of mapping. */
    private String _hash;

    /** Constructs new mapping with filename FILE and
     *  commit hash HASH.
     */
    public FileToShaMapping(File file, String hash) {
        _file = file;
        _hash = hash;
    }

    /** Constructs new mapping with file FILENAME and commit
     *  hash HASH.
     */
    public FileToShaMapping(String filename, String hash) {
        _file = new File(filename);
        _hash = hash;
    }

    /** Returns filename of mapping. */
    public String getFilename() {
        return _file.toString();
    }

    /** Returns commit hash of mapping. */
    public String getHash() {
        return _hash;
    }

    /** Sets commit hash of mapping to HASH. */
    public void setHash(String hash) {
        _hash = hash;
    }

    @Override
    public String toString() {
        return _file + " - " + _hash;
    }

    @Override
    public boolean equals(Object obj) {
        FileToShaMapping other = (FileToShaMapping) obj;
        return getFilename().equals(other.getFilename())
                && getHash().equals(other.getHash());
    }

    @Override
    public int hashCode() {
        return _file.hashCode() + _hash.hashCode();
    }
}
