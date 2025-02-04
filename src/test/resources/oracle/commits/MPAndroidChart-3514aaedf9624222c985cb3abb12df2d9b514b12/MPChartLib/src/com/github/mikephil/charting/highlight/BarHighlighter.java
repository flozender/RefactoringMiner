package com.github.mikephil.charting.highlight;

import android.util.Log;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.BarDataProvider;
import com.github.mikephil.charting.interfaces.BarLineScatterCandleBubbleDataProvider;

/**
 * Created by Philipp Jahoda on 22/07/15.
 */
public class BarHighlighter extends ChartHighlighter<BarDataProvider> {

    public BarHighlighter(BarDataProvider chart) {
        super(chart);
    }

    @Override
    public Highlight getHighlight(float x, float y) {

        Highlight h = super.getHighlight(x, y);

        if(h == null)
            return h;
        else {

            BarDataSet set = mChart.getBarData().getDataSetByIndex(h.getDataSetIndex());

            if (set.isStacked()) {

                // create an array of the touch-point
                float[] pts = new float[2];
                pts[1] = y;

                // take any transformer to determine the x-axis value
                mChart.getTransformer(set.getAxisDependency()).pixelsToValue(pts);

                return getStackedHighlight(set, h.getXIndex(), h.getDataSetIndex(), pts[1]);
            } else
                return h;
        }
    }

    @Override
    protected int getXIndex(float x) {

        if(!mChart.getBarData().isGrouped()) {
            return super.getXIndex(x);
        } else {

            float baseNoSpace = getBase(x);

            int setCount = mChart.getBarData().getDataSetCount();
            int xIndex = (int) baseNoSpace / setCount;

            int valCount = mChart.getData().getXValCount();

            if(xIndex < 0)
                xIndex = 0;
            else if(xIndex >= valCount)
                xIndex = valCount - 1;

            return xIndex;
        }
    }

    @Override
    protected int getDataSetIndex(int xIndex, float x, float y) {

        if(!mChart.getBarData().isGrouped()) {
            return 0;
        } else {

            float baseNoSpace = getBase(x);

            int setCount = mChart.getBarData().getDataSetCount();
            int dataSetIndex = (int) baseNoSpace % setCount;

            if (dataSetIndex < 0)
                dataSetIndex = 0;
            else if (dataSetIndex >= setCount)
                dataSetIndex = setCount - 1;

            return dataSetIndex;
        }
    }

    /**
     * This method creates the Highlight object that also indicates which value
     * of a stacked BarEntry has been selected.
     *
     * @param set
     * @param xIndex
     * @param dataSetIndex
     * @param yValue
     * @return
     */
    protected Highlight getStackedHighlight(BarDataSet set, int xIndex, int dataSetIndex, double yValue) {

        BarEntry entry = set.getEntryForXIndex(xIndex);

        if (entry != null) {
            int stackIndex = getClosestStackIndex(entry, (float) yValue);
            Highlight h = new Highlight(xIndex, dataSetIndex, stackIndex);
            return h;
        } else
            return null;
    }

    /**
     * Returns the index of the closest value inside the values array (for stacked barchart)
     * to the value given as a parameter.
     *
     * @param e
     * @param value
     * @return
     */
    protected int getClosestStackIndex(BarEntry e, float value) {

        Range[] ranges = getRanges(e);
        int stackIndex = 0;

        for(Range range : ranges) {
            if(range.contains(value))
                return stackIndex;
            else
                stackIndex++;
        }

        int length = ranges.length - 1;

        return (value > ranges[length].to) ? length : 0;
//
//        float[] vals = e.getVals();
//
//        if (vals == null)
//            return -1;
//
//        int index = 0;
//        float remainder = e.getNegativeSum();
//
//        while (index < vals.length - 1 && value > vals[index] + remainder) {
//            remainder += vals[index];
//            index++;
//        }
//
//        return index;
    }

    /**
     * Returns the base x-value to the corresponding x-touch value in pixels.
     * @param x
     * @return
     */
    protected float getBase(float x) {

        // create an array of the touch-point
        float[] pts = new float[2];
        pts[0] = x;

        // take any transformer to determine the x-axis value
        mChart.getTransformer(YAxis.AxisDependency.LEFT).pixelsToValue(pts);
        float xVal = pts[0];

        int setCount = mChart.getBarData().getDataSetCount();

        // calculate how often the group-space appears
        int steps = (int) ((float) xVal / ((float) setCount + mChart.getBarData().getGroupSpace()));

        float groupSpaceSum = mChart.getBarData().getGroupSpace() * (float) steps;

        float baseNoSpace = (float) xVal - groupSpaceSum;
        return baseNoSpace;
    }

    protected Range[] getRanges(BarEntry entry) {

        float[] values = entry.getVals();
        float negRemain = -entry.getNegativeSum();
        float posRemain = 0f;

        Range[] ranges = new Range[values.length];

        for(int i = 0; i < ranges.length; i++) {

            float value = values[i];

            if(value < 0) {
                ranges[i] = new Range(negRemain, negRemain+Math.abs(value));
                negRemain += Math.abs(value);
            } else {
                ranges[i] = new Range(posRemain, posRemain+value);
                posRemain += value;
            }
        }

        return ranges;
    }
}
