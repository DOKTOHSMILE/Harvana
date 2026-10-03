package com.harvana.app;


import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Helper class for Country → State → City cascading Spinners
* + automatic phone code + flag emoji
*
* JSON expected structure (from countries_states_cities_clean.json):
* [
*   {
*     "name": "Nigeria",
*     "phonecode": "234",
*     "emoji": "🇳🇬",
*     "states": [
*       {
*         "name": "Lagos",
*         "cities": [ {"name": "Ikeja"}, {"name": "Lekki"} ]
*       }
*     ]
*   }
* ]
*/
public class LocationHelper {
	
	private final Context context;
	private final List<Country> countries = new ArrayList<>();
	private final Map<String, Country> countryMap = new HashMap<>();
	
	public LocationHelper(Context context) {
		this.context = context.getApplicationContext();
		loadData();
	}
	
	// ==================== LOAD JSON ====================
	private void loadData() {
		try {
			InputStream is = context.getAssets().open("countries_states_cities.json");
			byte[] buffer = new byte[is.available()];
			is.read(buffer);
			is.close();
			
			String json = new String(buffer, StandardCharsets.UTF_8);
			JSONArray arr = new JSONArray(json);
			
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				Country country = new Country();
				country.name = obj.optString("name", "");
				country.phonecode = obj.optString("phonecode", "");
				country.emoji = obj.optString("emoji", "");
				
				JSONArray statesArr = obj.optJSONArray("states");
				if (statesArr != null) {
					for (int j = 0; j < statesArr.length(); j++) {
						JSONObject sObj = statesArr.getJSONObject(j);
						State state = new State();
						state.name = sObj.optString("name", "");
						
						JSONArray citiesArr = sObj.optJSONArray("cities");
						if (citiesArr != null) {
							for (int k = 0; k < citiesArr.length(); k++) {
								JSONObject cObj = citiesArr.getJSONObject(k);
								state.cities.add(cObj.optString("name", ""));
							}
						}
						country.states.add(state);
					}
				}
				
				countries.add(country);
				countryMap.put(country.name, country);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// ==================== PUBLIC API ====================
	
	/** Returns list of all country names (for Spinner 1) */
	public List<String> getCountryNames() {
		List<String> names = new ArrayList<>();
		for (Country c : countries) {
			names.add(c.name);
		}
		return names;
	}
	
	/**
* Returns phone code + flag emoji string
* Example: "🇳🇬 +234"
*/	
	public String getPhoneCodeAndFlag(String countryName) {
		Country c = countryMap.get(countryName);
		if (c == null) return "";
		return c.emoji + " +" + c.phonecode;
	}
	
	public String getFlag(String countryName) {
		Country c = countryMap.get(countryName);
		if (c == null) return "";
		return c.emoji;
	}
	
	public String getPhoneCode(String countryName) {
		Country c = countryMap.get(countryName);
		if (c == null) return "";
		return "+" + c.phonecode;
	}
	
	/** Returns list of state names for a country (for Spinner 2) */
	public List<String> getStateNames(String countryName) {
		List<String> names = new ArrayList<>();
		Country c = countryMap.get(countryName);
		if (c != null) {
			for (State s : c.states) {
				names.add(s.name);
			}
		}
		return names;
	}
	
	/** Returns list of city names for a country + state (for Spinner 3) */
	public List<String> getCityNames(String countryName, String stateName) {
		List<String> names = new ArrayList<>();
		Country c = countryMap.get(countryName);
		if (c != null) {
			for (State s : c.states) {
				if (s.name.equals(stateName)) {
					names.addAll(s.cities);
					break;
				}
			}
		}
		return names;
	}
	
	// ==================== SPINNER HELPERS ====================
	
	/** Fill any Spinner with a list of strings */
	public void setSpinnerData(Spinner spinner, List<String> items) {
		ArrayAdapter<String> adapter = new ArrayAdapter<>(
		context,
		android.R.layout.simple_spinner_item,
		items
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}
	
	// ==================== DATA MODELS ====================
	public static class Country {
		public String name;
		public String phonecode;
		public String emoji;
		public List<State> states = new ArrayList<>();
	}
	
	public static class State {
		public String name;
		public List<String> cities = new ArrayList<>();
	}
}
