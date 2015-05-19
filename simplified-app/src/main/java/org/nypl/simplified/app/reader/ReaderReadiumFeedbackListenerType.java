package org.nypl.simplified.app.reader;

/**
 * Functions called via the <tt>host_app_feedback.js</tt> file using the
 * <tt>readium</tt> URI scheme.
 */

public interface ReaderReadiumFeedbackListenerType
{
  /**
   * Called when an exception is raised when trying to dispatch a function.
   */

  void onReadiumFunctionDispatchError(
    Throwable x);

  /**
   * Called on receipt of a <tt>readium:initialize</tt> request.
   */

  void onReadiumFunctionInitialize();

  /**
   * Called when {@link #onReadiumFunctionInitialize()} raises an exception.
   */

  void onReadiumFunctionInitializeError(
    Throwable e);

  /**
   * Called on receipt of a <tt>readium:pagination-changed</tt> request.
   */

  void onReadiumFunctionPaginationChanged(
    ReaderPaginationChangedEvent e);

  /**
   * Called when {@link #onReadiumFunctionPaginationChanged()} raises an
   * exception.
   */

  void onReadiumFunctionPaginationChangedError(
    Throwable e);

  /**
   * Called on receipt of a <tt>readium:settings-applied</tt> request.
   */

  void onReadiumFunctionSettingsApplied();

  /**
   * Called when {@link #onReadiumFunctionSettingsApplied()} raises an
   * exception.
   */

  void onReadiumFunctionSettingsAppliedError(
    Throwable e);

  /**
   * Called when an unknown request is made.
   */

  void onReadiumFunctionUnknown(
    String text);

  /**
   * The status of the media overlay has changed; media is/is not playing.
   */

  void onReadiumMediaOverlayStatusChangedIsPlaying(
    boolean playing);

  /**
   * The status of the media overlay has changed; the overlay is available.
   */

  void onReadiumMediaOverlayStatusChangedIsAvailable(
    boolean available);

  /**
   * Called when {@link #onReadiumMediaOverlayStatusError()} raises an
   * exception.
   */

  void onReadiumMediaOverlayStatusError(
    Throwable e);
}
