import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.File;

public class CheckoutLeJOSRepository {

    public static void main(String[] args){

        try {

            final File tempLeJOS = new File("./tempLeJOS/");
            Git git = Git.cloneRepository()
                    .setURI( "git://git.code.sf.net/p/lejos/ev3/code")
                    .setDirectory(tempLeJOS)
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
