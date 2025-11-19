package com.example.project;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class SimpleBarChartView extends View {

    private List<ProgressActivity.ChartDataPoint> dataPoints = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private float maxValue = 100f;

    public SimpleBarChartView(Context context) {
        super(context);
        init();
    }

    public SimpleBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#2196F3"));
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#757575"));
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#E3F2FD"));
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<ProgressActivity.ChartDataPoint> data) {
        this.dataPoints = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        int padding = 40;
        int chartHeight = height - padding * 2;
        int chartWidth = width - padding * 2;

        // Draw background
        RectF bgRect = new RectF(padding, padding, width - padding, height - padding);
        canvas.drawRoundRect(bgRect, 20, 20, backgroundPaint);

        // Calculate bar width
        int barCount = dataPoints.size();
        float barSpacing = 30f;
        float barWidth = (chartWidth - (barSpacing * (barCount - 1))) / barCount;

        // Draw bars
        for (int i = 0; i < dataPoints.size(); i++) {
            ProgressActivity.ChartDataPoint point = dataPoints.get(i);
            float barHeight = (point.getValue() / maxValue) * (chartHeight - 60);

            float left = padding + (i * (barWidth + barSpacing));
            float top = height - padding - 40 - barHeight;
            float right = left + barWidth;
            float bottom = height - padding - 40;

            // Draw bar with rounded top
            RectF barRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(barRect, 10, 10, barPaint);

            // Draw label
            canvas.drawText(
                    point.getLabel(),
                    left + barWidth / 2,
                    height - padding,
                    textPaint
            );
        }
    }
}