package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** ID.
 * @author Kelvin Pang
 * */

/** Command/Gitlet class.
 *  @author Kelvin Pang
 */

public class Command implements Serializable {

    /** Save information after every commit. */
    public Command() {
        _directory = new File(".gitlet");
        String dir = _directory.getPath();

        _commits = Utils.join(_directory, "commits");
        _blobs = Utils.join(_directory, "blobs");
        _staging = Utils.join(_directory, "staging");
        _removed = new LinkedList<String>();
        HashMap<String, String> m1 = new HashMap<String, String>();
        File branchesFile = Utils.join(dir, "branches");
        if (branchesFile.exists()) {
            _branches = Utils.readObject(branchesFile, m1.getClass());
        } else {
            _branches = m1;
        }
        File headCommitFile = Utils.join(dir, "headCommit");
        if (headCommitFile.exists()) {
            _headCommitSHA = Utils.readObject(headCommitFile, String.class);
        }

        File currBranchFile = Utils.join(dir, "currBranch");
        if (currBranchFile.exists()) {
            _currBranch = Utils.readObject(currBranchFile, String.class);
        }

        LinkedList<String> x = new LinkedList<String>();
        File removedFile = Utils.join(dir, "removed");
        if (removedFile.exists()) {
            _removed = Utils.readObject(removedFile, x.getClass());
        } else {
            _removed = new LinkedList<String>();
        }


    }

    /** Serializing all fields. */
    private void serialize() {
        Utils.writeObject(Utils.join(_directory, "branches"), _branches);
        Utils.writeObject(Utils.join(_directory, "currBranch"), _currBranch);
        Utils.writeObject(Utils.join(_directory, "stage"), _staging);
        Utils.writeObject(Utils.join(_directory, "headCommit"), _headCommitSHA);
        Utils.writeObject(Utils.join(_directory, "removed"), _removed);
    }


    /** Create directory and commit. */
    public void init() {
        if (_directory.exists()) {
            throw Utils.error("A Gitlet version-control system"
                    + " already exists in the current directory.");
        }
        _branches = new HashMap<String, String>();

        _directory.mkdirs();
        _commits.mkdirs();
        _blobs.mkdirs();
        _staging.mkdirs();

        Commit initialCommit = new Commit(
                null, "initial commit", new Date(0));
        byte[] initial = Utils.serialize(initialCommit);
        _headCommitSHA = Utils.sha1(initial);
        File commit2 = Utils.join(".gitlet/commits", _headCommitSHA);
        Utils.writeContents(commit2, initial);
        _currBranch = "master";
        _branches.put("master", _headCommitSHA);
        serialize();
    }

    /** Add a file.
     * @param filename name of the file.
     * */
    public void add(String filename) {
        File addedFile = new File(filename);
        if (!addedFile.exists()) {
            throw Utils.error("File does not exist.");
        }
        byte[] fileContent = Utils.readContents(addedFile);
        String sHA1 = Utils.sha1(fileContent);
        File stagedFile = Utils.join(_staging, filename);

        Commit curCom = Utils.
                readObject(Utils.join(_commits, _headCommitSHA), Commit.class);
        if (curCom.gettrackedFiles().containsKey(filename)
                && curCom.gettrackedFiles().get(filename).equals(sHA1)) {
            addedFile.delete();
        } else {
            Utils.writeContents(stagedFile, fileContent);
        }

        for (int i = 0; i < _removed.size(); i++) {
            if (_removed.get(i).equals(filename)) {
                _removed.remove(i);
                break;
            }
        }

        serialize();
    }

    /** Remove a file.
     * @param filename name of the file.
     * */
    public void remove(String filename) {
        Commit com = Utils.readObject(Utils.join(_commits, _headCommitSHA),
                Commit.class);
        File stagedFile = Utils.join(_staging, filename);
        if (!stagedFile.exists() && !com.gettrackedFiles().
                containsKey(filename)) {
            throw Utils.error("No reason to remove the file.");
        }
        if (stagedFile.exists()) {
            stagedFile.delete();
        }

        if (com.gettrackedFiles().containsKey(filename)) {
            _removed.add(filename);
            com.gettrackedFiles().remove(filename);
            File workingFile = new File(filename);
            if (workingFile.exists()) {
                workingFile.delete();
            }
        }
        serialize();
    }

