import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.File;

public class CheckoutLeJOSCommonsRepository {

    public static void main(String[] args){

        try {

            final File tempLeJOS_commons = new File("../temp-lejos-commons");
            Git git = Git.cloneRepository()
                    .setURI("https://github.com/ev3dev-lang-java/lejos-commons.git")
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
