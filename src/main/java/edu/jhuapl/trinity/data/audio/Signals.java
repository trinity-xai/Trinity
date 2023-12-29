package edu.jhuapl.trinity.data.audio;

/*-
 * #%L
 * trinity-2023.10.03
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Some signal metric functions like energy, power etc.
 * @author mzechner
 *
 */
public class Signals 
{
	public static float mean( float[] signal )
	{
		float mean = 0;
		for( int i = 0; i < signal.length; i++ )
			mean+=signal[i];
		mean /= signal.length;
		return mean;
	}
	
	public static float energy( float[] signal )
	{
		float totalEnergy = 0;
		for( int i = 0; i < signal.length; i++ )		
			totalEnergy += (signal[i] * signal[i]);
		return totalEnergy;
	}

	public static float power(float[] signal ) 
	{	
		return energy( signal ) / signal.length;
	}
	
	public static float norm( float[] signal )
	{
		return (float)Math.sqrt( energy(signal) );
	}
	
	public static float minimum( float[] signal )
	{
		float min = Float.POSITIVE_INFINITY;
		for( int i = 0; i < signal.length; i++ )
			min = Math.min( min, signal[i] );
		return min;
	}
	
	public static float maximum( float[] signal )
	{
		float max = Float.NEGATIVE_INFINITY;
		for( int i = 0; i < signal.length; i++ )
			max = Math.max( max, signal[i] );
		return max;
	}
	
	public static void scale( float[] signal, float scale )
	{
		for( int i = 0; i < signal.length; i++ )
			signal[i] *= scale;
	}
}
