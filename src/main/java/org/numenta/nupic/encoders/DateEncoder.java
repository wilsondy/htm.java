/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.encoders;

import java.util.*;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.numenta.nupic.util.Tuple;

/**
 * DOCUMENTATION TAKEN DIRECTLY FROM THE PYTHON VERSION:
 *
 * A date encoder encodes a date according to encoding parameters specified in its constructor.
 *
 * The input to a date encoder is a datetime.datetime object. The output is
 * the concatenation of several sub-encodings, each of which encodes a different
 * aspect of the date. Which sub-encodings are present, and details of those
 * sub-encodings, are specified in the DateEncoder constructor.
 *
 * Each parameter describes one attribute to encode. By default, the attribute
 * is not encoded.
 *
 * season (season of the year; units = day):
 * (int) width of attribute; default radius = 91.5 days (1 season)
 * (tuple)  season[0] = width; season[1] = radius
 *
 * dayOfWeek (monday = 0; units = day)
 * (int) width of attribute; default radius = 1 day
 * (tuple) dayOfWeek[0] = width; dayOfWeek[1] = radius
 *
 * weekend (boolean: 0, 1)
 * (int) width of attribute
 *
 * holiday (boolean: 0, 1)
 * (int) width of attribute
 *
 * timeOfday (midnight = 0; units = hour)
 * (int) width of attribute: default radius = 4 hours
 * (tuple) timeOfDay[0] = width; timeOfDay[1] = radius
 *
 * customDays TODO: what is it?
 *
 * forced (default True) : if True, skip checks for parameters' settings; see {@code ScalarEncoders} for details
 *
 * @author utensil
 *
 * TODO Improve the document:
 *
 * - improve wording on unspecified attributes: "Each parameter describes one extra attribute(other than the datetime
 *   object itself) to encode. By default, the unspecified attributes are not encoded."
 * - change datetime.datetime object to joda-time object
 * - refer to DateEncoder::Builder, which where these parameters are defined.
 * - explain customDays here and at Python version
 */

public class DateEncoder extends Encoder<Date> {

    protected int width;

    //See DateEncoder.Builder for default values.

    protected Tuple season;
    protected ScalarEncoder seasonEncoder;

    protected Tuple dayOfWeek;
    protected ScalarEncoder dayOfWeekEncoder;

    protected Tuple weekend;
    protected ScalarEncoder weekendEncoder;

    protected Tuple customDays;
    protected ScalarEncoder customDaysEncoder;

    protected Tuple holiday;
    protected ScalarEncoder holidayEncoder;

    protected Tuple timeOfDay;
    protected ScalarEncoder timeOfDayEncoder;

    protected List<Integer> customDaysList = new ArrayList<>();

    // Currently the only holiday we know about is December 25
    // holidays is a list of holidays that occur on a fixed date every year
    protected List<Tuple> holidaysList = Arrays.asList(
            new Tuple(12, 25)
    );

    /**
     * Constructs a new {@code DateEncoder}
     *
     * Package private to encourage construction using the Builder Pattern
     * but still allow inheritance.
     */
    DateEncoder() {
    }

    /**
     * Returns a builder for building DateEncoder.
     * This builder may be reused to produce multiple builders
     *
     * @return a {@code DateEncoder.Builder}
     */
    public static DateEncoder.Builder builder() {
        return new DateEncoder.Builder();
    }

