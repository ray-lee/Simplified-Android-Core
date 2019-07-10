package org.nypl.simplified.taskrecorder.api

/**
 * A step in a task.
 *
 * @param <E> The precise type of associated error values
 */

data class TaskStep<E>(

  /**
   * A humanly-readable, localized description of the task step.
   */

  val description: String,

  /**
   * A humanly-readable, localized description of the resolution task step.
   */

  var resolution: String = "",

  /**
   * `true` if the task step failed. A failed step is assumed to have failed the task
   * as a whole.
   */

  var failed: Boolean = false,

  /**
   * The error value associated with this step.
   */

  var errorValue: E? = null,

  /**
   * The exception that failed the task, if any.
   */

  var exception: Throwable? = null)
