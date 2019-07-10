package org.nypl.simplified.app.helpstack;

import android.content.res.Resources;
import android.os.Bundle;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.NavigationDrawerActivity;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;

/**
 * The activity that shows Helpstack's main activity
 */

public final class HelpActivity extends NavigationDrawerActivity {

  /**
   * Construct help activity
   */

  public HelpActivity() {

  }

  @Override
  protected String navigationDrawerGetActivityTitle(final Resources resources) {
    return resources.getString(R.string.help);
  }

  @Override
  protected boolean navigationDrawerShouldShowIndicator() {
    return true;
  }

  @Override
  protected void onCreate(final @Nullable Bundle state) {
    super.onCreate(state);

    final HelpstackType helpstack = Simplified.getServices().getHelpStack();
    if (helpstack != null) {
      helpstack.show(HelpActivity.this);
      this.finish();
    }
  }

}
