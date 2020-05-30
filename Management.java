package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;

/** Management class for gitlet.
 *  @author Philipp
 */
public class Management implements Serializable {

    /** List of all branches tracked by this gitlet directory. */
    private List<Branch> _branches;

    /** Current head commit. */
    private String _head;

    /** List with files staged for removal. */
    private List<String> _filesToRemove;

    /** Currently active branch. */
    private String _currentBranch;

    /** True iff if during current code execution something had been output
     * to the standard output already.
     */
    private boolean _output = false;

    /** Constructs new management object. */
    public Management() {
        _branches = new LinkedList<Branch>();
        _head = null;
    }

    /** Updates branch BRANCHNAME with commit COMMITHASH. */
    public void updateBranch(String branchName, String commitHash) {
        if (!branchExists(branchName)) {
            _branches.add(new Branch(branchName, commitHash));
        } else {
            for (Branch b : _branches) {
                if (b.getName().equals(branchName)) {
                    b.setCommitHash(commitHash);
                }
            }
        }

    }

    /** Returns true iff branch BRANCHNAME exists. */
    public boolean branchExists(String branchName) {
        for (Branch b : _branches) {
            if (b.getName().equals(branchName)) {
                return true;
            }
        }
        return false;
    }

    /** Removes branch BRANCHNAME from management. */
    public void removeBranch(String branchName) {
        if (branchExists(branchName)) {
            for (Branch b : _branches) {
                if (b.getName().equals(branchName)) {
                    _branches.remove(b);
                    break;
                }
            }
        }
    }

    /** Sets head commit to COMMITHASH. */
    public void setHead(String commitHash) {
        _head = commitHash;
    }

    /** Returns branch head of branch BRANCHNAME. */
    public String getBranchHeadHash(String branchName) {
        if (!branchExists(branchName)) {
            throw Utils.error("No such branch exists.");
        }
        for (Branch b : _branches) {
            if (b.getName().equals(branchName)) {
                return b.getHead();
            }
        }
        return null;
    }

    /** Returns head commit of currently active branch. */
    public String getHeadCommit() {
        return _head;
    }

    /** Adds file FILENAME to staging for removal. */
    public void addRemoval(String filename) {
        if (_filesToRemove == null) {
            _filesToRemove = new LinkedList<String>();
        }
        boolean alreadyThere = false;
        for (String file : _filesToRemove) {
            if (file.equals(filename)) {
                alreadyThere = true;
                break;
            }
        }
        if (!alreadyThere) {
            _filesToRemove.add(filename);
        }
    }

    /** Deletes file FILENAME from staging for removal. */
    public void deleteFromRemoval(String filename) {
        if (_filesToRemove != null) {
            for (int i = 0; i < _filesToRemove.size(); i++) {
                if (_filesToRemove.get(i).equals(filename)) {
                    _filesToRemove.remove(i);
                }
            }
        }

    }

    /** Returns list of all files that are staged for removal. */
    public List<String> getRemovalFiles() {
        if (_filesToRemove == null) {
            _filesToRemove = new LinkedList<String>();
        }
        return _filesToRemove;
    }

    /** Sets current branch to BRANCH. */
    public void setCurrentBranch(String branch) {
        _currentBranch = branch;
    }

    /** Sets current branch head to HASH. */
    public void setCurrentBranchHead(String hash) {
        updateBranch(_currentBranch, hash);
    }

    /** Returns list of all branches. */
    public List<Branch> getBranches() {
        return _branches;
    }

    /** Returns name of currently active branch. */
    public String getCurrentBranch() {
        return _currentBranch;
    }

    /** Clears list of files that are staged for removal. */
    public void clearRemoval() {
        if (_filesToRemove != null) {
            _filesToRemove.clear();
        }
    }

    /** Returns commit hash of split point when merging
     *  currently active branch with branch BRANCHNAME.
     */
    public String findSplitPoint(String branchName) {
        String commString, splitPoint = null;
        Commit comm;
        Set<String> givenBranchCommits = new TreeSet<String>();
        LinkedList<String> queue = new LinkedList<String>();

        queue.add(getBranchHeadHash(branchName));
        while (queue.size() > 0) {
            commString = queue.poll();
            givenBranchCommits.add(commString);

            comm = Main.deserializeCommit(commString);
            for (int i = 0; i <= 1; i++) {
                if (comm.getParent(i) != null) {
                    queue.add(comm.getParent(i));
                }
            }
        }

        queue.add(getHeadCommit());
        while (queue.size() > 0) {
            commString = queue.poll();
            if (givenBranchCommits.contains(commString)) {
                splitPoint = commString;
                break;
            }

            comm = Main.deserializeCommit(commString);
            for (int i = 0; i <= 1; i++) {
                if (comm.getParent(i) != null) {
                    queue.add(comm.getParent(i));
                }
            }
        }
        return splitPoint;
    }

    /** Sets output flag. */
    public void setOutput() {
        _output = true;
    }

    /** Resets output flag. */
    public void resetOutput() {
        _output = false;
    }

    /** Returns whether something had been output already. */
    public boolean hasOutput() {
        return _output;
    }

    /** Contains all remote directories. */
    private Map<String, File> _remoteDirs;

    /** Adds directory REMOTEDIR as new remote directory with name
     *  REMOTENAME.
     */
    public void addRemoteDir(String remoteName, String remoteDir) {
        if (_remoteDirs == null) {
            _remoteDirs = new HashMap<String, File>();
        }
        if (_remoteDirs.containsKey(remoteName)) {
            throw Utils.error("A remote with that name already exists.");
        }
        remoteDir.replace('/', File.separatorChar);
        _remoteDirs.put(remoteName, new File(remoteDir));
    }

    /** Removes remote directory REMOTENAME.
     */
    public void rmRemoteDir(String remoteName) {
        if (_remoteDirs == null || !_remoteDirs.containsKey(remoteName)) {
            throw Utils.error("A remote with that name does not exist.");
        }
        _remoteDirs.remove(remoteName);
    }

    /** Returns directory of remote directory REMOTENAME. */
    public File getRemoteDir(String remoteName) {
        return _remoteDirs.get(remoteName);
    }

}
