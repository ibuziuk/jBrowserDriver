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

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.webkit.LoadListenerClient;

class DynamicHttpListener implements LoadListenerClient {
  private final AtomicInteger statusCode;
  private final long settingsId;
  private final AtomicLong frame = new AtomicLong();
  private final Object statusMonitor;
  private static final boolean TRACE = "true".equals(System.getProperty("jbd.trace"));
  private static final Method getStatusMonitor;
  private static final Method startStatusMonitor;
  private static final Method stopStatusMonitor;
  static {
    Method getStatusMonitorTmp = null;
    Method startStatusMonitorTmp = null;
    Method stopStatusMonitorTmp = null;
    try {
      Class statusMonitorClass = DynamicHttpListener.class.getClassLoader().loadClass("com.machinepublishers.jbrowserdriver.StatusMonitor");
      getStatusMonitorTmp = statusMonitorClass.getDeclaredMethod("get", long.class);
      getStatusMonitorTmp.setAccessible(true);
      startStatusMonitorTmp = statusMonitorClass.getDeclaredMethod("startStatusMonitor", String.class);
      startStatusMonitorTmp.setAccessible(true);
      stopStatusMonitorTmp = statusMonitorClass.getDeclaredMethod("stopStatusMonitor", String.class);
      stopStatusMonitorTmp.setAccessible(true);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    getStatusMonitor = getStatusMonitorTmp;
    startStatusMonitor = startStatusMonitorTmp;
    stopStatusMonitor = stopStatusMonitorTmp;
  }

  DynamicHttpListener(AtomicInteger statusCode, long settingsId) {
    this.statusCode = statusCode;
    this.settingsId = settingsId;
    Object statusMonitorTmp = null;
    try {
      statusMonitorTmp = getStatusMonitor.invoke(null, settingsId);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    statusMonitor = statusMonitorTmp;
  }

  private void trace(String label, long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    System.out.println(settingsId
        + "-" + label + "-> " + url
        + " ** {state: " + state
        + ", progress: " + progress
        + ", error: " + errorCode
        + ", contentType: "
        + contentType
        + ", frame: " + frame
        + "}");
  }

  @Override
  public void dispatchResourceLoadEvent(long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    if (TRACE) {
      trace("Rsrc", frame, state, url, contentType, progress, errorCode);
    }
  }

  @Override
  public void dispatchLoadEvent(long frame, int state, String url,
      String contentType, double progress, int errorCode) {
    try {
      if (state == LoadListenerClient.PAGE_STARTED || state == LoadListenerClient.PAGE_REDIRECTED
          || state == LoadListenerClient.DOCUMENT_AVAILABLE) {
        if (this.frame.get() == 0 || this.frame.get() == frame || statusCode.get() == 0) {
          if (url.startsWith("http://") || url.startsWith("https://")) {
            statusCode.set(0);
          }
          this.frame.set(frame);
        }
        startStatusMonitor.invoke(statusMonitor, url);
      } else if ((this.frame.get() == 0 || this.frame.get() == frame)
          && (state == LoadListenerClient.PAGE_FINISHED
              || state == LoadListenerClient.LOAD_FAILED || state == LoadListenerClient.LOAD_STOPPED)) {
        int code = (Integer) stopStatusMonitor.invoke(statusMonitor, url);
        if (statusCode.get() == 0 || url.startsWith("http://") || url.startsWith("https://")) {
          statusCode.set(state == LoadListenerClient.PAGE_FINISHED ? code : 499);
        }
        synchronized (statusCode) {
          statusCode.notifyAll();
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    if (TRACE) {
      trace("Page", frame, state, url, contentType, progress, errorCode);
    }
  }
}
