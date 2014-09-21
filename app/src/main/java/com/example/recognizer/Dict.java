/*
 * Copyright 2012, Institute of Cybernetics at Tallinn University of Technology
 *
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
 */

package com.example.recognizer;

import java.util.HashSet;
import java.util.Set;

public class Dict {

	private final StringBuilder mDict = new StringBuilder();
	private final Set<String> mWords = new HashSet<String>();

	private static final String NL = System.getProperty("line.separator");

	public void add(String key, String value) {
		if (! mWords.contains(key)) {
			mDict.append(key);
			mDict.append("  "); // two spaces
			mDict.append(value);
			mDict.append(NL);
			mWords.add(key);
		}
	}

	public String toString() {
		return mDict.toString();
	}

}