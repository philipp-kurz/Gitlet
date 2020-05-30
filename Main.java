package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Philipp
 */
public class Main {

    /** Path to Management file. */
    private static final String MGMT_FILE = "Management";

    /** Path to .gitlet directory. */
    private static final String GITLET_DIR = ".gitlet/";

    /** Path to blob directory within .gitlet. */
    private static final String BLOB_DIR = GITLET_DIR + "blobs/";

    /** Path to commit directory within .gitlet. */
    private static final String COMMIT_DIR = GITLET_DIR + "commits/";

    /** Path to staging directory within .gitlet. */
    private static final String STAGING_DIR = GITLET_DIR + "staging/";

    /** Default branch name. */
    private static final String MASTER_BRANCH = "master";

    /** Static method that checks if no .gitlet directory exists.
     *  If directory exists already, method throws an error.
     */
    private static void checkForGitlet() {
        if (!Files.exists(Paths.get(".gitlet"))) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
    }

    /** Static method that checks whether array ARGS has length LENGTH.
     *  Throws an error if it doesnt.
     */
    private static void checkOperandLength(String[] args, int length) {
        if (args.length != length) {
            throw Utils.error("Incorrect operands.");
        }
    }

    /** Static method that checks whether file with the name
     *  FILENAME already exists.
     */
    private static void checkFileExistence(String filename) {
        if (!Files.exists(Paths.get(filename))) {
            throw Utils.error("File does not exist.");
        }
    }

    /** Static method that returns file object for a file
     *  in the .gitlet directory and the filename FILENAME.
     */
    private static File getGitletFile(String filename) {
        return new File(GITLET_DIR + filename);
    }

    /** Static method that returns file object for a file
     *  in the staging directory and the filename FILENAME.
     */
    private static File getStagedFile(String filename) {
        return new File(STAGING_DIR + filename);
    }

    /** Static method that returns file object for a file
     *  in the commits directory and the filename FILENAME.
     */
    private static File getCommitFile(String filename) {
        if (!Files.exists(Paths.get(COMMIT_DIR))) {
            new File(COMMIT_DIR).mkdir();
        }
        return new File(COMMIT_DIR + filename);
    }

    /** Static method that returns file object for a file
     *  in the blobs directory and the filename FILENAME.
     */
    private static File getBlobFile(String filename) {
        if (!Files.exists(Paths.get(BLOB_DIR))) {
            new File(BLOB_DIR).mkdir();
        }
        return new File(BLOB_DIR + filename);
    }

    /** Static method that serializes MGMT as Management file.
     */
    private static void serializeManagement(Management mgmt) {
        Utils.writeObject(getGitletFile(MGMT_FILE), mgmt);
    }

    /** Static method that deserializes the Management file
     *  and returns it.
     */
    private static Management deserializeManagement() {
        return Utils.readObject(getGitletFile(MGMT_FILE), Management.class);
    }

    /** Static method that serializes commit COMM and stores it
     *  in the commits folder. Returns SHA1 hash (filename).
     */
    private static String serializeCommit(Commit comm) {
        String hash = Utils.sha1(Utils.serialize(comm));
        Utils.writeObject(getCommitFile(hash), comm);
        return hash;
    }

    /** Deserializes commit file HASH and returns Commit object.
     */
    public static Commit deserializeCommit(String hash) {
        if (hash == null) {
            throw Utils.error("No commit with that id exists.");
        }
        return Utils.readObject(getCommitFile(hash), Commit.class);
    }

    /** Static method that copies file FILENAME to the staging directory.
     */
    private static void copyToStaging(String filename) throws IOException {
        Files.copy(new File(filename).toPath(),
                getStagedFile(filename).toPath());
    }

    /** Static method that copies the blob BLOB as new file with name
     *  FILENAME into the working directory.
     */
    private static void copyToWorking(String blob,
                      String fileName) throws IOException {
        Files.copy(getBlobFile(blob).toPath(), new File(fileName).toPath());
    }

    /** Static method that returns full commit it for an
     *  ABBREVIATED commit id.
     */
    private static String findCommitID(String abbreviated) {
        String res = null;
        List<String> allCommits = Utils.plainFilenamesIn(COMMIT_DIR);
        for (String s : allCommits) {
            String substr = s.substring(0, abbreviated.length());
            if (substr.equals(abbreviated)) {
                res = s;
                break;
            }
        }
        return res;
    }

