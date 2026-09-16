package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * `LocalResourceType` specifies the *type*
 * of a resource localized by the `NodeManager`.
 *
 *
 * The *type* can be one of:
 *
 *  *
 * [.FILE] - Regular file i.e. uninterpreted bytes.
 *
 *  *
 * [.ARCHIVE] - Archive, which is automatically unarchived by the
 * `NodeManager`.
 *
 *  *
 * [.PATTERN] - A hybrid between [.ARCHIVE] and [.FILE].
 */
enum class LocalResourceType {
  /**
   * Archive, which is automatically unarchived by the `NodeManager`.
   */
  ARCHIVE,

  /**
   * Regular file i.e. uninterpreted bytes.
   */
  FILE,

  /**
   * A hybrid between archive and file.  Only part of the file is unarchived,
   * and the original file is left in place, but in the same directory as the
   * unarchived part.  The part that is unarchived is determined by pattern
   * in #[LocalResource].  Currently only jars support pattern, all
   * others will be treated like a #[LocalResourceType.ARCHIVE].
   */
  PATTERN
}