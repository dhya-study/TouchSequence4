package com.example.touchsequence;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.SharedPreferences;
import java.util.ArrayList;

public class TapService extends AccessibilityService {
    static TapService instance;
    private WindowManager wm;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayList<View> overlays=new ArrayList<>();
    private final ArrayList<WindowManager.LayoutParams> params=new ArrayList<>();
    private View controls;
    private boolean running=false;
    private int index=0,finished=0,delay=500,pause=1000,loops=0,sizeDp=36;
    private final int[] sequence={0,1,0,1,0,1,0,1,2};
    @Override protected void onServiceConnected(){super.onServiceConnected();instance=this;wm=(WindowManager)getSystemService(WINDOW_SERVICE);}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){}
    @Override public void onInterrupt(){stop();}
    @Override public void onDestroy(){hideOverlays();instance=null;super.onDestroy();}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable bg(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(40));d.setStroke(dp(1),Color.WHITE);return d;}
    private WindowManager.LayoutParams lp(int w,int h,int x,int y){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.x=x;p.y=y;
        if(android.os.Build.VERSION.SDK_INT>=30)p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        else if(android.os.Build.VERSION.SDK_INT>=28)p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        return p;
    }
    public void showOverlays(){
        hideOverlays();
        SharedPreferences pref=getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
        delay=pref.getInt("delay",500);pause=pref.getInt("pause",1000);loops=pref.getInt("loops",0);sizeDp=pref.getInt("size",36);
        android.util.DisplayMetrics dm=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(dm);
        final int width=dm.widthPixels,height=dm.heightPixels,sz=dp(sizeDp);
        for(int i=0;i<3;i++){
            final int target=i;
            TextView circle=new TextView(this);circle.setGravity(Gravity.CENTER);circle.setText(""+(i+1));circle.setTextColor(Color.WHITE);circle.setTextSize(Math.max(9,sizeDp*0.38f));
            circle.setBackground(bg(new int[]{0xFF2666DA,0xFF009688,0xFFEF6C00}[i]));
            int x=Math.min(width-sz,dp(30+i*(sizeDp+12)));int y=Math.min(height-sz,dp(240));
            WindowManager.LayoutParams p=lp(sz,sz,x,y);
            circle.setOnTouchListener(new View.OnTouchListener(){float startX,startY;int startPx,startPy;
                @Override public boolean onTouch(View v,MotionEvent e){
                    if(running)return true;
                    if(e.getAction()==MotionEvent.ACTION_DOWN){startX=e.getRawX();startY=e.getRawY();startPx=p.x;startPy=p.y;return true;}
                    if(e.getAction()==MotionEvent.ACTION_MOVE){p.x=Math.max(0,Math.min(width-sz,startPx+(int)(e.getRawX()-startX)));p.y=Math.max(0,Math.min(height-sz,startPy+(int)(e.getRawY()-startY)));wm.updateViewLayout(v,p);return true;}
                    return true;
                }
            });
            wm.addView(circle,p);overlays.add(circle);params.add(p);
        }
        LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(5),dp(4),dp(5),dp(4));bar.setBackground(bg(0xDD202020));
        Button action=new Button(this);action.setText("▶ 시작");bar.addView(action);action.setOnClickListener(v->{if(running){stop();action.setText("▶ 시작");}else{start();action.setText("■ 중지");}});
        Button close=new Button(this);close.setText("닫기");bar.addView(close);close.setOnClickListener(v->hideOverlays());
        WindowManager.LayoutParams cp=lp(dp(210),dp(65),Math.max(0,(width-dp(210))/2),Math.max(0,height-dp(65)-dp(110)));wm.addView(bar,cp);controls=bar;
    }
    private void start(){if(overlays.size()!=3)return;running=true;index=0;finished=0;
        for(int i=0;i<overlays.size();i++){
            WindowManager.LayoutParams p=params.get(i);
            p.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            wm.updateViewLayout(overlays.get(i),p);
        }
        handler.postDelayed(this::next,350);
    }
    private void stop(){running=false;handler.removeCallbacksAndMessages(null);
        for(int i=0;i<overlays.size();i++){
            WindowManager.LayoutParams p=params.get(i);p.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            try{wm.updateViewLayout(overlays.get(i),p);}catch(Exception ignored){}
        }
    }
    private void next(){if(!running||overlays.size()!=3)return;
        int id=sequence[index];WindowManager.LayoutParams p=params.get(id);
        float x=p.x+dp(sizeDp)/2f,y=p.y+dp(sizeDp)/2f;
        Path path=new Path();path.moveTo(x,y);
        GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(path,0,55);
        GestureDescription gesture=new GestureDescription.Builder().addStroke(stroke).build();
        boolean sent=dispatchGesture(gesture,new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription d){advance();}
            @Override public void onCancelled(GestureDescription d){stop();}
        },handler);
        if(!sent)stop();
    }
    private void advance(){if(!running)return;index++;
        if(index>=sequence.length){index=0;finished++;if(loops>0&&finished>=loops){stop();return;}handler.postDelayed(this::next,pause);}
        else handler.postDelayed(this::next,delay);
    }
    public void hideOverlays(){stop();for(View v:overlays){try{wm.removeView(v);}catch(Exception ignored){}}overlays.clear();params.clear();
        if(controls!=null){try{wm.removeView(controls);}catch(Exception ignored){}controls=null;}
    }
}
