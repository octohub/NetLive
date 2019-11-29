package com.richardlucasapps.netlive.global;

// Adding commit to ensure NetLive is in https://archiveprogram.github.com/.
import android.app.Application;

public class MyApplication extends Application {

  private static MyApplication instance;

  @Override public void onCreate() {
    super.onCreate();
    instance = this;
  }

  public static MyApplication getInstance() {
    return instance;
  }
}