    /** Commit a message.
     * @param commitmessage message.
     * */
    public void find(String commitmessage) {
        boolean found = false;
        for (String name : Utils.plainFilenamesIn(_commits)) {
            Commit com
                    = Utils.readObject(Utils.join(_commits, name),
                    Commit.class);
            if (com.getmessage().equals(commitmessage)) {
                Utils.message(name);
                found = true;
            }
        }
        if (!found) {
            throw Utils.error("Found no commit with that message.");
        }
    }

    /** This is the commit method.
     *  @param message the message.
     * */
    public void commit(String message) {
        if (message == null || message.length() == 0) {
            throw Utils.error("Please enter a commit message.");
        }
        List<String> stagedFiles = Utils.plainFilenamesIn(_staging);
        if (stagedFiles.size() == 0 && _removed != null
                && _removed.size() == 0) {
            throw Utils.error("No changes added to the commit.");
        }

        Commit oldHead = Utils.readObject(Utils.join(_commits,
                _headCommitSHA), Commit.class);
        Commit newCommit = new Commit(_headCommitSHA, message);
        newCommit.copyTrackedFiles(oldHead);

        for (String fileName : Utils.plainFilenamesIn(_staging)) {
            byte[] fileContent
                    = Utils.readContents(Utils.join(_staging, fileName));
            String fileHash = Utils.sha1(fileContent);
            if (!newCommit.gettrackedFiles().containsKey(fileName)) {
                newCommit.gettrackedFiles().put(fileName, fileHash);
                File stage = Utils.join(_staging, fileName);
                if (stage.exists()) {
                    stage.delete();
                }
            } else {
                String oldHash = oldHead.gettrackedFiles().get(fileName);
                if (!oldHash.equals(fileHash)) {
                    newCommit.gettrackedFiles().replace(fileName, oldHash,
                            fileHash);
                }
                File stage = Utils.join(_staging, fileName);
                if (stage.exists()) {
                    stage.delete();
                }
            }
            File blob = Utils.join(_blobs, fileHash);
            if (blob.exists()) {
                blob.delete();
            }
            Utils.writeContents(blob, fileContent);
        }
        for (String fileToBeRemoved : _removed) {
            if (newCommit.gettrackedFiles().containsKey(fileToBeRemoved)) {
                newCommit.gettrackedFiles().remove(fileToBeRemoved);
            }
        }

        _removed.clear();

        newCommit.setparent1(_headCommitSHA);
        byte[] newCom = Utils.serialize(newCommit);
        _headCommitSHA = Utils.sha1(newCom);
        File newFile = Utils.join(".gitlet/commits", _headCommitSHA);
        Utils.writeContents(newFile, newCom);
        _branches.replace(_currBranch, _headCommitSHA);
        serialize();
    }

    /** Log feature. */
    public void log() {
        Commit headCom = Utils.readObject(Utils.join(_commits,
                _headCommitSHA), Commit.class);
        String temp = _headCommitSHA;
        System.out.println("===");
        System.out.println("commit " + temp);
        System.out.println("Date: " + headCom.getdate());
        System.out.println(headCom.getmessage());
        System.out.println();
        temp = headCom.getparent1();
        while (temp != null) {
            headCom = Utils.readObject(Utils.join(_commits, temp),
                    Commit.class);
            System.out.println("===");
            System.out.println("commit " + temp);
            System.out.println("Date: " + headCom.getdate());
            System.out.println(headCom.getmessage());
            System.out.println();
            temp = headCom.getparent1();
        }
    }

    /** Log feature for everything. */
    public void globallog() {
        for (String name : Utils.plainFilenamesIn(_commits)) {
            Commit com = Utils.readObject(Utils.join(_commits, name),
                    Commit.class);
            Utils.message("===");
            Utils.message("commit " + name);
            Utils.message("Date: " + com.getdate());
            Utils.message(com.getmessage());
            Utils.message("");
        }
    }

    /** Checkout feature.
     *  @param filename name of file.
     * */
    public void checkout(String filename) {
        Commit commitFile = Utils.
                readObject(Utils.join(_commits, _headCommitSHA), Commit.class);
        String fileSHA = commitFile.gettrackedFiles().get(filename);
        File checkout = new File(filename);
        byte[] fileContent = Utils.readContents(Utils.join(_blobs, fileSHA));
        Utils.writeContents(checkout, fileContent);
        if (!checkout.exists()) {
            throw Utils.error("File does not exist in that commit.");
        }
        serialize();
    }

