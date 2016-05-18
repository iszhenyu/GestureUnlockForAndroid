package cn.xianging.gestureunlock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by xiaoz on 16/5/18.
 * Copyright © 2016年 xianging. All rights reserved.
 */
public class GestureUnlockView extends View {
    private static final String INSTANCE_STATE = "saved_instance";
    private static final String INSTANCE_COLOR_NORMAL = "color_normal";
    private static final String INSTANCE_COLOR_SELECTED = "color_selected";
    private static final String INSTANCE_COLOR_ERROR = "color_error";
    private static final String INSTANCE_POINT_RADIUS = "point_radius";
    private static final String INSTANCE_STROKE_WIDTH = "stroke_width";
    private static final String INSTANCE_LINE_WIDTH = "line_width";
    private static final String INSTANCE_RADIUS_FACTOR = "text_color";

    private boolean initialized = false;
    private boolean isGestureEnd = true;
    private boolean isGestureStart = false;
    private boolean isGestureValid = true;

    private OnGestureDoneListener mOnGestureDoneListener;

    private List<Line> mGestureLines = new ArrayList<>();
    private List<LockPoint> mGesturePoints = new ArrayList<>();
    private Line extraLine;

    private Handler mHandler = new Handler();

    private LockPoint[][] mLockPoints = new LockPoint[3][3];
    private float movingX;
    private float movingY;

    private Paint pointPaint;
    private Paint linePaint;

    private int colorNormal;
    private int colorSelected;
    private int colorError;
    private float pointRadius;
    private float strokeWidth;
    private float lineWidth;
    private float radiusFactor;

    public GestureUnlockView(Context context) {
        this(context, null);
    }

