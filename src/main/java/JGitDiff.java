import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

public class JGitDiff {

    public static void main(String[] args) throws Exception {

        File gitWorkDir = new File("../temp-lejos-mirror/");
        Git git = Git.open(gitWorkDir);

        String oldHash = "7eeedaf91385c0ae0cfe9435412160a2d46a1575";

        ObjectId headId = git.getRepository().resolve("HEAD^{tree}");
        ObjectId oldId = git.getRepository().resolve(oldHash + "^{tree}");

        ObjectReader reader = git.getRepository().newObjectReader();

        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        oldTreeIter.reset(reader, oldId);
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(reader, headId);

        List<DiffEntry> diffs= git.diff()
                .setNewTree(newTreeIter)
                .setOldTree(oldTreeIter)
                .call();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter df = new DiffFormatter(out);
        df.setRepository(git.getRepository());

        for(DiffEntry diff : diffs)
        {
            df.format(diff);
            diff.getOldId();
            String diffText = out.toString("UTF-8");
            System.out.println(diffText);
            out.reset();
        }
    }
}