    /** Extended checkout feature.
     * @param commit name of commit.
     * @param filename name of file.
     * */
    public void checkout2(String commit, String filename) {
        int length = commit.length();
        if (length != Utils.UID_LENGTH) {
            for (String hashCode : Utils.plainFilenamesIn(_commits)) {
                if (hashCode.substring(0, length).equals(commit)) {
                    commit = hashCode;
                    break;
                }
            }
        }
        File commitF = Utils.join(_commits, commit);
        if (!commitF.exists()) {
            throw Utils.error("No commit with that id exists.");
        }
        Commit commitFile = Utils.readObject(commitF, Commit.class);
        if (!commitFile.gettrackedFiles().containsKey(filename)) {
            throw Utils.error("File does not exist in that commit.");
        }
        String fileSHA = commitFile.gettrackedFiles().get(filename);
        File checkout2 = new File(filename);
        byte[] fileContent = Utils.readContents(Utils.join(_blobs, fileSHA));
        Utils.writeContents(checkout2, fileContent);

        serialize();
    }

    /** Extended checkout feature.
     * @param branchname name of branch.
     * */
    public void checkout3(String branchname) throws IOException {
        if (!_branches.containsKey(branchname)) {
            throw Utils.error("No such branch exists.");
        }
        if (branchname.equals(_currBranch)) {
            throw Utils.error("No need to checkout the current branch.");
        }
        String commitid = _branches.get(branchname);
        Commit commitReset = Utils.readObject(Utils.join(_commits,
                commitid), Commit.class);
        Commit former = Utils.readObject(Utils.join(_commits,
                _headCommitSHA), Commit.class);
        for (String workingDirect : Utils.plainFilenamesIn(".")) {
            if (!former.gettrackedFiles().containsKey(workingDirect)
                    && commitReset.gettrackedFiles().
                    containsKey(workingDirect)) {
                throw Utils.error("There is an untracked file in"
                        + " the way; delete it or add it first.");
            }
        }

        for (String trackedFile : former.gettrackedFiles().keySet()) {
            new File(trackedFile).delete();
        }
        for (Map.Entry<String, String> newTrackedFile
                : commitReset.gettrackedFiles().entrySet()) {
            Files.copy(Utils.join(_blobs, newTrackedFile.getValue()).toPath(),
                    new File(newTrackedFile.getKey()).toPath());
        }
        _currBranch = branchname;
        _headCommitSHA = commitid;

        for (String file : Utils.plainFilenamesIn(_staging)) {
            Utils.join(_staging, file).delete();
        }
        serialize();
    }

    /** Create a branch feature.
       * @param branchname name of branch.
     * */
    public void branch(String branchname) {
        if (_branches.containsKey(branchname)) {
            throw Utils.error("A branch with that name already exists.");
        }
        String sHA = _headCommitSHA;
        _branches.put(branchname, sHA);
        serialize();
    }

    /** Remove a branch feature.
     * @param branchname name of branch.
     * */
    public void rmbranch(String branchname) {
        if (!_branches.containsKey(branchname)) {
            throw Utils.error("A branch with that name does not exist.");
        }
        if (branchname.equals(_currBranch)) {
            throw Utils.error("Cannot remove the current branch.");
        }
        _branches.remove(branchname);
        serialize();
    }

    /** Reset feature.
     *  @param commitid the commit id.
     * */
    public void reset(String commitid) throws IOException {

        int length = commitid.length();
        if (length != Utils.UID_LENGTH) {
            for (String hashCode : Utils.plainFilenamesIn(_commits)) {
                if (hashCode.substring(0, length).equals(commitid)) {
                    commitid = hashCode;
                    break;
                }
            }
        }
        File x = Utils.join(_commits, commitid);
        if (!x.exists()) {
            throw Utils.error("No commit with that id exists.");
        }
        Commit commitReset = Utils.
                readObject(Utils.join(_commits, commitid), Commit.class);
        Commit former = Utils.
                readObject(Utils.join(_commits, _headCommitSHA), Commit.class);
        for (String workingDirect : Utils.plainFilenamesIn(".")) {
            if (!former.gettrackedFiles().containsKey(workingDirect)
                    && commitReset.gettrackedFiles().
                    containsKey(workingDirect)) {
                throw Utils.error("There is an untracked file in the way; "
                        + "delete it or add it first.");
            }
        }
        for (String trackedFile : former.gettrackedFiles().keySet()) {
            new File(trackedFile).delete();
        }
        for (Map.Entry<String, String> newTrackedFile
                : commitReset.gettrackedFiles().entrySet()) {
            Files.copy(Utils.join(_blobs, newTrackedFile.
                            getValue()).toPath(),
                    new File(newTrackedFile.getKey()).toPath());
        }
        _branches.replace(_currBranch, commitid);
        _headCommitSHA = commitid;

        for (String file : Utils.plainFilenamesIn(_staging)) {
            Utils.join(_staging, file).delete();
        }
        serialize();
    }