    /** Static method that implements gitlet init functionality.
     *  Creates .gitlet directory and initializes management file.
     */
    private static void init() {
        if (Files.exists(Paths.get(GITLET_DIR))) {
            throw Utils.error("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
        new File(GITLET_DIR).mkdir();
        new File(STAGING_DIR).mkdir();
        Commit initialCommit = Commit.newInitialCommit();
        String hash = serializeCommit(initialCommit);
        Management mgmt = new Management();
        mgmt.updateBranch(MASTER_BRANCH, hash);
        mgmt.setCurrentBranch(MASTER_BRANCH);
        mgmt.setHead(hash);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet add functionality.
     *  Adds file FILENAME to the staging area.
     */
    private static void add(String filename) throws IOException {
        Management mgmt = deserializeManagement();
        Commit head = deserializeCommit(mgmt.getHeadCommit());
        List<FileToShaMapping> previousFiles = head.getTrackedFiles();
        if (getStagedFile(filename).exists()) {
            getStagedFile(filename).delete();
        }
        byte[] fileContent = Utils.readContents(new File(filename));
        String hash = Utils.sha1(fileContent);
        boolean copyToStaging = true;
        if (previousFiles != null) {
            for (FileToShaMapping file : previousFiles) {
                if (file.getFilename().equals(filename)
                        && file.getHash().equals(hash)) {
                    copyToStaging = false;
                    break;
                }
            }
        }
        mgmt.deleteFromRemoval(filename);
        if (copyToStaging) {
            copyToStaging(filename);
        }
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet commit functionality.
     *  Commits changes with commit message MESSAGE that have been
     *  made to staging and removal area.
     *  Creates new commit file and stores it in the respective directory.
     */
    private static void commit(String message) {
        if (message.isEmpty()) {
            throw Utils.error("Please enter a commit message.");
        }
        Management mgmt = deserializeManagement();
        Commit head = deserializeCommit(mgmt.getHeadCommit());
        List<FileToShaMapping> previousFiles = head.getTrackedFiles();
        Commit comm = new Commit(message);
        comm.setTrackedFiles(previousFiles);
        comm.addParent(mgmt.getHeadCommit());
        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_DIR);
        if (stagedFiles.size() == 0
                && (mgmt.getRemovalFiles() == null
                || mgmt.getRemovalFiles().size() == 0)) {
            throw Utils.error("No changes added to the commit.");
        }
        for (String file : stagedFiles) {
            File stagedFile = getStagedFile(file);
            byte[] fileContent = Utils.readContents(stagedFile);
            String hash = Utils.sha1(fileContent);
            Utils.writeContents(getBlobFile(hash), fileContent);
            comm.updateTrackedFile(file, hash);
            stagedFile.delete();
        }
        if (mgmt.getRemovalFiles() != null) {
            for (String file : mgmt.getRemovalFiles()) {
                comm.removeTracking(file);
            }
        }
        mgmt.clearRemoval();
        String hash = serializeCommit(comm);
        mgmt.setHead(hash);
        mgmt.updateBranch(mgmt.getCurrentBranch(), hash);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet remove functionality.
     *  Removes file FILENAME from tracking.
     */
    private static void rm(String filename) {
        Management mgmt = deserializeManagement();
        Commit head = deserializeCommit(mgmt.getHeadCommit());
        List<FileToShaMapping> previousFiles = head.getTrackedFiles();
        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_DIR);

        boolean fileFound = false;
        boolean fileFoundInCommit = false;
        if (previousFiles != null) {
            for (FileToShaMapping file : previousFiles) {
                if (file.getFilename().equals(filename)) {
                    fileFound = true;
                    fileFoundInCommit = true;
                    break;
                }
            }
        }
        if (!fileFound) {
            for (String file : stagedFiles) {
                if (file.equals(filename)) {
                    fileFound = true;
                    break;
                }
            }
        }
        if (!fileFound) {
            throw Utils.error("No reason to remove the file.");
        }

        if (fileFoundInCommit) {
            mgmt.addRemoval(filename);
        }
        if (getStagedFile(filename).exists()) {
            getStagedFile(filename).delete();
        }
        if (new File(filename).exists() && fileFoundInCommit) {
            new File(filename).delete();
        }
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet log functionality.
     *  Outputs a list of all commits of the current branch.
     */
    private static void log() {
        Management mgmt = deserializeManagement();
        String commString = mgmt.getHeadCommit();
        Commit comm = deserializeCommit(commString);
        while (comm != null) {
            String str = "===\ncommit " + commString;
            if (comm.isMergeCommit()) {
                str += "\nMerge: " + comm.getParent(0).substring(0, 7);
                str += " " + comm.getParent(1).substring(0, 7);
            }
            str += "\nDate: " + comm.getDateString() + "\n";
            str += comm.getMessage() + "\n";
            Utils.message(str);
            commString = comm.getParent();
            if (commString != null) {
                comm = deserializeCommit(commString);
            } else {
                comm = null;
            }
        }
    }

    /** Static method that implements gitlet global-log functionality.
     *  Outputs list of all commits ever made in no particular order.
     */
    private static void globalLog() {
        List<String> commitHashes = Utils.plainFilenamesIn(COMMIT_DIR);
        for (String hash : commitHashes) {
            Commit comm = deserializeCommit(hash);
            String str = "===\ncommit " + hash;
            if (comm.isMergeCommit()) {
                str += "\nMerge: " + comm.getParent(0).substring(0, 7);
                str += " " + comm.getParent(1).substring(0, 7);
            }
            str += "\nDate: " + comm.getDateString() + "\n";
            str += comm.getMessage() + "\n";
            Utils.message(str);
        }
    }

    /** Static method that implements gitlet find functionality.
     *  Outputs list of commits that have MESSAGE as their commit message.
     */
    private static void find(String message) {
        boolean foundOne = false;
        List<String> commitHashes = Utils.plainFilenamesIn(COMMIT_DIR);
        if (commitHashes != null) {
            for (String hash : commitHashes) {
                Commit comm = deserializeCommit(hash);
                if (comm.getMessage().equals(message)) {
                    foundOne = true;
                    Utils.message(hash);
                }
            }
        }
        if (!foundOne) {
            Utils.message("Found no commit with that message.");
        }
    }

    /** Static method that outputs Modifications Not Staged For Commit
     *  part of the gitlet status command. Takes lists STAGEDFILES and
     *  REMOVALFILES as well as management object MGMT as argument and
     *  returns list trackedFilesString.
     */
    private static List<String> statusModified(List<String> stagedFiles,
                       List<String> removalFiles, Management mgmt) {
        Utils.message("\n=== Modifications Not Staged For Commit ===");
        String head = mgmt.getHeadCommit();
        Commit comm = deserializeCommit(head);
        List<String> modifiedButNotStaged = new LinkedList<String>();
        List<FileToShaMapping> trackedFiles = comm.getTrackedFiles();
        List<String> trackedFilesString = new LinkedList<String>();
        if (trackedFiles != null) {
            for (FileToShaMapping fileMapping : trackedFiles) {
                String name = fileMapping.getFilename();
                if (!removalFiles.contains(name)) {
                    trackedFilesString.add(name);
                }
                File file = new File(name);
                if (!file.exists()) {
                    if (!removalFiles.contains(name)) {
                        modifiedButNotStaged.add(name + " (deleted)");
                        continue;
                    }
                } else {
                    if (!stagedFiles.contains(name)
                            && !removalFiles.contains(name)) {
                        byte[] fileContent = Utils.readContents(new File(name));
                        String hash = Utils.sha1(fileContent);
                        if (!fileMapping.getHash().equals(hash)) {
                            modifiedButNotStaged.add(name + " (modified)");
                            continue;
                        }
                    }
                }
            }
        }
        for (String s : stagedFiles) {
            File file = new File(s);
            if (!file.exists()) {
                modifiedButNotStaged.add(s + " (deleted)");
                continue;
            } else {
                byte[] fileContent = Utils.readContents(file);
                String hash1 = Utils.sha1(fileContent);
                fileContent = Utils.readContents(getStagedFile(s));
                String hash2 = Utils.sha1(fileContent);
                if (!hash1.equals(hash2)) {
                    modifiedButNotStaged.add(s + " (modified)");
                    continue;
                }
            }
        }
        Collections.sort(modifiedButNotStaged);
        for (String s : modifiedButNotStaged) {
            Utils.message(s);
        }
        return trackedFilesString;
    }

    /** Static method that outputs Untracked Files part of gitlet status
     *  command. Takes lists WORKINGFILES, STAGEDFILES and
     *  TRACKEDFILESSTRING as arguments.
     */
    private static void statusUntracked(List<String> workingFiles,
                List<String> stagedFiles, List<String> trackedFilesString) {
        Utils.message("\n=== Untracked Files ===");
        List<String> untracked = new LinkedList<String>();
        for (String s : workingFiles) {
            if (!stagedFiles.contains(s) && !trackedFilesString.contains(s)) {
                untracked.add(s);
            }
        }
        Collections.sort(untracked);
        for (String s : untracked) {
            Utils.message(s);
        }
    }

    /** Static method that implements gitlet status functionality.
     *  Outputs a table that gives an overview about tracked and
     *  removed files that will be added to the next commit.
     */
    private static void status() {
        Management mgmt = deserializeManagement();
        List<String> workingFiles = Utils.plainFilenamesIn(".");
        List<String> branchList = new LinkedList<String>();
        String currBranch = mgmt.getCurrentBranch();
        for (Branch b : mgmt.getBranches()) {
            branchList.add(b.getName());
        }
        Collections.sort(branchList);
        Utils.message("=== Branches ===");
        for (String branch : branchList) {
            String out = "";
            if (branch.equals(currBranch)) {
                out += "*";
            }
            out += branch;
            Utils.message(out);
        }
        Utils.message("\n=== Staged Files ===");
        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_DIR);
        Collections.sort(stagedFiles);
        for (String file : stagedFiles) {
            Utils.message(file);
        }
        Utils.message("\n=== Removed Files ===");
        List<String> removalFiles = mgmt.getRemovalFiles();
        Collections.sort(removalFiles);
        for (String file : removalFiles) {
            Utils.message(file);
        }
        List<String> trackedFilesString = statusModified(stagedFiles,
                removalFiles, mgmt);
        statusUntracked(workingFiles, stagedFiles, trackedFilesString);
    }

    /** Static helper function that checks out file FILENAME from commit
     *  with commit ID COMMITID.
     */
    private static void checkoutHelper(String commitID,
                       String fileName) throws IOException {
        Commit comm = deserializeCommit(commitID);
        List<FileToShaMapping> tracked = comm.getTrackedFiles();
        FileToShaMapping mapping = null;
        if (tracked != null) {
            for (FileToShaMapping f : tracked) {
                if (f.getFilename().equals(fileName)) {
                    mapping = f;
                    break;
                }
            }
        }
        if (mapping == null) {
            throw Utils.error("File does not exist in that commit.");
        }
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        copyToWorking(mapping.getHash(), mapping.getFilename());
    }

    /** Static method that implements first version of the gitlet checkout.
     *  Checks out file FILENAME from latest commit.
     */
    private static void checkout1(String fileName) throws IOException {
        Management mgmt = deserializeManagement();
        String commString = mgmt.getHeadCommit();
        checkoutHelper(commString, fileName);
    };

    /** Static method that implements second version of gitlet checkout.
     *  Checks out file FILENAME from commit COMMITID.
     */
    private static void checkout2(String commitID,
                      String fileName) throws IOException {
        String blobName = findCommitID(commitID);
        checkoutHelper(blobName, fileName);
    }

    /** Static method that checks if there are any files in the working
     *  directory that are not tracked in the latest commit of branch
     *  BRANCHNAME. Retrieves information from Management object MGMT.
     */
    private static void checkForUntrackedFiles(String branchName,
                                               Management mgmt) {
        if (!mgmt.branchExists(branchName)) {
            throw Utils.error("No such branch exists.");
        } else if (mgmt.getCurrentBranch().equals(branchName)) {
            throw Utils.error("No need to checkout the current branch.");
        }
        Commit currentHead = deserializeCommit(mgmt.getHeadCommit());
        List<FileToShaMapping> currentFiles = currentHead.getTrackedFiles();

        String branchHeadHash = mgmt.getBranchHeadHash(branchName);
        Commit branchHead = deserializeCommit(branchHeadHash);
        List<FileToShaMapping> branchFiles = branchHead.getTrackedFiles();

        for (FileToShaMapping mapping : branchFiles) {
            if (new File(mapping.getFilename()).exists()) {
                boolean found = false;
                for (FileToShaMapping mapping2 : currentFiles) {
                    if (mapping2.getFilename().equals(mapping.getFilename())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw Utils.error("There is an untracked file in the way; "
                            + "delete it or add it first.");
                }
            }
        }
    }

    /** Static method that implements third version of gitlet checkout.
     *  Checks out last state of branch BRANCHNAME.
     */
    private static void checkout3(String branchName) throws IOException {
        Management mgmt = deserializeManagement();
        checkForUntrackedFiles(branchName, mgmt);
        Commit currentHead = deserializeCommit(mgmt.getHeadCommit());
        List<FileToShaMapping> currentFiles = currentHead.getTrackedFiles();

        for (FileToShaMapping mapping : currentFiles) {
            File file = new File(mapping.getFilename());
            if (file.exists()) {
                file.delete();
            }
        }

        String branchHeadHash = mgmt.getBranchHeadHash(branchName);
        Commit branchHead = deserializeCommit(branchHeadHash);
        List<FileToShaMapping> branchFiles = branchHead.getTrackedFiles();

        for (FileToShaMapping f : branchFiles) {
            copyToWorking(f.getHash(), f.getFilename());
        }

        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_DIR);
        for (String file : stagedFiles) {
            File stagedFile = getStagedFile(file);
            stagedFile.delete();
        }
        mgmt.clearRemoval();

        mgmt.setCurrentBranch(branchName);
        mgmt.setHead(branchHeadHash);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet branch functionality.
     *  Creates new branch with name BRANCHNAME.
     */
    private static void branch(String branchName) {
        Management mgmt = deserializeManagement();
        if (mgmt.branchExists(branchName)) {
            throw Utils.error("A branch with that name already exists.");
        }
        String headHash = mgmt.getHeadCommit();
        mgmt.updateBranch(branchName, headHash);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet rm-branch functionality.
     *  Removes branch with the name BRANCHNAME.
     */
    private static void rmBranch(String branchName) {
        Management mgmt = deserializeManagement();
        if (!mgmt.branchExists(branchName)) {
            throw Utils.error("A branch with that name does not exist.");
        } else if (mgmt.getCurrentBranch().equals(branchName)) {
            throw Utils.error("Cannot remove the current branch.");
        }
        mgmt.removeBranch(branchName);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet reset functionality.
     *  Resets working directory back to commit COMMITHASH.
     */
    private static void reset(String commitHash) throws IOException {
        commitHash = findCommitID(commitHash);
        Management mgmt = deserializeManagement();
        if (!getCommitFile(commitHash).exists()) {
            throw Utils.error("No commit with that id exists.");
        }
        Commit currentHead = deserializeCommit(mgmt.getHeadCommit());
        List<FileToShaMapping> currentFiles = currentHead.getTrackedFiles();

        Commit resetCommit = deserializeCommit(commitHash);
        List<FileToShaMapping> resetFiles = resetCommit.getTrackedFiles();

        for (FileToShaMapping mapping : resetFiles) {
            if (new File(mapping.getFilename()).exists()) {
                boolean found = false;
                for (FileToShaMapping mapping2 : currentFiles) {
                    if (mapping2.getFilename().equals(mapping.getFilename())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw Utils.error("There is an untracked file in the way; "
                            + "delete it or add it first.");
                }
            }
        }

        for (FileToShaMapping mapping : currentFiles) {
            File file = new File(mapping.getFilename());
            if (file.exists()) {
                file.delete();
            }
        }

        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_DIR);
        for (String filename : stagedFiles) {
            getStagedFile(filename).delete();
        }

        for (FileToShaMapping f : resetFiles) {
            copyToWorking(f.getHash(), f.getFilename());
        }
        mgmt.setHead(commitHash);
        mgmt.setCurrentBranchHead(commitHash);
        serializeManagement(mgmt);
    }

    /** Static method that checks if there are any uncommitted changes
     *  and throws an error if that is the case. Retrieves information
     *  from the MGMT management object.
     */
    private static void checkForUncommittedChanges(Management mgmt) {
        if (Utils.plainFilenamesIn(STAGING_DIR).size() > 0
            || mgmt.getRemovalFiles().size() > 0) {
            throw Utils.error("You have uncommitted changes.");
        }
    }

    /** Static method that checks if branch BRANCHNAME exists and that throws
     *  an error if that is not the case. Retrieves information from
     *  MGMT management object.
     */
    private static void checkBranchName(String branchName, Management mgmt) {
        if (!mgmt.branchExists(branchName)) {
            throw Utils.error("A branch with that name does not exist.");
        }
    }

    /** Static method that checks if branch BRANCHNAME is identical with
     *  current branch. Retrieves information from MGMT management object.
     */
    private static void checkIdenticalBranch(String branchName,
                                             Management mgmt) {
        if (branchName.equals(mgmt.getCurrentBranch())) {
            throw Utils.error("Cannot merge a branch with itself.");
        }
    }

    /** Static method that performs multiple checks for a merge with branch
     *  BRANCHNAME. Retrieves information from the management object MGMT.
     */
    private static void mergeChecks(String branchName, Management mgmt) {
        checkBranchName(branchName, mgmt);
        checkIdenticalBranch(branchName, mgmt);
        checkForUncommittedChanges(mgmt);
        checkForUntrackedFiles(branchName, mgmt);

    }

    /** Static method that checks whether SPLITPOINT is the same as latest
     *  commit of the current branch or the given branch BRANCHNAME.
     *  Retrieves information from the management object MGMT.
     */
    private static void checksSplitPoint(String splitPoint, String branchName,
                                         Management mgmt) throws IOException {
        String givenLastCommit = mgmt.getBranchHeadHash(branchName);
        if (splitPoint.equals(givenLastCommit)) {
            throw Utils.error("Given branch is an ancestor of the current "
                    + "branch");
        } else if (splitPoint.equals(mgmt.getHeadCommit())) {
            String currBranch = mgmt.getCurrentBranch();
            checkout3(branchName);
            mgmt = deserializeManagement();
            mgmt.setCurrentBranch(currBranch);
            serializeManagement(mgmt);
            throw Utils.error("Current branch fast-forwarded.");
        }
    }

    /** Static method that commits changes made through merge of branch with
     *  the name GIVENNAME and the commit hash GIVENHASH onto the current
     *  branch with the name CURRNAME and the commit hash CURRHASH.
     *  Retrieves information from the management object MGMT.
     */
    private static void mergeCommit(Management mgmt, String givenName,
                String givenHash, String currName, String currHash) {
        Commit head = deserializeCommit(mgmt.getHeadCommit());
        List<FileToShaMapping> previousFiles = head.getTrackedFiles();
        Commit comm = new Commit("Merged " + givenName
                +  " into " + currName + ".");
        comm.setTrackedFiles(previousFiles);
        comm.addParent(currHash);
        comm.addParent(givenHash);
        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_DIR);

        for (String file : stagedFiles) {
            File stagedFile = getStagedFile(file);
            byte[] fileContent = Utils.readContents(stagedFile);
            String hash = Utils.sha1(fileContent);
            Utils.writeContents(getBlobFile(hash), fileContent);
            comm.updateTrackedFile(file, hash);
            stagedFile.delete();
        }

        if (mgmt.getRemovalFiles() != null) {
            for (String file : mgmt.getRemovalFiles()) {
                comm.removeTracking(file);
                new File(file).delete();
            }
        }
        if (stagedFiles.size() == 0 && mgmt.getRemovalFiles().size() == 0
                && !mgmt.hasOutput()) {
            Utils.message("No changes added to the commit.");
            mgmt.setOutput();
        }
        mgmt.clearRemoval();

        String hash = serializeCommit(comm);
        mgmt.setHead(hash);
        mgmt.updateBranch(mgmt.getCurrentBranch(), hash);
    }

    /** Enumeration that represents different actions for files involved
     *  in a merge scenario.
     */
    private enum Action {
        BLANK, CHECKOUT, REMAIN, CONFLICT, REMOVE;
    }

    /** Static method that fills the map ALLFILES with respective actions
     *  from the action enum for a merge from the current branch with head
     *  commit CURRCOMMIT, given branch with head commit GIVENCOMMIT and
     *  split point with commit SPLITCOMMIT. Returns changed map.
     */
    private static Map<String, Action> getMergeActions(
            Map<String, Action> allFiles, Commit currCommit,
            Commit splitCommit, Commit givenCommit) {
        for (String file : allFiles.keySet()) {
            FileToShaMapping curr = currCommit.getMapping(file);
            FileToShaMapping split = splitCommit.getMapping(file);
            FileToShaMapping given = givenCommit.getMapping(file);

            if (curr != null && split != null && given != null
                && curr.equals(split) && given.equals(split)) {
                allFiles.replace(file, Action.REMAIN);
            } else if (curr != null && split != null && given != null
                        && !given.equals(split) && curr.equals(split)) {
                allFiles.replace(file, Action.CHECKOUT);
            } else if (curr != null && split != null && given != null
                        && given.equals(split) && !curr.equals(split))  {
                allFiles.replace(file, Action.REMAIN);
            } else if (split != null && ((curr == null && given == null)
                        || (curr != null && given != null
                        && curr.equals(given) && !curr.equals(split)))) {
                allFiles.replace(file, Action.REMAIN);
            } else if (split == null && given == null && curr != null) {
                allFiles.replace(file, Action.REMAIN);
            } else if (split == null && curr == null && given != null) {
                allFiles.replace(file, Action.CHECKOUT);
            } else if (split != null && given == null
                        && curr != null && curr.equals(split)) {
                allFiles.replace(file, Action.REMOVE);
            } else if (split != null && curr == null
                        && given != null && given.equals(split)) {
                allFiles.replace(file, Action.REMAIN);
            } else if (split != null && curr != null && given != null
                        && !curr.equals(split) && !given.equals(split)
                        && !curr.equals(given)) {
                allFiles.replace(file, Action.CONFLICT);
            } else if (split != null
                        && ((given == null && curr != null
                        && !curr.equals(split))
                        || (curr == null && given != null
                        && !given.equals(split)))) {
                allFiles.replace(file, Action.CONFLICT);
            } else if (split == null && given != null && curr != null
                    && !curr.equals(given)) {
                allFiles.replace(file, Action.CONFLICT);
            } else {
                throw Utils.error("Invalid case.");
            }
        }
        return allFiles;
    }

    /** Static method that performs actions based on the information
     *  in ALLFILES for a merge from the current branch with head commit
     *  CURRHASH and the given branch with commit GIVENHASH. Retrieves
     *  information from MGMT management file.
     */
    private static void performMergeActions(
            Map<String, Action> allFiles, String currHash,
            String givenHash, Management mgmt) throws IOException {
        Commit curr = deserializeCommit(currHash);
        Commit given = deserializeCommit(givenHash);
        FileToShaMapping f = null;
        for (Map.Entry<String, Action> entry : allFiles.entrySet()) {
            switch (entry.getValue()) {
            case BLANK:
            case REMAIN:
                break;
            case CHECKOUT:
                f = given.getMapping(entry.getKey());
                checkout2(givenHash, f.getFilename());
                copyToStaging(f.getFilename());
                break;
            case REMOVE:
                f = curr.getMapping(entry.getKey());
                mgmt.addRemoval(f.getFilename());
                break;
            case CONFLICT:
                String content = "<<<<<<< HEAD\n";
                f = curr.getMapping(entry.getKey());
                if (f != null) {
                    File blob = getBlobFile(f.getHash());
                    content += Utils.readContentsAsString(blob);
                }
                content += "=======\n";
                f = given.getMapping(entry.getKey());
                if (f != null) {
                    File blob = getBlobFile(f.getHash());
                    content += Utils.readContentsAsString(blob);
                }
                content += ">>>>>>>";
                Path file = Paths.get(entry.getKey());
                Files.deleteIfExists(file);
                Files.write(file, Collections.singleton(content));
                copyToStaging(entry.getKey());
                Utils.message("Encountered a merge conflict.");
                mgmt.setOutput();
                break;
            default:
                throw Utils.error("Invalid case.");
            }
        }
    }

    /** Static method that implements gitlet merge functionality.
     *  Performs merge of branch BRANCHNAME onto current branch.
     */
    private static void merge(String branchName) throws IOException {
        Management mgmt = deserializeManagement();
        mergeChecks(branchName, mgmt);

        String splitPoint = mgmt.findSplitPoint(branchName);
        checksSplitPoint(splitPoint, branchName, mgmt);
        Commit spCommit = deserializeCommit(splitPoint);
        List<FileToShaMapping> splitFiles = spCommit.getTrackedFiles();

        String givenHash = mgmt.getBranchHeadHash(branchName);
        Commit givenCommit = deserializeCommit(givenHash);
        List<FileToShaMapping> givenFiles = givenCommit.getTrackedFiles();

        String currHash = mgmt.getHeadCommit();
        Commit currCommit = deserializeCommit(currHash);
        List<FileToShaMapping> currFiles = currCommit.getTrackedFiles();

        Map<String, Action> allFiles = new TreeMap<String, Action>();
        for (FileToShaMapping map : splitFiles) {
            allFiles.put(map.getFilename(), Action.BLANK);
        }
        for (FileToShaMapping map : givenFiles) {
            allFiles.put(map.getFilename(), Action.BLANK);
        }
        for (FileToShaMapping map : currFiles) {
            allFiles.put(map.getFilename(), Action.BLANK);
        }
        allFiles = getMergeActions(allFiles, currCommit, spCommit, givenCommit);
        performMergeActions(allFiles, currHash, givenHash, mgmt);
        mergeCommit(mgmt, branchName, givenHash, mgmt.getCurrentBranch(),
                currHash);
        mgmt.resetOutput();
        serializeManagement(mgmt);
    }


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                throw Utils.error("Please enter a command.");
            }

            switch (args[0]) {
            case "init":
                checkOperandLength(args, 1);
                init();
                break;
            case "add":
                checkForGitlet();
                checkOperandLength(args, 2);
                checkFileExistence(args[1]);
                add(args[1]);
                break;
            case "commit":
                checkForGitlet();
                checkOperandLength(args, 2);
                commit(args[1]);
                break;
            case "rm":
                checkForGitlet();
                checkOperandLength(args, 2);
                rm(args[1]);
                break;
            case "log":
                checkForGitlet();
                log();
                break;
            case "global-log":
                checkForGitlet();
                globalLog();
                break;
            case "find":
                checkForGitlet();
                checkOperandLength(args, 2);
                find(args[1]);
                break;
            case "status":
                checkForGitlet();
                status();
                break;
            default:
                main2(args);
            }
        } catch (GitletException | IOException e) {
            Utils.message(e.getMessage());
        }
    }

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    private static void main2(String... args) {
        try {
            switch (args[0]) {
            case "checkout":
                checkForGitlet();
                if (args.length == 3 && args[1].equals("--")) {
                    checkout1(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    checkout2(args[1], args[3]);
                } else if (args.length == 2) {
                    checkout3(args[1]);
                } else {
                    throw Utils.error("Incorrect operands.");
                }
                break;
            case "branch":
                checkForGitlet();
                checkOperandLength(args, 2);
                branch(args[1]);
                break;
            case "rm-branch":
                checkForGitlet();
                checkOperandLength(args, 2);
                rmBranch(args[1]);
                break;
            case "reset":
                checkForGitlet();
                checkOperandLength(args, 2);
                reset(args[1]);
                break;
            case "merge":
                checkForGitlet();
                checkOperandLength(args, 2);
                merge(args[1]);
                break;
            default:
                mainEC(args);
            }
        } catch (GitletException | IOException e) {
            Utils.message(e.getMessage());
        }
    }

    /** Static method that implements gitlet add-remote functionality.
     *  Adds REMOTENAME with REMOTEDIR to Management.
     */
    private static void addRemote(String remoteName, String remoteDir) {
        Management mgmt = deserializeManagement();
        mgmt.addRemoteDir(remoteName, remoteDir);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet rm-remote functionality.
     *  Removes remote dir REMOTENAME.
     */
    private static void rmRemote(String remoteName) {
        Management mgmt = deserializeManagement();
        mgmt.rmRemoteDir(remoteName);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet push functionality.
     *  Pushes changes onto REMOTENAME REMOTEBRANCHNAME.
     */
    private static void push(String remoteName, String remoteBranchName)
            throws IOException {
        Management mgmt = deserializeManagement();
        File remDir = mgmt.getRemoteDir(remoteName);
        if (!remDir.exists()) {
            throw Utils.error("Remote directory not found.");
        }
        String localHead = mgmt.getHeadCommit();
        Management remoteMgmt = Utils.readObject(Utils.join(remDir,
                "Management"), Management.class);
        if (remoteMgmt.branchExists(remoteBranchName)) {
            String remBranchHead =
                    remoteMgmt.getBranchHeadHash(remoteBranchName);
            Commit comm = deserializeCommit(localHead);
            boolean found = false;
            String commString = localHead;
            while (comm != null) {
                if (commString.equals(remBranchHead)) {
                    found = true;
                    break;
                }
                commString = comm.getParent();
                if (commString != null) {
                    comm = deserializeCommit(commString);
                } else {
                    comm = null;
                }
            }
            if (!found) {
                throw Utils.error("Please pull down remote changes "
                        + "before pushing.");
            }
        }

        File remBlobDir = Utils.join(remDir, "blobs");
        List<String> localBlobs = Utils.plainFilenamesIn(BLOB_DIR);
        for (String blob : localBlobs) {
            Files.copy(getBlobFile(blob).toPath(),
                    Utils.join(remBlobDir, blob).toPath(),
                    REPLACE_EXISTING);
        }
        File remCommitDir = Utils.join(remDir, "commits");
        List<String> localCommits = Utils.plainFilenamesIn(COMMIT_DIR);
        for (String commit : localCommits) {
            Files.copy(getCommitFile(commit).toPath(),
                    Utils.join(remCommitDir, commit).toPath(),
                    REPLACE_EXISTING);
        }

        remoteMgmt.updateBranch(remoteBranchName, localHead);
        if (remoteBranchName.equals(MASTER_BRANCH)) {
            remoteMgmt.setHead(localHead);
        }
        Utils.writeObject(Utils.join(remDir, "Management"), remoteMgmt);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet fetch functionality.
     *  Fetches changes from REMOTENAME REMOTEBRANCHNAME.
     */
    private static void fetch(String remoteName, String remoteBranchName)
            throws IOException {
        Management mgmt = deserializeManagement();
        File remDir = mgmt.getRemoteDir(remoteName);
        if (!remDir.isDirectory()) {
            throw Utils.error("Remote directory not found.");
        }
        Management remoteMgmt = Utils.readObject(Utils.join(remDir,
                "Management"), Management.class);
        if (!remoteMgmt.branchExists(remoteBranchName)) {
            throw Utils.error("That remote does not have that branch.");
        }
        File remBlobDir = Utils.join(remDir, "blobs");
        List<String> remBlobs = Utils.plainFilenamesIn(remBlobDir);
        for (String blob : remBlobs) {
            Files.copy(Utils.join(remBlobDir, blob).toPath(),
                    getBlobFile(blob).toPath(), REPLACE_EXISTING);
        }
        File remCommitDir = Utils.join(remDir, "commits");
        List<String> remCommits = Utils.plainFilenamesIn(remCommitDir);
        for (String commit : remCommits) {
            Files.copy(Utils.join(remCommitDir, commit).toPath(),
                    getCommitFile(commit).toPath(), REPLACE_EXISTING);
        }
        String remBranchHead =
                remoteMgmt.getBranchHeadHash(remoteBranchName);
        mgmt.updateBranch(remoteName + "/" + remoteBranchName, remBranchHead);
        serializeManagement(mgmt);
    }

    /** Static method that implements gitlet fetch functionality.
     *  Fetches changes from REMOTENAME REMOTEBRANCHNAME.
     */
    private static void pull(String remoteName, String remoteBranchName)
            throws IOException {
        fetch(remoteName, remoteBranchName);
        merge(remoteName + "/" + remoteBranchName);
    }

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    private static void mainEC(String... args) {
        try {
            switch (args[0]) {
            case "add-remote":
                checkForGitlet();
                checkOperandLength(args, 3);
                addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                checkForGitlet();
                checkOperandLength(args, 2);
                rmRemote(args[1]);
                break;
            case "push":
                checkForGitlet();
                checkOperandLength(args, 3);
                push(args[1], args[2]);
                break;
            case "fetch":
                checkForGitlet();
                checkOperandLength(args, 3);
                fetch(args[1], args[2]);
                break;
            case "pull":
                checkForGitlet();
                checkOperandLength(args, 3);
                pull(args[1], args[2]);
                break;
            default:
                throw Utils.error("No command with that name exists.");
            }
        } catch (GitletException | IOException e) {
            Utils.message(e.getMessage());
        }
    }

}
