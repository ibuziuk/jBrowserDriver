/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Pause;
import com.machinepublishers.jbrowserdriver.Util.Sync;

class Navigation implements org.openqa.selenium.WebDriver.Navigation {
  private final AtomicReference<JBrowserDriver> driver;
  private final AtomicReference<JavaFxObject> view;
  private final long settingsId;

  Navigation(final AtomicReference<JBrowserDriver> driver,
      final AtomicReference<JavaFxObject> view, final long settingsId) {
    this.driver = driver;
    this.view = view;
    this.settingsId = settingsId;
  }

  @Override
  public void back() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      public Object perform() {
        try {
          view.get().call("getEngine").call("getHistory").call("go", -1);
        } catch (IndexOutOfBoundsException e) {
          Logs.exception(e);
        }
        return null;
      }
    }, settingsId);
  }

  @Override
  public void forward() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      public Object perform() {
        try {
          view.get().call("getEngine").call("getHistory").call("go", 1);
        } catch (IndexOutOfBoundsException e) {
          Logs.exception(e);
        }
        return null;
      }
    }, settingsId);
  }

  @Override
  public void refresh() {
    Util.exec(Pause.SHORT, new Sync<Object>() {
      public Object perform() {
        view.get().call("getEngine").call("reload");
        return null;
      }
    }, settingsId);
  }

  @Override
  public void to(String url) {
    driver.get().get(url);
  }

  @Override
  public void to(URL url) {
    driver.get().get(url.toExternalForm());
  }

}
