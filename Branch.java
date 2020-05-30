package gitlet;

import java.io.Serializable;

/** Branch class for gitlet.
 *  @author Philipp
 */
public class Branch implements Serializable {

    /** Name of the branch. */
    private String _name;

    /** Commit hash of the head commit of the branch. */
    private String _head;

    /** Creates new branch with NAME and COMMITHASH.
     */
    public Branch(String name, String commitHash) {
        _name = name;
        _head = commitHash;
    }

    /** Returns name of branch.
     */
    public String getName() {
        return _name;
    }

    /** Sets head of branch to COMMITHASH.
     */
    public void setCommitHash(String commitHash) {
        _head = commitHash;
    }

    /** Returns commit hash of my head commit.
     */
    public String getHead() {
        return _head;
    }

    @Override
    public String toString() {
        return "Branch " + _name;
    }
}