    /**
     * Init the {@code DateEncoder} with parameters
     */
    @SuppressWarnings("unchecked")
    public void init() {

        width = 0;

        // Because most of the ScalarEncoder fields have less than 21 bits(recommended in
        // ScalarEncoder.checkReasonableSettings), so for now we set forced to be true to
        // override.
        // TODO figure out how to remove this
        setForced(true);

        // Note: The order of adding encoders matters, must be in the following
        // season, dayOfWeek, weekend, customDays, holiday, timeOfDay

        if(isValidEncoderPropertyTuple(season)) {
            seasonEncoder = ScalarEncoder.builder()
                    .w((int) season.get(0))
                    .radius((double) season.get(1))
                    .minVal(0)
                    .maxVal(366)
                    .periodic(true)
                    .name("season")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(seasonEncoder);
        }

        if(isValidEncoderPropertyTuple(dayOfWeek)) {
            dayOfWeekEncoder = ScalarEncoder.builder()
                    .w((int) dayOfWeek.get(0))
                    .radius((double) dayOfWeek.get(1))
                    .minVal(0)
                    .maxVal(7)
                    .periodic(true)
                    .name("day of week")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(dayOfWeekEncoder);
        }

        if(isValidEncoderPropertyTuple(weekend)) {
            weekendEncoder = ScalarEncoder.builder()
                    .w((int) weekend.get(0))
                    .radius((double) weekend.get(1))
                    .minVal(0)
                    .maxVal(1)
                    .periodic(false)
                    .name("weekend")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(weekendEncoder);
        }

        if(isValidEncoderPropertyTuple(customDays)) {
            List<String> days = (List<String>) customDays.get(1);

            StringBuilder customDayEncoderName = new StringBuilder();

            if(days.size() == 1) {
                customDayEncoderName.append(days.get(0));
            } else {
                for(String day : days) {
                    customDayEncoderName.append(day).append(" ");
                }
            }

            customDaysEncoder = ScalarEncoder.builder()
                    .w((int) customDays.get(0))
                    .radius(1)
                    .minVal(0)
                    .maxVal(1)
                    .periodic(false)
                    .name(customDayEncoderName.toString())
                    .forced(this.isForced())
                    .build();
            //customDaysEncoder is special in naming
            addEncoder("customdays", customDaysEncoder);
            addCustomDays(days);
        }

        if(isValidEncoderPropertyTuple(holiday)) {
            holidayEncoder = ScalarEncoder.builder()
                    .w((int) holiday.get(0))
                    .radius((double) holiday.get(1))
                    .minVal(0)
                    .maxVal(1)
                    .periodic(false)
                    .name("holiday")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(holidayEncoder);
        }

        if(isValidEncoderPropertyTuple(timeOfDay)) {
            timeOfDayEncoder = ScalarEncoder.builder()
                    .w((int) timeOfDay.get(0))
                    .radius((double) timeOfDay.get(1))
                    .minVal(0)
                    .maxVal(24)
                    .periodic(true)
                    .name("time of day")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(timeOfDayEncoder);
        }
    }

    private boolean isValidEncoderPropertyTuple(Tuple encoderPropertyTuple) {
        return encoderPropertyTuple != null && (int)encoderPropertyTuple.get(0) != 0;
    }

    // Adapted from MultiEncoder
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addEncoder(String name, Encoder child) {
        super.addEncoder(this, name, child, width);

        for (Object d : child.getDescription()) {
            Tuple dT = (Tuple) d;
            description.add(new Tuple(dT.get(0), (int)dT.get(1) + getWidth()));
        }
        width += child.getWidth();
    }

    protected void addChildEncoder(ScalarEncoder encoder) {
        this.addEncoder(encoder.getName(), encoder);
    }

