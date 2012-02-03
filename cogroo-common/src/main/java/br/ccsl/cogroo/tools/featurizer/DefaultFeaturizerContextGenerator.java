/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreemnets.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package br.ccsl.cogroo.tools.featurizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A context generator for the Featurizer.
 */
public class DefaultFeaturizerContextGenerator implements FeaturizerContextGenerator {

  protected final String SE = "*SE*";
  protected final String SB = "*SB*";
  private static final int PREFIX_LENGTH = 4;
  private static final int SUFFIX_LENGTH = 4;

  private static Pattern hasCap = Pattern.compile("[A-Z]");
  private static Pattern hasNum = Pattern.compile("[0-9]");


  protected static String[] getPrefixes(String lex) {
    String[] prefs = new String[PREFIX_LENGTH];
    for (int li = 0, ll = PREFIX_LENGTH; li < ll; li++) {
      prefs[li] = lex.substring(0, Math.min(li + 1, lex.length()));
    }
    return prefs;
  }

  protected static String[] getSuffixes(String lex) {
    String[] suffs = new String[SUFFIX_LENGTH];
    for (int li = 0, ll = SUFFIX_LENGTH; li < ll; li++) {
      suffs[li] = lex.substring(Math.max(lex.length() - li - 1, 0));
    }
    return suffs;
  }

  public String[] getContext(int index, WordTag[] sequence, String[] priorDecisions, Object[] additionalContext) {
    String[] w = new String[sequence.length];
    String[] t = new String[sequence.length];
    WordTag.extract(sequence, w, t);
    return getContext(index,w,t,priorDecisions);
  }

  /**
   * Returns the context for making a pos tag decision at the specified token index given the specified tokens and previous tags.
   * @param i The index of the token for which the context is provided.
   * @param toks The tokens in the sentence.
   * @param tags pos-tags
   * @param preds The tags assigned to the previous words in the sentence.
   * @return The context for making a pos tag decision at the specified token index given the specified tokens and previous tags.
   */
  public String[] getContext(int i, String[] toks, String[] tags, String[] preds) {

    // Words in a 5-word window
    String w_2, w_1, w0, w1, w2;
    
    // Tags in a 5-word window
    String t_2, t_1, t0, t1, t2;
    
    // Previous predictions
    String p_2, p_1;
    
    w_2 = w_1 = w0 = w1 = w2 = null; 
    t_2 = t_1 = t0 = t1 = t2 = null;
    p_1 = p_2 = null;
    
    String lex = toks[i];
    
    if (i < 2) {
      w_2 = "w_2=bos";
      t_2 = "t_2=bos";
      p_2 = "p_2=bos";
    }
    else {
      w_2 = "w_2=" + toks[i - 2];
      t_2 = "t_2=" + tags[i - 2];
      p_2 = "p_2" + preds[i - 2];
    }

    if (i < 1) {
      w_1 = "w_1=bos";
      t_1 = "t_1=bos";
      p_1 = "p_1=bos";
    }
    else {
      w_1 = "w_1=" + toks[i - 1];
      t_1 = "t_1=" + tags[i - 1];
      p_1 = "p_1=" + preds[i - 1];
    }

    w0 = "w0=" + toks[i];
    t0 = "t0=" + tags[i];

    if (i + 1 >= toks.length) {
      w1 = "w1=eos";
      t1 = "t1=eos";
    }
    else {
      w1 = "w1=" + toks[i + 1];
      t1 = "t1=" + tags[i + 1];
    }

    if (i + 2 >= toks.length) {
      w2 = "w2=eos";
      t2 = "t2=eos";
    }
    else {
      w2 = "w2=" + toks[i + 2];
      t2 = "t2=" + tags[i + 2];
    }

    String[] features = new String[] {
        //add word features
        w_2,
        w_1,
        w0,
        w1,
        w2,
        w_1 + w0,
        w0 + w1,

        //add tag features
        t_2,
        t_1,
        t0,
        t1,
        t2,
        t_2 + t_1,
        t_1 + t0,
        t0 + t1,
        t1 + t2,
        t_2 + t_1 + t0,
        t_1 + t0 + t1,
        t0 + t1 + t2,

        //add pred tags
        p_2,
        p_1,
        p_2 + p_1,

        //add pred and tag
        p_1 + t_2,
        p_1 + t_1,
        p_1 + t0,
        p_1 + t1,
        p_1 + t2,
        p_1 + t_2 + t_1,
        p_1 + t_1 + t0,
        p_1 + t0 + t1,
        p_1 + t1 + t2,
        p_1 + t_2 + t_1 + t0,
        p_1 + t_1 + t0 + t1,
        p_1 + t0 + t1 + t2,

        //add pred and word
        p_1 + w_2,
        p_1 + w_1,
        p_1 + w0,
        p_1 + w1,
        p_1 + w2,
        p_1 + w_1 + w0,
        p_1 + w0 + w1
    };


    List<String> e = new ArrayList<String>();
    
      // do some basic suffix analysis
      String[] suffs = getSuffixes(lex);
      for (int j = 0; j < suffs.length; j++) {
        e.add("suf=" + suffs[j]);
      }

      String[] prefs = getPrefixes(lex);
      for (int j = 0; j < prefs.length; j++) {
        e.add("pre=" + prefs[j]);
      }
      // see if the word has any special characters
      if (lex.indexOf('-') != -1) {
        e.add("h");
      }
      
      if("prop".equals(tags[i]) && lex.contains("_")) {
        String fn = lex.substring(0, lex.indexOf("_"));
        String[] nprefs = getSuffixes(fn);
        for (int j = 0; j < prefs.length; j++) {
          e.add("nsuf=" + nprefs[j]);
        }
      }

      if (hasCap.matcher(lex).find()) {
        e.add("c");
      }

      if (hasNum.matcher(lex).find()) {
        e.add("d");
      }
      // end suffix
      
    String[] suffixContexts = e.toArray(new String[e.size()]);
    
    String[] context = new String[suffixContexts.length + features.length];
    System.arraycopy(features, 0, context, 0, features.length);
    System.arraycopy(suffixContexts, 0, context, features.length, suffixContexts.length);
    
    return context;
  }

}