    /** Status feature. */
    public void status() {
        ArrayList<String> sortbranches =
                new ArrayList<String>(_branches.keySet());
        Collections.sort(sortbranches);
        Utils.message("=== Branches ===");
        if (sortbranches != null) {
            for (String b : sortbranches) {
                if (_currBranch.equals(b)) {
                    Utils.message("*" + b);
                } else {
                    Utils.message(b);
                }
            }
        }
        Utils.message("\n=== Staged Files ===");
        List<String> stagedFiles = Utils.plainFilenamesIn(_staging);
        if (stagedFiles != null) {
            Collections.sort(stagedFiles);
            for (String stagedFile : stagedFiles) {
                Utils.message(stagedFile);
            }
        }
        Utils.message("\n=== Removed Files ===");
        if (_removed.size() > 0) {
            for (int i = 0; i < _removed.size(); i++) {
                Utils.message(_removed.get(i));
            }
        }
        Utils.message("\n=== Modifications Not Staged For Commit ===");
        Utils.message("\n=== Untracked Files ===");
        Utils.message("");
    }

    /** Merge method.
     * @param branchName name of branch.
     * */
    public void merge(String branchName) {
        if (Utils.plainFilenamesIn(_staging).size() > 0) {
            throw Utils.error("You have uncommitted changes.");
        } else if (!_branches.containsKey(branchName)) {
            throw Utils.error("A branch with that name does not exist.");
        } else if (_currBranch.equals(branchName)) {
            throw Utils.error("Cannot merge a branch with itself.");
        }
        String mergeCommitHash = _branches.get(branchName);
        Commit mergeCommit = Utils.readObject(Utils.join(_commits,
                mergeCommitHash), Commit.class);
        Commit currentCommit = Utils.readObject(Utils.join(_commits,
                _headCommitSHA), Commit.class);
        for (String workingDirect : Utils.plainFilenamesIn(".")) {
            if (!currentCommit.gettrackedFiles().containsKey(workingDirect)
                    && mergeCommit.gettrackedFiles().
                    containsKey(workingDirect)) {
                throw Utils.error("There is an untracked file in the way;"
                        + " delete it or add it first.");
            }
        }
        Commit currentCom = Utils.readObject(Utils.join(_commits,
                (_branches.get(_currBranch))), Commit.class);
        Commit givenCom = Utils.readObject(Utils.join(_commits,
                (_branches.get(branchName))), Commit.class);
        Commit splitpoint;
        String currentComSHA = currentCom.getparent1();
        String givenComSHA = givenCom.getparent1();
        LinkedList<Commit> current = new LinkedList<Commit>();
        LinkedList<Commit> given = new LinkedList<Commit>();
    }

    /** Merge helper method.
     * @param current list of current branch.
     * @param given list of given branch.
     * */
    private void mergeHelper(LinkedList<Commit> current,
                             LinkedList<Commit> given) {
        for (int i = 0; i < current.size(); i++) {
            for (int j = 0; j < given.size(); j++) {
                for (Map.Entry<String, String> currentTracked
                        : current.get(i).gettrackedFiles().entrySet()) {
                    for (Map.Entry<String, String> givenTracked
                            : given.get(i).gettrackedFiles().entrySet()) {
                        return;
                    }

                }
            }
        }
    }

    /** latest commit. */
    private String _headCommitSHA;
    /** current branch. */
    private String _currBranch;
    /** map of branches. */
    private HashMap<String, String> _branches;
    /** list of items to be removed. */
    private LinkedList<String> _removed;
    /** directory for commits. */
    private File _commits;
    /** directory for blobs. */
    private File _blobs;
    /** directory for stage. */
    private File _staging;
    /** gitlet. */
    private File _directory;

}
