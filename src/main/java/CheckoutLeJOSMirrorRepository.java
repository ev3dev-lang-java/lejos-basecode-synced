import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.File;

public class CheckoutLeJOSMirrorRepository {

    public static void main(String[] args){

        try {

            final File tempLeJOS_commons = new File("../temp-lejos-mirror");
            Git git = Git.cloneRepository()
                    .setURI("https://github.com/ev3dev-lang-java/lejos-ev3-code-mirror.git")
                    .setDirectory(tempLeJOS_commons)
                    .call();
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

}
