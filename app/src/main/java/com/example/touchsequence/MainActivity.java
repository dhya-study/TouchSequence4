package com.example.touchsequence;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.InputType;

public class MainActivity extends Activity {
    private EditText delay, pause, loops, size;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(36,40,36,20); root.setBackgroundColor(Color.WHITE);
        TextView heading = new TextView(this); heading.setText("터치 순서 4"); heading.setTextSize(26); heading.setTextColor(Color.BLACK);root.addView(heading);
        info(root,"클릭 순서: ① → ② → ③ → ④ 반복\n\n사용법: 접근성 권한을 켠 뒤 '클릭 위치 표시'를 누르고 원하는 앱을 여세요. 떠 있는 ①②③④ 동그라미를 눌러 이동한 다음 시작을 누르세요.");
        delay=field(root,"각 클릭 후 대기 시간 (밀리초, 예: 500)","500");
        pause=field(root,"한 바퀴(①②③④) 완료 후 대기 (밀리초, 예: 1000)","1000");
        loops=field(root,"전체 반복 횟수 (0 = 무한 반복)","0");
        size=field(root,"표시 원 크기 (dp, 20~80, 기본 36)","36");
        addButton(root,"1. 접근성 서비스 켜기",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addButton(root,"2. 클릭 위치 표시 / 설정 적용",()->{
            getPreferences(MODE_PRIVATE).edit().putInt("delay",num(delay,500,50,60000)).putInt("pause",num(pause,1000,0,600000)).putInt("loops",num(loops,0,0,100000)).putInt("size",num(size,36,20,80)).apply();
            if(TapService.instance!=null) TapService.instance.showOverlays();
            else infoDialog("접근성 설정에서 '터치 순서 4' 서비스를 먼저 활성화하세요.");
        });
        addButton(root,"모든 표시 숨기기 / 중지",()->{if(TapService.instance!=null) TapService.instance.hideOverlays();});
        info(root,"사용하는 앱 또는 게임의 정책을 확인하세요. 일부 게임·보안 화면에서는 자동 터치가 작동하지 않을 수 있습니다. 화면을 끄면 반복이 중단될 수 있습니다.");
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.addView(root);
        // Android 15+ (targetSdk 35) 강제 edge-to-edge 대응: 상태바/내비게이션바 영역만큼 여백 추가
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
    }
    private void info(LinearLayout root,String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.DKGRAY);t.setTextSize(15);t.setPadding(0,18,0,18);root.addView(t);}
    private EditText field(LinearLayout root,String title,String def){info(root,title);EditText e=new EditText(this);e.setSingleLine(true);e.setInputType(InputType.TYPE_CLASS_NUMBER);e.setText(def);root.addView(e);return e;}
    private int num(EditText e,int def,int lo,int hi){try{return Math.min(hi,Math.max(lo,Integer.parseInt(e.getText().toString())));}catch(Exception x){return def;}}
    private void addButton(LinearLayout root,String title,Runnable r){Button b=new Button(this);b.setText(title);root.addView(b);b.setOnClickListener(v->r.run());}
    private void infoDialog(String s){new android.app.AlertDialog.Builder(this).setMessage(s).setPositiveButton("확인",null).show();}
}
