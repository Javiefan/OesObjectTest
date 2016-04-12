package com.bwts.oestest.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Javie on 16/4/12.
 */
public class Statistics {

    public static double getVariance(List<Long> data) {
        double mean = getMean(data);
        double temp = 0;
        for(double a :data)
            temp += (mean-a)*(mean-a);
        return temp/data.size();
    }

    public static double getRange(List<Long> data) {
        long max = data.get(0);
        long min = data.get(0);
        for(long number:data) {
            if (number > max) {
                max = number;
            }
            if (number < min) {
                min = number;
            }
        }
        return max - min;
    }

    public static double getMean(List<Long> data) {
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/data.size();
    }

    public static void main(String[] args) {
        List<Long> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add((long) i * 2);
        }
        System.out.println(getRange(data));
    }
}
