/**
 * Copyright (C) 2012 cogroo <cogroo@cogroo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cogroo.formats.ad;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.chunker.ChunkSample;
import opennlp.tools.formats.ad.ADChunkSampleStream;
import opennlp.tools.formats.ad.ADSentenceStream;
import opennlp.tools.formats.ad.ADSentenceStream.Sentence;
import opennlp.tools.formats.ad.ADSentenceStream.SentenceParser.Leaf;
import opennlp.tools.formats.ad.ADSentenceStream.SentenceParser.Node;
import opennlp.tools.formats.ad.ADSentenceStream.SentenceParser.TreeElement;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

/**
 * Parser for Floresta Sita(c)tica Arvores Deitadas corpus, output to for the
 * Portuguese Chunker training.
 * <p>
 * The heuristic to extract chunks where based o paper 'A Machine Learning
 * Approach to Portuguese Clause Identification', (Eraldo Fernandes, Cicero
 * Santos and Ruy Milidiú).<br>
 * <p>
 * Data can be found on this web site:<br>
 * http://www.linguateca.pt/floresta/corpus.html
 * <p>
 * Information about the format:<br>
 * Susana Afonso.
 * "Árvores deitadas: Descrição do formato e das opções de análise na Floresta Sintáctica"
 * .<br>
 * 12 de Fevereiro de 2006.
 * http://www.linguateca.pt/documentos/Afonso2006ArvoresDeitadas.pdf
 * <p>
 * Detailed info about the NER tagset:
 * http://beta.visl.sdu.dk/visl/pt/info/portsymbol.html#semtags_names
 * <p>
 * <b>Note:</b> Do not use this class, internal use only!
 */
