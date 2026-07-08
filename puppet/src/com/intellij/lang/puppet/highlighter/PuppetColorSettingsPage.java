package com.intellij.lang.puppet.highlighter;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

public class PuppetColorSettingsPage implements ColorSettingsPage {

  private static final AttributesDescriptor[] ourDescriptors = {
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.keywords"), PuppetSyntaxHighlighterColors.KEYWORD),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.names"), PuppetSyntaxHighlighterColors.NAME),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.digits"), PuppetSyntaxHighlighterColors.DIGIT),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.double.quoted.string"), PuppetSyntaxHighlighterColors.DQ_STRING),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.single.quoted.string"), PuppetSyntaxHighlighterColors.SQ_STRING),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.regexp"), PuppetSyntaxHighlighterColors.REGEX),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.escape.sequences"), PuppetSyntaxHighlighterColors.ESCAPE_SEQUENCE),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.variables"), PuppetSyntaxHighlighterColors.VARIABLE),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.variable.interpolations"), PuppetSyntaxHighlighterColors.VARIABLE_INTERPOLATION),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.variable.interpolation.tags"), PuppetSyntaxHighlighterColors.VARIABLE_INTERPOLATION_TAGS),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.operators"), PuppetSyntaxHighlighterColors.OPERATION_SIGN),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.resource.references"), PuppetSyntaxHighlighterColors.RESOURCE_REFERENCE),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.heredoc.tags"), PuppetSyntaxHighlighterColors.HEREDOC_TAGS),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.parentheses"), PuppetSyntaxHighlighterColors.PARENTHS),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.braces"), PuppetSyntaxHighlighterColors.BRACES),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.brackets"), PuppetSyntaxHighlighterColors.BRACKETS),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.comments"), PuppetSyntaxHighlighterColors.BLOCK_COMMENT),
    new AttributesDescriptor(PuppetBundle.message("highlighting.colors.bad.character"), PuppetSyntaxHighlighterColors.BAD_CHARACTER),
  };

  @Override
  public @Nullable Icon getIcon() {
    return PuppetFileType.INSTANCE.getIcon();
  }

  @Override
  public @NotNull SyntaxHighlighter getHighlighter() {
    return new PuppetHighlighter(null);
  }

  @Override
  public @NotNull String getDemoText() {
    return """
      #Here's a class sample
      class unix {
        file {
          '/etc/passwd':
            owner => 'root',
            group => 'root',
            mode  => '0644';
          '/etc/shadow':
            owner => 'root',
            group => 'root',
            mode  => '0440';
        }
      }
      /*
      Defined resource type
      */
      define svn_repo($path) {
        exec { "create_repo_${name}":
          command => "/usr/bin/svnadmin create ${path}/${title}",
          unless  => "/bin/test -d ${path}",
        }
        if $require =~ /\\\\wrequire/ {
          Exec["create_repo_${name}"] {
            require +> $require,
          }
        }
      }

      svn_repo { 'puppet':
        path    => '/var/svn',
        require => Package['subversion'],
      }
      #Arithmetical operations
      $one = 1
      $one_thirty = 1.30
      $two = 2.034e-2

      $result = ((( $two + 2) * $one_thirty) + 4 * 5.45) - (6 << ($two + 4)) + (0x800 + -9)""";
  }

  @Override
  public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @Override
  public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
    return ourDescriptors;
  }

  @Override
  public ColorDescriptor @NotNull [] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @Override
  public @NotNull String getDisplayName() {
    return PuppetBundle.message("language.name");
  }
}
