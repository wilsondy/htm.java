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
package org.numenta.nupic.examples.qt;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.CLAClassifier;
//import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.ArrayUtils;
/**
 * Quick and dirty example of tying together a network of components.
 * This should hold off peeps until the Network API is complete.
 * (see: https://github.com/numenta/htm.java/wiki/Roadmap)
 * 
 * <p>Warning: Sloppy sketchpad code, but it works!</p>
 * 
 * <p><em><b>
 * To see the pretty printed test output and Classification results, uncomment all
 * the print out lines below
 * </b></em></p>
 * 
 * @author PDove
 * @author cogmission
 */
public class QuickTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	Parameters params = getParameters();
    	System.out.println(params);
    	
    	//Layer components
    	ScalarEncoder.Builder dayBuilder =
			ScalarEncoder.builder()
				.n(8)
				.w(3)
				.radius(1.0)
				.minVal(1.0)
				.maxVal(8)
				.periodic(true)
				.forced(true)
				.resolution(1);
        ScalarEncoder encoder = dayBuilder.build();
    	SpatialPooler sp = new SpatialPooler();
    	TemporalMemory tm = new TemporalMemory();
    	CLAClassifier classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.3, 0);
    	
    	Layer<Double> layer = getLayer(params, encoder, sp, tm, classifier);
    	
    	for(double i = 1, x = 0;x < 10000;i = (i == 7 ? 1 : i + 1), x++) {  // USE "X" here to control run length
    		if (i == 1) tm.reset(layer.getMemory());
    		runThroughLayer(layer, i, (int)i, (int)x);
    	}
    	
    }
    
    public static Parameters getParameters() {
    	Parameters parameters = Parameters.getAllDefaultParameters();
    	parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.setParameterByKey(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITIONS, false);
        parameters.setParameterByKey(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.setParameterByKey(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.setParameterByKey(KEY.MAX_BOOST, 10.0);
        parameters.setParameterByKey(KEY.SEED, 42);
        parameters.setParameterByKey(KEY.SP_VERBOSITY, 0);
        
        //Temporal Memory specific
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 5);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 4);
        
        return parameters;
    }

	public static <T> void runThroughLayer(Layer<T> l, T input, int recordNum, int sequenceNum) {
    	l.input(input, recordNum, sequenceNum);
    }
    
	public static Layer<Double> getLayer(Parameters p, ScalarEncoder e, SpatialPooler s, TemporalMemory t, CLAClassifier c) {
    	Layer<Double> l = new LayerImpl(p, e, s, t, c);
    	return l;
    }
    
    ////////////////// Preliminary Network API Toy ///////////////////
    
    interface Layer<T> {
    	public void input(T value, int recordNum, int iteration);
    	public int[] getPredicted();
    	public Connections getMemory();
    	public int[] getActual();
    }
    
    /**
     * I'm going to make an actual Layer, this is just temporary so I can
     * work out the details while I'm completing this for Peter
     * 
     * @author David Ray
     *
     */
    static class LayerImpl implements Layer<Double> {
    	private Parameters params;
    	
    	private Connections memory = new Connections();
    	
    	private ScalarEncoder encoder;
    	private SpatialPooler spatialPooler;
    	private TemporalMemory temporalMemory;
//    	private CLAClassifier classifier;
    	private Map<String, Object> classification = new LinkedHashMap<String, Object>();
    	
    	private int columnCount;
    	private int cellsPerColumn;
//    	private int theNum;
    	
    	private int[] predictedColumns;
    	private int[] actual;
    	private int[] lastPredicted;
    	
    	public LayerImpl(Parameters p, ScalarEncoder e, SpatialPooler s, TemporalMemory t, CLAClassifier c) {
    		this.params = p;
    		this.encoder = e;
    		this.spatialPooler = s;
    		this.temporalMemory = t;
//    		this.classifier = c;
    		
    		params.apply(memory);
    		spatialPooler.init(memory);
    		temporalMemory.init(memory);
    		
    		columnCount = memory.getPotentialPools().getMaxIndex() + 1; //If necessary, flatten multi-dimensional index 
    		cellsPerColumn = memory.getCellsPerColumn();
    	}
    	
    	@Override
    	public void input(Double value, int recordNum, int sequenceNum) {
//    		String recordOut = "";
//    		switch(recordNum) {
//    			case 1: recordOut = "Monday (1)";break; 
//    			case 2: recordOut = "Tuesday (2)";break;
//    			case 3: recordOut = "Wednesday (3)";break;
//    			case 4: recordOut = "Thursday (4)";break;
//    			case 5: recordOut = "Friday (5)";break;
//    			case 6: recordOut = "Saturday (6)";break;
//    			case 7: recordOut = "Sunday (7)";break;
//    		}
    		
    		if(recordNum == 1) {
//    			theNum++;
//    			System.out.println("--------------------------------------------------------");
//    			System.out.println("Iteration: " + theNum);
    		}
//    		System.out.println("===== " + recordOut + "  - Sequence Num: " + sequenceNum + " =====");
    		
    		int[] output = new int[columnCount];
    		
    		//Input through encoder
//    		System.out.println("ScalarEncoder Input = " + value);
    		int[] encoding = encoder.encode(value);
//    		System.out.println("ScalarEncoder Output = " + Arrays.toString(encoding));
    		int bucketIdx = encoder.getBucketIndices(value)[0];
    		
    		//Input through spatial pooler
    		spatialPooler.compute(memory, encoding, output, true, true);
//    		System.out.println("SpatialPooler Output = " + Arrays.toString(output));
    		
    		//Input through temporal memory
    		int[] input = actual = ArrayUtils.where(output, ArrayUtils.WHERE_1);
    		ComputeCycle cc = temporalMemory.compute(memory, input, true);
    		lastPredicted = predictedColumns;
    		predictedColumns = getSDR(cc.predictiveCells()); //Get the active column indexes
//    		System.out.println("TemporalMemory Input = " + Arrays.toString(input));
//    		System.out.print("TemporalMemory Prediction = " + Arrays.toString(predictedColumns));
    		
    		classification.put("bucketIdx", bucketIdx);
    		classification.put("actValue", value);
//    		ClassifierResult<Double> result = classifier.compute(recordNum, classification, predictedColumns, true, true);
    		
//    		System.out.println("  |  CLAClassifier 1 step prob = " + Arrays.toString(result.getStats(1)) + "\n");
    		
//    		System.out.println("");
    	}
    	
    	public int[] inflateSDR(int[] SDR, int len) {
    		int[] retVal = new int[len];
    		for(int i : SDR) {
    			retVal[i] = 1;
    		}
    		return retVal;
    	}
    	
    	public int[] getSDR(Set<Cell> cells) {
    		int[] retVal = new int[cells.size()];
    		int i = 0;
    		for(Iterator<Cell> it = cells.iterator();i < retVal.length;i++) {
    			retVal[i] = it.next().getIndex();
    			retVal[i] /= cellsPerColumn; // Get the column index
    		}
    		Arrays.sort(retVal);
    		retVal = ArrayUtils.unique(retVal);
    		
    		return retVal;
    	}
    	
    	/**
         * Returns the next predicted value.
         * 
         * @return the SDR representing the prediction
         */
    	@Override
        public int[] getPredicted() {
        	return lastPredicted;
        }
        
        /**
         * Returns the actual columns in time t + 1 to compare
         * with {@link #getPrediction()} which returns the prediction
         * at time t for time t + 1.
         * @return
         */
    	@Override
        public int[] getActual() {
            return actual;
        }
        
        /**
         * Simple getter for external reset
         * @return
         */
        public Connections getMemory() {
        	return memory;
        }
    }
    
    
}