public class ADChunkBasedHeadFinderSampleStream implements
    ObjectStream<ChunkSample> {

  private final ObjectStream<ADSentenceStream.Sentence> adSentenceStream;

  private int start = -1;
  private int end = -1;

  private int index = 0;

  private boolean useCGTags;

  private boolean expandME;

  /**
   * Creates a new {@link NameSample} stream from a line stream, i.e.
   * {@link ObjectStream}< {@link String}>, that could be a
   * {@link PlainTextByLineStream} object.
   * 
   * @param lineStream
   *          a stream of lines as {@link String}
   */
  public ADChunkBasedHeadFinderSampleStream(ObjectStream<String> lineStream,
      boolean useCGTag, boolean expandME) {
    this.useCGTags = useCGTag;
    this.expandME = expandME;
    this.adSentenceStream = new ADSentenceStream(lineStream);
  }

  /**
   * Creates a new {@link NameSample} stream from a {@link InputStream}
   * 
   * @param in
   *          the Corpus {@link InputStream}
   * @param charsetName
   *          the charset of the Arvores Deitadas Corpus
   */
  public ADChunkBasedHeadFinderSampleStream(InputStream in, String charsetName,
      boolean useCGTag, boolean expandME) {
    this.useCGTags = useCGTag;
    this.expandME = expandME;
    try {
      this.adSentenceStream = new ADSentenceStream(new PlainTextByLineStream(
          in, charsetName));
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is available on all JVMs, will never happen
      throw new IllegalStateException(e);
    }
  }

  public ChunkSample read() throws IOException {

    Sentence paragraph;
    while ((paragraph = this.adSentenceStream.read()) != null) {

      if (end > -1 && index >= end) {
        // leave
        return null;
      }

      if (start > -1 && index < start) {
        index++;
        // skip this one
      } else {
        Node root = paragraph.getRoot();
        List<String> sentence = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        List<String> target = new ArrayList<String>();

        processRoot(root, sentence, tags, target);

        if (sentence.size() > 0) {
          index++;
          return new ChunkSample(sentence, tags, target);
        }

      }

    }
    return null;
  }

  private void processRoot(Node root, List<String> sentence, List<String> tags,
      List<String> target) {
    if (root != null) {
      TreeElement[] elements = root.getElements();
      for (int i = 0; i < elements.length; i++) {
        if (elements[i].isLeaf()) {
          processLeaf((Leaf) elements[i], false, "O", sentence, tags, target,
              false);
        } else {
          processNode((Node) elements[i], sentence, tags, target, null);
        }
      }
    }
  }

  // private void processNode(Node node, List<String> sentence, List<String>
  // tags,
  // List<String> target) {
  // String phraseTag = getChunkTag(node.getSyntacticTag());
  //
  // TreeElement[] elements = node.getElements();
  // for (int i = 0; i < elements.length; i++) {
  // if (elements[i].isLeaf()) {
  // boolean isIntermediate = false;
  // if ( i > 0 && elements[i - 1].isLeaf() && phraseTag != null &&
  // !phraseTag.equals("O")) {
  // isIntermediate = true;
  // }
  // processLeaf((Leaf) elements[i], isIntermediate, phraseTag, sentence,
  // tags, target);
  // } else {
  // processNode((Node) elements[i], sentence, tags, target);
  // }
  // }
  // }

  private void processNode(Node node, List<String> sentence, List<String> tags,
      List<String> target, String inheritedTag) {
    String phraseTag = getChunkTag(node.getSyntacticTag());

    boolean inherited = false;
    if (phraseTag.equals("O") && inheritedTag != null) {
      phraseTag = inheritedTag;
      inherited = true;
    }

    TreeElement[] elements = node.getElements();
    for (int i = 0; i < elements.length; i++) {
      if (elements[i].isLeaf()) {
        boolean isIntermediate = false;
        if (i > 0 && elements[i - 1].isLeaf() && phraseTag != null
            && !phraseTag.equals("O")) {
          isIntermediate = true;
        }
        if (inherited && target.size() > 0
            && target.get(target.size() - 1).endsWith(phraseTag)) {
          isIntermediate = true;
        }
        processLeaf((Leaf) elements[i], isIntermediate, phraseTag, sentence,
            tags, target, inherited);
      } else {
        processNode((Node) elements[i], sentence, tags, target, phraseTag);
      }
    }
  }

  private void processLeaf(Leaf leaf, boolean isIntermediate, String phraseTag,
      List<String> sentence, List<String> tags, List<String> target,
      boolean isInherited) {
    String phrase;
    String pos;

    if (leaf.getFunctionalTag() != null && phraseTag.equals("O")) {
      if (leaf.getFunctionalTag().equals("v-fin")) {
        phraseTag = "VP";
      } else if (leaf.getFunctionalTag().equals("n")) {
        phraseTag = "NP";
      }
    }

    phraseTag = ADChunkSampleStream.convertPhraseTag(phraseTag);

    if (!phraseTag.equals("O")) {
      if (isIntermediate) {
        phrase = "I-" + phraseTag;
      } else {
        phrase = "B-" + phraseTag;
      }
    } else {
      phrase = phraseTag;
    }

    sentence.add(leaf.getLexeme());
    if (leaf.getSyntacticTag() == null) {
      pos = leaf.getLexeme();
    } else {
      pos = ADChunkSampleStream.convertFuncTag(leaf.getFunctionalTag(),
          this.useCGTags);
    }
    tags.add(pos + "|" + phrase);
    if (!isInherited && "H".equals(leaf.getSyntacticTag())
        && !"O".equals(phraseTag)) {
      target.add("B-H");
    } else {
      target.add("O");
    }

  }

  private String getChunkTag(String tag) {

    String phraseTag = tag.substring(tag.lastIndexOf(":") + 1);

    // maybe we should use only np, vp and pp, but will keep ap and advp.
    if (phraseTag.equals("np") || phraseTag.equals("vp")
        || phraseTag.equals("pp") || phraseTag.equals("ap")
        || phraseTag.equals("advp")) {
      phraseTag = phraseTag.toUpperCase();
    } else {
      phraseTag = "O";
    }
    return phraseTag;
  }

  public void setStart(int aStart) {
    this.start = aStart;
  }

  public void setEnd(int aEnd) {
    this.end = aEnd;
  }

  public void reset() throws IOException, UnsupportedOperationException {
    adSentenceStream.reset();
  }

  public void close() throws IOException {
    adSentenceStream.close();
  }

}
