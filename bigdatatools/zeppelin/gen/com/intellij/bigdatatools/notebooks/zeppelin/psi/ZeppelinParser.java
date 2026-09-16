// This is a generated file. Not intended for manual editing.
package com.intellij.bigdatatools.notebooks.zeppelin.psi;

import com.intellij.bigdatatools.zeppelin.notebook.parser.ZeppelinPsiRemapper;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class ZeppelinParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return notebook_file(b, l + 1);
  }

  /* ********************************************************** */
  // cell_marker cell_magic? source
  public static boolean cell(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cell")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CELL, "<cell>");
    r = cell_marker(b, l + 1);
    r = r && cell_1(b, l + 1);
    r = r && source(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // cell_magic?
  private static boolean cell_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cell_1")) return false;
    cell_magic(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // MAGIC
  public static boolean cell_magic(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cell_magic")) return false;
    if (!nextTokenIs(b, MAGIC)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MAGIC);
    exit_section_(b, m, CELL_MAGIC, r);
    return r;
  }

  /* ********************************************************** */
  // CODE_MARKER | SQL_MARKER | HIVE_MARKER |SCALA_MARKER | MARKDOWN_MARKER | SH_MARKER | RAW_MARKER
  public static boolean cell_marker(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cell_marker")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CELL_MARKER, "<cell marker>");
    //IT WAS ADDED TO SUPPORT custom User Interpreters do not remove it!!!!
    ZeppelinPsiRemapper.remapCellMarker(b);
    r = consumeToken(b, CODE_MARKER);
    if (!r) r = consumeToken(b, SQL_MARKER);
    if (!r) r = consumeToken(b, HIVE_MARKER);
    if (!r) r = consumeToken(b, SCALA_MARKER);
    if (!r) r = consumeToken(b, MARKDOWN_MARKER);
    if (!r) r = consumeToken(b, SH_MARKER);
    if (!r) r = consumeToken(b, RAW_MARKER);
    if (!r) r = consumeToken(b, PYTHON_MARKER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // stem_cell? cell*
  public static boolean notebook(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notebook")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NOTEBOOK, "<notebook>");
    r = notebook_0(b, l + 1);
    r = r && notebook_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // stem_cell?
  private static boolean notebook_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notebook_0")) return false;
    stem_cell(b, l + 1);
    return true;
  }

  // cell*
  private static boolean notebook_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notebook_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!cell(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "notebook_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // notebook
  static boolean notebook_file(PsiBuilder b, int l) {
    return notebook(b, l + 1);
  }

  /* ********************************************************** */
  // CODE_SOURCE| SQL_SOURCE | HIVE_SOURCE | SCALA_SOURCE | MARKDOWN_SOURCE | SH_SOURCE | RAW_SOURCE?
  public static boolean source(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "source")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SOURCE, "<source>");
    //IT WAS ADDED TO SUPPORT custom User Interpreters do not remove it!!!!
    ZeppelinPsiRemapper.remapCellSource(b);
    r = consumeToken(b, CODE_SOURCE);
    if (!r) r = consumeToken(b, SQL_SOURCE);
    if (!r) r = consumeToken(b, HIVE_SOURCE);
    if (!r) r = consumeToken(b, SCALA_SOURCE);
    if (!r) r = consumeToken(b, MARKDOWN_SOURCE);
    if (!r) r = consumeToken(b, SH_SOURCE);
    if (!r) r = consumeToken(b, PYTHON_SOURCE);
    if (!r) r = source_6(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // RAW_SOURCE?
  private static boolean source_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "source_6")) return false;
    consumeToken(b, RAW_SOURCE);
    return true;
  }

  /* ********************************************************** */
  // source
  public static boolean stem_cell(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stem_cell")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STEM_CELL, "<stem cell>");
    r = source(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
