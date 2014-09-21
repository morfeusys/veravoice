package com.example.recognizer;

public class Grammar {

    private final String mJsgf;
    private final Dict mDict;
    private final PhonMapper mPhonMapper;

    public Grammar(String[] commands, PhonMapper phonMapper) {
        mDict = new Dict();
        mPhonMapper = phonMapper;

        StringBuilder sb = new StringBuilder();
        sb.append("#JSGF V1.0;\ngrammar commands;\n");
        sb.append("public <command> = <commands>+;\n");
        sb.append("<commands> = ");

        for (int i = 0; i < commands.length; i++) {
            String command = commands[i];
            addWords(command);
            if (i > 0) sb.append(" | ");
            sb.append("[").append(command).append("]");
        }

        sb.append(";\n");
        mJsgf = sb.toString();
    }

    public String getJsgf() {
        return mJsgf;
    }

    public String getDict() {
        return mDict.toString();
    }

    public void addWords(String text) {
        String[] words = text.split(" ");
        for (String word : words) {
            mDict.add(word, mPhonMapper.getPronoun(word));
        }
    }
}