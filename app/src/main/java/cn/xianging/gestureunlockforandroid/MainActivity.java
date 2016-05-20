package cn.xianging.gestureunlockforandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.util.LinkedHashSet;

import cn.xianging.gestureunlock.GestureUnlockView;

public class MainActivity extends AppCompatActivity
        implements GestureUnlockView.OnGestureDoneListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        GestureUnlockView mUnlockView = (GestureUnlockView) findViewById(R.id.gesture_unlock_view);
        if (mUnlockView != null) {
            mUnlockView.setOnGestureDoneListener(this);
        }
    }

    @Override
    public boolean isValidGesture(int pointCount) {
        if (pointCount < 4) {
            Toast.makeText(this, "不得少于4位", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void onGestureDone(LinkedHashSet<Integer> numbers) {
        String str = "";
        for (Integer num : numbers) {
            str += num;
        }
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

}
