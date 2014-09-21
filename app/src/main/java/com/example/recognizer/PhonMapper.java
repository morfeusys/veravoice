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

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class PhonMapper {

    private static final String TAG = "PhonMapper";

    private static final String[] GARBAGE = {"а","е","ё","и","о","у","э","ю","я"};

    private static final Map<Pattern, String> PATTERNS = new LinkedHashMap<Pattern, String>() {{
        put(Pattern.compile("б([аоуэырлнвзмдг])"), "P$1");
        put(Pattern.compile("г([аоуэырлнвзмдб])"), "Q$1");
        put(Pattern.compile("в([аоуэырлнгзмдб])"), "W$1");

        put(Pattern.compile("б([еиюёяь])"), "B$1");
        put(Pattern.compile("г([еиюёяь])"), "G$1");
        put(Pattern.compile("к([еиюёяь])"), "K$1");
        put(Pattern.compile("л([еиюёяь])"), "L$1");
        put(Pattern.compile("в([еиюёяь])"), "V$1");
        put(Pattern.compile("м([еиюёяь])"), "M$1");
        put(Pattern.compile("н([еиюёяь])"), "N$1");
        put(Pattern.compile("р([еиюёяь])"), "R$1");
        put(Pattern.compile("х([еиюёяь])"), "H$1");
        put(Pattern.compile("т([еиюёяь])"), "T$1");
        put(Pattern.compile("д([еиюёяь])"), "D$1");
        put(Pattern.compile("ф([еиюёяь])"), "F$1");
        put(Pattern.compile("с([еиюёяь])"), "S$1");

        put(Pattern.compile("ей"), "J");
        put(Pattern.compile("^е"), "E");
        put(Pattern.compile("^я"), "Y");
        put(Pattern.compile("^ю"), "'");
        put(Pattern.compile("([аоуеэы])ю"), "$1'");
        put(Pattern.compile("(у|ю)(к)$"), "U$2");
        put(Pattern.compile("ой$"), "I");
    }};

    private static final Map<String, String> phons = new LinkedHashMap<String, String>() {{
        put("E", "j e");
        put("Y", "j ae");
        put("U", "uu");
        put("W", "v");
        put("J", "ee j");
        put("I", "oo j");
        put("'", "j u");

        put("P", "b");
        put("Q", "g");
        put("B", "bb");
        put("R", "rr");
        put("G", "gg");
        put("K", "kk");
        put("L", "ll");
        put("V", "vv");
        put("M", "mm");
        put("H", "hh");
        put("N", "nn");
        put("T", "tt");
        put("D", "dd");
        put("F", "ff");
        put("S", "ss");

        put("а", "a");
        put("б", "p");
        put("в", "f");
        put("г", "k");
        put("д", "d");
        put("е", "e");
        put("ё", "j oo");
        put("ж", "zh");
        put("з", "z");
        put("и", "i");
        put("й", "j");
        put("к", "k");
        put("л", "l");
        put("м", "m");
        put("н", "n");
        put("о", "ay");
        put("п", "p");
        put("р", "r");
        put("с", "s");
        put("т", "t");
        put("у", "u");
        put("ф", "f");
        put("х", "h");
        put("ц", "c");
        put("ч", "ch");
        put("ш", "sh");
        put("щ", "sch");
        put("ы", "y");
        put("э", "ay");
        put("ю", "u");
        put("я", "a");
    }};

    private Map<String, ArrayList<String>> mPhons = new HashMap<String, ArrayList<String>>();

    public PhonMapper() {
    }

    public PhonMapper(InputStream is) {
        try {
            BufferedReader bis = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = bis.readLine()) != null) {
                line = line.trim();
                if(line.length() > 0) {
                    String[] data = line.split("  ");
                    ArrayList<String> list = new ArrayList<String>(Arrays.asList(data[1].split(" ")));
                    mPhons.put(data[0], list);
                }
            }
            bis.close();
        } catch (IOException e) {
            Log.e(TAG, "Can't read phonemes", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String[] getGarbage() {
        return GARBAGE;
    }

    public String getPronoun(String str) {
        ArrayList<String> phons = getPhons(str);
        return TextUtils.join(" ", phons);
    }

    public ArrayList<String> getPhons(String str) {
        ArrayList<String> phons = new ArrayList<String>();

        str = str.toLowerCase();

        if(mPhons.containsKey(str)) {
            return mPhons.get(str);
        }

        for (Entry<Pattern, String> entry : PATTERNS.entrySet()) {
            str = entry.getKey().matcher(str).replaceAll(entry.getValue());
        }

        for (String ch : str.split("")) {
            String phon = getPhon(ch);
            if (phon != null) {
                phons.add(phon);
            }
        }

        return phons;
    }


    private String getPhon(String str) {
        return phons.get(str);
    }

}