    protected void addCustomDays(List<String> daysList) {
        for(String dayStr : daysList)
        {
            switch (dayStr.toLowerCase())
            {
                case "mon":
                case "monday":
                    customDaysList.add(0);
                    break;
                case "tue":
                case "tuesday":
                    customDaysList.add(1);
                    break;
                case "wed":
                case "wednesday":
                    customDaysList.add(2);
                    break;
                case "thu":
                case "thursday":
                    customDaysList.add(3);
                    break;
                case "fri":
                case "friday":
                    customDaysList.add(4);
                    break;
                case "sat":
                case "saturday":
                    customDaysList.add(5);
                    break;
                case "sun":
                case "sunday":
                    customDaysList.add(6);
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("Unable to understand %s as a day of week", dayStr)
                    );
            }
        }
    }

    public Tuple getSeason() {
        return season;
    }

    public void setSeason(Tuple season) {
        this.season = season;
    }

    public Tuple getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Tuple dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Tuple getWeekend() {
        return weekend;
    }

    public void setWeekend(Tuple weekend) {
        this.weekend = weekend;
    }

    public Tuple getCustomDays() {
        return customDays;
    }

    public void setCustomDays(Tuple customDays) {
        this.customDays = customDays;
    }

    public Tuple getHoliday() {
        return holiday;
    }

    public void setHoliday(Tuple holiday) {
        this.holiday = holiday;
    }

    public Tuple getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(Tuple timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getN() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getW() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDelta() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    // Adapted from MultiEncoder
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void encodeIntoArray(Date inputData, int[] output) {

        if(inputData == null) {
            throw new IllegalArgumentException("DateEncoder requires a valid Date object but got null");
        }

        // Get the scalar values for each sub-field
        TDoubleList scalars = getScalars(inputData);

        int fieldCounter = 0;
        for (EncoderTuple t : getEncoders(this)) {
            Encoder encoder = t.getEncoder();
            int offset = t.getOffset();

            int[] tempArray = new int[encoder.getWidth()];
            encoder.encodeIntoArray(scalars.get(fieldCounter), tempArray);

            System.arraycopy(tempArray, 0, output, offset, tempArray.length);

            ++fieldCounter;
        }
    }

    /**
     * Returns the input in the same format as is returned by topDownCompute().
     * For most encoder types, this is the same as the input data.
     * For instance, for scalar and category types, this corresponds to the numeric
     * and string values, respectively, from the inputs. For datetime encoders, this
     * returns the list of scalars for each of the sub-fields (timeOfDay, dayOfWeek, etc.)
     *
     * This method is essentially the same as getScalars() except that it returns
     * strings
     * @param inputData 	The input data in the format it is received from the data source
     *
     * @return A list of values, in the same format and in the same order as they
     * are returned by topDownCompute.
     *
     * @return	list of encoded values in String form
     */
    public List<String> getEncodedValues(Date inputData) {
        List<String> values = new ArrayList<>();

        List<String> encodedValues = getEncodedValues(inputData);

        for (String v : encodedValues) {
            values.add(String.valueOf(v));
        }

        return values;
    }

    /**
     * Returns an {@link TDoubleList} containing the sub-field scalar value(s) for
     * each sub-field of the inputData. To get the associated field names for each of
     * the scalar values, call getScalarNames().
     *
     * @param inputData	the input value, in this case a date object
     * @return	a list of one input double
     */
    public TDoubleList getScalars(Date inputData) {
        if(inputData == null) {
            throw new IllegalArgumentException("DateEncoder requires a valid Date object but got null");
        }

        TDoubleList values = new TDoubleArrayList();

        DateTime date = new DateTime(inputData);

        //Get the scalar values for each sub-field

        double timeOfDay = date.getHourOfDay() + date.getMinuteOfHour() / 60.0;

        // The day of week was 1 based, so convert to 0 based
        int dayOfWeek = date.getDayOfWeek() - 1; // + timeOfDay / 24.0

        if(seasonEncoder != null) {
            // The day of year was 1 based, so convert to 0 based
            double dayOfYear = date.getDayOfYear() - 1;
            values.add(dayOfYear);
        }

        if(dayOfWeekEncoder != null) {
            values.add(dayOfWeek);
        }

        if(weekendEncoder != null) {

            //saturday, sunday or friday evening
            boolean isWeekend = dayOfWeek == 6 || dayOfWeek == 5 ||
                    (dayOfWeek == 4 && timeOfDay > 18);

            int weekend = isWeekend ? 1 : 0;

            values.add(weekend);
        }

        if(customDaysEncoder != null) {
            boolean isCustomDays = customDaysList.contains(dayOfWeek);

            int customDay = isCustomDays ? 1 : 0;

            values.add(customDay);
        }

        if(holidayEncoder != null) {
            // A "continuous" binary value. = 1 on the holiday itself and smooth ramp
            //  0->1 on the day before the holiday and 1->0 on the day after the holiday.

            double holidayness = 0;

            for(Tuple h : holidaysList) {
                //hdate is midnight on the holiday
                DateTime hdate = new DateTime(date.getYear(), (int)h.get(0), (int)h.get(1), 0, 0, 0);

                if(date.isAfter(hdate)) {
                    Duration diff = new Interval(hdate, date).toDuration();
                    long days = diff.getStandardDays();
                    if(days == 0) {
                        //return 1 on the holiday itself
                        holidayness = 1;
                        break;
                    } else if(days == 1) {
                        //ramp smoothly from 1 -> 0 on the next day
                        holidayness = 1.0 - ((diff.getStandardSeconds() - 86400.0 * days) / 86400.0);
                        break;
                    }

                } else {
                    //TODO This is not the same as when date.isAfter(hdate), why?
                    Duration diff = new Interval(date, hdate).toDuration();
                    long days = diff.getStandardDays();
                    if(days == 0) {
                        //ramp smoothly from 0 -> 1 on the previous day
                        holidayness = 1.0 - ((diff.getStandardSeconds() - 86400.0 * days) / 86400.0);
                        //TODO Why no break?
                    }
                }
            }

            values.add(holidayness);
        }

        if(timeOfDayEncoder != null) {
            values.add(timeOfDay);
        }

        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO Why can getBucketValues return null for some encoders, e.g. MultiEncoder
    public <S> List<S> getBucketValues(Class<S> returnType) {
        return null;
    }

    /**
     * Returns an array containing the sub-field bucket indices for
     * each sub-field of the inputData. To get the associated field names for each of
     * the buckets, call getScalarNames().
     * @param  	input 	The data from the source. This is typically a object with members.
     *
     * @return 	array of bucket indices
     */
    public int[] getBucketIndices(Date input) {

        TDoubleList scalars = getScalars(input);

        TIntList l = new TIntArrayList();
        List<EncoderTuple> encoders = getEncoders(this);
        if(encoders != null && encoders.size() > 0) {
            int i = 0;
            for(EncoderTuple t : encoders) {
                l.addAll(t.getEncoder().getBucketIndices(scalars.get(i)));
                ++i;
            }
        }else{
            throw new IllegalStateException("Should be implemented in base classes that are not " +
                    "containers for other encoders");
        }
        return l.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void setLearning(boolean learningEnabled) {
        for (EncoderTuple t : getEncoders(this)) {
            Encoder encoder = t.getEncoder();
            encoder.setLearningEnabled(learningEnabled);
        }
    }

    /**
     * Returns a {@link Encoder.Builder} for constructing {@link DateEncoder}s
     *
     * The base class architecture is put together in such a way where boilerplate
     * initialization can be kept to a minimum for implementing subclasses.
     * Hopefully! :-)
     *
     * @see ScalarEncoder.Builder#setStuff(int)
     */
    public static class Builder extends Encoder.Builder<DateEncoder.Builder, DateEncoder> {

        //    Ignore leap year differences -- assume 366 days in a year
        //    Radius = 91.5 days = length of season
        //    Value is number of days since beginning of year (0 - 355)
        protected Tuple season = new Tuple(0, 91.5);

        // Value is day of week (floating point)
        // Radius is 1 day
        protected Tuple dayOfWeek = new Tuple(0, 1.0);

        // Binary value.
        protected Tuple weekend = new Tuple(0, 1.0);

        // Custom days encoder, first argument in tuple is width
        // second is either a single day of the week or a list of the days
        // you want encoded as ones.
        protected Tuple customDays = new Tuple(0, new ArrayList<String>());

        // A "continuous" binary value. = 1 on the holiday itself and smooth ramp
        //  0->1 on the day before the holiday and 1->0 on the day after the holiday.
        protected Tuple holiday = new Tuple(0, 1.0);

        // Value is time of day in hours
        // Radius = 4 hours, e.g. morning, afternoon, evening, early night,
        //  late night, etc.
        protected Tuple timeOfDay = new Tuple(0, 4.0);

        private Builder() {}

        @Override
        public DateEncoder build() {
            //Must be instantiated so that super class can initialize
            //boilerplate variables.
            encoder = new DateEncoder();

            //Call super class here
            super.build();

            ////////////////////////////////////////////////////////
            //  Implementing classes would do setting of specific //
            //  vars here together with any sanity checking       //
            ////////////////////////////////////////////////////////
            DateEncoder e = ((DateEncoder)encoder);

            e.setSeason(this.season);
            e.setDayOfWeek(this.dayOfWeek);
            e.setWeekend(this.weekend);
            e.setHoliday(this.holiday);
            e.setTimeOfDay(this.timeOfDay);
            e.setCustomDays(this.customDays);

            ((DateEncoder)encoder).init();

            return (DateEncoder)encoder;
        }

        /**
         * Set how many bits are used to encode season
         */
        public DateEncoder.Builder season(int season, double radius) {
            this.season = new Tuple(season, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode season
         */
        public DateEncoder.Builder season(int season) {
            return this.season(season, (double) this.season.get(1));
        }

        /**
         * Set how many bits are used to encode dayOfWeek
         */
        public DateEncoder.Builder dayOfWeek(int dayOfWeek, double radius) {
            this.dayOfWeek = new Tuple(dayOfWeek, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode dayOfWeek
         */
        public DateEncoder.Builder dayOfWeek(int dayOfWeek) {
            return this.dayOfWeek(dayOfWeek, (double) this.dayOfWeek.get(1));
        }

        /**
         * Set how many bits are used to encode weekend
         */
        public DateEncoder.Builder weekend(int weekend, double radius) {
            this.weekend = new Tuple(weekend, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode weekend
         */
        public DateEncoder.Builder weekend(int weekend) {
            return this.weekend(weekend, (double) this.weekend.get(1));
        }

        /**
         * Set how many bits are used to encode customDays
         */
        public DateEncoder.Builder customDays(int customDays, List<String> customDaysList) {
            this.customDays = new Tuple(customDays, customDaysList);
            return this;
        }

        /**
         * Set how many bits are used to encode customDays
         */
        @SuppressWarnings("unchecked")
        public DateEncoder.Builder customDays(int customDays) {
            return this.customDays(customDays, (ArrayList<String>) this.customDays.get(1));
        }

        /**
         * Set how many bits are used to encode holiday
         */
        public DateEncoder.Builder holiday(int holiday, double radius) {
            this.holiday = new Tuple(holiday, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode holiday
         */
        public DateEncoder.Builder holiday(int holiday) {
            return this.holiday(holiday, (double) this.holiday.get(1));
        }

        /**
         * Set how many bits are used to encode timeOfDay
         */
        public DateEncoder.Builder timeOfDay(int timeOfDay, double radius) {
            this.timeOfDay = new Tuple(timeOfDay, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode timeOfDay
         */
        public DateEncoder.Builder timeOfDay(int timeOfDay) {
            return this.timeOfDay(timeOfDay, (double) this.timeOfDay.get(1));
        }

        /**
         * Set the name of the encoder
         */
        public DateEncoder.Builder name(String name) {
            this.name = name;
            return this;
        }


    }
}
