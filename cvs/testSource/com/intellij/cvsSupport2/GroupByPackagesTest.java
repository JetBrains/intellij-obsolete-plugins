package com.intellij.cvsSupport2;

import com.intellij.openapi.vcs.update.GroupByPackages;
import junit.framework.TestCase;

import java.io.File;
import java.util.*;

/**
 * author: lesya
 */
public class GroupByPackagesTest extends TestCase{
  private static final File ROOT = new File("r/root");
  private static final File ROOT1 = new File(ROOT, "root1");
  private static final File ROOT2 = new File(ROOT, "root2");
  private static final File DIR11 = new File(ROOT1, "dir1");
  private static final File DIR12 = new File(ROOT1, "dir2");
  private static final File DIR21 = new File(ROOT2, "dir1");
  private static final File DIR22 = new File(ROOT2, "dir2");
  private static final File FILE111 = new File(DIR11, "file1.txt");
  private static final File FILE112 = new File(DIR11, "file2.txt");
  private static final File FILE121 = new File(DIR12, "file1.txt");
  private static final File FILE122 = new File(DIR12, "file2.txt");
  private static final File FILE211 = new File(DIR21, "file1.txt");
  private static final File FILE212 = new File(DIR21, "file2.txt");
  private static final File FILE221 = new File(DIR22, "file1.txt");
  private static final File FILE222 = new File(DIR22, "file2.txt");

  public void test(){
    ArrayList<File> files = new ArrayList<>();

    files.add(FILE111);
    files.add(FILE112);
    files.add(FILE121);
    files.add(FILE122);
    files.add(FILE211);
    files.add(FILE212);
    files.add(FILE221);
    files.add(FILE222);

    GroupByPackages groupByPackages = new GroupByPackages(files);

    compareCollections(new File[]{ROOT}, groupByPackages.getRoots());
    compareCollections(new File[]{ROOT1, ROOT2}, groupByPackages.getChildren(ROOT));
    compareCollections(new File[]{DIR11, DIR12}, groupByPackages.getChildren(ROOT1));
    compareCollections(new File[]{DIR21, DIR22}, groupByPackages.getChildren(ROOT2));
    compareCollections(new File[]{FILE111, FILE112}, groupByPackages.getChildren(DIR11));
    compareCollections(new File[]{FILE121, FILE122}, groupByPackages.getChildren(DIR12));
    compareCollections(new File[]{FILE211, FILE212}, groupByPackages.getChildren(DIR21));
    compareCollections(new File[]{FILE221, FILE222}, groupByPackages.getChildren(DIR22));
  }

  public void testGroupForOneFile(){
    GroupByPackages groupByPackages = new GroupByPackages(Collections.singleton(FILE111));
    compareCollections(new File[]{FILE111}, groupByPackages.getRoots());
  }

  private static void compareCollections(File[] expected, Collection<File> actual){
    assertEquals(new HashSet<>(Arrays.asList(expected)), new HashSet<>(actual));
  }
}
