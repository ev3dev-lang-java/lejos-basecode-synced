/***************************************************************************************************
 * Copyright (c) 2016 Rüdiger Herrmann
 * All rights reserved. This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Rüdiger Herrmann - initial API and implementation
 **************************************************************************************************/

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit.Type;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class DiffLearningTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private Git git;

  @Before
  public void setUp() throws GitAPIException {
    git = Git.init().setDirectory( tempFolder.getRoot() ).call();
  }

  @After
  public void tearDown() {
    git.getRepository().close();
  }
  
  @Test
  public void simplestDiffCommand() throws Exception {
    writeFile( "file.txt", "existing line\n" );
    git.add().addFilepattern( "file.txt" ).call();
    writeFile( "file.txt", "existing line\nadded line" );
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    List<DiffEntry> diffEntries = git.diff().setOutputStream( outputStream ).call();

    assertTrue( outputStream.toByteArray().length > 0 );
    assertEquals( 1, diffEntries.size() );
    assertEquals( "file.txt", diffEntries.get( 0 ).getOldPath() );
    assertEquals( "file.txt", diffEntries.get( 0 ).getNewPath() );
    assertEquals( MODIFY, diffEntries.get( 0 ).getChangeType() );
  }
  
  @Test
  public void diffCommandWithoutOutput() throws Exception {
    writeFile( "file.txt", "existing line\n" );
    git.add().addFilepattern( "file.txt" ).call();
    writeFile( "file.txt", "existing line\nadded line" );
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    List<DiffEntry> diffEntries = git.diff()
      .setOutputStream( outputStream )
      .setShowNameAndStatusOnly( true )
      .call();
    
    assertEquals( 0, outputStream.toByteArray().length );
    assertEquals( 1, diffEntries.size() );
  }
  
  @Test
  public void diffCommandWithoutChanges() throws Exception {
    List<DiffEntry> diffEntries = git.diff().call();
      
    assertEquals( 0, diffEntries.size() );
  }
  
  @Test
  public void diffUnchanged() throws Exception {
    writeFile( "file.txt", "existing line\n" );
    git.add().addFilepattern( "file.txt" ).call();
    
    List<DiffEntry> diffEntries = git.diff().call();

    assertTrue( diffEntries.isEmpty() );
  }

  @Test
  public void diffTwoCommits() throws Exception {
    RevCommit oldCommit = commitFile( "file.txt", "existing line\n" );
    RevCommit newCommit = commitFile( "file.txt", "existing line\nadded line" );
    
    List<DiffEntry> diffEntries = git.diff()
      .setOldTree( getCanonicalTreeParser( oldCommit ) )
      .setNewTree( getCanonicalTreeParser( newCommit ) )
      .call();

    assertEquals( 1, diffEntries.size() );
    assertEquals( "file.txt", diffEntries.get( 0 ).getOldPath() );
    assertEquals( MODIFY, diffEntries.get( 0 ).getChangeType() );
  }
  
  @Test
  public void diffWithPathFilter() throws Exception {
    RevCommit oldCommit = commitFile( "file.txt", "content" );
    RevCommit newCommit = commitFile( "file.txt", "changed content" );
    
    List<DiffEntry> diffEntries = git.diff()
      .setOldTree( getCanonicalTreeParser( oldCommit ) )
      .setNewTree( getCanonicalTreeParser( newCommit ) )
      .setPathFilter( PathFilter.create( "other-file.txt" ) )
      .call();
    
    assertEquals( 0, diffEntries.size() );
  }
  
  @Test
  public void insertLine() throws Exception {
    RevCommit oldCommit = commitFile( "file.txt", "line1\nline3\n" );
    RevCommit newCommit = commitFile( "file.txt", "line1\nline2\nline3\n" );
    AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( oldCommit );
    AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( newCommit );

    EditList editList = computeEditList( oldTreeIterator, newTreeIterator );

    assertEquals( 1, editList.size() );
    assertEquals( Type.INSERT, editList.get( 0 ).getType() );
    assertEquals( 1, editList.get( 0 ).getBeginA() );
    assertEquals( 1, editList.get( 0 ).getEndA() );
    assertEquals( 1, editList.get( 0 ).getBeginB() );
    assertEquals( 2, editList.get( 0 ).getEndB() );
  }
  
  @Test
  public void deleteLine() throws Exception {
    RevCommit oldCommit = commitFile( "file.txt", "line1\nline2\nline3\n" );
    RevCommit newCommit = commitFile( "file.txt", "line1\nline3\n" );
    AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( oldCommit );
    AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( newCommit );

    EditList editList = computeEditList( oldTreeIterator, newTreeIterator );

    assertEquals( 1, editList.size() );
    assertEquals( Type.DELETE, editList.get( 0 ).getType() );
    assertEquals( 1, editList.get( 0 ).getBeginA() );
    assertEquals( 2, editList.get( 0 ).getEndA() );
    assertEquals( 1, editList.get( 0 ).getBeginB() );
    assertEquals( 1, editList.get( 0 ).getEndB() );
  }
  
  @Test
  public void modifyLine() throws Exception {
    RevCommit oldCommit = commitFile( "file.txt", "line1\nline2\nline3\n" );
    RevCommit newCommit = commitFile( "file.txt", "line1\nline2 and more\nline3\n" );
    AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( oldCommit );
    AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( newCommit );

    EditList editList = computeEditList( oldTreeIterator, newTreeIterator );

    assertEquals( 1, editList.size() );
    assertEquals( Type.REPLACE, editList.get( 0 ).getType() );
    assertEquals( 1, editList.get( 0 ).getBeginA() );
    assertEquals( 2, editList.get( 0 ).getEndA() );
    assertEquals( 1, editList.get( 0 ).getBeginB() );
    assertEquals( 2, editList.get( 0 ).getEndB() );
  }
  
  @Test
  public void diffFormatter() throws Exception {
    RevCommit oldCommit = commitFile( "file.txt", "existing line\n" );
    RevCommit newCommit = commitFile( "file.txt", "existing line\nadded line\n" );
    AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( oldCommit );
    AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( newCommit );

    DiffFormatter diffFormatter = new DiffFormatter( NullOutputStream.INSTANCE );
    diffFormatter.setRepository( git.getRepository() );
    List<DiffEntry> diffEntries = diffFormatter.scan( oldTreeIterator, newTreeIterator );
    diffFormatter.close();
    
    assertEquals( 1, diffEntries.size() );
  }
  
  @Test
  public void diffFormatterWithRename() throws Exception {
    writeFile( "oldfile.txt", "unchanged content" );
    git.add().addFilepattern( "oldfile.txt" ).call();
    RevCommit oldCommit = git.commit().setMessage( "old commit" ).call();
    writeFile( "newfile.txt", "unchanged content" );
    git.rm().addFilepattern( "oldfile.txt" ).call();
    git.add().addFilepattern( "newfile.txt" ).call();
    RevCommit newCommit = git.commit().setMessage( "new commit" ).call();
    AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( oldCommit );
    AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( newCommit );

    DiffFormatter diffFormatter = new DiffFormatter( NullOutputStream.INSTANCE );
    diffFormatter.setRepository( git.getRepository() );
    diffFormatter.setDetectRenames( true );
    List<DiffEntry> diffEntries = diffFormatter.scan( oldTreeIterator, newTreeIterator );
    diffFormatter.close();
    
    assertEquals( 1, diffEntries.size() );
    assertEquals( RENAME, diffEntries.get( 0 ).getChangeType() );
    assertEquals( "oldfile.txt", diffEntries.get( 0 ).getOldPath() );
    assertEquals( "newfile.txt", diffEntries.get( 0 ).getNewPath() );
  }
  
  @Ignore // see https://dev.eclipse.org/mhonarc/lists/jgit-dev/msg03166.html
  @Test
  public void diffFormatterWithIgnoreWhitespace() throws Exception {
    RevCommit oldCommit = commitFile( "file.txt", "first line\n" );
    RevCommit newCommit = commitFile( "file.txt", "first line \n" );
    AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( oldCommit );
    AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( newCommit );

    DiffFormatter diffFormatter = new DiffFormatter( NullOutputStream.INSTANCE );
    diffFormatter.setRepository( git.getRepository() );
    diffFormatter.setDiffComparator( RawTextComparator.WS_IGNORE_ALL );
    List<DiffEntry> diffEntries = diffFormatter.scan( oldTreeIterator, newTreeIterator );
    diffFormatter.close();

    assertEquals( 0, diffEntries.size() );
  }

  private RevCommit commitFile( String name, String content ) throws IOException, GitAPIException {
    writeFile( name, content );
    git.add().addFilepattern( name ).call();
    return git.commit().setMessage( "commit message" ).call();
  }

  private EditList computeEditList( AbstractTreeIterator oldTreeIterator,
                                    AbstractTreeIterator newTreeIterator )
    throws IOException
  {
    try( DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE ) ) {
      diffFormatter.setRepository( git.getRepository() );
      List<DiffEntry> diffEntries = diffFormatter.scan( oldTreeIterator, newTreeIterator );
      FileHeader fileHeader = diffFormatter.toFileHeader( diffEntries.get( 0 ) );
      return fileHeader.toEditList();
    }
  }
  
  private File writeFile( String name, String content ) throws IOException {
    File file = new File( git.getRepository().getWorkTree(), name );
    try( FileOutputStream outputStream = new FileOutputStream( file ) ) {
      outputStream.write( content.getBytes( UTF_8 ) );
    }
    return file;
  }

  private AbstractTreeIterator getCanonicalTreeParser( ObjectId commitId ) throws IOException {
    try( RevWalk walk = new RevWalk( git.getRepository() ) ) {
      RevCommit commit = walk.parseCommit( commitId );
      ObjectId treeId = commit.getTree().getId();
      try( ObjectReader reader = git.getRepository().newObjectReader() ) {
        return new CanonicalTreeParser( null, reader, treeId );
      }
    }
  }
  
}