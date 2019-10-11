package org.nypl.simplified.ui.branding

/**
 * A service that provides a splash screen image.
 */

interface BrandingSplashServiceType {

  /**
   * Provide a splash image resource for the application.
   */

  @androidx.annotation.DrawableRes
  fun splashImageResource(): Int

}