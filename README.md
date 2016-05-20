# GestureUnlockForAndroid

## 效果图

![image](https://github.com/xiaoza/GestureUnlockForAndroid/blob/master/gestureunlockforandroid_01.png)

![image](https://github.com/xiaoza/GestureUnlockForAndroid/blob/master/gestureunlockforandroid_02.png)

![image](https://github.com/xiaoza/GestureUnlockForAndroid/blob/master/gestureunlockforandroid_03.png)

## 使用

### 在 module 的 build.gradle 添加 dependency

```bash
dependencies {
      compile 'cn.xianging:gestureunlock:0.1.1'
}
```

### 布局文件

```bash
<cn.xianging.gestureunlock.GestureUnlockView
        android:id="@+id/gesture_unlock_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:color_normal="@color/gray"
        app:color_selected="@color/green"
        app:color_error="@color/red"
        app:stroke_width="2dp"
        app:line_width="2dp"
        />
```

### 代码

实现 GestureUnlockView.OnGestureDoneListener 接口

```bash
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
```