    public GestureUnlockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureUnlockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.GestureUnlockView, 0, 0);
        if (array != null) {
            colorNormal = array.getColor(R.styleable.GestureUnlockView_color_normal, Color.GRAY);
            colorError = array.getColor(R.styleable.GestureUnlockView_color_error, Color.RED);
            colorSelected = array.getColor(R.styleable.GestureUnlockView_color_selected, Color.BLUE);
            pointRadius = array.getDimension(R.styleable.GestureUnlockView_point_radius, 30.0f);
            strokeWidth = array.getDimension(R.styleable.GestureUnlockView_stroke_width, 3.0f);
            lineWidth = array.getDimension(R.styleable.GestureUnlockView_line_width, 6.0f);
            radiusFactor = array.getFloat(R.styleable.GestureUnlockView_radius_factor, 3.0f);
            array.recycle();
        }
        initPaints();
    }

    private void initPaints() {
        this.pointPaint = new Paint();
        this.pointPaint.setColor(colorNormal);
        this.pointPaint.setAntiAlias(true);
        this.pointPaint.setStyle(Paint.Style.FILL);
        this.pointPaint.setStrokeWidth(strokeWidth);

        this.linePaint = new Paint();
        this.linePaint.setColor(colorSelected);
        this.linePaint.setAntiAlias(true);
        this.linePaint.setStrokeWidth(lineWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!this.initialized) {
            initLockPoints();
            this.initialized = true;
        }
        drawLockPoints(canvas);
        drawLines(canvas);
    }

    private void initLockPoints() {
        int width = getWidth();
        int height = getHeight();

        int availableSize = width > height ? height : width;
        float radius;
        float margin;

        radius = availableSize / 10;
        if (pointRadius > 0 && (pointRadius * radiusFactor) < radius) {
            radius = pointRadius * radiusFactor;
        }
        margin = (availableSize - 6.0F * radius) / 4.0F;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float pointX = (width - availableSize) / 2 + margin * (col + 1) + radius * (2 * (col + 1) - 1);
                float pointY = (height - availableSize) / 2 + margin * (row + 1) + radius * (2 * (row + 1) - 1);
                this.mLockPoints[row][col] = new LockPoint(pointX, pointY, radius / radiusFactor, 1 + (col + row * 3));
            }
        }
    }

    private void drawLockPoints(Canvas canvas) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mLockPoints[i][j].draw(canvas);
            }
        }
    }

    private void drawLines(Canvas canvas) {
        if (!mGesturePoints.isEmpty()) {
            if (!mGestureLines.isEmpty()) {
                for (Line line : mGestureLines) {
                    line.draw(canvas);
                }
            }
            if (isGestureStart && !isGestureEnd) {
                LockPoint lastPoint = mGesturePoints.get(mGesturePoints.size() - 1);
                if (extraLine == null) {
                    extraLine = new Line(lastPoint.circle.centerX, lastPoint.circle.centerY, movingX, movingY);
                } else {
                    extraLine.startX = lastPoint.circle.centerX;
                    extraLine.startY = lastPoint.circle.centerY;
                    extraLine.endX = movingX;
                    extraLine.endY = movingY;
                }
                extraLine.draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isGestureValid) {
            return super.onTouchEvent(event);
        }

        this.movingX = event.getX();
        this.movingY = event.getY();
        int action = event.getAction();
        LockPoint hitPoint = null;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                hitPoint = checkPoint(movingX, movingY);
                this.isGestureStart = true;
                this.isGestureEnd = false;
                break;
            case MotionEvent.ACTION_UP:
                this.isGestureStart = false;
                this.isGestureEnd = true;
                hitPoint = null;
                break;
            case MotionEvent.ACTION_MOVE:
                hitPoint = checkPoint(movingX, movingY);
                break;
        }

        if (isGestureStart && !isGestureEnd) {
            if (hitPoint != null && !mGesturePoints.contains(hitPoint)) {
                // 大于0个点的时候开始需要画线
                if (!mGesturePoints.isEmpty()) {
                    LockPoint lastPoint = mGesturePoints.get(mGesturePoints.size() - 1);
                    Line newLine = new Line(lastPoint.circle.centerX, lastPoint.circle.centerY, hitPoint.circle.centerX, hitPoint.circle.centerY);
                    mGestureLines.add(newLine);
                }
                mGesturePoints.add(hitPoint);
                setSelectedPointsState(State.PRESSED);
            }

            postInvalidate();
        }
        if (isGestureEnd) {
            postInvalidate();
            isGestureValid = false;

            LinkedHashSet<Integer> selectedNumbers = new LinkedHashSet<>();
            int size = mGesturePoints.size();
            for (int k = 0; k < size; k++) {
                selectedNumbers.add(mGesturePoints.get(k).number);
            }
            if (this.mOnGestureDoneListener != null) {
                if (mOnGestureDoneListener.isValidGesture(selectedNumbers.size())) {
                    this.mOnGestureDoneListener.onGestureDone(selectedNumbers);
                } else {
                    setSelectedPointsState(State.ERROR);
                }
            }
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    GestureUnlockView.this.resetSelectedPointsState();
                }
            }, 1000L);
        }
        return true;
    }

    private LockPoint checkPoint(float x, float y) {
        int rows = mLockPoints.length;
        for (int i = 0; i < rows; i++) {
            int cols = mLockPoints[i].length;
            for (int j = 0; j < cols; j++) {
                if (mLockPoints[i][j] != null &&
                        mLockPoints[i][j].circle != null &&
                        mLockPoints[i][j].circle.isHit(x, y)) {
                    return mLockPoints[i][j];
                }
            }
        }
        return null;
    }

    /**
     * 重置状态
     */
    private void resetSelectedPointsState() {
        if (!mGesturePoints.isEmpty()) {
            for (LockPoint point : mGesturePoints) {
                point.changeState(State.NORMAL);
            }
            this.mGesturePoints.clear();
            this.mGestureLines.clear();
            this.isGestureValid = true;
            postInvalidate();
        }
    }

    private void setSelectedPointsState(State newState) {
        if (!mGesturePoints.isEmpty()) {
            for (LockPoint point : mGesturePoints) {
                point.changeState(newState);
            }
            if (newState == State.ERROR && !mGestureLines.isEmpty()) {
                for (Line line : mGestureLines) {
                    line.state = newState;
                }
            }
            postInvalidate();
        }
    }

    public void setOnGestureDoneListener(OnGestureDoneListener paramGestureDoneListener) {
        this.mOnGestureDoneListener = paramGestureDoneListener;
    }

    public interface OnGestureDoneListener {
        boolean isValidGesture(int pointCount);
        void onGestureDone(LinkedHashSet<Integer> numbers);
    }

    enum State {
        NORMAL, PRESSED, ERROR
    }

    class LockPoint {
        Circle circle;
        int number;

        LockPoint(float x, float y, float radius, int number) {
            this.number = number;
            this.circle = new Circle(x, y, radius);
        }

        void changeState(State newState) {
            this.circle.state = newState;
        }

        void draw(Canvas canvas) {
            if (this.circle != null) {
                this.circle.draw(canvas);
            }
        }

        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof LockPoint) {
                return this.number == ((LockPoint) object).number;
            }
            return false;
        }
    }

    class Line {
        float startX, startY, endX, endY;
        State state = State.PRESSED;

        Line(float startX, float startY, float endX, float endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        private void preparePaintColor() {
            if (this.state == State.PRESSED) {
                linePaint.setColor(colorSelected);
            } else {
                linePaint.setColor(colorError);
            }
        }

        void draw(Canvas canvas) {
            preparePaintColor();
            canvas.drawLine(this.startX, this.startY, this.endX, this.endY, linePaint);
        }
    }

    class Circle {
        float centerX, centerY, radius, outerRadius;
        State state = State.NORMAL;

        Circle(float pointX, float pointY, float radius) {
            this.centerX = pointX;
            this.centerY = pointY;
            this.radius = radius;
            this.outerRadius = radius * radiusFactor;
        }

        private void preparePaintColor() {
            switch (this.state) {
                case NORMAL:
                    pointPaint.setColor(colorNormal);
                    break;
                case PRESSED:
                    pointPaint.setColor(colorSelected);
                    break;
                case ERROR:
                    pointPaint.setColor(colorError);
                    break;
            }
        }

        void draw(Canvas canvas) {
            preparePaintColor();
            canvas.drawCircle(this.centerX, this.centerY, this.radius, pointPaint);
            if (this.state == State.PRESSED || this.state == State.ERROR) {
                pointPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(this.centerX, this.centerY, this.outerRadius, pointPaint);
                pointPaint.setStyle(Paint.Style.FILL);
            }
        }

        boolean isHit(float paramFloat1, float paramFloat2) {
            return (Math.abs(paramFloat1 - this.centerX) < this.outerRadius) && (Math.abs(paramFloat2 - this.centerY) < this.outerRadius);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putInt(INSTANCE_COLOR_NORMAL, this.colorNormal);
        bundle.putInt(INSTANCE_COLOR_SELECTED, this.colorSelected);
        bundle.putInt(INSTANCE_COLOR_ERROR, this.colorError);
        bundle.putFloat(INSTANCE_POINT_RADIUS, this.pointRadius);
        bundle.putFloat(INSTANCE_STROKE_WIDTH, this.strokeWidth);
        bundle.putFloat(INSTANCE_LINE_WIDTH, this.lineWidth);
        bundle.putFloat(INSTANCE_RADIUS_FACTOR, this.radiusFactor);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            colorNormal = bundle.getInt(INSTANCE_COLOR_NORMAL);
            colorSelected = bundle.getInt(INSTANCE_COLOR_SELECTED);
            colorError = bundle.getInt(INSTANCE_COLOR_ERROR);
            pointRadius = bundle.getFloat(INSTANCE_POINT_RADIUS);
            strokeWidth = bundle.getFloat(INSTANCE_STROKE_WIDTH);
            lineWidth = bundle.getFloat(INSTANCE_LINE_WIDTH);
            radiusFactor = bundle.getFloat(INSTANCE_RADIUS_FACTOR);
            initPaints();
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }
}
