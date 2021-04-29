package fr.polytech.larynxapp.controller.analysis;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import fr.polytech.larynxapp.model.audio.AudioData;


/**
 * The class calculating fundamentalFreq (fundamental frequency), Jitter and Shimmer of the voice
 */
public class FeaturesCalculator {

	/**
	 * The ending of the area where the next period is to be searched.
	 */
	private int nextPeriodSearchingAreaEnd = (int) hzToPeriod( 40 );

	/**
	 * The list of the position of the maximum of the periods.
	 */
	private static List<Integer> pitchPositions;

	/**
	 * The list of the periods' length.
	 */
	private List<Integer> periods ;


	/**
	 * The data to analyse.
	 */
	private List<Short> data;

	/**
	 * The fundamental frequency of the data.
	 */
	private float fundamentalFreq = 0f;

	/**
	 * The list of pitches (voice frequencies)
	 */
	private List<Float> pitches;

	/**
	 * the context of the application
	 */
	private Context context;

	/**
	 * Two constants used to calculate pitch
	 */
	final private int BASE_FRAGMENT = 200;
	final private int OFFSET = 100;
	private List<Float> T = new ArrayList<>();

	/**
	 * FeaturesCalculator sole builder.
	 *
	 * @param audioData the audio data containing the data to analyse.
	 */
	public FeaturesCalculator(AudioData audioData, List<Float> pitches) {
		this.data = audioData.getData_processed();
		this.pitches = pitches;
		periods = new ArrayList<>();
		pitchPositions = new ArrayList<>();
		fundamentalFreq = 0f;
		initPeriodsSearch();
		searchPitchPositions();
	}

	/**
	 * FeaturesCalculator's constructor
	 * @param audioData the data that wants to be processed
	 */
	public FeaturesCalculator(AudioData audioData){
		this.data = audioData.getData_processed();
		pitches = new ArrayList<Float>();
		periods = new ArrayList<>();
		pitchPositions = new ArrayList<>();
	}

	/**
	 * context's setter
	 * @param context the application's context
	 */
	public void setContext(Context context) {
		this.context = context;
	}

	/**
	 * The method initializing the research of the periods.
	 *
	 * Finds the beginning and the ending of the area where the first period is to be searched.
	 * Filters the data into the dataFiltered list.
	 */
	public void initPeriodsSearch() {

		//init fundamentalFreq
		calculatefundamentalFreq();

		//set the first search area
		final double confidenceLevel = 5 / 100.;
		nextPeriodSearchingAreaEnd = (int) Math.floor( hzToPeriod( fundamentalFreq ) * ( 1 + confidenceLevel ) );
	}

	/**
	 * The method calculating the periods in the data and searching the pitches positions.
	 *
	 * Fills the pitchPositions and the periods lists.
	 */
	public void searchPitchPositions() {

		for(float pitch : pitches)
			periods.add((int) hzToPeriod(pitch));

		int periodMaxPitch;
		int periodMaxPitchIndex = 0;
		int periodBeginning     = 0;

		//search each periods maxima
		for ( int period = 0; period < periods.size() - 1; period++ ) {
			periodMaxPitch = 0;

			//search a maximum
			for ( int i = periodBeginning; i < periodBeginning + periods.get( period ); i++ ) {
				if(i < data.size()){
					if ( periodMaxPitch < data.get( i ) ) {
						periodMaxPitch = data.get( i );
						periodMaxPitchIndex = i;
					}
				}

			}
			periodBeginning += periods.get( period );
			pitchPositions.add( periodMaxPitchIndex );
		}

	}

	/**
	 * Returns the period length of the given value in Hz considering the sample rate (44.1kHz).
	 *
	 * @param hz the value in Hz
	 * @return the equivalent period length
	 */
	private float hzToPeriod(float hz ) {
		int sampling = 44100;
		return sampling / hz;
	}

	// FEATURE NUMBER 1 : SHIMMER

