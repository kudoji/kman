package com.kudoji.kman.enums;

import java.time.LocalDate;
import java.util.HashMap;

import static java.time.temporal.TemporalAdjusters.*;

/**
 * Different report periods
 */
public enum ReportPeriod {
    PREVIOUSYEAR("Previous year"),
    THISYEAR("This year"),
    THISYEARTILLDATE("This year till current date"),
    PREVIOUSMONTH("Previous month"),
    THISMONTH("This month"),
    THISMONTHTILLDATE("This month till current date"),
    YESTERDAY("Yesterday"),
    TODAY("Today"),
    CUSTOM("Custom...");

    private String description;

    private ReportPeriod(String description){
        this.description = description;
    }

    @Override
    public String toString(){
        return this.description;
    }

    /**
     * Returns start and end date based on current enum's value
     *
     * @return HashMap with keys:
     *          - start -   value is start date
     *          - end   -   value is end date
     */
    public HashMap<String, LocalDate> getPeriod(){
        LocalDate now = LocalDate.now();

        HashMap<String, LocalDate> result = new HashMap<>();
        result.put("start", now);
        result.put("end", now);

        if (this.name() == "PREVIOUSYEAR") {
            result.put("start", now.minusYears(1).with(firstDayOfYear()));
            result.put("end", now.minusYears(1).with(lastDayOfYear()));
        }else if (this.name() == "THISYEAR"){
            result.put("start", now.with(firstDayOfYear()));
            result.put("end", now.with(lastDayOfYear()));
        }else if (this.name() == "THISYEARTILLDATE") {
            result.put("start", now.with(firstDayOfYear()));
            result.put("end", now);
        }else if (this.name() == "PREVIOUSMONTH"){
            result.put("start", now.minusMonths(1).with(firstDayOfMonth()));
            result.put("end", now.minusMonths(1).with(lastDayOfMonth()));
        }else if (this.name() == "THISMONTH"){
            result.put("start", now.with(firstDayOfMonth()));
            result.put("end", now.with(lastDayOfMonth()));
        }else if (this.name() == "THISMONTHTILLDATE") {
            result.put("start", now.with(firstDayOfMonth()));
            result.put("end", now);
        }else if (this.name() == "YESTERDAY"){
            result.put("start", now.minusDays(1));
            result.put("end", now.minusDays(1));
        }else if (this.name() == "TODAY"){
            result.put("start", now);
            result.put("end", now);
        }else if (this.name() == "CUSTOM"){
            result.put("start", now);
            result.put("end", now);
        }

        return result;
    }
}
