package com.screenpulse.healthshortcut;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One-tap launcher to the OEM's original usage dashboard. No usage metrics are synthesized. */
public final class MainActivity extends Activity {
  private static final String PREFS = "health_shortcut";
  private static final String TARGET = "activity";
  private static final String ACTION_WELLBEING = "android.settings.DIGITAL_WELLBEING_SETTINGS";
  private static final int NAVY = Color.rgb(22, 40, 54);
  private static final int TEAL = Color.rgb(3, 148, 157);
  private static final int SUBTLE = Color.rgb(100, 119, 129);
  private static final String[] PACKAGES = {
      "com.android.settings", "com.vivo.settings", "com.vivo.healthy", "com.vivo.screenhealth",
      "com.vivo.usagestats", "com.vivo.childmode", "com.google.android.apps.wellbeing"
  };
  private final List<TargetActivity> candidates = new ArrayList<>();
  private boolean autoAttempted = false;

  private static final class TargetActivity {
    final String pkg;
    final String clazz;
    final String label;
    final int rank;
    TargetActivity(String pkg, String clazz, String label, int rank) {
      this.pkg = pkg; this.clazz = clazz; this.label = label; this.rank = rank;
    }
    String key() { return pkg + "/" + clazz; }
    @Override public String toString() { return label + "  ·  " + clazz; }
  }

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    scan();
    buildUi();
    String saved = getPreferences().getString(TARGET, null);
    if (saved != null && !saved.isEmpty()) {
      autoAttempted = true;
      if (!launchComponent(saved)) getPreferences().edit().remove(TARGET).apply();
    } else {
      // Direct launch only when the device exposes a clearly named, exported OEM screen.
      if (candidates.size() > 0 && candidates.get(0).rank >= 115
          && (candidates.size() == 1 || candidates.get(1).rank < 115)) {
        autoAttempted = true;
        if (launchComponent(candidates.get(0).key())) {
          getPreferences().edit().putString(TARGET, candidates.get(0).key()).apply();
        }
      }
    }
  }

  private SharedPreferences getPreferences() { return getSharedPreferences(PREFS, MODE_PRIVATE); }

  private void scan() {
    candidates.clear();
    PackageManager pm = getPackageManager();
    Map<String, TargetActivity> found = new LinkedHashMap<>();
    for (String pkg : PACKAGES) {
      try {
        PackageInfo info;
        if (Build.VERSION.SDK_INT >= 33) {
          info = pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES));
        } else {
          //noinspection deprecation
          info = pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
        }
        if (info.activities == null) continue;
        for (ActivityInfo a : info.activities) {
          if (!a.exported || !a.enabled) continue;
          String label;
          try { label = a.loadLabel(pm).toString(); } catch (Exception e) { label = a.name; }
          int score = score(label, a.name, pkg);
          if (score > 0) found.put(pkg + "/" + a.name, new TargetActivity(pkg, a.name, label, score));
        }
      } catch (Exception ignored) { /* Unavailable package on this model is expected. */ }
    }
    candidates.addAll(found.values());
    Collections.sort(candidates, (a, b) -> Integer.compare(b.rank, a.rank));
  }

  private static int score(String label, String className, String pkg) {
    String l = label.toLowerCase(Locale.ROOT);
    String c = className.toLowerCase(Locale.ROOT);
    if (l.contains("健康使用设备")) return 150;
    if (l.contains("使用统计") || l.contains("屏幕使用时间")) return 135;
    if (l.contains("数字健康") || l.contains("数字健康与家长控制") || l.contains("digital wellbeing")) return 125;
    if (c.contains("digitalwellbeing") || c.contains("healthusage") || c.contains("screenusage") || c.contains("screenonusage") || c.contains("screenused")) return 110;
    if (c.contains("usagestats") || c.contains("screentime") || c.contains("timemanagement")) return 90;
    if (c.contains("wellbeing") || c.contains("deviceusage")) return 85;
    if (l.contains("应用使用情况") || l.contains("用机时间") || l.contains("屏幕时间")) return 75;
    return 0;
  }

  private boolean launchComponent(String flattened) {
    try {
      ComponentName component = ComponentName.unflattenFromString(flattened);
      if (component == null) return false;
      Intent intent = new Intent(Intent.ACTION_MAIN);
      intent.setComponent(component);
      startActivity(intent);
      return true;
    } catch (SecurityException | android.content.ActivityNotFoundException e) {
      Toast.makeText(this, "此入口被系统限制，试试其他入口", Toast.LENGTH_LONG).show();
      return false;
    } catch (Exception e) {
      Toast.makeText(this, "该入口无法打开", Toast.LENGTH_LONG).show();
      return false;
    }
  }

  private void openBySettingsAction() {
    try { startActivity(new Intent(ACTION_WELLBEING)); }
    catch (Exception e) {
      Toast.makeText(this, "手机不支持公开的数字健康跳转接口", Toast.LENGTH_SHORT).show();
      openSystemSettings();
    }
  }

  private void openSystemSettings() {
    try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    catch (Exception e) { Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show(); }
  }

  private void chooseTarget() {
    scan();
    if (candidates.isEmpty()) {
      new AlertDialog.Builder(this)
        .setTitle("未找到公开的直达入口")
        .setMessage("系统可能隐藏了“健康使用设备”的页面。可先通过设置 → 健康使用设备进入，或使用 vivo 官方桌面屏幕使用时间原子组件。\n\n这个工具不会修改或伪造系统使用时长。")
        .setNegativeButton("取消", null)
        .setPositiveButton("打开设置", (d,w) -> openSystemSettings()).show();
      return;
    }
    String[] choices = new String[Math.min(candidates.size(), 16)];
    for (int i=0; i<choices.length; i++) choices[i]=candidates.get(i).label + "\n" + candidates.get(i).clazz;
    new AlertDialog.Builder(this)
      .setTitle("选择系统入口 · 点击测试")
      .setItems(choices, (d,which) -> {
        TargetActivity a=candidates.get(which);
        if (launchComponent(a.key())) {
          getPreferences().edit().putString(TARGET, a.key()).apply();
          Toast.makeText(this,"已记录入口，下次点图标直接跳转；返回本页可更换",Toast.LENGTH_LONG).show();
        }
      })
      .setNegativeButton("取消", null)
      .show();
  }

  private int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
  private GradientDrawable shape(int color, int radius) {
    GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
  }
  private TextView text(String str, float sp, int color, boolean bold) {
    TextView t=new TextView(this); t.setText(str); t.setTextSize(sp); t.setTextColor(color);
    t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL); return t;
  }
  private void spacer(LinearLayout l,int height) { View v=new View(this); l.addView(v,new LinearLayout.LayoutParams(1,dp(height))); }
  private Button button(String caption, boolean primary, View.OnClickListener action) {
    Button b=new Button(this); b.setText(caption); b.setTextSize(16); b.setAllCaps(false);
    b.setTextColor(primary?Color.WHITE:NAVY); b.setBackground(shape(primary?TEAL:Color.WHITE,17));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(57));lp.topMargin=dp(11);
    b.setElevation(dp(2)); b.setOnClickListener(action); b.setLayoutParams(lp); return b;
  }
  private void buildUi() {
    ScrollView scroller=new ScrollView(this);scroller.setFillViewport(true);scroller.setBackgroundColor(0xFFF6F8F7);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(44),dp(24),dp(30));
    scroller.addView(root);
    TextView brand=text("SCREEN / SHORTCUT",12,TEAL,true);brand.setLetterSpacing(.25f);root.addView(brand);
    spacer(root,10);
    root.addView(text("健康使用设备",32,NAVY,true));
    spacer(root,7);
    root.addView(text("一键进入手机系统自己的使用统计，不再重复计算",14,SUBTLE,false));
    spacer(root,27);
    LinearLayout intro=new LinearLayout(this);intro.setOrientation(LinearLayout.VERTICAL);intro.setPadding(dp(21),dp(22),dp(21),dp(21));
    intro.setBackground(shape(0xFFE0F4F3,24));
    intro.addView(text("系统原始数据 · 直接查看",21,NAVY,true));
    spacer(intro,9);
    intro.addView(text("这个应用不记录、修改或同步任何屏幕时长。它只负责打开 vivo / Android 系统中现有的统计页面。",14,SUBTLE,false));
    root.addView(intro);
    spacer(root,20);
    root.addView(button("打开健康使用设备",true,v -> {
      String target=getPreferences().getString(TARGET,null);
      if (target!=null && launchComponent(target)) return;
      chooseTarget();
    }));
    root.addView(button("查找并选择系统入口",false,v -> chooseTarget()));
    root.addView(button("使用标准数字健康跳转",false,v -> openBySettingsAction()));
    root.addView(button("打开手机设置",false,v -> openSystemSettings()));
    spacer(root,22);
    root.addView(text("自动找到的候选页面：" + candidates.size() + " 个",14,NAVY,true));
    spacer(root,8);
    root.addView(text("首次可选择能正常打开的页面，以后从桌面点“健康使用设备入口”即可直达。部分 vivo 系统页面未开放给第三方应用，遇到限制请使用系统原子组件。",13,SUBTLE,false));
    spacer(root,22);
    TextView clear=text("清除已记住的跳转目标",13,TEAL,false);clear.setGravity(Gravity.CENTER);
    clear.setPadding(0,dp(8),0,dp(8));
    clear.setOnClickListener(v -> {getPreferences().edit().remove(TARGET).apply(); Toast.makeText(this,"已清除，下次将重新检测",Toast.LENGTH_SHORT).show();});
    root.addView(clear);
    setContentView(scroller);
  }
}