	/**
	 * The method calculating the Shimmer
	 *
	 * @return the Shimmer
	 */
	public double getShimmer() {
		int           minAmp     = 0; // figures the minium of the amplitude.
		int           maxAmp; // figures the maxium of the amplitude.
		double  amplitude = 0;
		List<Double> amplitudes = new ArrayList<Double>();
		double sum = 0;
		for ( int i = 0; i < pitchPositions.size() - 1; i++ ) {
			// get each pitch
			maxAmp = data.get( pitchPositions.get( i ) );
			for ( int j = pitchPositions.get( i ); j < pitchPositions.get( i + 1 ); j++ ) {
				if ( minAmp > data.get( j ) ) {
					minAmp = data.get( j );
				}
			}
			amplitude = maxAmp - minAmp;
			amplitudes.add(amplitude);
			minAmp = 9999;
		}
///Math.log(10)
		for(int j = 0; j < amplitudes.size() - 1; j++){
			double element = Math.abs(20*(Math.log(amplitudes.get(j+1)/amplitudes.get(j))));
			sum = sum + element;
		}
		double result1 = sum/(amplitudes.size() - 1);
		return result1;

	}

	// FEATURE NUMBER 2 : JITTER

	/**
	 * The method calculating the Jitter (corresponds to ddp in Praat)
	 *
	 * @return the Jitter
	 */
	public double getJitter() {
		double sumOfDifferenceOfPeriods = 0.0;        // sum of difference between every two periods
		double sumOfPeriods             = 0.0;        // sum of all periods
		double numberOfPeriods          = periods.size();   //set as double for double division

		// JITTER FORMULA (RELATIVE)
		for ( int i = 0; i < periods.size() - 1; i++ ) {
			sumOfDifferenceOfPeriods += Math.abs( periods.get( i ) - periods.get( i + 1 ) );
			sumOfPeriods += periods.get( i );
		}

		// add the last period into sum
		if ( !periods.isEmpty() ) {
			sumOfPeriods += periods.get( periods.size() - 1 );
		}

		double meanPeriod = sumOfPeriods / numberOfPeriods;

		// calculate jitter (relative)
		return ( sumOfDifferenceOfPeriods / ( numberOfPeriods - 1 ) ) / meanPeriod;
	}

	// FEATURE NUMBER 3 : FUNDAMENTAL FREQUENCY

	/**
	 * Getter for the fundamental frequency
	 *
	 * @return the fundamental frequency
	 */
	public double getfundamentalFreq() {
		if ( fundamentalFreq == 0f )
			calculatefundamentalFreq();

		return fundamentalFreq;
	}

	/**
	 * The method finding the fundamental frequency of the data.
	 *
	 * To increase efficiency, this method only test the frequencies between 40Hz to 400Hz.
	 */
	private void calculatefundamentalFreq() {
		int count;
		float f0 = 0;
		System.out.println("pitches");
		System.out.println(pitches);
		for(count = 0; count < pitches.size(); count++)
		{
			f0 += pitches.get(count);
		}
		if(count != 0)
			fundamentalFreq =  f0 / count;
		else
			fundamentalFreq = 0;
	}

	private Float PeriodToPitch(float period){
		int sampling = 14700;
		return  sampling / period;
	}

	/**
	 * The method calculating the pitch periods
	 */
	public List<Float> calculatePitches(){
		List<Integer> res = new ArrayList<Integer>();
		int size = data.size();
		int maxAmp = 0;
		int startPos = 0;
		// get the first pitch in the basic period
		for (int i = 0; i < BASE_FRAGMENT; i ++){
			if (maxAmp < data.get(i)){
				maxAmp = data.get(i);
				// set this position as the start position
				startPos = i;
			}
		}
		Log.v("startPos", String.valueOf(startPos));
		// find every pitch in all the fragments
		int pos = startPos + OFFSET; // set current position
		int posAmpMax;
		while(startPos < 1000){
			if(data.get(pos) > 0) { // only read the positive data

				posAmpMax = 0;
				maxAmp = 0;
				// access to all the data in this fragment
				while (pos < startPos + BASE_FRAGMENT) {
					// find the pitch and mark this position
					if (maxAmp < data.get(pos)) {
						maxAmp = data.get(pos);
						posAmpMax = pos;
					}
					pos++;
				}
				// add pitch position into the list
				pitchPositions.add(posAmpMax);
				res.add(posAmpMax);
				// update the start position and the current position
				startPos = posAmpMax;
				pos =  startPos + OFFSET;
			}else{
				pos ++;
			}
		}

		// calculate all periods and add them into list
		for(int i = 0; i < pitchPositions.size() - 1; i++){
			float period = (float)(pitchPositions.get(i+1) - pitchPositions.get(i));
			T.add(period);
			pitches.add(PeriodToPitch(period));
		}
		pitchPositions.clear();
		return pitches;
	}

}
